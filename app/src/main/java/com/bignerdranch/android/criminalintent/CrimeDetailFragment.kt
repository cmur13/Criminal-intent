package com.bignerdranch.android.criminalintent

import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.bignerdranch.android.criminalintent.databinding.FragmentCrimeDetailBinding
import kotlinx.coroutines.launch
import java.util.Date
import android.text.format.DateFormat
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.core.view.doOnLayout
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.UUID


private const val DATE_FORMAT = "EEE, MMM, dd"
class CrimeDetailFragment: Fragment() {
    private var _binding: FragmentCrimeDetailBinding? = null

    private val binding
        get() = checkNotNull(_binding){
            "Cannot access binding because it is null. Is the view visible?"
        }
    private val args: CrimeDetailFragmentArgs by navArgs()

    private val crimeDetailViewModel: CrimeDetailViewModel by viewModels{
        CrimeDetailViewModelFactory(args.crimeId)
    }

    // registering for a result
    private val selectSuspect = registerForActivityResult(
        ActivityResultContracts.PickContact()){ uri: Uri? ->
        // invoke parseContactSelection
        uri?.let { parseContactSelection(it)
        }
    }

    private val takePhoto = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ){
            didTakePhoto: Boolean ->
        if(didTakePhoto && photoName != null){
            crimeDetailViewModel.updateCrime {oldCrime->
                oldCrime.copy(photoFileName = photoName)
            }
        }
    }
    private var photoName: String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding= FragmentCrimeDetailBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.apply {
            crimeTitle.doOnTextChanged { text, _, _, _ ->
                crimeDetailViewModel.updateCrime { oldCrime ->
                    oldCrime.copy(title = text.toString())
                }

            }
            crimeSolved.setOnCheckedChangeListener { _, isChecked ->
                crimeDetailViewModel.updateCrime { oldCrime ->
                    oldCrime.copy(isSolved = isChecked)
                }
            }
            // sending an implicity intent
            crimeSuspect.setOnClickListener {
                selectSuspect.launch(null)
            }
            val selectSuspectIntent = selectSuspect.contract.createIntent(
                requireContext(),
                null
            )
            crimeSuspect.isEnabled = canResolveIntent(selectSuspectIntent)

            crimeCamera.setOnClickListener{
                photoName = "IMG_${Date()}.JPG"
                val photoFile = File(
                    requireContext().applicationContext.filesDir,photoName  // add for challenge: detail display
                )
                val photoUri = FileProvider.getUriForFile(
                    requireContext(),
                    "com.bignerdranch.android.criminalintent.fileprovider",
                    photoFile
                )
                takePhoto.launch(photoUri)
            }
            val captureImageIntent = takePhoto.contract.createIntent(
                requireContext(),
                null
            )
            crimeCamera.isEnabled = canResolveIntent(captureImageIntent)
        }
        viewLifecycleOwner.lifecycleScope.launch{
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED){
                crimeDetailViewModel.crime.collect{crime->
                    crime?.let{ updateUi(it) }
                }
            }
        }
        setFragmentResultListener(
            DatePickerFragment.REQUEST_KEY_DATE
        ) { _, bundle ->
            val newDate = bundle.getSerializable(DatePickerFragment.BUNDLE_KEY_DATE) as Date
            crimeDetailViewModel.updateCrime { crime ->
                val calendar = Calendar.getInstance().apply { time = crime.date }
                val newCalendar = Calendar.getInstance().apply { time = newDate }
                // Update only the date part
                calendar.set(Calendar.YEAR, newCalendar.get(Calendar.YEAR))
                calendar.set(Calendar.MONTH, newCalendar.get(Calendar.MONTH))
                calendar.set(Calendar.DAY_OF_MONTH, newCalendar.get(Calendar.DAY_OF_MONTH))
                crime.copy(date = calendar.time)
            }
        }

        setFragmentResultListener(
            TimePickerFragment.REQUEST_KEY_TIME
        ) { _, bundle ->
            val newTime = bundle.getSerializable(TimePickerFragment.BUNDLE_KEY_TIME) as Date
            crimeDetailViewModel.updateCrime { crime ->
                val calendar = Calendar.getInstance().apply { time = crime.date }
                val newCalendar = Calendar.getInstance().apply { time = newTime }
                // Update only the time part
                calendar.set(Calendar.HOUR_OF_DAY, newCalendar.get(Calendar.HOUR_OF_DAY))
                calendar.set(Calendar.MINUTE, newCalendar.get(Calendar.MINUTE))
                crime.copy(date = calendar.time)
            }
        }
    } // end of onViewCreated

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // updating the UI
    private fun updateUi(crime: Crime){
        binding.apply{
            if(crimeTitle.text.toString() != crime.title){
                crimeTitle.setText(crime.title)
            }
            crimeDate.text = formatDate(crime.date, "EEE MMM dd, yyyy")
            crimeDate.setOnClickListener {
                findNavController().navigate(
                    CrimeDetailFragmentDirections.selectDate(crime.date)
                )
            }
            crimeTime.text = formatDate(crime.date, "HH:mm zz")
            crimeTime.setOnClickListener{
                findNavController().navigate(
                    CrimeDetailFragmentDirections.selectTime(crime.date)
                )
            }
            crimeSolved.isChecked = crime.isSolved

            // set a listener on the new crime report button
            crimeReport.setOnClickListener{
                val reportIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, getCrimeReport(crime))
                    putExtra(
                        Intent.EXTRA_SUBJECT,
                        getString(R.string.crime_report_subject)
                    )
                }
                val chooserIntent = Intent.createChooser(
                    reportIntent,
                    getString(R.string.send_report)
                )
                startActivity(chooserIntent)
            }
            crimeSuspect.text = crime.suspect.ifEmpty{
                getString(R.string.crime_suspect_text)
            }
            updatePhoto(crime.photoFileName)

            // code for chapter 17 challenge:detail display
            crimePhoto.setOnClickListener{
                val zoom = crime.photoFileName?.let{
                    ZoomDialogFragment.newInstance(it)
                }
                zoom?.show(childFragmentManager, null)
            }
        }
    } //end updateUi()

    // adding a getCrimeReport to return a complete report
    private fun getCrimeReport(crime: Crime): String{
        val solvedString = if(crime.isSolved){
            getString(R.string.crime_report_solved)
        } else{
            getString(R.string.crime_report_unsolved)
        }
        val dateString = DateFormat.format(DATE_FORMAT, crime.date).toString()
        val suspectText = if(crime.suspect.isBlank()){
            getString(R.string.crime_report_no_suspect)
        } else{
            getString(R.string.crime_report_suspect, crime.suspect)
        }
        return getString(
            R.string.crime_report,
            crime.title, dateString, solvedString, suspectText
        )
    }

    // retrieve the contact's name from the contacts applicaation
    private fun parseContactSelection(contactUri: Uri){
        val queryFields = arrayOf(ContactsContract.Contacts.DISPLAY_NAME)
        val queryCursor = requireActivity().contentResolver. query(contactUri, queryFields,null, null, null)
        queryCursor?.use { cursor ->
            if (cursor.moveToFirst()){
                val suspect = cursor.getString(0)
                crimeDetailViewModel.updateCrime { oldCrime ->
                    oldCrime.copy(suspect = suspect)
                }
            }
        }
    } // end parseContactSelection

    //resovling intents
    private fun canResolveIntent(intent: Intent): Boolean{
        val packageManager: PackageManager = requireActivity().packageManager
        val resolvedActivity: ResolveInfo? = packageManager.resolveActivity(
            intent,
            PackageManager.MATCH_DEFAULT_ONLY
        )
        return resolvedActivity != null
    }

    // updating crimePhoto
    private fun updatePhoto(photoFileName: String?){
        if(binding.crimePhoto.tag != photoFileName) {
            val photoFile = photoFileName?.let {
                File(requireContext().applicationContext.filesDir, it)
            }
            if (photoFile?.exists() == true) {
                binding.crimePhoto.doOnLayout { measuredView ->
                    val scaledBitmap = getScaledBitmap(
                        photoFile.path,
                        measuredView.width,
                        measuredView.height
                    )
                    binding.crimePhoto.setImageBitmap(scaledBitmap)
                    binding.crimePhoto.tag = photoFileName
                }
            }
        } else{
            binding.crimePhoto.setImageBitmap(null)
            binding.crimePhoto.tag=null
        }
    }

    // challenge: deleting a crime
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.fragment_crime_detail, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.delete_crime -> {
                deleteCrime(args.crimeId)
                findNavController().popBackStack()
                true
            }

            else -> super.onContextItemSelected(item)
        }
    }
    // format date method
    private fun formatDate(date: Date, pattern: String): String{
        val formatter = SimpleDateFormat(pattern, Locale.getDefault())
        return formatter.format(date)
    }
    private fun deleteCrime(id: UUID) {
        viewLifecycleOwner.lifecycleScope.launch {
            val crime = crimeDetailViewModel.getCrime(id)
            crimeDetailViewModel.deleteCrime(crime)
        }
        Toast.makeText(requireContext(), "Crime deleted.", Toast.LENGTH_SHORT).show()
    }
} // end CrimeDetailFragment