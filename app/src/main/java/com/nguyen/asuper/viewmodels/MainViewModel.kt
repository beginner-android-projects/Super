package com.nguyen.asuper.viewmodels

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
import com.nguyen.asuper.data.ApiResponse
import com.nguyen.asuper.data.Driver
import com.nguyen.asuper.data.OriginDestination
import com.nguyen.asuper.databinding.FragmentMapBinding
import com.nguyen.asuper.repository.MainRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class MainViewModel(private val repository: MainRepository) : ViewModel(){

    private val autoSuggestionsResponse: MutableLiveData<ApiResponse<List<AutocompletePrediction>>> = MutableLiveData()
    private val directionResponse: MutableLiveData<ApiResponse<Direction>> = MutableLiveData()
    private val findDriverResponse: MutableLiveData<ApiResponse<Driver>> = MutableLiveData()
    private val driverDirectionResponse: MutableLiveData<ApiResponse<Direction>> = MutableLiveData()

    val originDestination: MutableLiveData<OriginDestination> = MutableLiveData(OriginDestination())
    val autoSuggestionsList: LiveData<List<AutocompletePrediction>> = Transformations.map(autoSuggestionsResponse){
        it.data
    }
    val direction: LiveData<List<LatLng>> = Transformations.map(directionResponse){
        it.data?.routeList?.get(0)?.overviewPolyline?.pointList
    }
    val directionStatus: LiveData<Boolean> = Transformations.map(directionResponse){
        it.status
    }

    val directionMsg: LiveData<String> = Transformations.map(directionResponse){
        it.message
    }

    val directionDuration: LiveData<String> = Transformations.map(directionResponse){
        timeConverter(it.data?.routeList?.get(0)?.totalDuration)
    }

    private val minutesDuration: MutableLiveData<Int> = MutableLiveData()

    val fareEstimated: MutableLiveData<Double> = MutableLiveData()

    val carSize: MutableLiveData<Int> = MutableLiveData()

    val driver: LiveData<Driver> = Transformations.map(findDriverResponse){
        it.data
    }

    val driverLatLng: LiveData<LatLng> = Transformations.map(findDriverResponse){
        it.data?.foundLocation
    }

    val driverDirection: LiveData<Direction> = Transformations.map(driverDirectionResponse){
        it.data
    }

    val carIconResource: MutableLiveData<Int> = MutableLiveData(R.drawable.taxi_car_icon)

    fun getAutoCompleteSuggestion(query: String, placesClient: PlacesClient){
        repository.getAutocompleteSuggestions(query, placesClient, fun(response: ApiResponse<List<AutocompletePrediction>>){
            autoSuggestionsResponse.value = response
        })
    }

    fun getOriginLatLng(placeId: String, placesClient: PlacesClient){
        repository.getPlaceLatLng(placeId, placesClient, fun(response: ApiResponse<Place>){
            response.status?.let{
                if(it) {
                    Log.d("Map", "Before getting direction")
                    val temp = OriginDestination(response.data?.latLng, originDestination.value?.destination)
                    originDestination.value = temp
                }
            }
        })
    }

    fun getDestinationLatLng(placeId: String, placesClient: PlacesClient){
        repository.getPlaceLatLng(placeId, placesClient, fun(response: ApiResponse<Place>){
            response.status?.let{
                if(it) {
                    Log.d("Map", "Before getting direction")
                    val temp = OriginDestination(originDestination.value?.origin, response.data?.latLng)
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

    fun updateOriginDestinationLatLng(newOrigin: LatLng? = originDestination.value?.origin, newDestination: LatLng? = originDestination.value?.destination){
        val temp = OriginDestination(newOrigin, newDestination)
        originDestination.value = temp
    }

    fun changeCarOption(option: String){
        when(option){
            "taxi" -> {
                carSize.value = 4
                fareEstimated.value = (minutesDuration.value)?.toDouble()
                carIconResource.value = R.drawable.taxi_car_icon
            }
            "superx" -> {
                carSize.value = 4
                fareEstimated.value = (minutesDuration.value?.times(2))?.toDouble()
                carIconResource.value = R.drawable.superx_car_icon
            }
            "black" -> {
                carSize.value = 6
                fareEstimated.value = (minutesDuration.value?.times(4))?.toDouble()
                carIconResource.value = R.drawable.black_car_icon
            }
            "suv" -> {
                carSize.value = 8
                fareEstimated.value = (minutesDuration.value?.times(6))?.toDouble()
                carIconResource.value = R.drawable.suv_car_icon
            }
        }
    }

    fun findDriver(){
        originDestination.value?.origin?.let {
            repository.findDriver(it, fun(response: ApiResponse<Driver>){
                CoroutineScope(Main).launch {
                    findDriverResponse.value = response
                }

            })
        }
    }

    fun getDriverDirection(){
        driverLatLng.value?.let{
            repository.getDirection(it, originDestination.value?.origin!!, fun(response: ApiResponse<Direction>){
                Log.d("Map", "Direction response: $response")
                response.status?.let {status ->
                    if(status) driverDirectionResponse.value = response
                }
            })
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

}

