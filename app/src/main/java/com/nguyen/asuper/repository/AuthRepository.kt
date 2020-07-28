package com.nguyen.asuper.repository

import android.net.Uri
import android.util.Log
import com.nguyen.asuper.data.*
import com.nguyen.asuper.util.FirebaseUtil
import com.nguyen.asuper.util.SavedSharedPreferences
import com.nguyen.asuper.util.SavedSharedPreferences.currentLoggedUser
import com.nguyen.asuper.util.SavedSharedPreferences.mGson
import java.io.ByteArrayOutputStream
import java.net.URI

class AuthRepository{


    fun registerUser(avatar: Uri?, username: String, email: String, password: String, confirmPassword: String, callback: (ApiResponse<Unit>) -> Unit){
        Log.d("Register", "Register new user...")

        if(username.isBlank() || email.isBlank() || password.isBlank() || confirmPassword.isBlank()){
            callback.invoke(ApiResponse(null, "You have to enter all the fields!", false))
            return
        } else if(password != confirmPassword){
            callback.invoke(ApiResponse(null, "Your password and confirm password don't match!", false))
            return
        }

        FirebaseUtil.getAuth().createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener {task ->
                if(task.isSuccessful) {
                    val user = FirebaseUtil.getCurrentUser()
                    user?.let{user ->
                        Log.d("Register", "Success: ${user.uid}")

                        if(avatar != null){
                            uploadAvatar(user.uid, avatar, fun(uriString: String){
                                currentLoggedUser = User(user.uid, username, email, avatar = uriString)
                                insertOrUpdateUser(fun(result: Boolean){
                                    if (result) callback.invoke(ApiResponse(200, "Update Successfully!", true))
                                    else callback.invoke(ApiResponse(null, "Cannot update at the moment!", false))
                                })
                            })
                        } else {
                            currentLoggedUser = User(user.uid, username, email)
                            insertOrUpdateUser(fun(result: Boolean){
                                if (result) callback.invoke(ApiResponse(200, "Register Successfully!", true))
                                else callback.invoke(ApiResponse(null, "Cannot register at the moment!", false))
                            })
                        }
                    }
                } else {
                    Log.d("Register", "Fail")
                    callback.invoke(ApiResponse(null , "Fail to register!", false))
                }
            }
            .addOnFailureListener {
                Log.d("Register", "Fail: ${it.message}")
                callback.invoke(ApiResponse(null, it.message, false))
            }
    }

    fun logInUser(email: String, password: String, callback: (ApiResponse<Unit>) -> Unit){
        Log.d("Login", "Logging in user...")

        if(email.isBlank() || password.isBlank()){
            callback.invoke(ApiResponse(null, "Please enter both email and password", false))
            return
        }
        FirebaseUtil.getAuth().signInWithEmailAndPassword(email, password)
            .addOnCompleteListener {task ->
                if(task.isSuccessful) {
                    val user = FirebaseUtil.getCurrentUser()
                    Log.d("Login", "Success: ${user.toString()}")

                    user?.uid?.let {
                        getUser(it, fun(user: User){
                            currentLoggedUser = user
                            callback.invoke(ApiResponse(200, "Login Successfully!", true))
                        })
                    }

                } else {
                    Log.d("Login", "Fail")
                    callback.invoke(ApiResponse(null , "Fail to Login!", false))
                }
            }
            .addOnFailureListener {
                Log.d("Login", "Fail: ${it.message}")
                callback.invoke(ApiResponse(null, it.message, false))
            }
    }

    fun resetPassword(email: String, callback: (ApiResponse<Unit>) -> Unit){
        Log.d("Reset", "Reset user password...")

        if(email.isBlank()){
            callback.invoke(ApiResponse(null, "Please enter your email!", false))
            return
        }

        FirebaseUtil.getAuth().sendPasswordResetEmail(email)
            .addOnCompleteListener {task ->
                if(task.isSuccessful){
                    Log.d("Reset", "Success")
                    callback.invoke(ApiResponse(200, "", true))
                } else {
                    Log.d("Reset", "Fail")
                    callback.invoke(ApiResponse(null, "There is something wrong!", false))
                }
            }
            .addOnFailureListener {
                Log.d("Reset", "Fail: ${it.message}")
                callback.invoke(ApiResponse(null, it.message, false))
            }
    }

    fun saveUserLocation(home: Location?, work: Location?, callback: (Boolean) -> Unit){
        val user = currentLoggedUser?.copy(home = home, work = work)
        currentLoggedUser = user
        insertOrUpdateUser(callback)
    }

    private fun insertOrUpdateUser(callback: (Boolean) -> Unit){
        currentLoggedUser?.let {
            val home = mGson.toJson(it.home)
            val work = mGson.toJson(it.work)
            Log.d("FireStore", "$home and $work")
            val user = hashMapOf(
                "user_id" to it.id,
                "username" to it.username,
                "home" to home,
                "work" to work,
                "avatar" to it.avatar,
                "used_coupons" to it.usedCoupons,
                "trips" to it.trips
            )
            FirebaseUtil.getDb().collection("users").document(it.id!!)
                .set(user)
                .addOnSuccessListener {
                    Log.d("FireStore", "Add/Update user successfully")
                    callback.invoke(true)
                }
                .addOnFailureListener { e ->
                    Log.d("FireStore", "Fail to add/update user", e)
                    callback.invoke(false)
                }
        }
    }

    private fun getUser(id: String, callback: (user: User) -> Unit) {
        FirebaseUtil.getDb().collection("users").document(id)
            .get()
            .addOnSuccessListener { document ->
                if (document != null) {
                    Log.d("FireStore", "Getting user successfully: ${document.data}")
                    val home = mGson.fromJson(document.data?.get("home") as String?, Location::class.java)
                    val work = mGson.fromJson(document.data?.get("work") as String?, Location::class.java)
                    val user = User(
                        document.data?.get("user_id") as String?,
                        document.data?.get("username") as String?,
                        document.data?.get("email") as String?,
                        home,
                        work,
                        document.data?.get("avatar") as String?,
                        document.data?.get("used_coupon") as HashMap<String, Boolean>?
                    )
                    callback.invoke(user)
                } else {
                    Log.d("FireStore", "Not found user")
                }
            }
            .addOnFailureListener { exception ->
                Log.d("FireStore", "get failed with ", exception)
            }
    }

    private fun uploadAvatar(userId: String, uri: Uri, callback: (uri: String) -> Unit){
        val avatarFolder = FirebaseUtil.getStorageRef().child("avatars/${userId}")
        val uploadTask = avatarFolder.putFile(uri)

        uploadTask
            .addOnSuccessListener {
                Log.d("Register", "Upload success: ${it.storage}")
            }
            .addOnFailureListener{
                Log.d("Register", "Fail: ${it.message}")
            }
            .continueWithTask { task ->
                if (!task.isSuccessful) {
                    task.exception?.let {
                        throw it
                    }
                }
                avatarFolder.downloadUrl
            }
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val downloadUri = task.result
                    Log.d("Register", "Uri: ${downloadUri.toString()}")
                    downloadUri?.let {
                        callback.invoke(it.toString())
                    }

                } else {
                    // Handle failures
                    Log.d("Register", "Fail to complete loading avatar!")
                }
            }
    }







}