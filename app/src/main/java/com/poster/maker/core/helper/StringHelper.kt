package poster.maker.core.helper

import android.content.Context
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import androidx.core.content.res.ResourcesCompat
import poster.maker.core.custom.text.CustomTypefaceSpan
import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.Locale

object StringHelper {
    fun generateRandomImageFileName(): String {
        val randomNumber = (100000000000..999999999999).random()
        return "IMG_$randomNumber.png"
    }
    fun generateRandomVideoFileName(): String {
        val randomNumber = (100000000000..999999999999).random()
        return "VD_$randomNumber.mp4"
    }

    fun generateRandomString(length: Int = 12): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
        return (1..length).map { chars.random() }.joinToString("")
    }

    fun formatNumber(input: String): String {
        val number = input.toLongOrNull() ?: return input
        val formatter = NumberFormat.getInstance(Locale.GERMANY) as DecimalFormat
        return formatter.format(number)
    }

    internal fun upperFirstCharacter(str: String): String {
        return str.capitalize(Locale.ROOT)
    }

    internal fun convertToLowerCase(input: String): String {
        return input.lowercase()
    }

    internal fun formatDecimal(number: Double, decimalPlaces: Int): String {
        val pattern = "#." + "0".repeat(decimalPlaces)
        val decimalFormat = DecimalFormat(pattern)
        return decimalFormat.format(number)
    }

    fun changeColor(
        context: Context,
        text: String,
        color: Int,
        fontfamily: Int,
    ): SpannableString {
        val spannableString = SpannableString(text)
        spannableString.setSpan(
            ForegroundColorSpan(context.getColor(color)),
            0,
            text.length,
            SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        val font = ResourcesCompat.getFont(context, fontfamily)
        val typefaceSpan = CustomTypefaceSpan("", font)
        spannableString.setSpan(
            typefaceSpan, 0, text.length, SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        return spannableString
    }
}
