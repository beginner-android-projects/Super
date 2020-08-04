package com.nguyen.asuper.data

data class ApiResponse<T> (
    var code: Int? = null,
    var message: String? = "",
    var status: Boolean,
    var data: T? =  null
)