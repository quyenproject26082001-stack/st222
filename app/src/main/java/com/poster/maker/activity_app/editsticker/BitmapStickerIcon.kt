package poster.maker.activity_app.editsticker

import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.drawable.Drawable

class BitmapStickerIcon(
    val drawable: Drawable,
    val position: IconPosition,
    iconSizePx: Int = -1
) {
    var x: Float = 0f
    var y: Float = 0f
    val width: Float = if (iconSizePx > 0) iconSizePx.toFloat() else drawable.intrinsicWidth.toFloat()
    val height: Float = if (iconSizePx > 0) iconSizePx.toFloat() else drawable.intrinsicHeight.toFloat()
    val radius: Float = (width + height) / 4f

    private val matrix = Matrix()
    var iconEvent: StickerIconEvent? = null

    enum class IconPosition {
        TOP_LEFT,
        TOP_RIGHT,
        BOTTOM_LEFT,
        BOTTOM_RIGHT
    }

    fun draw(canvas: Canvas, paint: Paint) {
        canvas.save()
        canvas.concat(matrix)
        drawable.setBounds(0, 0, width.toInt(), height.toInt())
        drawable.draw(canvas)
        canvas.restore()
    }

    fun getMatrix(): Matrix = matrix

    fun updatePosition(x: Float, y: Float, rotation: Float) {
        this.x = x
        this.y = y
        matrix.reset()
        matrix.postRotate(rotation, width / 2f, height / 2f)
        matrix.postTranslate(x - width / 2f, y - height / 2f)
    }
}
