package com.example.augmentedrealityglasses.weather.state

import android.location.Location

sealed class GeolocationResult {
    data class Success(val data: Location) : GeolocationResult()
    object NoPermissionGranted : GeolocationResult()
    object NotAvailable : GeolocationResult()
    data class Error(val exception: Throwable) : GeolocationResult()
}