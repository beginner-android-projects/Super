package com.nguyen.asuper.ui.main.adapter

import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.nguyen.asuper.R
import com.nguyen.asuper.data.Driver
import com.nguyen.asuper.databinding.DialogEditUserNameBinding
import com.nguyen.asuper.databinding.DialogNotifyDriverComeBinding
import com.nguyen.asuper.repository.AuthRepository.Companion.currentUser
import com.nguyen.asuper.viewmodels.MainViewModel
import kotlinx.android.synthetic.main.dialog_payment.view.*

class EditUserDialogFragment(private val mainViewModel: MainViewModel, private val onPositiveButtonClick: (String) -> Unit): DialogFragment() {


    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            val builder = AlertDialog.Builder(it)
            // Get the layout inflater
            val inflater = requireActivity().layoutInflater;

            // Inflate and set the layout for the dialog
            // Pass null as the parent view because its going in the dialog layout
            val binding = DialogEditUserNameBinding.inflate(inflater, null, false)

            binding.user = currentUser

            builder.setView(binding.root)
                .setCancelable(false)
                .setTitle("New Name")
                .setPositiveButton("Ok") { _, _ ->
                    val newName = binding.newUsernameEditText.text.toString()
                    println("New name $newName")
                    if(newName != "") {
                        dismiss()
                        mainViewModel.saveUserName(newName)
                        onPositiveButtonClick.invoke(newName)
                    } else {
                        Toast.makeText(requireContext(), "Name cannot be empty!", Toast.LENGTH_SHORT).show()
                    }
                }
                .setNegativeButton("Cancel") { _, _ ->
                    dismiss()
                }
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }
}