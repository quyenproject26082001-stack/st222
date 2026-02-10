package poster.maker.core.extensions

import android.content.Context
import android.util.Log

fun Context.dLog(content: String) {
    Log.d("nbhieu", content)
}

fun Context.eLog(content: String) {
    Log.e("nbhieu", content)
}

fun Context.iLog(content: String) {
    Log.i("nbhieu", content)
}

fun Context.wLog(content: String) {
    Log.w("nbhieu", content)
}