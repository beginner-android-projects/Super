package com.nguyen.asuper.data

import com.google.android.gms.maps.model.LatLng

data class Driver (
    var id: String,
    var name: String? = null,
    var avatar: String? = null,
    var rating: Double? = null,
    var ratingCount: Long = 1,
    var foundLocation: LatLng? = null

)