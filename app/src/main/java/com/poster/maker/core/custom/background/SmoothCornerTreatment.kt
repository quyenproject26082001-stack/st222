package poster.maker.core.custom.background

import com.google.android.material.shape.CornerTreatment
import com.google.android.material.shape.ShapePath

/**
 * CornerTreatment mô phỏng Corner Smoothing giống Figma.
 *
 * @param smoothing hệ số bo mượt (0f - 1f).
 */
class SmoothCornerTreatment(
    private val smoothing: Float = 0.6f
) : CornerTreatment() {

    override fun getCornerPath(
        shapePath: ShapePath,
        angle: Float,
        interpolation: Float,
        radius: Float
    ) {
        val r = radius * interpolation
        val smooth = smoothing.coerceIn(0f, 1f)

        // Chiều dài đoạn cong smoothing
        val distance = r * smooth

        // Bắt đầu từ bên trái (0, r)
        shapePath.reset(0f, r)

        // Bézier cong mềm từ (0,r) -> (r,0)
        shapePath.quadToPoint(
            0f, 0f,
            distance, 0f
        )

        // Nối tiếp ra tới (r,0)
        shapePath.lineTo(r, 0f)
    }
}
