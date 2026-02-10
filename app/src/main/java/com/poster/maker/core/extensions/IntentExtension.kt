package poster.maker.core.extensions

import android.app.Activity
import android.app.ActivityOptions
import android.content.Intent
import android.os.Build
import android.os.Parcelable
import poster.maker.R
import poster.maker.core.utils.key.IntentKey

fun Activity.startIntent(targetActivity: Class<*>) {
    val intent = Intent(this, targetActivity)
    startActivity(intent)
}

fun Activity.startIntent(targetActivity: Class<*>, value: String) {
    val intent = Intent(this, targetActivity)
    intent.putExtra(IntentKey.INTENT_KEY, value)
    startActivity(intent)
}

inline fun <reified T> Activity.startIntent(targetActivity: Class<*>, key: String, value: T) {
    val intent = Intent(this, targetActivity)
    when (value){
        is String -> intent.putExtra(IntentKey.INTENT_KEY, value)
        is Int -> intent.putExtra(IntentKey.INTENT_KEY, value)
        is Boolean -> intent.putExtra(IntentKey.INTENT_KEY, value)
        is Float -> intent.putExtra(IntentKey.INTENT_KEY, value)
        is Long -> intent.putExtra(IntentKey.INTENT_KEY, value)
    }
    startActivity(intent)
}

fun Activity.startIntentRightToLeft(targetActivity: Class<*>) {
    val intent = Intent(this, targetActivity)
    val option =
        ActivityOptions.makeCustomAnimation(this, R.anim.slide_in_right, R.anim.slide_out_left)
    startActivity(intent, option.toBundle())
}

inline fun <reified T> Activity.startIntentRightToLeft(targetActivity: Class<*>, value: T) {
    val intent = Intent(this, targetActivity)

    when (value) {
        is String -> intent.putExtra(IntentKey.INTENT_KEY, value)
        is Int -> intent.putExtra(IntentKey.INTENT_KEY, value)
        is Boolean -> intent.putExtra(IntentKey.INTENT_KEY, value)
        is Float -> intent.putExtra(IntentKey.INTENT_KEY, value)
        is Long -> intent.putExtra(IntentKey.INTENT_KEY, value)
        else -> throw IllegalArgumentException(
            "Unsupported type: ${T::class.java.simpleName}"
        )
    }

    val option = ActivityOptions.makeCustomAnimation(
        this, R.anim.slide_in_right, R.anim.slide_out_left
    )
    startActivity(intent, option.toBundle())
}

inline fun <reified T> Activity.startIntentRightToLeft(
    targetActivity: Class<*>, key: String, value: T
) {
    val intent = Intent(this, targetActivity)
    when (value) {
        is String -> intent.putExtra(key, value)
        is Int -> intent.putExtra(key, value)
        is Boolean -> intent.putExtra(key, value)
        is Float -> intent.putExtra(key, value)
        is Long -> intent.putExtra(key, value)
        else -> throw IllegalArgumentException(
            "Unsupported type: ${T::class.java.simpleName}"
        )
    }

    val option =
        ActivityOptions.makeCustomAnimation(this, R.anim.slide_in_right, R.anim.slide_out_left)
    startActivity(intent, option.toBundle())
}

fun Activity.startIntentLeftToRight(targetActivity: Class<*>) {
    val intent = Intent(this, targetActivity)
    val options =
        ActivityOptions.makeCustomAnimation(this, R.anim.slide_in_left, R.anim.slide_out_right)
    startActivity(intent, options.toBundle())
}

inline fun <reified T> Activity.startIntentLeftToRight(targetActivity: Class<*>, value: T) {
    val intent = Intent(this, targetActivity)

    when (value) {
        is String -> intent.putExtra(IntentKey.INTENT_KEY, value)
        is Int -> intent.putExtra(IntentKey.INTENT_KEY, value)
        is Boolean -> intent.putExtra(IntentKey.INTENT_KEY, value)
        is Float -> intent.putExtra(IntentKey.INTENT_KEY, value)
        is Long -> intent.putExtra(IntentKey.INTENT_KEY, value)
        else -> throw IllegalArgumentException(
            "Unsupported type: ${T::class.java.simpleName}"
        )
    }

    val option =
        ActivityOptions.makeCustomAnimation(this, R.anim.slide_in_left, R.anim.slide_out_right)
    startActivity(intent, option.toBundle())
}

inline fun <reified T> Activity.startIntentLeftToRight(
    targetActivity: Class<*>, key: String, value: T
) {
    val intent = Intent(this, targetActivity)
    when (value) {
        is String -> intent.putExtra(key, value)
        is Int -> intent.putExtra(key, value)
        is Boolean -> intent.putExtra(key, value)
        is Float -> intent.putExtra(key, value)
        is Long -> intent.putExtra(key, value)
        else -> throw IllegalArgumentException(
            "Unsupported type: ${T::class.java.simpleName}"
        )
    }

    val option =
        ActivityOptions.makeCustomAnimation(this, R.anim.slide_in_left, R.anim.slide_out_right)
    startActivity(intent, option.toBundle())
}

inline fun <reified T : Parcelable> Intent.getParcelableCompat(key: String): T? {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getParcelableExtra(key, T::class.java)
    } else {
        @Suppress("DEPRECATION") getParcelableExtra(key)
    }
}

fun Activity.startIntentWithClearTop(targetActivity: Class <*>) {
    val intent = Intent(this, targetActivity)
    startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK))
}
