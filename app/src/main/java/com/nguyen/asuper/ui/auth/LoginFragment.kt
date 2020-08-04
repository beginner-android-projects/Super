package com.nguyen.asuper.ui.auth

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.nguyen.asuper.R
import com.nguyen.asuper.databinding.FragmentLoginBinding
import com.nguyen.asuper.repository.AuthRepository.Companion.currentUser
import com.nguyen.asuper.ui.MainActivity
import com.nguyen.asuper.util.SavedSharedPreferences.currentLoggedUserId
import com.nguyen.asuper.viewmodels.AuthViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel


class LoginFragment : Fragment() {

    private val authViewModel by viewModel<AuthViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val binding = FragmentLoginBinding.inflate(inflater, container, false)

        authViewModel.loginStatus.observe(viewLifecycleOwner, Observer {
            binding.loadingIcon.visibility = View.GONE
            binding.isError = it.not()
            if(it){
                startActivity(Intent(requireContext(), MainActivity::class.java))
                activity?.finish()
                currentLoggedUserId = currentUser.id
            }
        })

        authViewModel.loginMsg.observe(viewLifecycleOwner, Observer {
            binding.loginErrorBar.errorMsg = it
        })

        binding.moveToRegister.setOnClickListener {
            findNavController().navigate(R.id.action_login_to_register)
        }

        binding.loginButton.setOnClickListener {
            binding.isError = false
            binding.loadingIcon.visibility = View.VISIBLE
            val email = binding.emailLogin.text.toString()
            val password = binding.passwordLogin.text.toString()

            authViewModel.logInUser(email, password)
        }

        binding.resetPasswordButton.setOnClickListener {
            findNavController().navigate(R.id.action_login_to_reset_password)
        }

        return binding.root
    }

}