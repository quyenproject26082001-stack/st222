package poster.maker.core.custom.text

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView
import poster.maker.R
import androidx.core.content.withStyledAttributes

class DoubleStrokeTextView : AppCompatTextView {

    private var outerStrokeWidth = 0f
    private var outerStrokeColor: Int = Color.BLUE

    private var innerStrokeWidth = 0f
    private var innerStrokeColor: Int = Color.WHITE

    private var strokeJoin: Paint.Join = Paint.Join.ROUND
    private var strokeMiter = 10f

    constructor(context: Context) : super(context) {
        init(null)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init(attrs)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        init(attrs)
    }

    private fun init(attrs: AttributeSet?) {
        if (attrs != null) {
            context.withStyledAttributes(attrs, R.styleable.DoubleStrokeTextView) {

                outerStrokeColor =
                    getColor(R.styleable.DoubleStrokeTextView_outerStrokeColor, Color.TRANSPARENT)
                outerStrokeWidth = getDimension(R.styleable.DoubleStrokeTextView_outerStrokeWidth, 0f)

                innerStrokeColor =
                    getColor(R.styleable.DoubleStrokeTextView_innerStrokeColor, Color.TRANSPARENT)
                innerStrokeWidth = getDimension(R.styleable.DoubleStrokeTextView_innerStrokeWidth, 0f)

                strokeMiter = getDimension(R.styleable.DoubleStrokeTextView_strokeMiter, 10f)

                strokeJoin = when (getInt(R.styleable.DoubleStrokeTextView_strokeJoinStyle, 2)) {
                    0 -> Paint.Join.MITER
                    1 -> Paint.Join.BEVEL
                    else -> Paint.Join.ROUND
                }

            }
        }
    }

    fun setDoubleStroke(
        outerColor: Int,
        outerWidth: Float,
        innerColor: Int,
        innerWidth: Float,
        join: Paint.Join = Paint.Join.ROUND,
        miter: Float = 10f
    ) {
        this.outerStrokeColor = outerColor
        this.outerStrokeWidth = outerWidth
        this.innerStrokeColor = innerColor
        this.innerStrokeWidth = innerWidth
        this.strokeJoin = join
        this.strokeMiter = miter
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        val paint = paint

        val originalColor = currentTextColor
        val originalStyle = paint.style
        val originalJoin = paint.strokeJoin
        val originalMiter = paint.strokeMiter
        val originalStrokeWidth = paint.strokeWidth

        // Vẽ stroke ngoài (màu xanh)
        paint.style = Paint.Style.STROKE
        paint.strokeJoin = strokeJoin
        paint.strokeMiter = strokeMiter
        paint.strokeWidth = outerStrokeWidth
        setTextColor(outerStrokeColor)
        super.onDraw(canvas)

        // Vẽ stroke trong (màu trắng)
        paint.strokeWidth = innerStrokeWidth
        setTextColor(innerStrokeColor)
        super.onDraw(canvas)

        // Vẽ text chính (màu trắng fill)
        paint.style = Paint.Style.FILL
        setTextColor(originalColor)
        super.onDraw(canvas)

        // Khôi phục lại paint ban đầu
        paint.style = originalStyle
        paint.strokeJoin = originalJoin
        paint.strokeMiter = originalMiter
        paint.strokeWidth = originalStrokeWidth
    }
}
