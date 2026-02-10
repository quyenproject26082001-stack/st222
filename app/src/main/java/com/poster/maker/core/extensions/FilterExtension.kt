package poster.maker.core.extensions

import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint

fun createPaint(matrixValues: FloatArray): Paint {
    val paint = Paint()
    paint.colorFilter = ColorMatrixColorFilter(ColorMatrix(matrixValues))
    return paint
}

fun createPaint(matrix: ColorMatrix): Paint {
    val paint = Paint()
    paint.colorFilter = ColorMatrixColorFilter(matrix)
    return paint
}