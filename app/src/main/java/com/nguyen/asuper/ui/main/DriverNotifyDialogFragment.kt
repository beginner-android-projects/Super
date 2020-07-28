package com.nguyen.asuper.ui.main

import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.nguyen.asuper.R
import com.nguyen.asuper.data.Driver
import com.nguyen.asuper.databinding.DialogNotifyDriverComeBinding
import com.nguyen.asuper.viewmodels.MainViewModel
import kotlinx.android.synthetic.main.dialog_payment.view.*

class DriverNotifyDialogFragment(private val mainViewModel: MainViewModel, private val onPositiveButtonClick: () -> Unit): DialogFragment() {


    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            val builder = AlertDialog.Builder(it)
            // Get the layout inflater
            val inflater = requireActivity().layoutInflater;

            // Inflate and set the layout for the dialog
            // Pass null as the parent view because its going in the dialog layout
            val binding = DialogNotifyDriverComeBinding.inflate(inflater, null, false)

            binding.driver = mainViewModel.trip.value?.driver

            builder.setView(binding.root)
                .setCancelable(false)
                .setPositiveButton("Ok", DialogInterface.OnClickListener { _, _ ->
                    dismiss()
                    onPositiveButtonClick.invoke()
                })
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }
}