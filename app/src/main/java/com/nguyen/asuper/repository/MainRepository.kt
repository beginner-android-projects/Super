package com.nguyen.asuper.repository

import android.util.Log
import com.akexorcist.googledirection.DirectionCallback
import com.akexorcist.googledirection.GoogleDirection
import com.akexorcist.googledirection.constant.TransportMode
import com.akexorcist.googledirection.model.Direction
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.model.*
import com.google.android.libraries.places.api.net.*
import com.nguyen.asuper.data.ApiResponse
import com.nguyen.asuper.data.Driver
import com.nguyen.asuper.util.FirebaseUtil
import com.nguyen.asuper.util.SavedSharedPreferences.currentUserLatitude
import com.nguyen.asuper.util.SavedSharedPreferences.currentUserLongitude


class MainRepository{

    fun getAutocompleteSuggestions(query: String, placesClient: PlacesClient, callback: (ApiResponse<List<AutocompletePrediction>>) -> Unit){
        val token = AutocompleteSessionToken.newInstance()

        // Create a RectangularBounds object.
        val bounds = RectangularBounds.newInstance(
            LatLng(currentUserLatitude, currentUserLongitude),
            LatLng(currentUserLatitude + 0.00001, currentUserLongitude + 0.00001)
        )
        // Use the builder to create a FindAutocompletePredictionsRequest.
        val request =
            FindAutocompletePredictionsRequest.builder()
                // Call either setLocationBias() OR setLocationRestriction().
                .setLocationBias(bounds)
                .setOrigin(LatLng(currentUserLatitude, currentUserLongitude))
                .setTypeFilter(TypeFilter.ADDRESS)
                .setSessionToken(token)
                .setQuery(query)
                .build()
        placesClient.findAutocompletePredictions(request)
            .addOnSuccessListener { response: FindAutocompletePredictionsResponse ->
                callback.invoke(ApiResponse(200, "", true, response.autocompletePredictions))
            }.addOnFailureListener { exception: Exception? ->
                if (exception is ApiException) {
                    Log.d("Map", "Place not found: " + exception.statusCode)
                }
            }
    }

    fun getPlaceLatLng(id: String, placesClient: PlacesClient, callback: (ApiResponse<Place>) -> Unit){
        Log.d("Map","Getting Place Lat Lng...")
        // Specify the fields to return.
        val placeFields = listOf(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG)

        val request = FetchPlaceRequest.newInstance(id, placeFields)

        placesClient.fetchPlace(request)
            .addOnSuccessListener { response: FetchPlaceResponse ->
                val place = response.place
                Log.d("Map","Success")
                callback.invoke(ApiResponse(200, "Get Place Successfully!", true, place))
            }
            .addOnFailureListener { exception: Exception ->
                if (exception is ApiException) {
                    Log.d("Map","Fail")
                    Log.e("Map", "Place not found: ${exception.message}")
                    val statusCode = exception.statusCode
                    callback.invoke(ApiResponse(statusCode, "Get Place Successfully!", false))
                }
            }

    }


    fun getDirection(originLatLng: LatLng, destinationLatLng: LatLng, callback: (ApiResponse<Direction>) -> Unit){
        Log.d("Map","Getting Direction... $originLatLng $destinationLatLng")
        GoogleDirection.withServerKey("AIzaSyBcnoHY2ZzBo34VSJGtF7g4wW3pLrr7SGQ")
            .from(LatLng(originLatLng.latitude, originLatLng.longitude))
            .to(LatLng(destinationLatLng.latitude, destinationLatLng.longitude))
            .transportMode(TransportMode.DRIVING)
            .execute(object : DirectionCallback {
                override fun onDirectionSuccess(direction: Direction) {
                    if (direction.isOK) {
                        Log.d("Map","Success: ${direction.routeList[0].totalDuration}")

                        callback.invoke(ApiResponse(200, "Get direction successfully", true, direction))
                    } else {
                        Log.d("Map","Fail: ${direction.errorMessage}")
                        callback.invoke(ApiResponse(null, "Fail to get direction", false))
                    }
                }

                override fun onDirectionFailure(t: Throwable) {
                }
            })
    }



    fun findDriver(originLatLng: LatLng, callback:(ApiResponse<Driver>) -> Unit){
        Log.d("Map", "Finding driver...")
        val driverLatLng = LatLng(originLatLng.latitude + 0.002, originLatLng.longitude + 0.002)
//        Timer("SettingUp", false).schedule(3000) {
//            callback.invoke(ApiResponse(200, "Successfully find a driver!", true, driverLatLng))
//        }

        FirebaseUtil.getDb().collection("drivers")
            .get()
            .addOnSuccessListener { querySnapshot ->
                val drivers: ArrayList<Driver> = ArrayList()
                querySnapshot.documents.forEach {
                    drivers.add(Driver(
                        it.data?.get("name") as String?,
                        it.data?.get("avatar") as String?,
                        it.data?.get("rating") as Double,
                        driverLatLng
                    ))
                }

                callback.invoke(
                    ApiResponse(200, "Successfully find a driver!", true, drivers[(0 until drivers.size).random()])
                )


            }
            .addOnFailureListener { exception ->

            }



    }

}