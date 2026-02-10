package poster.maker.core.extensions

import android.app.Activity
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log
import poster.maker.core.utils.DataLocal
import poster.maker.core.utils.state.HandleState



fun Context.initNetworkMonitor() {
    val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            Log.d("nbhieu", "onAvailable")
            DataLocal.isConnectInternet.postValue(true)
        }
        override fun onLost(network: Network) {
            Log.d("nbhieu", "onLost")
            DataLocal.isConnectInternet.postValue(false)
        }
    }
    val request = NetworkRequest.Builder().build()
    connectivityManager.registerNetworkCallback(request, networkCallback)
}

