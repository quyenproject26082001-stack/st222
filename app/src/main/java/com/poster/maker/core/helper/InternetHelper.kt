package poster.maker.core.helper

import android.app.Activity
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import poster.maker.core.utils.state.HandleState

object InternetHelper {
    fun checkInternet(context: Context, state : ((HandleState) -> Unit) = {}){
        if (isInternetAvailable(context)){
            state.invoke(HandleState.SUCCESS)
        }else{
            state.invoke(HandleState.FAIL)
        }
    }

    fun checkInternet(context: Context) : Boolean{
        return isInternetAvailable(context)
    }

    fun isInternetAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}