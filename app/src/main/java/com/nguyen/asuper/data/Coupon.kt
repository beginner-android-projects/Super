package com.nguyen.asuper.data

data class Coupon (
    var id: String? = null,
    var name: String? = null,
    var code: String? = null,
    var description: String? = null,
    var discount: Long? = null,
    var backgroundImg: String? = null
)