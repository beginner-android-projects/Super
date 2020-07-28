package com.nguyen.asuper.ui.main

import android.app.Dialog
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.nguyen.asuper.databinding.DialogTripSummaryBinding
import com.nguyen.asuper.viewmodels.MainViewModel

class DriverTripSummaryDialogFragment(private val mainViewModel: MainViewModel): DialogFragment() {


    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            val builder = AlertDialog.Builder(it)
            // Get the layout inflater
            val inflater = requireActivity().layoutInflater;

            // Inflate and set the layout for the dialog
            // Pass null as the parent view because its going in the dialog layout
            val binding = DialogTripSummaryBinding.inflate(inflater, null, false)

            binding.trip = mainViewModel.trip.value
            binding.fare = mainViewModel.trip.value?.fare

            binding.submitRatingButton.setOnClickListener {
                val rating = binding.driverRatingInput.rating
                if(rating != 0f){
                    mainViewModel.submitRating(rating.toDouble())
                    dismiss()
                }
            }


            binding.tripPreview.setImageBitmap(mainViewModel.trip.value?.preview)

            builder.setView(binding.root)
                .setCancelable(true)
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }
}