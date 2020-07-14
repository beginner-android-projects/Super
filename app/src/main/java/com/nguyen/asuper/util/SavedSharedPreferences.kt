package com.nguyen.asuper.util

import android.content.Context
import android.content.SharedPreferences
import com.google.android.libraries.places.api.model.AutocompletePrediction
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.nguyen.asuper.data.SearchHistory
import com.nguyen.asuper.data.User
import java.lang.reflect.Type

object SavedSharedPreferences {
    private const val NAME = "StringSharedPreferences"
    private const val MODE = Context.MODE_PRIVATE
    private lateinit var preferences: SharedPreferences
    lateinit var mGson : Gson

    //SharedPreferences variables
    private const val CURRENT_LOGGED_USER = "current_logged_user"
    private const val CURRENT_LOGGED_USER_LAT = "current_logged_user_latitude"
    private const val CURRENT_LOGGED_USER_LNG = "current_logged_user_longitude"
    private const val SEARCH_HISTORY = "search_history"


    fun init(context: Context) {
        preferences = context.getSharedPreferences(NAME, MODE)
        mGson = Gson()
    }

    //an inline function to put variable and save it
    private inline fun SharedPreferences.edit(operation: (SharedPreferences.Editor) -> Unit) {
        val editor = edit()
        operation(editor)
        editor.apply()
    }


    var currentLoggedUser: User?
        get() {
            val json = preferences.getString(CURRENT_LOGGED_USER, "")
            if (json === "") return null
            return mGson.fromJson(json, User::class.java)
        }
        set(value) {
            val json = mGson.toJson(value)
            preferences.edit {
                it.putString(CURRENT_LOGGED_USER, json)
            }
        }

    var currentUserLatitude: Double
        get() = preferences.getFloat(CURRENT_LOGGED_USER_LAT, 0.0F).toDouble()
        set(value){
            preferences.edit {
                it.putFloat(CURRENT_LOGGED_USER_LAT, value.toFloat())
            }
        }

    var currentUserLongitude: Double
        get() = preferences.getFloat(CURRENT_LOGGED_USER_LNG, 0.0F).toDouble()
        set(value){
            preferences.edit {
                it.putFloat(CURRENT_LOGGED_USER_LNG, value.toFloat())
            }
        }

    var searchHistories: List<SearchHistory>
        get() {
            val json = preferences.getString(SEARCH_HISTORY, "")
            if (json === "") return emptyList()
            val type: Type = object : TypeToken<List<SearchHistory>>() {}.type
            return mGson.fromJson(json, type)
        }
        set(value) {
            val json = mGson.toJson(value)
            preferences.edit {
                it.putString(SEARCH_HISTORY, json)
            }
        }

}