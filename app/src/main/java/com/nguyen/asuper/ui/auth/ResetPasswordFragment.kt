package com.nguyen.asuper.ui.auth

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.nguyen.asuper.R
import com.nguyen.asuper.databinding.FragmentResetPasswordBinding
import com.nguyen.asuper.viewmodels.AuthViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel


class ResetPasswordFragment : Fragment() {

    private val authViewModel by viewModel<AuthViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val binding = FragmentResetPasswordBinding.inflate(inflater, container, false)

        authViewModel.resetPasswordMsg.observe(viewLifecycleOwner, Observer {
            binding.resetErrorBar.errorMsg = it
        })

        authViewModel.resetPasswordStatus.observe(viewLifecycleOwner, Observer {
            binding.isError = it.not()
            if(it){
                findNavController().popBackStack()
                Toast.makeText(requireContext(), "An email has been sent to your email to reset your password!", Toast.LENGTH_LONG).show()
            }
        })


        binding.resetPasswordButton.setOnClickListener {
            binding.isError = false
            authViewModel.resetPassword(binding.emailResetPassword.text.toString())
        }

        binding.closeButton.setOnClickListener {
            findNavController().popBackStack()
        }

        return binding.root
    }

}