package poster.maker.core.custom.text

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Paint.Join
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView
import poster.maker.R
import androidx.core.graphics.toColorInt


class StrokeTextView : AppCompatTextView {
    private var strokeWidth = 0f
    private var strokeColor: Int = Color.WHITE
    private var strokeJoin: Join? = null
    private var strokeMiter = 0f

    constructor(context: Context) : this(context, null) {
        init(null)
    }

    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0) {
        init(attrs)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        init(attrs)
    }

    @SuppressLint("Recycle")
    private fun init(attrs: AttributeSet?) {
        if (attrs != null) {
            val a = context.obtainStyledAttributes(attrs, R.styleable.StrokeTextView)
            if (a.hasValue(R.styleable.StrokeTextView_strokeColor)) {
                val strokeWidth = a.getDimensionPixelSize(R.styleable.StrokeTextView_strokeWidth, 1).toFloat()
                val strokeColor = a.getColor(R.styleable.StrokeTextView_strokeColor, -0x1000000)
                val strokeMiter = a.getDimensionPixelSize(R.styleable.StrokeTextView_strokeMiter, 10).toFloat()
                var strokeJoin: Join? = null
                when (a.getInt(R.styleable.StrokeTextView_strokeJoinStyle, 0)) {
                    0 -> strokeJoin = Join.MITER
                    1 -> strokeJoin = Join.BEVEL
                    2 -> strokeJoin = Join.ROUND
                }
                setStroke(strokeWidth, strokeColor, strokeJoin!!, strokeMiter)
            }
        }
    }

    fun setStroke(width: Float, color: Int, join: Join, miter: Float) {
        strokeWidth = width
        strokeColor = color
        strokeJoin = join
        strokeMiter = miter
    }
    fun setStrokeTitle(){
        strokeWidth = 2.5f
        strokeColor = "#FFFFFF".toColorInt()
        strokeJoin = Join.ROUND
        strokeMiter = 5f
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val restoreColor = this.currentTextColor
        if (strokeColor != null) {
            val paint = this.paint
            paint.style = Paint.Style.STROKE
            paint.strokeJoin = strokeJoin
            paint.strokeMiter = strokeMiter
            this.setTextColor(strokeColor)
            paint.strokeWidth = strokeWidth
            super.onDraw(canvas)
            paint.style = Paint.Style.FILL
            this.setTextColor(restoreColor)
        }
    }

}