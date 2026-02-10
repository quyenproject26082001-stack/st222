package poster.maker.core.helper

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Resources
import android.util.TypedValue

object UnitHelper {
    fun spToPx(spVal: Float): Int {
        val r = Resources.getSystem()
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, spVal, r.displayMetrics).toInt()
    }

    fun pxToDpInt(context: Context, px: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, px.toFloat(), context.resources.displayMetrics).toInt()
    }

    fun pxToDpFloat(context: Context, px: Int): Float {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, px.toFloat(), context.resources.displayMetrics).toFloat()
    }

    fun dpToPx(context: Context, dp: Int): Int {
        return (dp * context.resources.displayMetrics.density).toInt()
    }
    fun dpToPx(context: Context, dp: Float): Float {
        return (dp * context.resources.displayMetrics.density).toFloat()
    }
    @SuppressLint("DefaultLocale")
    fun formatFileSize(sizeInBytes: Long): String {
        return when {
            sizeInBytes < 1024 -> "$sizeInBytes Bytes"
            sizeInBytes < 1024 * 1024 -> String.format("%.1f KB", sizeInBytes / 1024.0)
            sizeInBytes < 1024 * 1024 * 1024 -> String.format("%.1f MB", sizeInBytes / (1024.0 * 1024))
            else -> String.format("%.1f GB", sizeInBytes / (1024.0 * 1024 * 1024))
        }
    }
}

