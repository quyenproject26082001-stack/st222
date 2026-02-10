package poster.maker.core.extensions

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import android.widget.TextView
import com.google.android.material.shape.CornerFamily
import com.google.android.material.shape.MaterialShapeDrawable
import com.google.android.material.shape.ShapeAppearanceModel
import androidx.core.graphics.toColorInt
import com.google.android.material.imageview.ShapeableImageView
import poster.maker.core.custom.background.SmoothCornerTreatment
import poster.maker.core.helper.UnitHelper


// Hinh bat giac
fun setBackgroundButtonSolidBlue(context: Context, button: TextView) {
    val drawableButtonBlue = MaterialShapeDrawable().apply {
        setTint("#002DB3".toColorInt())
        setStroke(UnitHelper.pxToDpFloat(context, 2), Color.WHITE)
        shapeAppearanceModel = ShapeAppearanceModel.builder().setTopLeftCorner(
            CornerFamily.CUT, UnitHelper.pxToDpFloat(context, 6)
        ).setTopRightCorner(
            CornerFamily.CUT, UnitHelper.pxToDpFloat(context, 6)
        ).setBottomLeftCorner(CornerFamily.CUT, UnitHelper.pxToDpFloat(context, 6))
            .setBottomRightCorner(
                CornerFamily.CUT, UnitHelper.pxToDpFloat(context, 6)
            ).build()
    }
    button.background = drawableButtonBlue
}

fun setBackgroundButtonSolidWhite(context: Context, button: TextView) {
    val drawableButtonWhite = MaterialShapeDrawable().apply {
        setStroke(UnitHelper.pxToDpFloat(context, 2), "#002DB3".toColorInt())
        setTint(Color.WHITE)
        shapeAppearanceModel = ShapeAppearanceModel.builder().setTopLeftCorner(
            CornerFamily.CUT, UnitHelper.pxToDpFloat(context, 6)
        ).setTopRightCorner(
            CornerFamily.CUT, UnitHelper.pxToDpFloat(context, 6)
        ).setBottomLeftCorner(CornerFamily.CUT, UnitHelper.pxToDpFloat(context, 6))
            .setBottomRightCorner(
                CornerFamily.CUT, UnitHelper.pxToDpFloat(context, 6)
            ).build()
    }
    button.background = drawableButtonWhite
}

fun setBackgroundSolidTransparent(context: Context, view: View) {
    val drawableTransparent = MaterialShapeDrawable().apply {
        setStroke(UnitHelper.pxToDpFloat(context, 2), Color.WHITE)
        setTint(Color.TRANSPARENT)
        shapeAppearanceModel = ShapeAppearanceModel.builder()
            .setTopLeftCorner(CornerFamily.CUT, UnitHelper.pxToDpFloat(context, 12))
            .setTopRightCorner(CornerFamily.CUT, UnitHelper.pxToDpFloat(context, 12))
            .setBottomLeftCorner(CornerFamily.CUT, UnitHelper.pxToDpFloat(context, 12))
            .setBottomRightCorner(CornerFamily.CUT, UnitHelper.pxToDpFloat(context, 12)).build()
    }
    view.background = drawableTransparent
}

fun setBackgroundButtonSolidBlue(context: Context, button: ViewGroup) {
    val drawableButtonBlue = MaterialShapeDrawable().apply {
        setTint("#002DB3".toColorInt())
        setStroke(UnitHelper.pxToDpFloat(context, 12), Color.WHITE)
        shapeAppearanceModel = ShapeAppearanceModel.builder().setTopLeftCorner(
            CornerFamily.CUT, UnitHelper.pxToDpFloat(context, 6)
        ).setTopRightCorner(
            CornerFamily.CUT, UnitHelper.pxToDpFloat(context, 6)
        ).setBottomLeftCorner(CornerFamily.CUT, UnitHelper.pxToDpFloat(context, 6))
            .setBottomRightCorner(
                CornerFamily.CUT, UnitHelper.pxToDpFloat(context, 6)
            ).build()
    }
    button.background = drawableButtonBlue
}

fun setBackgroundButtonSolidBlueCadet(context: Context, button: RelativeLayout) {
    val drawableButtonBlue = MaterialShapeDrawable().apply {
        setTint("#B3253257".toColorInt())
        setStroke(UnitHelper.pxToDpFloat(context, 2), "#787272".toColorInt())
        shapeAppearanceModel = ShapeAppearanceModel.builder().setTopLeftCorner(
            CornerFamily.CUT, UnitHelper.pxToDpFloat(context, 6)
        ).setTopRightCorner(
            CornerFamily.CUT, UnitHelper.pxToDpFloat(context, 6)
        ).setBottomLeftCorner(CornerFamily.CUT, UnitHelper.pxToDpFloat(context, 6))
            .setBottomRightCorner(
                CornerFamily.CUT, UnitHelper.pxToDpFloat(context, 6)
            ).build()
    }
    button.background = drawableButtonBlue
}

// Conner
fun setBackgroundConnerSmooth(context: Context, view: View, radius: Int = 8){
    val drawableShape = MaterialShapeDrawable().apply {
        fillColor = ColorStateList.valueOf("#FFE9A9".toColorInt())
        setStroke(UnitHelper.pxToDpFloat(context, 2), "#F68300".toColorInt())
        shapeAppearanceModel = ShapeAppearanceModel.builder()
            .setAllCorners(SmoothCornerTreatment(1f))
            .setAllCornerSizes(UnitHelper.pxToDpFloat(context, radius))
            .build()
    }
    view.background = drawableShape
}

fun setBackgroundConnerSmooth(context: Context, view: View, solidColor: String, radius: Int = 8){
    val drawableShape = MaterialShapeDrawable().apply {
        fillColor = ColorStateList.valueOf(solidColor.toColorInt())
        shapeAppearanceModel = ShapeAppearanceModel.builder()
            .setAllCorners(SmoothCornerTreatment(1f))
            .setAllCornerSizes(UnitHelper.pxToDpFloat(context, radius))
            .build()
    }
    view.background = drawableShape
}
fun setBackgroundConnerSmooth(context: Context, view: View, solidColor: String, strokeColor: String, radius: Int = 8){
    val drawableShape = MaterialShapeDrawable().apply {
        fillColor = ColorStateList.valueOf(solidColor.toColorInt())
        setStroke(UnitHelper.pxToDpFloat(context, 2), strokeColor.toColorInt())
        shapeAppearanceModel = ShapeAppearanceModel.builder()
            .setAllCorners(SmoothCornerTreatment(1f))
            .setAllCornerSizes(UnitHelper.pxToDpFloat(context, radius))
            .build()
    }
    view.background = drawableShape
}
fun setBackgroundConnerSmooth(context: Context, view: View, solidColor: String, strokeColor: String, strokeWidth: Int, radius: Int = 8){
    val drawableShape = MaterialShapeDrawable().apply {
        fillColor = ColorStateList.valueOf(solidColor.toColorInt())
        setStroke(UnitHelper.pxToDpFloat(context, strokeWidth), strokeColor.toColorInt())
        shapeAppearanceModel = ShapeAppearanceModel.builder()
            .setAllCorners(SmoothCornerTreatment(1f))
            .setAllCornerSizes(UnitHelper.pxToDpFloat(context, radius))
            .build()
    }
    view.background = drawableShape
}
fun setBackgroundConnerSmoothImage(context: Context, view: ShapeableImageView, radius: Int = 8){
    val drawableShape = MaterialShapeDrawable().apply {
        shapeAppearanceModel = ShapeAppearanceModel.builder()
            .setAllCorners(SmoothCornerTreatment(1f))
            .setAllCornerSizes(UnitHelper.pxToDpFloat(context, radius))
            .build()
    }
    view.shapeAppearanceModel = drawableShape.shapeAppearanceModel
}