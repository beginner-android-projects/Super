package com.nguyen.asuper.ui.main

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
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
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.libraries.places.api.Places
import com.nguyen.asuper.R
import com.nguyen.asuper.databinding.FragmentMapBinding
import com.nguyen.asuper.ui.MainActivity
import com.nguyen.asuper.ui.main.adapter.AutoCompleteAdapter
import com.nguyen.asuper.util.SavedSharedPreferences.currentLoggedUser
import com.nguyen.asuper.util.SavedSharedPreferences.currentUserLatitude
import com.nguyen.asuper.util.SavedSharedPreferences.currentUserLongitude
import com.nguyen.asuper.viewmodels.MainViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel


class MapFragment : Fragment(), OnMapReadyCallback, LocationListener {
    companion object{
        var CURRENT_EDIT_TEXT = "ORIGIN"
        private const val LOCATION_REQUEST_CODE = 1
        private const val MIN_TIME_BTW_UPDATES = 1000 * 3
        private const val MIN_DISTANCE_BTW_UPDATES = 10f
    }

    private lateinit var mMap: GoogleMap
    private lateinit var originEditText: EditText
    private lateinit var destinationEditText: EditText


    private var mapFragment: SupportMapFragment? = null
    private var originMarker: Marker? = null
    private var destinationMarker: Marker? = null
    private var driverMarker: Marker? = null
    private var polyline: Polyline? = null
    private var driverPolyline: Polyline? = null
    private var carIconResource: Int = R.drawable.taxi_car_icon

    private var mainHandler: Handler? = null

    private val mainViewModel by viewModel<MainViewModel>()



    @SuppressLint("ClickableViewAccessibility")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val binding = FragmentMapBinding.inflate(inflater, container, false)

        Log.d("Map", "On Create view map fragment!")
        originEditText = binding.origin
        destinationEditText = binding.destination

        binding.currentOption = "taxi"

        currentLoggedUser?.home?.let {
            binding.homeGroup.visibility = View.VISIBLE
            binding.home = it
        } ?: run {
            binding.homeGroup.visibility = View.GONE
        }

        currentLoggedUser?.work?.let {
            binding.workGroup.visibility = View.VISIBLE
            binding.work = it
        } ?: run {
            binding.workGroup.visibility = View.GONE
        }

        binding.closeHomeAndWork.setOnClickListener {
            binding.homeAndWork.visibility = View.GONE
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
            //findNavController().navigate(R.id.coupon_fragment)
            showCouponDialog()
        }

        binding.driverMenu.cancelButton.setOnClickListener {
            cancelTrip(binding)
        }


        val placesClient = Places.createClient(requireContext())

        mapFragment = childFragmentManager.findFragmentById(R.id.map_view) as SupportMapFragment?
        mapFragment?.getMapAsync(this)

        checkPermission(Manifest.permission.ACCESS_FINE_LOCATION, fun(result: Boolean){
            if(result) trackLocation()
        })

        val recyclerView = binding.autoCompleteRecyclerview
        recyclerView.layoutManager  = LinearLayoutManager(requireContext())

        val adapter = AutoCompleteAdapter(ArrayList(), placesClient, mainViewModel,
            updateOriginEditText = fun(text: String){
                updateOriginEditText(text)
            },
            updateDestinationEditText = fun(text: String){
                updateDestinationEditText(text)
            })

        recyclerView.adapter = adapter

        mainViewModel.autoSuggestionsList.observe(viewLifecycleOwner, Observer {
            adapter.switchList(it)
        })

        mainViewModel.originDestination.observe(viewLifecycleOwner, Observer {
            if(it.origin != null && it.destination != null){
                binding.loadingIcon.visibility = View.VISIBLE
                mainViewModel.getDirection(it.origin!!, it.destination!!)
            }
        })

        mainViewModel.direction.observe(viewLifecycleOwner, Observer {
            drawDirection(it.routeList[0].overviewPolyline.pointList, binding)

            val builder: LatLngBounds.Builder = LatLngBounds.builder()
            builder.include(originMarker?.position)
            builder.include(destinationMarker?.position)
            val bounds = builder.build()

            val cameraUpdate = CameraUpdateFactory.newLatLngBounds(bounds, 0)
            mMap.animateCamera(cameraUpdate)
            mMap.snapshot { bitmap ->
                mainViewModel.saveTripPreviewBitmap(bitmap)
            }
            binding.loadingIcon.visibility = View.GONE
        })

        mainViewModel.directionStatus.observe(viewLifecycleOwner, Observer {
            binding.isError = it.not()
        })

        mainViewModel.directionMsg.observe(viewLifecycleOwner, Observer {
            binding.mapErrorBar.errorMsg = it
        })

        mainViewModel.directionDuration.observe(viewLifecycleOwner, Observer {
            binding.eta.text = it
        })

        mainViewModel.fareEstimated.observe(viewLifecycleOwner, Observer {
            binding.minFare.text = "$$it"
            binding.paymentMenu.fare = "$$it"
        })

        mainViewModel.fareCouponApplied.observe(viewLifecycleOwner, Observer {
            binding.minFare.text = "$$it"
            binding.paymentMenu.fare = "$$it"
        })

        mainViewModel.carSize.observe(viewLifecycleOwner, Observer {
            binding.carSize.text = "$it PEOPLE"
        })

        mainViewModel.driverLatLng.observe(viewLifecycleOwner, Observer {
            mainViewModel.getDriverDirection()
        })

        mainViewModel.driverDirection.observe(viewLifecycleOwner, Observer {
            mMap.uiSettings.isScrollGesturesEnabled = true
            simulateDriverComing(it.routeList[0].overviewPolyline.pointList as ArrayList<LatLng>, binding)
        })

        mainViewModel.carIconResource.observe(viewLifecycleOwner, Observer {
            carIconResource = it
        })

        mainViewModel.currentCoupon.observe(viewLifecycleOwner, Observer {
            binding.paymentMenu.chooseCouponButton.text = it.code
        })

        mainViewModel.trip.observe(viewLifecycleOwner, Observer {
            binding.driverVisibility = true
            binding.driverMenu.cancelable = true
            binding.driverMenu.driver = it.driver
            binding.mapRadar.stopAnimation()
            binding.mapRadar.visibility = View.GONE
        })

        mainViewModel.paymentMethod.observe(viewLifecycleOwner, Observer {
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

    override fun onMapReady(googleMap: GoogleMap) {
        if(this::mMap.isInitialized) return
        Log.d("Map", "Map Ready!")
        mMap = googleMap
        mMap.setMinZoomPreference(15f)
        try {
            googleMap.setMapStyle(
                MapStyleOptions.loadRawResourceStyle(
                    requireContext(), R.raw.style_json
                )
            )

        } catch (e: Exception) {
            Log.d("Map", "Can't find style. Error: ", e)
        }
        Log.d("Map", "Ready: $currentUserLatitude $currentUserLongitude")
        val currentLocation = LatLng(currentUserLatitude, currentUserLongitude)
        originMarker = mMap.addMarker(MarkerOptions().position(currentLocation).title("Current location").icon(BitmapDescriptorFactory.fromResource(R.drawable.origin_icon)))
        mMap.animateCamera(CameraUpdateFactory.newLatLng(currentLocation))
    }


    @SuppressLint("MissingPermission")
    private fun trackLocation(){

        Log.d("Map", "Tracking Location....")

        val locationManager =  activity?.getSystemService(AppCompatActivity.LOCATION_SERVICE) as LocationManager

        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, MIN_TIME_BTW_UPDATES.toLong(), MIN_DISTANCE_BTW_UPDATES, this)
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
                    trackLocation()
                } else {
                    Toast.makeText(requireContext(), "Permission denied!", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onLocationChanged(location: Location?) {
        location?.let {
            currentUserLatitude = it.latitude
            currentUserLongitude = it.longitude
            if(mainViewModel.originDestination.value?.origin == null) updateOriginLatLng(LatLng(currentUserLatitude, currentUserLongitude))
            Log.d("Map", "changed: $currentUserLatitude $currentUserLongitude")
            val currentLocation = LatLng(it.latitude, it.longitude)
            originMarker?.remove()
            originMarker = mMap.addMarker(MarkerOptions().position(currentLocation).title("Current location").icon(BitmapDescriptorFactory.fromResource(R.drawable.origin_icon)))
            mMap.animateCamera(CameraUpdateFactory.newLatLng(currentLocation))
        }
    }

    override fun onStatusChanged(p0: String?, p1: Int, p2: Bundle?) {
        TODO("Not yet implemented")
    }

    override fun onProviderEnabled(p0: String?) {
        TODO("Not yet implemented")
    }

    override fun onProviderDisabled(p0: String?) {
        TODO("Not yet implemented")
    }



    private fun homeOnClick(binding: FragmentMapBinding){
        val listener = View.OnClickListener {
            when(CURRENT_EDIT_TEXT){
                "Origin" -> {
                    originEditText.setText("Home")
                    originEditText.clearFocus()
                    mainViewModel.updateOriginDestinationLatLng(
                        newOrigin = currentLoggedUser?.home?.let { home -> LatLng(home.lat!!, home.lng!! ) }
                    )
                }
                "Destination" -> {
                    destinationEditText.setText("Home")
                    destinationEditText.clearFocus()
                    mainViewModel.updateOriginDestinationLatLng(
                        newDestination = currentLoggedUser?.home?.let { home -> LatLng(home.lat!!, home.lng!! ) }
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
                    updateOriginLatLng(currentLoggedUser?.work?.let { work -> LatLng(work.lat!!, work.lng!! ) })
                    mainViewModel.updateOriginDestinationLatLng(
                        newOrigin = currentLoggedUser?.work?.let { work -> LatLng(work.lat!!, work.lng!! ) }
                    )
                }
                "Destination" -> {
                    destinationEditText.setText("Work")
                    destinationEditText.clearFocus()
                    mainViewModel.updateOriginDestinationLatLng(
                        newDestination = currentLoggedUser?.work?.let { work -> LatLng(work.lat!!, work.lng!! ) }
                    )
                }
            }
        }
        binding.workName.setOnClickListener(listener)
        binding.workAddress.setOnClickListener(listener)
        binding.workIcon.setOnClickListener(listener)
    }

    private fun currentLocationOnClick(binding: FragmentMapBinding){
        val listener = View.OnClickListener {
            when(CURRENT_EDIT_TEXT){
                "Origin" -> {
                    originEditText.setText("Current Location")
                    originEditText.clearFocus()
                    updateOriginLatLng(LatLng(currentUserLatitude, currentUserLongitude))
                }
                "Destination" -> {
                    destinationEditText.setText("Current Location")
                    destinationEditText.clearFocus()
                    updateDestinationLatLng(LatLng(currentUserLatitude, currentUserLongitude))
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

    private fun updateOriginLatLng(newOrigin: LatLng?){
        mainViewModel.updateOriginDestinationLatLng( newOrigin = newOrigin)
    }

    private fun updateDestinationLatLng(newDestination: LatLng?){
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
        mMap.animateCamera(CameraUpdateFactory.newLatLng(originMarker?.position))
        (activity as MainActivity?)?.showActionBar()
    }

    private fun cancelTrip(binding: FragmentMapBinding) {
        resetLayout(binding)
    }





}