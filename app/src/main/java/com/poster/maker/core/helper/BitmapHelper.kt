package poster.maker.core.helper

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.net.Uri
import android.renderscript.Allocation
import android.renderscript.Element
import android.renderscript.RenderScript
import android.renderscript.ScriptIntrinsicBlur
import android.util.Log
import android.view.View
import androidx.core.graphics.createBitmap
import androidx.exifinterface.media.ExifInterface
import java.io.File

object BitmapHelper {
    fun uriToBitmap(context: Context, uri: Uri): Bitmap? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            BitmapFactory.decodeStream(inputStream).also {
                inputStream?.close()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("nbhieu", "uriToBitmap: ${e.message}")
            null
        }
    }

    fun rotateBitmap(bitmap: Bitmap, degrees: Float): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(degrees)
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    fun convertPathsToBitmaps(context: Context, paths: List<String>): List<Bitmap> {
        val bitmaps = mutableListOf<Bitmap>()
        paths.forEachIndexed { index, path ->
            val uri = Uri.fromFile(File(path))
            var bitmap = uriToBitmap(context, uri)
            if (bitmap != null) {
                val exif = ExifInterface(path)
                val orientation = exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL
                )

                if (orientation != ExifInterface.ORIENTATION_ROTATE_90 && orientation != ExifInterface.ORIENTATION_ROTATE_180 && orientation != ExifInterface.ORIENTATION_ROTATE_270) {
                    bitmaps.add(bitmap)
                } else {
                    bitmap = when (orientation) {
                        ExifInterface.ORIENTATION_ROTATE_90 -> rotateBitmap(bitmap, 90f)
                        ExifInterface.ORIENTATION_ROTATE_180 -> rotateBitmap(bitmap, 180f)
                        ExifInterface.ORIENTATION_ROTATE_270 -> rotateBitmap(bitmap, 270f)
                        else -> bitmap
                    }

                    bitmaps.add(bitmap)
                }

            }
        }
        return bitmaps
    }

    @Throws(OutOfMemoryError::class)
    fun createBimapFromView(view: View): Bitmap {
        try {
            val output = createBitmap(view.width, view.height)
            val canvas = Canvas(output)
            view.draw(canvas)
            return output
        } catch (error: OutOfMemoryError) {
            throw error
        }
    }

    /**
     * Blur bitmap using RenderScript
     * @param context Android context
     * @param bitmap Original bitmap
     * @param radius Blur radius (1-25)
     * @return Blurred bitmap
     */
    fun blurBitmap(context: Context, bitmap: Bitmap, radius: Float = 15f): Bitmap {
        val outputBitmap = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)

        try {
            val renderScript = RenderScript.create(context)
            val input = Allocation.createFromBitmap(renderScript, bitmap)
            val output = Allocation.createFromBitmap(renderScript, outputBitmap)

            val script = ScriptIntrinsicBlur.create(renderScript, Element.U8_4(renderScript))
            script.setRadius(radius.coerceIn(1f, 25f))
            script.setInput(input)
            script.forEach(output)

            output.copyTo(outputBitmap)

            input.destroy()
            output.destroy()
            script.destroy()
            renderScript.destroy()
        } catch (e: Exception) {
            e.printStackTrace()
            return bitmap
        }

        return outputBitmap
    }
}