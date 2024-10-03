package com.bignerdranch.android.criminalintent

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.net.toUri
import androidx.fragment.app.DialogFragment
import com.bignerdranch.android.criminalintent.databinding.DialogFragmentZoomBinding
import java.io.File

private const val PHOTO_FILE_NAME = "PHOTO_FILE_NAME"

class ZoomDialogFragment : DialogFragment() {

    private var _binding: DialogFragmentZoomBinding? = null
    private val binding
        get() = checkNotNull(_binding) {
            "Cannot access binding because it is null. Is the view visible?"
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = DialogFragmentZoomBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val photoFileName = arguments?.getString(PHOTO_FILE_NAME)

        val photoFile = photoFileName?.let {
            File(requireContext().applicationContext.filesDir, it)
        }
        binding.crimePhotoZoom.setImageURI(photoFile?.toUri())
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance(photoFileName: String): ZoomDialogFragment {
            val frag = ZoomDialogFragment()
            val args = Bundle()
            args.putSerializable(PHOTO_FILE_NAME, photoFileName)
            frag.arguments = args
            return frag
        }
    }
}