package poster.maker.core.extensions

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import android.view.WindowManager
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.FontRes
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.FragmentActivity
import androidx.core.graphics.createBitmap
import poster.maker.R
import poster.maker.core.helper.RateHelper
import poster.maker.core.helper.SharePreferenceHelper
import poster.maker.core.helper.SoundHelper
import poster.maker.core.utils.DataLocal
import poster.maker.core.utils.state.RateState

// ----------------------------
// Visibility extensions
// ----------------------------
fun View.visible() { visibility = View.VISIBLE }
fun View.invisible() { visibility = View.INVISIBLE }
fun View.gone() { visibility = View.GONE }
fun View.select() { isSelected = true }

// ----------------------------
// Toast extensions
// ----------------------------
fun Activity.showToast(message: Int, duration: Int = Toast.LENGTH_SHORT) {
    Toast.makeText(this, ContextCompat.getString(this, message), duration).show()
}
fun Activity.showToast(message: String, duration: Int = Toast.LENGTH_SHORT) {
    Toast.makeText(this, message, duration).show()
}

// ----------------------------
// Navigation / Transition
// ----------------------------
fun Activity.handleBackLeftToRight() {
    finish()
    overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
}
fun Activity.handleBackRightToLeft() {
    finish()
    overridePendingTransition(R.anim.slide_out_right, R.anim.slide_in_left)
}
fun Context.handleBackFragmentFromRight() {
    if (this is FragmentActivity) {
        overridePendingTransition(R.anim.slide_out_left, R.anim.slide_in_right)
        supportFragmentManager.popBackStack()
    }
}
fun Activity.hideNavigation(isBlack: Boolean = true) {
    window?.setFlags(
        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
    )
    window.decorView.systemUiVisibility = if (isBlack) {
        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
    } else {
        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
    }
}
// ----------------------------
// Click utils
// ----------------------------
fun View.setOnSingleClick(interval: Long = 200, action: (View) -> Unit) {
    setOnClickListener {
        if (System.currentTimeMillis() - DataLocal.lastClickTime >= interval) {
            action(it)
            DataLocal.lastClickTime = System.currentTimeMillis()
        }
    }
}
fun View.setOnSingleClickWithSound(interval: Long = 500, action: (View) -> Unit) {
    setOnClickListener {
        if (System.currentTimeMillis() - DataLocal.lastClickTime >= interval) {
            SoundHelper.playSound(R.raw.touch)
            action(it)
            DataLocal.lastClickTime = System.currentTimeMillis()
        }
    }
}

// ----------------------------
// UI Capture
// ----------------------------
@Throws(OutOfMemoryError::class)
fun createBitmapFromView(view: View): Bitmap {
    return try {
        val output = createBitmap(view.width, view.height)
        val canvas = Canvas(output)
        view.draw(canvas)
        output
    } catch (error: OutOfMemoryError) {
        throw error
    }
}

// ----------------------------
// TextView
// ----------------------------
fun TextView.setFont(@FontRes resId: Int) {
    typeface = ResourcesCompat.getFont(context, resId)
}

fun TextView.setTextContent(context: Context, resId: Int) {
    text = context.getString(resId)
}

fun Context.strings(resId: Int) : String {
    return getString(resId)
}

// ----------------------------
// SeekBar
// ----------------------------
fun SeekBar.onProgressChanged(action: (progress: Int) -> Unit) {
    setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
        override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
            action(progress)
        }
        override fun onStartTrackingTouch(seekBar: SeekBar?) {}
        override fun onStopTrackingTouch(seekBar: SeekBar?) {}
    })
}
