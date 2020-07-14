package com.nguyen.asuper.data

import com.google.android.gms.maps.model.LatLng

data class OriginDestination(
    var origin: LatLng? = null,
    var destination: LatLng? = null
)