package com.nguyen.asuper.repository

import android.net.Uri
import android.util.Log
import com.nguyen.asuper.data.ApiResponse
import com.nguyen.asuper.data.MapLocation
import com.nguyen.asuper.data.OriginDestination
import com.nguyen.asuper.data.User
import com.nguyen.asuper.util.FirebaseUtil
import com.nguyen.asuper.util.SavedSharedPreferences
import com.nguyen.asuper.util.SavedSharedPreferences.mGson

class AuthRepository{

    companion object {
        lateinit var currentUser: User
    }


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
                                val user = User(user.uid, username, email, avatar = uriString)
                                insertOrUpdateUserInFireStore(user, fun(result: Boolean){
                                    if (result) callback.invoke(ApiResponse(200, "Update Successfully!", true))
                                    else callback.invoke(ApiResponse(null, "Cannot update at the moment!", false))
                                })
                            })
                        } else {
                            val user = User(user.uid, username, email)
                            insertOrUpdateUserInFireStore(user, fun(result: Boolean){
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
                            currentUser = user
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

    fun saveUserLocation(home: MapLocation?, work: MapLocation?, callback: (Boolean) -> Unit){
        val user = currentUser.copy(home = home, work = work)
        insertOrUpdateUserInFireStore(user, callback)
    }

    private fun insertOrUpdateUserInFireStore(newUser: User, callback: (Boolean) -> Unit){
        val user = hashMapOf(
            "user_id" to newUser.id,
            "username" to newUser.username,
            "email" to newUser.email,
            "home" to newUser.home,
            "work" to newUser.work,
            "avatar" to newUser.avatar
        )
        FirebaseUtil.getDb().collection("users").document(newUser.id!!)
            .set(user)
            .addOnSuccessListener {
                Log.d("FireStore", "Add/Update user successfully")
                currentUser = newUser
                callback.invoke(true)
            }
            .addOnFailureListener { e ->
                Log.d("FireStore", "Fail to add/update user", e)
                callback.invoke(false)
            }
    }

    fun updateUserLocally() {
        currentUser.let {
            FirebaseUtil.getDb().collection("users").document(it.id!!)
                .get()
                .addOnSuccessListener { document ->
                    if (document != null) {
                        Log.d("FireStore", "Getting user successfully: ${document.data}")
                        val homeMap = document.data?.get("home") as HashMap<String, Object>
                        val workMap = document.data?.get("work") as HashMap<String, Object>
                        val home = MapLocation(homeMap["id"].toString(), homeMap["name"].toString(), homeMap["address"].toString(), homeMap["lat"] as Double, homeMap["lng"] as Double)
                        val work = MapLocation(workMap["id"].toString(), workMap["name"].toString(), workMap["address"].toString(), workMap["lat"] as Double, workMap["lng"] as Double)
                        val user = User(
                            document.data?.get("user_id") as String?,
                            document.data?.get("username") as String?,
                            document.data?.get("email") as String?,
                            home,
                            work,
                            document.data?.get("avatar") as String?
                        )
                        currentUser = user
                    } else {
                        Log.d("FireStore", "Not found user")
                    }
                }
                .addOnFailureListener { exception ->
                    Log.d("FireStore", "get failed with ${exception.message}")
                }
        }

    }

    fun getUser(id: String, callback: (user: User) -> Unit) {
        FirebaseUtil.getDb().collection("users").document(id)
            .get()
            .addOnSuccessListener { document ->
                if (document != null) {
                    val user = User(
                        document.data?.get("user_id") as String?,
                        document.data?.get("username") as String?,
                        document.data?.get("email") as String?,
                        mGson.fromJson(mGson.toJsonTree(document.data?.get("home")), MapLocation::class.java),
                        mGson.fromJson(mGson.toJsonTree(document.data?.get("work")), MapLocation::class.java),
                        document.data?.get("avatar") as String?
                    )
                    Log.d("FireStore", "user: $user")
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