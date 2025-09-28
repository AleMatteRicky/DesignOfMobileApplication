package com.example.augmentedrealityglasses.internet

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class InternetConnectionManager(private val context: Context) {

    var status by mutableStateOf<ConnectivityStatus>(ConnectivityStatus.Unavailable)
        private set

    private val connectivityManager = context.getSystemService(ConnectivityManager::class.java)

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            updateForNetwork(network)
        }

        override fun onLost(network: Network) {
            status = ConnectivityStatus.Unavailable
        }

        override fun onCapabilitiesChanged(
            network: Network,
            networkCapabilities: NetworkCapabilities
        ) {
            updateFromCapabilities(networkCapabilities)
        }
    }

    private fun updateForNetwork(network: Network) {
        val networkCapabilities = connectivityManager.getNetworkCapabilities(network)
        if (networkCapabilities == null) {
            status = ConnectivityStatus.Unavailable
            return
        }
        updateFromCapabilities(networkCapabilities)
    }

    private fun updateFromCapabilities(networkCapabilities: NetworkCapabilities) {
        val isValidated =
            networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        status =
            if (isValidated) ConnectivityStatus.ValidatedInternet else ConnectivityStatus.Other
    }

    fun start() {
        connectivityManager.registerDefaultNetworkCallback(networkCallback)
    }

    fun stop() {
        try {
            connectivityManager.unregisterNetworkCallback(networkCallback)
        } catch (_: Exception) {
        }
    }
}

sealed class ConnectivityStatus{
    data object Unavailable : ConnectivityStatus()
    data object ValidatedInternet : ConnectivityStatus()
    data object Other : ConnectivityStatus()
}
