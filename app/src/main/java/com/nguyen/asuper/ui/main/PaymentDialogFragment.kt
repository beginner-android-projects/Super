package com.nguyen.asuper.ui.main

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.nguyen.asuper.R
import com.nguyen.asuper.viewmodels.MainViewModel
import kotlinx.android.synthetic.main.dialog_payment.view.*

class PaymentDialogFragment(private val mainViewModel: MainViewModel): DialogFragment() {


    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            val builder = AlertDialog.Builder(it)
            // Get the layout inflater
            val inflater = requireActivity().layoutInflater;

            // Inflate and set the layout for the dialog
            // Pass null as the parent view because its going in the dialog layout
            val view = inflater.inflate(R.layout.dialog_payment, null)
            view.visa.setOnClickListener {
                mainViewModel.setPaymentMethod("Visa/Debit Card")
                dismiss()
            }

            view.google_pay.setOnClickListener {
                mainViewModel.setPaymentMethod("Google Pay")
                dismiss()
            }

            view.apple_pay.setOnClickListener {
                mainViewModel.setPaymentMethod("Apple Pay")
                dismiss()
            }

            builder.setView(view)
                .setCancelable(false)
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }
}