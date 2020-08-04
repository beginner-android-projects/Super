package com.nguyen.asuper.util

import android.content.Context
import android.content.SharedPreferences
import com.google.android.libraries.places.api.model.AutocompletePrediction
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.nguyen.asuper.data.MapLocation
import com.nguyen.asuper.data.SearchHistory
import com.nguyen.asuper.data.User
import java.lang.reflect.Type

object SavedSharedPreferences {
    private const val NAME = "StringSharedPreferences"
    private const val MODE = Context.MODE_PRIVATE
    private lateinit var preferences: SharedPreferences
    lateinit var mGson : Gson

    //SharedPreferences variables
    private const val CURRENT_USER_LOCATION = "current_user_location"
    private const val CURRENT_LOGGED_USER_ID = "current_logged_user_id"
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

    var currentLoggedUserId: String?
        get() = preferences.getString(CURRENT_LOGGED_USER_ID, null)
        set(value){
            preferences.edit{
                it.putString(CURRENT_LOGGED_USER_ID, value)
            }
        }

    var currentUserLocation: MapLocation?
        get() {
            val json = preferences.getString(CURRENT_USER_LOCATION, "")
            if (json === "") return null
            return mGson.fromJson(json, MapLocation::class.java)
        }
        set(value) {
            val json = mGson.toJson(value)
            preferences.edit {
                it.putString(CURRENT_USER_LOCATION, json)
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