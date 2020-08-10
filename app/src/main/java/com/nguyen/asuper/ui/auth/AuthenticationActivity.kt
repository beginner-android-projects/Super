package com.nguyen.asuper.ui.auth

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import com.nguyen.asuper.R
import com.nguyen.asuper.ui.main.MainActivity
import com.nguyen.asuper.util.SavedSharedPreferences.currentLoggedUserId
import com.nguyen.asuper.viewmodels.AuthViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel


class AuthenticationActivity : AppCompatActivity() {
    private val authViewModel by viewModel<AuthViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("Debug", "test: $currentLoggedUserId")
        if(currentLoggedUserId != null){
            authViewModel.getUser(currentLoggedUserId!!)
            authViewModel.loginStatus.observe(this, Observer {
                if(it){
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                } else setContentView(R.layout.activity_authentication)
            })

        } else {
            setContentView(R.layout.activity_authentication)
        }

    }

}