package poster.maker.core.custom.imageview

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.drawable.BitmapDrawable
import android.util.AttributeSet
import android.widget.ImageView

class CustomImageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : ImageView(context, attrs, defStyle) {

    // Phương thức để đảo ngược hình ảnh
    fun flipImage() {
        if (drawable != null) {
            val bitmapDrawable = drawable as BitmapDrawable
            val bitmap = bitmapDrawable.bitmap

            val matrix = Matrix()
            matrix.setScale(-1f, 1f)

            val flippedBitmap =
                Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            setImageBitmap(flippedBitmap)
        }
    }
}