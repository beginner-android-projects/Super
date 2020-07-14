package com.nguyen.asuper.data

import android.net.Uri

data class User (
    var id: String? = null,
    var username: String? = null,
    var email: String? = null,
    var home: Location? = null,
    var work: Location? = null,
    var avatar: String? = null
)