package com.bignerdranch.android.criminalintent

import android.app.Dialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.widget.TimePicker
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.setFragmentResult
import androidx.navigation.fragment.navArgs
import java.util.Calendar
import java.util.GregorianCalendar

class TimePickerFragment : DialogFragment() {

    private val args: TimePickerFragmentArgs by navArgs()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        val timeListener =
            TimePickerDialog.OnTimeSetListener { _: TimePicker, hour: Int, minute: Int ->

                val calendar = Calendar.getInstance()
                calendar.time = args.crimeTime
                val year = calendar.get(Calendar.YEAR)
                val month = calendar.get(Calendar.MONTH)
                val day = calendar.get(Calendar.DAY_OF_MONTH)

                val resultDate = GregorianCalendar(year, month, day, hour, minute).time

                setFragmentResult(REQUEST_KEY_TIME, bundleOf(BUNDLE_KEY_TIME to resultDate))
            }

        val calendar = Calendar.getInstance()
        calendar.time = args.crimeTime
        val initialHour = calendar.get(Calendar.HOUR_OF_DAY)
        val initialMinute = calendar.get(Calendar.MINUTE)
        val is24HR = true

        return TimePickerDialog(
            requireContext(), timeListener, initialHour, initialMinute, is24HR
        )
    }

    companion object {
        const val REQUEST_KEY_TIME = "REQUEST_KEY_TIME"
        const val BUNDLE_KEY_TIME = "BUNDLE_KEY_TIME"
    }
}