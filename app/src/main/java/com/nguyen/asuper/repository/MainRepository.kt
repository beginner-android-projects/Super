package com.nguyen.asuper.repository

import android.graphics.Bitmap
import android.util.Log
import com.akexorcist.googledirection.DirectionCallback
import com.akexorcist.googledirection.GoogleDirection
import com.akexorcist.googledirection.constant.TransportMode
import com.akexorcist.googledirection.model.Direction
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.model.*
import com.google.android.libraries.places.api.net.*
import com.nguyen.asuper.data.*
import com.nguyen.asuper.di.repositoryModule
import com.nguyen.asuper.util.FirebaseUtil
import com.nguyen.asuper.util.SavedSharedPreferences
import com.nguyen.asuper.util.SavedSharedPreferences.currentLoggedUser
import com.nguyen.asuper.util.SavedSharedPreferences.currentUserLatitude
import com.nguyen.asuper.util.SavedSharedPreferences.currentUserLongitude
import java.time.LocalDateTime
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap


class MainRepository{

    private var currentTripPreview: Bitmap? = null

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

    fun requestRide(originDestination: OriginDestination, fare: Double, direction: Direction, carType: String, paymentMethod: String, coupon: Coupon? = null,callback: (ApiResponse<Trip>) -> Unit){
        //Apply the coupon
        if(coupon != null) {
            applyCoupon(coupon, fun(result) {
                if (result) {
                    findDriver(originDestination.origin!!, fun(driver: Driver?){
                        if(driver != null){
                            val trip = Trip(
                                id = UUID.randomUUID().toString(),
                                originDestination = originDestination,
                                date = Calendar.getInstance().time,
                                direction = direction,
                                driver = driver,
                                carType = carType,
                                paymentMethod = paymentMethod,
                                fare = fare,
                                couponUsed = coupon,
                                preview = currentTripPreview
                            )
                            callback.invoke(ApiResponse(200, "Request Ride successfully!", true, trip))
                        } else callback.invoke(ApiResponse(null, "Couldn't find a driver!", false))
                    })
                } else {
                    callback.invoke(ApiResponse(null, "Coupon couldn't be applied!", false))
                }
            })
        } else {
            //Finding the driver
            findDriver(originDestination.origin!!, fun(driver: Driver?){
                if(driver != null){
                    val trip = Trip(
                        id = UUID.randomUUID().toString(),
                        originDestination = originDestination,
                        date = Calendar.getInstance().time,
                        direction = direction,
                        driver = driver,
                        carType = carType,
                        paymentMethod = paymentMethod,
                        fare = fare,
                        preview = currentTripPreview
                    )
                    callback.invoke(ApiResponse(200, "Request Ride successfully!", true, trip))
                } else callback.invoke(ApiResponse(null, "Couldn't find a driver!", false, null))
            })
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
                    Log.d("Map", "Fail: ${t.message}")
                    callback.invoke(ApiResponse(null, "Fail: ${t.message}", false))
                }
            })
    }



    private fun findDriver(originLatLng: LatLng, callback:(Driver?) -> Unit){
        Log.d("Map", "Finding driver...")
        val driverLatLng = LatLng(originLatLng.latitude + 0.002, originLatLng.longitude + 0.002)

        FirebaseUtil.getDb().collection("drivers")
            .get()
            .addOnSuccessListener { querySnapshot ->
                val drivers: ArrayList<Driver> = ArrayList()
                querySnapshot.documents.forEach {
                    drivers.add(Driver(
                        it.data?.get("driver_id") as String,
                        it.data?.get("name") as String?,
                        it.data?.get("avatar") as String?,
                        it.data?.get("rating") as Double,
                        it.data?.get("rating_count") as Long,
                        driverLatLng
                    ))
                }

                callback.invoke(drivers[(0 until drivers.size).random()])
            }
            .addOnFailureListener { exception ->
                callback.invoke(null)
            }
    }

    private fun insertOrUpdateUser(callback: (Boolean) -> Unit){
        currentLoggedUser?.let {
            val home = SavedSharedPreferences.mGson.toJson(it.home)
            val work = SavedSharedPreferences.mGson.toJson(it.work)
            Log.d("FireStore", "$home and $work and ${it.trips}")
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


    fun getCoupons(callback: (ApiResponse<List<Coupon>>) -> Unit){
        FirebaseUtil.getDb().collection("Coupon")
            .get()
            .addOnSuccessListener { querySnapshot ->
                val user = currentLoggedUser?.copy()
                val couponList: ArrayList<Coupon> = ArrayList()
                querySnapshot.documents.forEach {
                    if(user?.usedCoupons == null) user?.usedCoupons = HashMap()
                    val  coupon = Coupon(
                        it.data?.get("id") as String,
                        it.data?.get("name") as String,
                        it.data?.get("code") as String,
                        it.data?.get("description") as String,
                        it.data?.get("discount") as Long,
                        it.data?.get("background_image") as String
                    )
                    if(user?.usedCoupons!![coupon.id!!] == null){
                        user?.usedCoupons!![coupon.id!!] = false
                        couponList.add(coupon)
                    } else if(user?.usedCoupons!![coupon.id!!] == false){
                        couponList.add(coupon)
                    }
                }

                currentLoggedUser = user
                insertOrUpdateUser{
                    if (it) callback.invoke(ApiResponse(200, "get coupons successfully", true, couponList))
                    else callback.invoke(ApiResponse(null, "Fail to get coupons", false))
                }
            }
            .addOnFailureListener { exception ->
                callback.invoke(ApiResponse(null, exception.message, false))
            }
    }


    private fun applyCoupon(coupon: Coupon, callback: (Boolean) -> Unit){
        val user = currentLoggedUser?.copy()
        user?.usedCoupons?.put(coupon.id!!, true)
        currentLoggedUser = user

        insertOrUpdateUser {
            if(it){
                callback.invoke(true)
            } else {
                callback.invoke(false)
            }
        }
    }

    fun submitRating(rating: Double, driver: Driver, callback: (ApiResponse<Unit>) -> Unit){
        val newRatingCount = driver.ratingCount + 1
        val newRating = (driver.rating?.times(driver.ratingCount)?.plus(rating))?.div(newRatingCount)

        val updatedDriver = hashMapOf(
            "avatar" to driver.avatar,
            "driver_id" to driver.id,
            "name" to driver.name,
            "rating" to newRating,
            "rating_count" to newRatingCount
        )

        FirebaseUtil.getDb().collection("drivers").document(driver.id)
            .set(updatedDriver)
            .addOnSuccessListener {
                Log.d("FireStore", "Submit Rating successfully")
                callback.invoke(ApiResponse(null, "Submit rating successfully", true))
            }
            .addOnFailureListener { e ->
                Log.d("FireStore", "Fail to submit rating")
                callback.invoke(ApiResponse(null, e.message, false))
            }
    }

    fun saveTripToDatabase(trip: Trip?, callback : (ApiResponse<Unit>) -> Unit){
        if (trip == null) callback.invoke(ApiResponse(null, "The trip cannot be null", false))
        currentLoggedUser?.let {
            val user = it.copy()
            val trips = (if(it.trips == null || it.trips?.size == 0) HashMap() else user.trips!!)
            trips[trip!!.id] = true
            user.trips = trips
            currentLoggedUser = user
            insertOrUpdateUser{result ->
                if(result) {
                    val newTrip = hashMapOf(
                        "trip_id" to trip.id,
                        "date" to trip.date,
                        "driver" to trip.driver,
                        "direction" to trip.direction,
                        "origin_destination" to trip.originDestination,
                        "fare" to trip.fare,
                        "car_type" to trip.carType,
                        "coupon_used" to trip.couponUsed,
                        "payment_method" to trip.paymentMethod,
                        "preview_image" to SavedSharedPreferences.mGson.toJson(trip.preview)
                    )
                    FirebaseUtil.getDb().collection("trips").document(trip!!.id)
                        .set(newTrip)
                        .addOnSuccessListener {
                            Log.d("FireStore", "Add trip successfully")
                            callback.invoke(ApiResponse(null, "Save trip successfully", true))
                        }
                        .addOnFailureListener { e ->
                            Log.d("FireStore", "Fail to add trip", e)
                            callback.invoke(ApiResponse(null, e.message, false))
                        }
                }
                else callback.invoke(ApiResponse(null, "The trip cannot be saved", false))
            }
        }

    }


    fun saveTripPreviewBitmap(preview: Bitmap){
        currentTripPreview = preview
    }
}