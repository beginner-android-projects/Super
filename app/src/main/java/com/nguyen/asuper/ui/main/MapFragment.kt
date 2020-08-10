package com.nguyen.asuper.ui.main

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.libraries.places.api.Places
import com.nguyen.asuper.R
import com.nguyen.asuper.data.MapLocation
import com.nguyen.asuper.databinding.FragmentMapBinding
import com.nguyen.asuper.repository.AuthRepository.Companion.currentUser
import com.nguyen.asuper.ui.main.adapter.AutoCompleteAdapter
import com.nguyen.asuper.util.SavedSharedPreferences.currentUserLocation
import com.nguyen.asuper.viewmodels.MainViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.util.*
import kotlin.collections.ArrayList


class MapFragment : Fragment(), OnMapReadyCallback, LocationListener, GoogleMap.OnCameraMoveListener, GoogleMap.OnCameraIdleListener, GoogleMap.OnMapLongClickListener{
    companion object{
        var CURRENT_EDIT_TEXT = "ORIGIN"
        private const val LOCATION_REQUEST_CODE = 1
        private const val MIN_TIME_BTW_UPDATES = 1000 * 3
        private const val MIN_DISTANCE_BTW_UPDATES = 10f
    }

    private lateinit var mMap: GoogleMap
    private lateinit var originEditText: EditText
    private lateinit var destinationEditText: EditText
    private lateinit var binding: FragmentMapBinding

    private var mapFragment: SupportMapFragment? = null
    private var originMarker: Marker? = null
    private var destinationMarker: Marker? = null
    private var driverMarker: Marker? = null
    private var polyline: Polyline? = null
    private var driverPolyline: Polyline? = null
    private var carIconResource: Int = R.drawable.taxi_car_icon

    private var mainHandler: Handler? = null

    private val mainViewModel by viewModel<MainViewModel>()

    private var onStopFlag = false
    private var dragMarkerFlag = false


    @SuppressLint("ClickableViewAccessibility")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        binding = FragmentMapBinding.inflate(inflater, container, false)


        Log.d("Map", "On Create view map fragment!")
        originEditText = binding.origin
        destinationEditText = binding.destination

        binding.currentOption = "taxi"

        Log.d("Map", "current user: $currentUser")

        currentUser.home?.let {
            binding.homeGroup.visibility = View.VISIBLE
            binding.home = it
        } ?: run {
            binding.homeGroup.visibility = View.GONE
        }

        currentUser.work?.let {
            binding.workGroup.visibility = View.VISIBLE
            binding.work = it
        } ?: run {
            binding.workGroup.visibility = View.GONE
        }

        binding.closeHomeAndWork.setOnClickListener {
            binding.homeAndWork.visibility = View.GONE
        }

        binding.closeSearchResult.setOnClickListener {
            binding.searchResultsContainer.visibility = View.GONE
        }

        binding.taxiOption.setOnClickListener{
            binding.currentOption = "taxi"
            mainViewModel.changeCarOption("taxi")
        }

        binding.superOption.setOnClickListener{
            binding.currentOption = "superx"
            mainViewModel.changeCarOption("superx")
        }

        binding.blackOption.setOnClickListener{
            binding.currentOption = "black"
            mainViewModel.changeCarOption("black")
        }

        binding.suvOption.setOnClickListener{
            binding.currentOption = "suv"
            mainViewModel.changeCarOption("suv")
        }

        binding.closeButton.setOnClickListener {
            closeMenu(binding)
        }

        binding.orderButton.setOnClickListener {
            openPaymentMenu(binding)
        }

        binding.backButton.setOnClickListener {
            closePaymentMenu(binding)
        }

        binding.paymentMenu.requestButton.setOnClickListener {
            binding.apply {
                paymentVisibility = false
                backButton.visibility = View.GONE
                mapRadar.setShowCircles(true)
                mapRadar.visibility = View.VISIBLE
                mapRadar.startAnimation()
            }
            mMap.uiSettings.isScrollGesturesEnabled = false
            mMap.moveCamera(CameraUpdateFactory.newLatLng(originMarker?.position))


            mainViewModel.requestRide()
        }

        binding.paymentMenu.switchPaymentButton.setOnClickListener {
            showPaymentDialog()
        }

        binding.paymentMenu.chooseCouponButton.setOnClickListener {
            showCouponDialog()
        }

        binding.driverMenu.cancelButton.setOnClickListener {
            cancelTrip(binding)
        }

        binding.paymentMenu.cancelCoupon.setOnClickListener {
            mainViewModel.cancelCoupon()
        }

        val placesClient = Places.createClient(requireContext())

        mapFragment = childFragmentManager.findFragmentById(R.id.map_view) as SupportMapFragment?
        mapFragment?.getMapAsync(this)

        checkPermission(Manifest.permission.ACCESS_FINE_LOCATION, fun(result: Boolean){
            if(result) getCurrentLocation()
        })

        val recyclerView = binding.autoCompleteRecyclerview
        recyclerView.layoutManager  = LinearLayoutManager(requireContext())

        val adapter = AutoCompleteAdapter(ArrayList(), placesClient, mainViewModel,
            updateOriginEditText = fun(text: String){
                updateOriginEditText(text)
                mainViewModel.resetToken()
            },
            updateDestinationEditText = fun(text: String){
                updateDestinationEditText(text)
                mainViewModel.resetToken()
            })

        recyclerView.adapter = adapter

        mainViewModel.autoSuggestionsList.observe(viewLifecycleOwner, Observer {
            if(onStopFlag) return@Observer
            adapter.switchList(it)
        })

        mainViewModel.originDestination.observe(viewLifecycleOwner, Observer {
            if(onStopFlag) return@Observer
            if(it.origin != null && it.destination != null){

                if(it.origin!!.address == null){
                    it.origin!!.address = getAddress(LatLng(it.origin!!.lat!!, it.origin!!.lng!!))
                }

                if(it.destination!!.address == null){
                    it.destination!!.address = getAddress(LatLng(it.destination!!.lat!!, it.destination!!.lng!!))
                }

                if(it.origin!!.address == it.destination!!.address) {
                    displayError("The pickup location is the same as the destination")
                    Log.d("Map","Origin: ${it.origin!!} ")
                    Log.d("Map", "Destination: ${it.destination!!}")
                    return@Observer
                }
                binding.loadingIcon.visibility = View.VISIBLE
                mainViewModel.getDirection(LatLng(it.origin!!.lat!!, it.origin!!.lng!!), LatLng(it.destination!!.lat!!, it.destination!!.lng!!))
            }
        })

        mainViewModel.direction.observe(viewLifecycleOwner, Observer {
            if(onStopFlag) return@Observer
            drawDirection(it.routeList[0].overviewPolyline.pointList, binding)

            mMap.snapshot { bitmap ->
                mainViewModel.saveTripPreviewBitmap(bitmap)
            }
            binding.loadingIcon.visibility = View.GONE
        })


        mainViewModel.errorMsg.observe(viewLifecycleOwner, Observer {
            (activity as MainActivity?)?.showErrorMessage(it)
            binding.loadingIcon.visibility = View.GONE
            displayError(it)
        })

        mainViewModel.directionDuration.observe(viewLifecycleOwner, Observer {
            if(onStopFlag) return@Observer
            binding.eta.text = it
        })

        mainViewModel.fareEstimated.observe(viewLifecycleOwner, Observer {
            if(onStopFlag) return@Observer
            binding.minFare.text = "$$it"
            binding.paymentMenu.fare = "$$it"
        })

        mainViewModel.fareCouponApplied.observe(viewLifecycleOwner, Observer {
            if(onStopFlag) return@Observer
            binding.minFare.text = "$$it"
            binding.paymentMenu.fare = "$$it"
        })

        mainViewModel.carSize.observe(viewLifecycleOwner, Observer {
            if(onStopFlag) return@Observer
            binding.carSize.text = "$it PEOPLE"
        })

        mainViewModel.driverLatLng.observe(viewLifecycleOwner, Observer {
            if(onStopFlag) return@Observer
            mainViewModel.getDriverDirection()
        })

        mainViewModel.driverDirection.observe(viewLifecycleOwner, Observer {
            if(onStopFlag) return@Observer
            Log.d("Map", "Before driver coming")
            mMap.uiSettings.isScrollGesturesEnabled = true
            simulateDriverComing(it.routeList[0].overviewPolyline.pointList as ArrayList<LatLng>, binding)
        })

        mainViewModel.carIconResource.observe(viewLifecycleOwner, Observer {
            carIconResource = it
        })

        mainViewModel.currentCoupon.observe(viewLifecycleOwner, Observer {
            if(onStopFlag) return@Observer
            if(it == null){
                binding.paymentMenu.chooseCouponButton.text = "Choose coupon"
                binding.paymentMenu.cancelCoupon.visibility = View.GONE
            }
            else {
                binding.paymentMenu.chooseCouponButton.text = it.code
                binding.paymentMenu.cancelCoupon.visibility = View.VISIBLE
            }
        })

        mainViewModel.trip.observe(viewLifecycleOwner, Observer {
            if(onStopFlag) return@Observer
            binding.driverVisibility = true
            binding.driverMenu.cancelable = true
            binding.driverMenu.driver = it.driver
            binding.driverMenu.rating = it.driver?.rating
            binding.mapRadar.stopAnimation()
            binding.mapRadar.visibility = View.GONE
        })

        mainViewModel.paymentMethod.observe(viewLifecycleOwner, Observer {
            Log.d("Map", "Payment: on change $onStopFlag")
            binding.paymentMenu.payment = it
            when(it){
                "Visa/Debit Card" -> binding.paymentMenu.paymentIcon.setImageResource(R.drawable.visa_icon)
                "Google Pay" -> binding.paymentMenu.paymentIcon.setImageResource(R.drawable.google_pay_icon)
                "Apple Pay" -> binding.paymentMenu.paymentIcon.setImageResource(R.drawable.apple_pay_icon)
            }
        })


        workOnClick(binding)
        homeOnClick(binding)
        currentLocationOnClick(binding)

        originEditText.addTextChangedListener(object : TextWatcher{
            override fun afterTextChanged(text: Editable?) {
                if(onStopFlag) return
                if(text.toString().isBlank()) binding.searchResultsContainer.visibility = View.GONE
                else binding.searchResultsContainer.visibility = View.VISIBLE
                mainViewModel.getAutoCompleteSuggestion(text.toString(), placesClient)
                updateOriginLatLng(null)
            }

            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

        })

        destinationEditText.addTextChangedListener(object : TextWatcher{
            override fun afterTextChanged(text: Editable?) {
                if(onStopFlag) return
                if(text.toString().isBlank()) binding.searchResultsContainer.visibility = View.GONE
                else binding.searchResultsContainer.visibility = View.VISIBLE
                mainViewModel.getAutoCompleteSuggestion(text.toString(), placesClient)
                updateDestinationLatLng(null)
            }

            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

        })

        originEditText.setOnTouchListener { _, motionEvent ->
            if(motionEvent.action == MotionEvent.ACTION_UP)  {
                CURRENT_EDIT_TEXT = "Origin"
                binding.homeAndWork.visibility = View.VISIBLE
                binding.searchResultsContainer.visibility = View.VISIBLE
                originEditText.text.toString().let {
                    if(it == "Home" || it == "Work" || it == "Current Location"){
                        originEditText.text.clear()
                    }
                }
            }
            false
        }

        destinationEditText.setOnTouchListener { _, motionEvent ->
            if(motionEvent.action == MotionEvent.ACTION_UP)  {
                CURRENT_EDIT_TEXT = "Destination"
                binding.homeAndWork.visibility = View.VISIBLE
                binding.searchResultsContainer.visibility = View.VISIBLE
                destinationEditText.text.toString().let {
                    if(it == "Home" || it == "Work" || it == "Current Location"){
                        destinationEditText.text.clear()
                    }
                }
            }
            false
        }

        return binding.root
    }


    override fun onStop() {
        onStopFlag = true
        super.onStop()
    }

    override fun onMapReady(googleMap: GoogleMap) {
        if(this::mMap.isInitialized) return

        updateOriginLatLng(newOrigin = currentUserLocation)

        Log.d("Map", "Map Ready: ${currentUserLocation?.address}")
        mMap = googleMap


        try {
            mMap.setMapStyle(
                MapStyleOptions.loadRawResourceStyle(
                    requireContext(), R.raw.style_json
                )
            )

        } catch (e: Exception) {
            Log.d("Map", "Can't find style. Error: ", e)
        }
        val currentLocation = currentUserLocation?.let {
            LatLng(it.lat!!, it.lng!!)
        } ?: run {
            LatLng(0.0, 0.0)
        }

        originMarker = mMap.addMarker(MarkerOptions().position(currentLocation).draggable(false)
                .title("Current location")
                .icon(BitmapDescriptorFactory
                .fromResource(R.drawable.origin_icon)))


        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(originMarker?.position, 15f))
        mMap.setOnMapLongClickListener(this@MapFragment)
        mMap.setOnCameraMoveListener(this@MapFragment)
        mMap.setOnCameraIdleListener(this@MapFragment)
        mMap.uiSettings.isCompassEnabled = false
        mMap.uiSettings.isMapToolbarEnabled = false


    }


    private fun checkPermission(permission: String, callback: (result: Boolean) -> Unit) {
        val request = ContextCompat.checkSelfPermission(
            requireContext(),
            permission
        ) == PackageManager.PERMISSION_GRANTED

        if (!request) {
            requestPermissions(arrayOf(permission), LOCATION_REQUEST_CODE)
        } else {
            callback.invoke(true)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when(requestCode){
            1 -> {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    getCurrentLocation()
                } else {
                    Toast.makeText(requireContext(), "Permission denied!", Toast.LENGTH_SHORT).show()
                    activity?.finish()
                }
            }
        }
    }

    override fun onLocationChanged(location: Location?) {
        location?.let {
            Log.d("Map", "Location changed!")
            currentUserLocation = MapLocation(lat = location.latitude, lng = location.longitude)
            if(mainViewModel.originDestination.value?.origin == null) updateOriginLatLng(MapLocation(lat = location.latitude, lng = location.longitude))
            val currentLocation = LatLng(it.latitude, it.longitude)
            originMarker?.remove()
            originMarker = mMap.addMarker(MarkerOptions().position(currentLocation).title("Current location").icon(BitmapDescriptorFactory.fromResource(R.drawable.origin_icon)))
            mMap.animateCamera(CameraUpdateFactory.newLatLng(currentLocation))
        }
    }

    override fun onStatusChanged(p0: String?, p1: Int, p2: Bundle?) {

    }

    override fun onProviderEnabled(p0: String?) {

    }

    override fun onProviderDisabled(p0: String?) {

    }

    fun displayError(message: String){
        Log.d("Map", "Displaying error!")
        CoroutineScope(Main).launch(){
            (activity as MainActivity?)?.showErrorMessage(message)
            delay(3_000L)
            (activity as MainActivity?)?.hideErrorMessage()
        }
    }

    private fun homeOnClick(binding: FragmentMapBinding){
        val listener = View.OnClickListener {
            when(CURRENT_EDIT_TEXT){
                "Origin" -> {
                    originEditText.setText("Home")
                    originEditText.clearFocus()
                    mainViewModel.updateOriginDestinationLatLng(
                        newOrigin = currentUser.home
                    )
                }
                "Destination" -> {
                    destinationEditText.setText("Home")
                    destinationEditText.clearFocus()
                    mainViewModel.updateOriginDestinationLatLng(
                        newDestination = currentUser.home
                    )
                }
            }
        }

        binding.homeName.setOnClickListener(listener)
        binding.homeAddress.setOnClickListener(listener)
        binding.homeIcon.setOnClickListener(listener)
    }

    private fun workOnClick(binding: FragmentMapBinding){
        val listener = View.OnClickListener {
            when(CURRENT_EDIT_TEXT){
                "Origin" -> {
                    originEditText.setText("Work")
                    originEditText.clearFocus()
                    updateOriginLatLng(currentUser.work)
                    mainViewModel.updateOriginDestinationLatLng(
                        newOrigin = currentUser.work
                    )
                }
                "Destination" -> {
                    destinationEditText.setText("Work")
                    destinationEditText.clearFocus()
                    mainViewModel.updateOriginDestinationLatLng(
                        newDestination = currentUser.work
                    )
                }
            }
        }
        binding.workName.setOnClickListener(listener)
        binding.workAddress.setOnClickListener(listener)
        binding.workIcon.setOnClickListener(listener)
    }

    private fun currentLocationOnClick(binding: FragmentMapBinding){
        Log.d("Map", "Clicking current location")

        val listener = View.OnClickListener {
            getCurrentLocation()
            when(CURRENT_EDIT_TEXT){
                "Origin" -> {
                    originEditText.setText("Current Location")
                    originEditText.clearFocus()
                    updateOriginLatLng(currentUserLocation)
                }
                "Destination" -> {
                    destinationEditText.setText("Current Location")
                    destinationEditText.clearFocus()
                    updateDestinationLatLng(currentUserLocation)
                }
            }
        }

        binding.currentLocationIcon.setOnClickListener(listener)
        binding.currentLocationName.setOnClickListener(listener)
    }

    private fun updateOriginEditText(text: String){
        originEditText.setText(text)
        originEditText.clearFocus()
        destinationEditText.requestFocus()
        CURRENT_EDIT_TEXT = "Destination"
    }

    private fun updateDestinationEditText(text: String){
        destinationEditText.setText(text)
        destinationEditText.clearFocus()
        CURRENT_EDIT_TEXT = "Origin"
    }

    private fun updateOriginLatLng(newOrigin: MapLocation?){
        mainViewModel.updateOriginDestinationLatLng( newOrigin = newOrigin)
    }

    private fun updateDestinationLatLng(newDestination: MapLocation?){
        mainViewModel.updateOriginDestinationLatLng( newDestination = newDestination)
    }

    private fun openMenu(binding: FragmentMapBinding){
        val activity = (activity as MainActivity?)
        binding.homeAndWork.visibility = View.GONE
        binding.searchResultsContainer.visibility = View.GONE
        binding.searchBarGroup.visibility = View.GONE
        binding.closeButton.visibility = View.VISIBLE
        binding.menuGroup.visibility = View.VISIBLE
        activity?.apply {
            hideKeyboard()
            hideActionBar()
        }
    }

    private fun closeMenu(binding: FragmentMapBinding){
        val activity = (activity as MainActivity?)
        binding.searchBarGroup.visibility = View.VISIBLE
        binding.closeButton.visibility = View.GONE
        binding.menuGroup.visibility = View.GONE
        polyline?.remove()
        destinationMarker?.remove()
        activity?.apply {
            showActionBar()
        }
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(originMarker?.position, 15f))
    }

    private fun openPaymentMenu(binding: FragmentMapBinding){
        binding.closeButton.visibility = View.GONE
        binding.menuGroup.visibility = View.GONE
        binding.paymentVisibility = true
        binding.backButton.visibility = View.VISIBLE
    }

    private fun closePaymentMenu(binding: FragmentMapBinding){
        binding.paymentVisibility = false
        binding.backButton.visibility = View.GONE
        openMenu(binding)
    }

    private fun drawDirection(points: List<LatLng>, binding: FragmentMapBinding){
        openMenu(binding)
        originMarker?.remove()
        destinationMarker?.remove()
        originMarker = mMap.addMarker(MarkerOptions().position(points[0]).title("Pick up location").icon(BitmapDescriptorFactory.fromResource(R.drawable.origin_icon)))
        destinationMarker = mMap.addMarker(MarkerOptions().position(points[points.size - 1]).title("Destination").icon(BitmapDescriptorFactory.fromResource(R.drawable.destination_icon)))
        polyline?.remove()
        val options = PolylineOptions()
            .clickable(true)
            .addAll(points)
        polyline= mMap.addPolyline(options)
        polyline?.width = 5f
        polyline?.color = Color.parseColor("#74BFFF")
        val cap = CustomCap(BitmapDescriptorFactory.fromResource(R.drawable.cap_icon), 4f)
        polyline?.startCap = cap
        polyline?.endCap = cap


        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(originMarker?.position, 15f ))
    }

    private fun simulateDriverComing(points: ArrayList<LatLng>, binding: FragmentMapBinding){

        mainHandler = Handler(Looper.getMainLooper())

        mainHandler!!.post(object : Runnable {
            override fun run() {
                driverPolyline?.remove()

                val options = PolylineOptions()
                    .clickable(true)
                    .addAll(points)
                driverPolyline = mMap.addPolyline(options)
                driverPolyline?.width = 5f
                driverPolyline?.color = Color.parseColor("#2E6C31")
                val driverCap = CustomCap(BitmapDescriptorFactory.fromResource(carIconResource), 4f)
                driverPolyline?.startCap = driverCap
                points.removeAt(0)
                if(points.isEmpty()) {
                    driverMarker?.remove()
                    driverMarker =
                        mMap.addMarker(
                            MarkerOptions()
                                .position(originMarker?.position!!)
                                .icon(BitmapDescriptorFactory.fromResource(carIconResource))
                        )

                    binding.driverMenu.cancelable = false
                    showDriverNotifyDialog(polyline?.points as ArrayList<LatLng>, binding)
                    return
                }
                mainHandler!!.postDelayed(this, 2000)
            }
        })
    }


    private fun simulateDriverGoing(points: ArrayList<LatLng>, binding: FragmentMapBinding){

        mainHandler = Handler(Looper.getMainLooper())

        mainHandler!!.post(object : Runnable {
            override fun run() {
                driverMarker?.remove()
                polyline?.remove()
                val options = PolylineOptions()
                    .clickable(true)
                    .addAll(points)
                polyline = mMap.addPolyline(options)
                polyline?.width = 5f
                polyline?.color = Color.parseColor("#74BFFF")
                val driverCap = CustomCap(BitmapDescriptorFactory.fromResource(carIconResource), 4f)
                polyline?.startCap = driverCap
                //mMap.moveCamera(CameraUpdateFactory.newLatLng(points[0]))
                points.removeAt(0)
                if(points.isEmpty()) {
                    mainViewModel.saveTrip()
                    driverMarker?.remove()
                    driverMarker =
                        mMap.addMarker(
                            MarkerOptions()
                                .position(destinationMarker?.position!!)
                                .icon(BitmapDescriptorFactory.fromResource(carIconResource))
                        )
                    resetLayout(binding)
                    showDriverRatingDialog()
                    return
                }
                mainHandler!!.postDelayed(this, 1000)
            }
        })
    }


    private fun showPaymentDialog() {
        val newFragment = PaymentDialogFragment(mainViewModel)
        newFragment.show(childFragmentManager, "payment")
    }

    private fun showCouponDialog() {
        val newFragment = CouponDialogFragment(mainViewModel)
        newFragment.show(childFragmentManager, "coupon")
    }

    private fun showDriverNotifyDialog(points: ArrayList<LatLng>, binding: FragmentMapBinding) {
        val newFragment = DriverNotifyDialogFragment(mainViewModel, fun(){
            simulateDriverGoing(points, binding)
        })
        newFragment.show(childFragmentManager, "driver")
    }

    private fun showDriverRatingDialog(){
        val newFragment = DriverTripSummaryDialogFragment(mainViewModel)
        newFragment.show(childFragmentManager, "summary")
    }

    private fun resetLayout(binding: FragmentMapBinding) {
        binding.driverVisibility = false
        binding.searchBarGroup.visibility = View.VISIBLE
        destinationMarker?.remove()
        driverMarker?.remove()
        driverPolyline?.remove()
        polyline?.remove()
        mainHandler?.removeCallbacksAndMessages(null);
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(originMarker?.position, 15f))
        (activity as MainActivity?)?.showActionBar()
    }

    private fun cancelTrip(binding: FragmentMapBinding) {
        resetLayout(binding)
    }

    override fun onResume() {
        onStopFlag = false
        currentUser.home?.let {
            binding.homeGroup.visibility = View.VISIBLE
            binding.home = it
        } ?: run {
            binding.homeGroup.visibility = View.GONE
        }

        currentUser.work?.let {
            binding.workGroup.visibility = View.VISIBLE
            binding.work = it
        } ?: run {
            binding.workGroup.visibility = View.GONE
        }
        super.onResume()
    }

    private fun getAddress(latLng: LatLng) : String{
        Log.d("Map", "Getting address $latLng")
        if(context == null) return ""
        val geocoder = Geocoder(context, Locale.getDefault())
        val addresses : List<Address>
        try{
            addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)
            if(addresses.size == 1){
                Log.d("Map", "Address ${addresses[0].getAddressLine(0)}")
                return addresses[0].getAddressLine(0)
            }
        } catch (e: Exception){
            Log.d("Map", "Error getting address: ${e.message}")
            displayError(e.message + "")
        }
        return ""
    }

    @SuppressLint("MissingPermission")
    private fun getCurrentLocation(){
        Log.d("Map","Getting current location....")
        val locationRequest = LocationRequest()

        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY


        LocationServices.getFusedLocationProviderClient(requireActivity())
            .requestLocationUpdates(locationRequest, object : LocationCallback() {
                override fun onLocationResult(locationResult: LocationResult?) {
                    if(locationResult != null && locationResult.locations.isNotEmpty()){
                        val newCurrentLocation = MapLocation()
                        val latestIndex = locationResult.locations.size - 1
                        newCurrentLocation.lat = locationResult.locations[latestIndex].latitude
                        newCurrentLocation.lng = locationResult.locations[latestIndex].longitude
                        newCurrentLocation.address = getAddress(LatLng(newCurrentLocation.lat!!, newCurrentLocation.lng!!))

                        Log.d("Map","Updating current location: ${currentUserLocation?.address} to ${newCurrentLocation.address}")
                        if(newCurrentLocation.address == currentUserLocation?.address) return

                        if(originEditText.text.toString() == "Current Location" && !dragMarkerFlag){
                            updateOriginLatLng(MapLocation(lat = newCurrentLocation.lat, lng = newCurrentLocation.lng, address = newCurrentLocation.address))
                            originMarker?.remove()
                            val latLng = LatLng(locationResult.locations[latestIndex].latitude, locationResult.locations[latestIndex].longitude)
                            originMarker = mMap.addMarker(MarkerOptions().position(latLng).title("Current location").icon(BitmapDescriptorFactory.fromResource(R.drawable.origin_icon)))
                            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
                            Log.d("Map","Getting current location: ${newCurrentLocation.address}")
                        }

                        currentUserLocation = newCurrentLocation
                    } else displayError("Cannot get location!")
                    super.onLocationResult(locationResult)
                }

                override fun onLocationAvailability(locationAvailability: LocationAvailability?) {
                    locationAvailability?.isLocationAvailable?.let {
                        if(!it) displayError("Cannot get location!")
                    }
                    super.onLocationAvailability(locationAvailability)
                }
            }, Looper.getMainLooper())
    }

    override fun onCameraMove() {
        if(dragMarkerFlag){
            Log.d("Map","Camera moving....")
            originMarker?.remove()
            // display imageView
            binding.originDrag?.visibility = View.VISIBLE
        }

    }

    override fun onCameraIdle() {
        if(dragMarkerFlag){
            // hiding imageView
            binding.originDrag.visibility = View.GONE
            // customizing map marker with a custom icon
            // and place it on the current map camera position
            val markerOptions = MarkerOptions().position(mMap.cameraPosition.target)
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.origin_icon))
            originMarker = mMap.addMarker(markerOptions)
        }

    }

    override fun onMapLongClick(p0: LatLng?) {
        Log.d("Click", "Long click...")
        if(binding.searchBarGroup.visibility == View.VISIBLE || dragMarkerFlag){
            dragMarkerFlag = dragMarkerFlag.not()
            if(dragMarkerFlag){
                mMap.uiSettings.isZoomGesturesEnabled = false
                Toast.makeText(requireContext(), "You are in dragging mode! Long press again to exit this mode.", Toast.LENGTH_SHORT).show()
                (activity as MainActivity?)?.hideActionBar()
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(originMarker?.position, 18f))
                binding.searchBarGroup.visibility = View.GONE
                originMarker?.remove()
                binding.originDrag.visibility = View.VISIBLE
                Log.d("Click", "Enter dragging mode")
            } else {
                binding.searchBarGroup.visibility = View.VISIBLE
                mMap.uiSettings.isZoomGesturesEnabled = true
                val newLocation = MapLocation()
                (activity as MainActivity?)?.showActionBar()
                newLocation.lat = originMarker?.position?.latitude
                newLocation.lng = originMarker?.position?.longitude
                newLocation.address = getAddress(LatLng(newLocation.lat!!, newLocation.lng!!))
                Log.d("Click", "Exit dragging mode ${newLocation.address}")
                originEditText.setText(newLocation.address)
                binding.searchResultsContainer.visibility = View.GONE
                mainViewModel.updateOriginDestinationLatLng(newOrigin = newLocation)
                Log.d("Click", "Exit dragging mode")
            }
        }
    }


}