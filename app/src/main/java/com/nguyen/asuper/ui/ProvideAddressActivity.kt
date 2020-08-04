package com.nguyen.asuper.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.Autocomplete
import com.google.android.libraries.places.widget.AutocompleteActivity
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode
import com.nguyen.asuper.R
import com.nguyen.asuper.data.MapLocation
import com.nguyen.asuper.viewmodels.AuthViewModel
import kotlinx.android.synthetic.main.activity_provide_address.*
import org.koin.androidx.viewmodel.ext.android.viewModel

class ProvideAddressActivity : AppCompatActivity() {

    companion object {
        private const val AUTOCOMPLETE_HOME_REQUEST_CODE = 1
        private const val AUTOCOMPLETE_WORK_REQUEST_CODE = 2

    }

    private var home: MapLocation? = null
    private var work: MapLocation? = null

    private val authViewModel by viewModel<AuthViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_provide_address)

        // Set the fields to specify which types of place data to
        // return after the user has made a selection.
        val fields = listOf(Place.Field.ID, Place.Field.NAME, Place.Field.ADDRESS, Place.Field.LAT_LNG)

        // Start the autocomplete intent.
        home_address_button.setOnClickListener {
            val intent = Autocomplete.IntentBuilder(AutocompleteActivityMode.FULLSCREEN, fields)
                .build(this)
            startActivityForResult(intent, AUTOCOMPLETE_HOME_REQUEST_CODE)
        }

        work_address_button.setOnClickListener {
            val intent = Autocomplete.IntentBuilder(AutocompleteActivityMode.FULLSCREEN, fields)
                .build(this)
            startActivityForResult(intent, AUTOCOMPLETE_WORK_REQUEST_CODE)
        }

        save_address_button.setOnClickListener {
            if(home != null || work != null){
                loading_icon_address.visibility = View.VISIBLE
                authViewModel.saveHomeAndWorkLocation(home, work, fun(result: Boolean){
                    loading_icon_address.visibility = View.GONE
                    finish()
                    if(!result){
                        Toast.makeText(this, "Cannot save location!",Toast.LENGTH_SHORT).show()
                    }
                })
            } else {
                Toast.makeText(this, "Nothing changed!",Toast.LENGTH_SHORT).show()
            }

        }


        skip_button.setOnClickListener {
            finish()
        }

        close_button.setOnClickListener {
            finish()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {

        if(requestCode != AUTOCOMPLETE_HOME_REQUEST_CODE && requestCode != AUTOCOMPLETE_WORK_REQUEST_CODE) {
            super.onActivityResult(requestCode, resultCode, data)
            return
        }

        when (resultCode) {
            Activity.RESULT_OK -> {
                data?.let {
                    val place = Autocomplete.getPlaceFromIntent(it)
                    Log.d("Address", "$place")
                    if(requestCode == AUTOCOMPLETE_HOME_REQUEST_CODE){
                        home_address_button.text = place.address
                        home = MapLocation(place.id!!, place.name, place.address, place.latLng?.latitude, place.latLng?.longitude)
                    }
                    else {
                        work_address_button.text = place.address
                        work = MapLocation(place.id!!, place.name, place.address, place.latLng?.latitude, place.latLng?.longitude)
                    }

                }
            }
            AutocompleteActivity.RESULT_ERROR -> {
                // TODO: Handle the error.
                data?.let {
                    val status = Autocomplete.getStatusFromIntent(it)
                    Log.d("Address", status.statusMessage!!)
                }
            }
            Activity.RESULT_CANCELED -> {
                // The user canceled the operation.
            }
        }
        return

    }
}