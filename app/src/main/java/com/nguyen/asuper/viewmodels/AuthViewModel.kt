package com.nguyen.asuper.viewmodels

import android.net.Uri
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import com.nguyen.asuper.data.ApiResponse
import com.nguyen.asuper.data.Location
import com.nguyen.asuper.repository.AuthRepository

class AuthViewModel(private val repository: AuthRepository) : ViewModel(){

    private val registerResponse: MutableLiveData<ApiResponse<Unit>> = MutableLiveData()
    private val loginResponse: MutableLiveData<ApiResponse<Unit>> = MutableLiveData()
    private val resetPasswordResponse: MutableLiveData<ApiResponse<Unit>> = MutableLiveData()

    val registerStatus: LiveData<Boolean> = Transformations.map(registerResponse){
        it.status
    }

    val registerMsg: LiveData<String> = Transformations.map(registerResponse){
        it.message
    }

    val loginStatus: LiveData<Boolean> = Transformations.map(loginResponse){
        it.status
    }

    val loginMsg: LiveData<String> = Transformations.map(loginResponse){
        it.message
    }

    val resetPasswordStatus: LiveData<Boolean> = Transformations.map(resetPasswordResponse){
        it.status
    }

    val resetPasswordMsg: LiveData<String> = Transformations.map(resetPasswordResponse){
        it.message
    }

    fun registerUser(avatar: Uri?, username: String, email: String, password: String, confirmPassword: String){
        repository.registerUser(avatar, username, email, password, confirmPassword, fun (response: ApiResponse<Unit>){
            registerResponse.value = response
        })
    }

    fun logInUser(email: String, password: String){
        repository.logInUser(email, password, fun (response: ApiResponse<Unit>){
            loginResponse.value = response
        })
    }

    fun resetPassword(email: String){
        repository.resetPassword(email, fun(response: ApiResponse<Unit>){
            resetPasswordResponse.value = response
        })
    }

    fun saveHomeAndWorkLocation(home: Location?, work: Location?, callback: (Boolean) -> Unit) {
        repository.saveUserLocation(home, work, callback)
    }


}

