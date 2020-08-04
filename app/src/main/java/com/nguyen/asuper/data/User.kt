package com.nguyen.asuper.data

data class User (
    var id: String? = null,
    var username: String? = null,
    var email: String? = null,
    var home: MapLocation? = null,
    var work: MapLocation? = null,
    var avatar: String? = null
)