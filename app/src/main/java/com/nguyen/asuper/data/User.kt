package com.nguyen.asuper.data

data class User (
    var id: String? = null,
    var username: String? = null,
    var email: String? = null,
    var home: Location? = null,
    var work: Location? = null,
    var avatar: String? = null,
    var usedCoupons: HashMap<String, Boolean>? = null,
    var trips: HashMap<String, Boolean>? = null
)