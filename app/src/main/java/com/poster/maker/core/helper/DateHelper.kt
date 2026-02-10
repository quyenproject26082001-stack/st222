package poster.maker.core.helper

import android.annotation.SuppressLint
import java.text.SimpleDateFormat
import java.util.Date

object DateHelper {
    @SuppressLint("SimpleDateFormat")
    fun getCurrentDate(): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd")
        return dateFormat.format(Date())
    }
}