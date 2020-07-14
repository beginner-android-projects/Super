package com.nguyen.asuper.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.nguyen.asuper.R
import com.nguyen.asuper.ui.auth.RegisterFragment.Companion.PICK_IMAGE_REQUEST
import com.nguyen.asuper.util.SavedSharedPreferences.currentLoggedUser


class AuthenticationActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        if(currentLoggedUser != null){
            startActivity(Intent(this, MainActivity::class.java))
        } else {
            setContentView(R.layout.activity_authentication)
        }
    }

}