package com.nguyen.asuper.viewmodels

import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import com.akexorcist.googledirection.model.Direction
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.model.AutocompletePrediction
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.PlacesClient
import com.nguyen.asuper.R
import com.nguyen.asuper.data.*
import com.nguyen.asuper.repository.MainRepository
import java.util.concurrent.TimeUnit

class MainViewModel(private val repository: MainRepository) : ViewModel(){

    private val autoSuggestionsResponse: MutableLiveData<ApiResponse<List<AutocompletePrediction>>> = MutableLiveData()
    private val directionResponse: MutableLiveData<ApiResponse<Direction>> = MutableLiveData()
    private val requestRideResponse: MutableLiveData<ApiResponse<Trip>> = MutableLiveData()
    private val driverDirectionResponse: MutableLiveData<ApiResponse<Direction>> = MutableLiveData()
    private val couponListResponse: MutableLiveData<ApiResponse<List<Coupon>>> = MutableLiveData()
    private val submitRatingResponse: MutableLiveData<ApiResponse<Unit>> = MutableLiveData()
    private val tripHistoryResponse: MutableLiveData<ApiResponse<List<Trip>>> = MutableLiveData()

    val originDestination: MutableLiveData<OriginDestination> = MutableLiveData(OriginDestination())
    val autoSuggestionsList: LiveData<List<AutocompletePrediction>> = Transformations.map(autoSuggestionsResponse){
        it.data
    }
    val direction: LiveData<Direction> = Transformations.map(directionResponse){
        it.data
    }
    val directionStatus: LiveData<Boolean> = Transformations.map(directionResponse){
        it.status
    }

    val directionMsg: LiveData<String> = Transformations.map(directionResponse){
        it.message
    }

    val errorMsg: MutableLiveData<String> = MutableLiveData()

    val directionDuration: LiveData<String> = Transformations.map(directionResponse){
        timeConverter(it.data?.routeList?.get(0)?.totalDuration)
    }

    private val minutesDuration: MutableLiveData<Int> = MutableLiveData()

    val fareEstimated: MutableLiveData<Double> = MutableLiveData()

    val carSize: MutableLiveData<Int> = MutableLiveData()
    private val carType: MutableLiveData<String> = MutableLiveData("Taxi")


    val trip: LiveData<Trip> = Transformations.map(requestRideResponse){
        Log.d("Trip", "Trip")
        it.data
    }

    val driverLatLng: LiveData<LatLng> = Transformations.map(requestRideResponse){
        it.data?.driver?.foundLocation
    }

    val driverDirection: LiveData<Direction> = Transformations.map(driverDirectionResponse){
        it.data
    }

    val carIconResource: MutableLiveData<Int> = MutableLiveData(R.drawable.taxi_car_icon)

    val paymentMethod: MutableLiveData<String> = MutableLiveData("Visa")

    val couponList: LiveData<List<Coupon>> = Transformations.map(couponListResponse){
        it.data
    }

    val fareCouponApplied: MutableLiveData<Double> = MutableLiveData()

    val currentCoupon: MutableLiveData<Coupon> = MutableLiveData()

    val submitRatingStatus: LiveData<Boolean> = Transformations.map(submitRatingResponse){
        it.status
    }

    val tripsList : LiveData<List<Trip>> = Transformations.map(tripHistoryResponse){
        it.data
    }

    val currentTripPreviewBitmap: MutableLiveData<Bitmap> = MutableLiveData()

    fun getAutoCompleteSuggestion(query: String, placesClient: PlacesClient){
        repository.getAutocompleteSuggestions(query, placesClient, fun(response: ApiResponse<List<AutocompletePrediction>>){
            autoSuggestionsResponse.value = response
        })
    }

    fun getOriginLatLng(placeId: String, placesClient: PlacesClient){
        repository.getPlaceLatLng(placeId, placesClient, fun(response: ApiResponse<Place>){
            if(response.status) {
                response.data?.let {
                    val origin = MapLocation(it.id, it.name, it.address, it.latLng?.latitude, it.latLng?.longitude)
                    val temp = OriginDestination(origin, originDestination.value?.destination)
                    originDestination.value = temp
                }
            }
        })
    }

    fun getDestinationLatLng(placeId: String, placesClient: PlacesClient){
        repository.getPlaceLatLng(placeId, placesClient, fun(response: ApiResponse<Place>){
            if(response.status){
                response.data?.let {
                    val destination = MapLocation(it.id, it.name, it.address, it.latLng?.latitude, it.latLng?.longitude)
                    val temp = OriginDestination(originDestination.value?.origin, destination)
                    originDestination.value = temp
                }
            }
        })
    }

    fun getDirection(origin: LatLng, destination: LatLng){
        repository.getDirection(origin, destination, fun(response: ApiResponse<Direction>){
            Log.d("Map", "Direction response: $response")
            response.status?.let {
                if(it) directionResponse.value = response
                changeCarOption("taxi")
            }
        })
    }

    fun updateOriginDestinationLatLng(newOrigin: MapLocation? = originDestination.value?.origin, newDestination: MapLocation? = originDestination.value?.destination){
        val temp = OriginDestination(newOrigin, newDestination)
        originDestination.value = temp
    }

    fun changeCarOption(option: String){
        when(option){
            "taxi" -> {
                carType.value = "Taxi"
                carSize.value = 4
                val duration = if(minutesDuration.value != null) minutesDuration.value else 0
                fareEstimated.value = duration?.toDouble()
                carIconResource.value = R.drawable.taxi_car_icon
            }
            "superx" -> {
                carType.value = "SuperX"
                carSize.value = 4
                val duration = if(minutesDuration.value != null) minutesDuration.value else 0
                fareEstimated.value = (duration?.times(2))?.toDouble()
                carIconResource.value = R.drawable.superx_car_icon
            }
            "black" -> {
                carType.value = "Black"
                carSize.value = 6
                val duration = if(minutesDuration.value != null) minutesDuration.value else 0
                fareEstimated.value = (duration?.times(4))?.toDouble()
                carIconResource.value = R.drawable.black_car_icon
            }
            "suv" -> {
                carType.value = "SUV"
                carSize.value = 8
                val duration = if(minutesDuration.value != null) minutesDuration.value else 0
                fareEstimated.value = (duration?.times(6))?.toDouble()
                carIconResource.value = R.drawable.suv_car_icon
            }
        }
    }


    fun requestRide(){
        val fare = if(currentCoupon.value != null) fareCouponApplied.value!! else fareEstimated.value!!
        repository.requestRide(
            originDestination = originDestination.value!!,
            fare =  fare,
            direction = direction.value!!,
            paymentMethod = paymentMethod.value!!,
            carType = carType.value!!,
            coupon = currentCoupon.value,
            callback = fun(response: ApiResponse<Trip>){
                if(response.status) {
                    requestRideResponse.value = response
                    currentCoupon.value = null
                } else errorMsg.value = response.message
            }
        )
    }

    fun getDriverDirection(){
        driverLatLng.value?.let{
            originDestination.value?.origin?.let {origin ->
                repository.getDirection(it, LatLng(origin.lat!!, origin.lng!!), fun(response: ApiResponse<Direction>){
                    Log.d("Map", "Direction response: $response")
                    if(response.status) driverDirectionResponse.value = response
                })
            }

        }
    }

    private fun timeConverter(seconds: Long?) : String{
        if (seconds == null) return ""
        if (seconds == 0.toLong()) return "0 minute"
        minutesDuration.value = TimeUnit.SECONDS.toMinutes(seconds).toInt()
        Log.d("Map", "Convert $seconds")
        val hours = if(seconds > 3600) TimeUnit.SECONDS.toHours(seconds) else 0
        val minutes= if (seconds > 3600)  TimeUnit.SECONDS.toMinutes(seconds - 3600 * hours) else TimeUnit.SECONDS.toMinutes(seconds)
        val hourString: String = if(hours > 0) "$hours hours " else ""
        val minuteString: String = if(minutes > 0) "$minutes minutes " else ""

        return "$hourString$minuteString"
    }

    fun setPaymentMethod(method: String){
        paymentMethod.value = method
    }

    fun getCoupons(){
        repository.getCoupons {
            couponListResponse.value = it
        }
    }

    fun pickCoupon(coupon: Coupon){
        currentCoupon.value = coupon
        val newFare = fareEstimated.value?.times(100.minus(coupon.discount!!))?.div(100)
        fareCouponApplied.value = newFare
    }

    fun saveTripPreviewBitmap(preview: Bitmap){
        currentTripPreviewBitmap.value = preview
    }

    fun saveTrip(){
        trip.value?.let {trip ->
            repository.saveTripToDatabase(trip, currentTripPreviewBitmap.value) {
                if(!it.status){
                    errorMsg.value = it.message
                }
            }
        }

    }

    fun submitRating(rating: Double){
        Log.d("FireStore", "HERE")
        trip.value?.driver?.let { driver ->
            Log.d("FireStore", "HERE2")
            repository.submitRating(rating, driver){
                submitRatingResponse.value = it
                if(it.status!!) errorMsg.value = it.message
            }
        }

    }

    fun getTripHistory(){
        repository.getTripHistory {
            tripHistoryResponse.value = it
        }
    }

    fun saveAvatar(userId: String?, uri: Uri?){
        if(userId == null || uri == null){
            errorMsg.value = "Can't save avatar! Try again!"
            return
        }
        repository.uploadAvatar(userId, uri){
            repository.updateUser(avatar = it) {response ->
                if(!response.status){
                    errorMsg.value = response.message
                }
            }
        }
    }

    fun saveUserName(id: String?, newName: String) {
        repository.updateUser(username = newName) {response ->
            if(!response.status){
                errorMsg.value = response.message
            }
        }
    }


//    fun reset() {
//        autoSuggestionsResponse.value = null
//        directionResponse.value = null
//        requestRideResponse.value = null
//        driverDirectionResponse.value = null
//        couponListResponse.value = null
//        submitRatingResponse.value = null
//        tripHistoryResponse.value = null
//        originDestination.value = null
//        errorMsg.value = null
//        minutesDuration.value = null
//        fareEstimated.value = null
//        carSize.value = null
//        carType.value = null
//        carIconResource.value = null
//        paymentMethod.value = null
//        fareCouponApplied.value = null
//        currentCoupon.value = null
//    }

}

