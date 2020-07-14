package com.nguyen.asuper.data

import com.google.android.gms.maps.model.LatLng

data class Driver (
    var name: String? = null,
    var avatar: String? = null,
    var rating: Double? = null,
    var foundLocation: LatLng? = null

)