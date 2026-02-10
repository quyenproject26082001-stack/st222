package poster.maker.activity_app.editsticker

import android.content.Context
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import kotlin.math.atan2
import kotlin.math.hypot

class StickerTouchListener(private val context: Context) : View.OnTouchListener {

    private var lastX = 0f
    private var lastY = 0f
    private var startDistance = 0f
    private var startRotation = 0f
    private var isMultiTouch = false

    override fun onTouch(view: View, event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                lastX = event.rawX
                lastY = event.rawY
                isMultiTouch = false
                bringToFront(view)
                return true
            }

            MotionEvent.ACTION_POINTER_DOWN -> {
                if (event.pointerCount == 2) {
                    isMultiTouch = true
                    startDistance = getDistance(event)
                    startRotation = getRotation(event) - view.rotation
                }
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                if (isMultiTouch && event.pointerCount == 2) {
                    // Scale and rotate
                    val currentDistance = getDistance(event)
                    if (startDistance > 0) {
                        val scale = currentDistance / startDistance
                        view.scaleX *= scale
                        view.scaleY *= scale
                        startDistance = currentDistance
                    }

                    val rotation = getRotation(event) - startRotation
                    view.rotation = rotation
                } else {
                    // Move
                    val deltaX = event.rawX - lastX
                    val deltaY = event.rawY - lastY

                    view.x += deltaX
                    view.y += deltaY

                    lastX = event.rawX
                    lastY = event.rawY
                }
                return true
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                if (event.pointerCount <= 1) {
                    isMultiTouch = false
                }
                return true
            }
        }
        return false
    }

    private fun getDistance(event: MotionEvent): Float {
        if (event.pointerCount < 2) return 0f
        val dx = event.getX(0) - event.getX(1)
        val dy = event.getY(0) - event.getY(1)
        return hypot(dx, dy)
    }

    private fun getRotation(event: MotionEvent): Float {
        if (event.pointerCount < 2) return 0f
        val dx = (event.getX(1) - event.getX(0)).toDouble()
        val dy = (event.getY(1) - event.getY(0)).toDouble()
        return Math.toDegrees(atan2(dy, dx)).toFloat()
    }

    private fun bringToFront(view: View) {
        val parent = view.parent as? ViewGroup
        parent?.bringChildToFront(view)
        parent?.invalidate()
    }
}
