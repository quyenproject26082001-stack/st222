package com.ocmaker.pixcel.maker.data.model.draw

import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.drawable.Drawable


class DrawDraw(override var drawable: Drawable, private val filePath: String) : Draw() {

    private val bounds = Rect(0, 0, width, height)

    override val width: Int
        get() = drawable.intrinsicWidth
    override val height: Int
        get() = drawable.intrinsicHeight

    override fun draw(canvas: Canvas) {
        val matrix = getMatrix()
        canvas.save()
        canvas.concat(matrix)
        drawable.bounds = bounds
        drawable.draw(canvas)
        canvas.restore()
    }

    override fun setDrawable(drawable: Drawable): DrawDraw {
        this.drawable = drawable
        return this
    }

    override val drawablePath: String
        get() = filePath

    override fun setAlpha(alpha: Int): Draw {
        drawable.alpha = alpha
        return this
    }

}

