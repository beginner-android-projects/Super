package com.nguyen.asuper.ui.auth

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.nguyen.asuper.databinding.FragmentRegisterBinding
import com.nguyen.asuper.ui.main.MainActivity
import com.nguyen.asuper.viewmodels.AuthViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel


class RegisterFragment : Fragment() {

    companion object{
        const val PICK_IMAGE_REQUEST = 1
    }

    private val authViewModel by viewModel<AuthViewModel>()
    private lateinit var binding: FragmentRegisterBinding
    private var avatar: Uri? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentRegisterBinding.inflate(inflater, container, false)

        authViewModel.registerStatus.observe(viewLifecycleOwner, Observer {
            binding.loadingIcon.visibility = View.GONE
            binding.isError = it.not()
            if(it){
                startActivity(Intent(requireContext(), MainActivity::class.java))
            }
        })

        authViewModel.registerMsg.observe(viewLifecycleOwner, Observer {
            binding.registerErrorBar.errorMsg = it
        })

        binding.closeButton.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.userAvatar.setOnClickListener {
            val intent = Intent()
            intent.type = "image/*";
            intent.action = Intent.ACTION_GET_CONTENT;
            startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE_REQUEST);
        }

        binding.registerButton.setOnClickListener {
            binding.isError = false
            binding.loadingIcon.visibility = View.VISIBLE
            val username = binding.usernameRegister.text.toString()
            val email = binding.emailRegister.text.toString()
            val password = binding.passwordRegister.text.toString()
            val confirmPassword = binding.confirmPasswordRegister.text.toString()

            authViewModel.registerUser(avatar, username, email, password, confirmPassword)
        }

        return binding.root
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(resultCode != Activity.RESULT_OK) return

        if(requestCode == PICK_IMAGE_REQUEST){
            avatar = data?.data
            Glide.with(requireContext())
                .load(avatar)
                .centerCrop()
                .into(binding.avatarImageView)
        }
    }
}