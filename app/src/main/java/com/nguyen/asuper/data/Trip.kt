package com.nguyen.asuper.data

import android.graphics.Bitmap
import com.akexorcist.googledirection.model.Direction
import java.util.*

data class Trip (
    val id: String,
    var date: Date,
    var driver: Driver,
    var direction: Direction,
    var originDestination: OriginDestination,
    var fare: Double,
    var carType: String,
    var couponUsed: Coupon? = null,
    var paymentMethod: String,
    var preview: Bitmap? = null
)