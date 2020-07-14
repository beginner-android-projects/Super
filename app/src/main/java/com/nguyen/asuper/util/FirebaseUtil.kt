package com.nguyen.asuper.util

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference

object FirebaseUtil {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var storage: FirebaseStorage


    fun init(){
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        storage = FirebaseStorage.getInstance()
    }

    fun getAuth(): FirebaseAuth{
        return auth
    }

    fun getDb(): FirebaseFirestore{
        return db
    }

    fun getCurrentUser() : FirebaseUser?{
        return auth.currentUser
    }

    fun getStorageRef(): StorageReference{
        return storage.reference
    }



}