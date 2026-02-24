package com.wanted.poster.maker.core.listener.listenerdraw

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.Drawable
import android.view.MotionEvent
import androidx.annotation.IntDef
import com.piratemaker.postermaker.core.custom.drawview.DrawView
import androidx.core.graphics.toColorInt
import com.ocmaker.pixcel.maker.data.model.draw.DrawableDraw
import com.piratemaker.postermaker.listener.listenerdraw.DrawEvent
import com.wanted.poster.maker.core.utils.key.DrawKey

class BitmapDrawIcon(drawable: Drawable?, @Gravity gravity: Int, private val sizePx: Int = -1) : DrawableDraw(drawable!!, "nbhieu"),
    DrawEvent {
    @IntDef(*[DrawKey.TOP_LEFT, DrawKey.RIGHT_TOP, DrawKey.LEFT_BOTTOM, DrawKey.RIGHT_BOTTOM])
    @Retention(AnnotationRetention.SOURCE)
    annotation class Gravity

    var radius = DrawKey.DEFAULT_RADIUS
    var x = 0f
    var y = 0f

    @get:Gravity
    @Gravity
    var positionDefault = DrawKey.TOP_LEFT
    var event: DrawEvent? = null

    override val width: Int
        get() = if (sizePx > 0) sizePx else super.width
    override val height: Int
        get() = if (sizePx > 0) sizePx else super.height

    init {
        positionDefault = gravity
    }

    override fun onActionDown(tattooView: DrawView?, event: MotionEvent?) {
        if (this.event != null) {
            this.event!!.onActionDown(tattooView, event)
        }
    }

    override fun onActionMove(tattooView: DrawView?, event: MotionEvent?) {
        if (this.event != null) {
            this.event!!.onActionMove(tattooView, event)
        }
    }

    override fun onActionUp(tattooView: DrawView?, event: MotionEvent?) {
        if (this.event != null) {
            this.event!!.onActionUp(tattooView, event)
        }
    }


    fun draw(canvas: Canvas, paint: Paint) {
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 5f
        paint.color = "#513323".toColorInt()
        canvas.save()
        canvas.concat(getMatrix())
        drawable.setBounds(0, 0, width, height)
        drawable.draw(canvas)
        canvas.restore()
    }

}

