package com.nguyen.asuper.repository

import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import com.akexorcist.googledirection.DirectionCallback
import com.akexorcist.googledirection.GoogleDirection
import com.akexorcist.googledirection.constant.TransportMode
import com.akexorcist.googledirection.model.Direction
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.model.*
import com.google.android.libraries.places.api.net.*
import com.google.firebase.firestore.Query
import com.nguyen.asuper.data.*
import com.nguyen.asuper.repository.AuthRepository.Companion.currentUser
import com.nguyen.asuper.util.FirebaseUtil
import com.nguyen.asuper.util.SavedSharedPreferences.currentUserLocation
import com.nguyen.asuper.util.SavedSharedPreferences.mGson
import java.io.ByteArrayOutputStream
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap


class MainRepository{

    var currentToken: AutocompleteSessionToken? = null


    fun getAutocompleteSuggestions(query: String, placesClient: PlacesClient, callback: (ApiResponse<List<AutocompletePrediction>>) -> Unit){
        if(currentToken == null) currentToken = AutocompleteSessionToken.newInstance()

        // Create a RectangularBounds object.
        val bounds = currentUserLocation?.let{ currentUserLocation ->
             RectangularBounds.newInstance(
                LatLng(currentUserLocation.lat!!, currentUserLocation.lng!!),
                LatLng(currentUserLocation.lat!! + 0.00001, currentUserLocation.lng!! + 0.00001)
            )
        }

        // Use the builder to create a FindAutocompletePredictionsRequest.
        val request =
            FindAutocompletePredictionsRequest.builder()
                // Call either setLocationBias() OR setLocationRestriction().
                .setLocationBias(bounds)
                .setOrigin(LatLng(currentUserLocation?.lat!!, currentUserLocation?.lng!!))
                .setTypeFilter(TypeFilter.ADDRESS)
                .setSessionToken(currentToken)
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
                    findDriver(LatLng(originDestination.origin?.lat!!, originDestination.origin?.lng!!), fun(driver: Driver?){
                        if(driver != null){
                            val trip = Trip(
                                id = UUID.randomUUID().toString(),
                                originDestination = originDestination,
                                date = Calendar.getInstance().time,
                                driver = driver,
                                userId = currentUser.id!!,
                                carType = carType,
                                paymentMethod = paymentMethod,
                                fare = fare,
                                couponUsed = coupon
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
            findDriver(LatLng(originDestination.origin?.lat!!, originDestination.origin?.lng!!), fun(driver: Driver?){
                if(driver != null){
                    val trip = Trip(
                        id = UUID.randomUUID().toString(),
                        originDestination = originDestination,
                        date = Calendar.getInstance().time,
                        driver = driver,
                        userId = currentUser.id!!,
                        carType = carType,
                        paymentMethod = paymentMethod,
                        fare = fare
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
                        it.data?.get("name") as String,
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

    private fun insertOrUpdateUser(newUser: User, callback: (Boolean) -> Unit){
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


    fun getCoupons(callback: (ApiResponse<List<Coupon>>) -> Unit){
        FirebaseUtil.getDb().collection("Coupon")
            .get()
            .addOnSuccessListener { querySnapshot ->
                val couponList: ArrayList<Coupon> = ArrayList()
                querySnapshot.documents.forEach {
                    val  coupon = Coupon(
                        it.data?.get("id") as String?,
                        it.data?.get("name") as String?,
                        it.data?.get("code") as String?,
                        it.data?.get("description") as String?,
                        it.data?.get("discount") as Long?,
                        it.data?.get("background_image") as String?,
                        it.data?.get("user_used") as HashMap<String, Boolean>?
                    )

                    coupon.userUsed?.let{userUsed ->
                        if(userUsed[currentUser.id] == null){
                            couponList.add(coupon)
                        }
                    } ?: run {
                        couponList.add(coupon)
                    }
                }
                Log.d("FireStore", "couponList: ${couponList.size}")
                callback.invoke(ApiResponse(null, "Getting coupon list successfully", true, couponList))
            }
            .addOnFailureListener { exception ->
                callback.invoke(ApiResponse(null, exception.message, false))
            }
    }


    private fun applyCoupon(coupon: Coupon, callback: (Boolean) -> Unit){
        val userUsed: HashMap<String, Boolean> = if(coupon.userUsed == null) HashMap() else coupon.userUsed!!
        currentUser.id?.let { userUsed.put(it, true) }

        val couponApplied = hashMapOf(
            "background_image" to coupon.backgroundImg,
            "code" to coupon.code,
            "description" to coupon.description,
            "discount" to coupon.discount,
            "id" to coupon.id,
            "name" to coupon.name,
            "user_used" to userUsed
        )

        coupon.id?.let {
            FirebaseUtil.getDb().collection("Coupon").document(it)
                .set(couponApplied)
                .addOnSuccessListener {
                    Log.d("FireStore", "Apply coupon successfully!")
                    callback.invoke(true)
                }
                .addOnFailureListener { e ->
                    Log.d("FireStore", "Fail to apply coupon ${e.message}")
                    callback.invoke(false)
                }
        }
    }

    fun submitRating(rating: Double, driver: Driver, callback: (ApiResponse<Unit>) -> Unit){
        val newRatingCount = driver.ratingCount?.plus(1)
        val newRating = newRatingCount?.let {
            (driver.ratingCount?.let { driver.rating?.times(it)?.plus(rating) })?.div(
                it
            )
        }

        val updatedDriver = hashMapOf(
            "avatar" to driver.avatar,
            "driver_id" to driver.id,
            "name" to driver.name,
            "rating" to newRating,
            "rating_count" to newRatingCount
        )

        driver.id?.let {
            FirebaseUtil.getDb().collection("drivers").document(it)
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
    }

    fun saveTripToDatabase(trip: Trip, tripPreviewBitmap: Bitmap?, callback : (ApiResponse<Unit>) -> Unit){

        saveTripPreview(trip.id, tripPreviewBitmap) {
            val newTrip = hashMapOf(
                "trip_id" to trip.id,
                "date" to trip.date,
                "driver" to trip.driver,
                "user_id" to trip.userId,
                "origin_destination" to trip.originDestination,
                "fare" to trip.fare,
                "car_type" to trip.carType,
                "coupon_used" to trip.couponUsed,
                "payment_method" to trip.paymentMethod,
                "preview_image" to it
            )

            trip.id?.let {
                FirebaseUtil.getDb().collection("trips").document(it)
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
        }

    }


    fun getTripHistory(callback: (ApiResponse<List<Trip>>) -> Unit){
        FirebaseUtil.getDb().collection("trips").whereEqualTo("user_id", currentUser.id).orderBy("date", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { querySnapshot ->
                val tripsList: ArrayList<Trip> = ArrayList()
                querySnapshot.documents.forEach{
                    val trip = Trip(
                        id = it.data?.get("trip_id") as String?,
                        date = it.getTimestamp("date")?.toDate(),
                        driver = mGson.fromJson(mGson.toJsonTree(it.data?.get("driver")), Driver::class.java),
                        userId = it.data?.get("user_id") as String?,
                        originDestination = mGson.fromJson(mGson.toJsonTree(it.data?.get("origin_destination")), OriginDestination::class.java),
                        fare = it.data?.get("fare") as Double?,
                        couponUsed = mGson.fromJson(mGson.toJsonTree(it.data?.get("coupon_used")), Coupon::class.java),
                        carType = it.data?.get("car_type") as String?,
                        paymentMethod = it.data?.get("payment_method") as String?,
                        preview = it.data?.get("preview_image") as String?
                    )
                    Log.d("FireStore", "trip: $trip")
                    tripsList.add(trip)
                }

                Log.d("FireStore", "tripsList: ${tripsList.size}")
                callback.invoke(ApiResponse(200, "Getting trip history successfully!", true, tripsList))
            }
            .addOnFailureListener { exception ->
                Log.d("FireStore", "Fail to get trip history: ${exception.message}")
                callback.invoke(ApiResponse(null, exception.message, false))
            }
    }

    private fun saveTripPreview(tripId: String?, preview: Bitmap?, callback: (uri: String) -> Unit){
        if(tripId == null || preview == null) {
            callback.invoke("")
            return
        }
        Log.d("FireStore", "Uploading preview...")
        val baos = ByteArrayOutputStream()
        preview.compress(Bitmap.CompressFormat.JPEG, 100, baos)
        val data = baos.toByteArray()

        val tripFolder = FirebaseUtil.getStorageRef().child("trips/${tripId}")
        val uploadTask = tripFolder.putBytes(data)

        uploadTask
            .addOnSuccessListener {
                Log.d("FireStore", "Upload preview success: ${it.storage}")
            }
            .addOnFailureListener{
                Log.d("FireStore", "Fail: ${it.message}")
            }
            .continueWithTask { task ->
                if (!task.isSuccessful) {
                    task.exception?.let {
                        throw it
                    }
                }
                tripFolder.downloadUrl
            }
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val downloadUri = task.result
                    Log.d("FireStore", "Uri: ${downloadUri.toString()}")
                    downloadUri?.let {
                        callback.invoke(it.toString())
                    }

                } else {
                    // Handle failures
                    Log.d("FireStore", "Fail to complete loading trip preview")
                }
            }
    }

    fun uploadAvatar(userId: String, uri: Uri, callback: (uri: String) -> Unit) {
        val avatarFolder = FirebaseUtil.getStorageRef().child("avatars/${userId}")
        val uploadTask = avatarFolder.putFile(uri)

        uploadTask
            .addOnSuccessListener {
                Log.d("FireStore", "Upload success: ${it.storage}")
            }
            .addOnFailureListener {
                Log.d("FireStore", "Fail: ${it.message}")
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
                    Log.d("FireStore", "Uri: ${downloadUri.toString()}")
                    downloadUri?.let {
                        callback.invoke(it.toString())
                    }

                } else {
                    // Handle failures
                    Log.d("FireStore", "Fail to complete loading avatar!")
                    callback.invoke("")
                }
            }
    }

    fun updateUser(username: String? = null, avatar: String? = null, callback: (ApiResponse<Unit>) -> Unit){
        val newUser = currentUser.copy(
            username = username ?: currentUser.username,
            avatar = avatar ?: currentUser.avatar
        )

        val userMap = hashMapOf(
            "user_id" to newUser.id,
            "username" to newUser.username,
            "email" to newUser.email,
            "home" to newUser.home,
            "work" to newUser.work,
            "avatar" to newUser.avatar
        )

        Log.d("FireStore", "Updating user...")

        FirebaseUtil.getDb().collection("users").document(newUser.id!!)
            .set(userMap)
            .addOnSuccessListener {
                Log.d("FireStore", "Add/Update user successfully")
                currentUser = newUser
                callback.invoke(ApiResponse(null, "Update user successfully!", true))
            }
            .addOnFailureListener { e ->
                Log.d("FireStore", "Fail to add/update user ${e.message}")
                callback.invoke(ApiResponse(null, "Fail: ${e.message}", false))
            }
            .addOnCanceledListener {
                Log.d("FireStore", "Cancel!")
            }
    }

    fun resetToken(){
        currentToken = null
    }
}