package poster.maker.core.helper

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation
import java.security.MessageDigest

/**
 * Glide Transformation to create shadow bitmap that follows the alpha channel of the image
 * This creates realistic shadow that follows the contour of the object (like icon shadow)
 */
class ShadowTransformation(
    private val shadowRadius: Float = 20f,
    private val shadowAlpha: Float = 0.7f
) : BitmapTransformation() {

    override fun transform(
        pool: BitmapPool,
        toTransform: Bitmap,
        outWidth: Int,
        outHeight: Int
    ): Bitmap {
        val width = toTransform.width
        val height = toTransform.height

        // Calculate padding needed for blur to extend beyond edges
        // Use 3x shadowRadius to ensure blur has enough space to be visible outside
        val padding = (shadowRadius * 3).toInt().coerceAtLeast(10)

        // Create output bitmap LARGER than input to allow shadow to appear outside edges
        val outputWidth = width + padding * 2
        val outputHeight = height + padding * 2
        val output = pool.get(outputWidth, outputHeight, Bitmap.Config.ARGB_8888)
        output.eraseColor(Color.TRANSPARENT)

        val canvas = Canvas(output)

        // Step 1: Draw the original bitmap in the CENTER (offset by padding)
        // This creates transparent space around the image for blur to expand into
        val alphaPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        canvas.drawBitmap(toTransform, padding.toFloat(), padding.toFloat(), alphaPaint)

        // Step 2: Apply black color while preserving alpha channel (creates silhouette)
        val blackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb((shadowAlpha * 255).toInt(), 0, 0, 0)
            xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        }
        canvas.drawRect(0f, 0f, outputWidth.toFloat(), outputHeight.toFloat(), blackPaint)

        // Step 3: Apply blur if radius > 0
        // Blur will now expand into the transparent padding area, creating shadow outside edges
        if (shadowRadius > 0) {
            return fastBlur(output, shadowRadius.toInt(), pool)
        }

        return output
    }

    /**
     * Fast stack blur algorithm
     * Approximates Gaussian blur efficiently
     */
    private fun fastBlur(source: Bitmap, radius: Int, pool: BitmapPool): Bitmap {
        if (radius < 1) return source

        val width = source.width
        val height = source.height
        val pixels = IntArray(width * height)
        source.getPixels(pixels, 0, width, 0, 0, width, height)

        // Horizontal blur
        blurHorizontal(pixels, width, height, radius)

        // Vertical blur
        blurVertical(pixels, width, height, radius)

        val output = pool.get(width, height, Bitmap.Config.ARGB_8888)
        output.setPixels(pixels, 0, width, 0, 0, width, height)

        return output
    }

    private fun blurHorizontal(pixels: IntArray, width: Int, height: Int, radius: Int) {
        val div = radius + radius + 1
        val r = IntArray(width * height)
        val g = IntArray(width * height)
        val b = IntArray(width * height)
        val a = IntArray(width * height)

        var rsum: Int
        var gsum: Int
        var bsum: Int
        var asum: Int

        for (y in 0 until height) {
            rsum = 0
            gsum = 0
            bsum = 0
            asum = 0

            for (i in -radius until radius + 1) {
                val pixel = pixels[y * width + (i.coerceIn(0, width - 1))]
                rsum += (pixel shr 16) and 0xff
                gsum += (pixel shr 8) and 0xff
                bsum += pixel and 0xff
                asum += (pixel shr 24) and 0xff
            }

            for (x in 0 until width) {
                r[y * width + x] = rsum / div
                g[y * width + x] = gsum / div
                b[y * width + x] = bsum / div
                a[y * width + x] = asum / div

                val i1 = y * width + ((x - radius).coerceAtLeast(0))
                val i2 = y * width + ((x + radius + 1).coerceAtMost(width - 1))
                val p1 = pixels[i1]
                val p2 = pixels[i2]

                rsum += ((p2 shr 16) and 0xff) - ((p1 shr 16) and 0xff)
                gsum += ((p2 shr 8) and 0xff) - ((p1 shr 8) and 0xff)
                bsum += (p2 and 0xff) - (p1 and 0xff)
                asum += ((p2 shr 24) and 0xff) - ((p1 shr 24) and 0xff)
            }
        }

        for (i in pixels.indices) {
            pixels[i] = (a[i] shl 24) or (r[i] shl 16) or (g[i] shl 8) or b[i]
        }
    }

    private fun blurVertical(pixels: IntArray, width: Int, height: Int, radius: Int) {
        val div = radius + radius + 1
        val r = IntArray(width * height)
        val g = IntArray(width * height)
        val b = IntArray(width * height)
        val a = IntArray(width * height)

        var rsum: Int
        var gsum: Int
        var bsum: Int
        var asum: Int

        for (x in 0 until width) {
            rsum = 0
            gsum = 0
            bsum = 0
            asum = 0

            for (i in -radius until radius + 1) {
                val pixel = pixels[(i.coerceIn(0, height - 1)) * width + x]
                rsum += (pixel shr 16) and 0xff
                gsum += (pixel shr 8) and 0xff
                bsum += pixel and 0xff
                asum += (pixel shr 24) and 0xff
            }

            for (y in 0 until height) {
                r[y * width + x] = rsum / div
                g[y * width + x] = gsum / div
                b[y * width + x] = bsum / div
                a[y * width + x] = asum / div

                val i1 = ((y - radius).coerceAtLeast(0)) * width + x
                val i2 = ((y + radius + 1).coerceAtMost(height - 1)) * width + x
                val p1 = pixels[i1]
                val p2 = pixels[i2]

                rsum += ((p2 shr 16) and 0xff) - ((p1 shr 16) and 0xff)
                gsum += ((p2 shr 8) and 0xff) - ((p1 shr 8) and 0xff)
                bsum += (p2 and 0xff) - (p1 and 0xff)
                asum += ((p2 shr 24) and 0xff) - ((p1 shr 24) and 0xff)
            }
        }

        for (i in pixels.indices) {
            pixels[i] = (a[i] shl 24) or (r[i] shl 16) or (g[i] shl 8) or b[i]
        }
    }

    override fun updateDiskCacheKey(messageDigest: MessageDigest) {
        messageDigest.update("shadow_contour_${shadowRadius}_${shadowAlpha}".toByteArray())
    }

    override fun equals(other: Any?): Boolean {
        if (other is ShadowTransformation) {
            return shadowRadius == other.shadowRadius && shadowAlpha == other.shadowAlpha
        }
        return false
    }

    override fun hashCode(): Int {
        return "shadow_contour_${shadowRadius}_${shadowAlpha}".hashCode()
    }
}

