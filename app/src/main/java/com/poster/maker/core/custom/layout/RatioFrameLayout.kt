package poster.maker.core.custom.layout

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.ImageView
import poster.maker.R
import poster.maker.core.custom.imageview.StrokeImageView
import poster.maker.core.extensions.setOnSingleClick
import kotlin.compareTo

class RatioFrameLayout @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    data class RatioBounds(val startX: Float, val startY: Float, val endX: Float, val endY: Float)

    private val childBounds = mutableMapOf<FrameLayout, RatioBounds>()
    private val imageViews = mutableListOf<StrokeImageView>()

    var selectedView: StrokeImageView? = null
        private set
    private val selectedViews = mutableSetOf<StrokeImageView>()

    // ratio mặc định = không ép (0f)
    private var aspectRatio: Float = 0f

    init {
        context.theme.obtainStyledAttributes(attrs, R.styleable.RatioFrameLayout, 0, 0).apply {
            try {
                val ratioStr = getString(R.styleable.RatioFrameLayout_ratio)
                ratioStr?.let {
                    aspectRatio = parseRatio(it)
                }
            } finally {
                recycle()
            }
        }
    }

    fun setAspectRatio(ratio: Float) {
        aspectRatio = ratio
        requestLayout()
    }

    fun addImageWithRatio(bitmap: Bitmap, bounds: RatioBounds, event: EventRatioFrame): StrokeImageView {
        val container = FrameLayout(context).apply {
            id = generateViewId()
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        }

        val imageView = StrokeImageView(context).apply {
            id = generateViewId()
            setImageBitmap(bitmap)
            scaleType = ImageView.ScaleType.CENTER_CROP
            isFocusedStroke = false

            setOnSingleClick {
                setSelectedView(this, container, event)
            }
        }

        container.addView(
            imageView,
            LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        )

        addView(container)
        imageViews.add(imageView)
        childBounds[container] = bounds

        return imageView
    }

    private fun setSelectedView(view: StrokeImageView, container: FrameLayout, event: EventRatioFrame) {
        imageViews.forEach {
            it.isFocusedStroke = false
            selectedViews.remove(it)
            val parent = it.parent as? FrameLayout
            if (parent != null && parent.childCount > 1) {
                parent.removeViewAt(1)
            }
        }

        selectedViews.add(view)
        view.isFocusedStroke = true
        selectedView = view

        val smallButton = ImageView(context).apply {
            id = generateViewId()
            setBackgroundColor(Color.TRANSPARENT)
            scaleType = ImageView.ScaleType.CENTER_INSIDE
        }

        val params = LayoutParams(80, 80).apply {
            gravity = Gravity.TOP or Gravity.END
            setMargins(0, 20, 20, 0)
        }
        container.addView(smallButton, params)

        event.onImageClick(view, smallButton)

        requestLayout()
    }

    fun clearFocusView() {
        imageViews.forEach {
            it.isFocusedStroke = false
            selectedViews.remove(it)
            val parent = it.parent as? FrameLayout
            if (parent != null && parent.childCount > 1) {
                parent.removeViewAt(1)
            }
        }
        requestLayout()
    }

    fun getAllImageViews(): List<ImageView> = imageViews

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        if (aspectRatio > 0f) {
            val width = MeasureSpec.getSize(widthMeasureSpec)
            val height = (width / aspectRatio).toInt()
            val newHeightSpec = MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY)
            super.onMeasure(widthMeasureSpec, newHeightSpec)
        } else {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        }
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        val parentWidth = width
        val parentHeight = height

        for ((container, ratio) in childBounds) {
            var left = (ratio.startX * parentWidth).toInt()
            var top = (ratio.startY * parentHeight).toInt()
            var right = (ratio.endX * parentWidth).toInt()
            var bottom = (ratio.endY * parentHeight).toInt()

            val imageView = container.getChildAt(0) as StrokeImageView

            if (selectedViews.contains(imageView)) {
                // offset giống margin
                left += 10
                top += 10
                right -= 10
                bottom -= 10
            }

            // đo lại container theo size mới
            val widthSpec = MeasureSpec.makeMeasureSpec(right - left, MeasureSpec.EXACTLY)
            val heightSpec = MeasureSpec.makeMeasureSpec(bottom - top, MeasureSpec.EXACTLY)
            container.measure(widthSpec, heightSpec)

            // đặt vị trí cho container
            container.layout(left, top, right, bottom)
        }
    }


    private fun createStrokeDrawable(focused: Boolean): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(Color.TRANSPARENT)
            if (focused) setStroke(6, Color.RED) else setStroke(0, Color.TRANSPARENT)
        }
    }

    private fun parseRatio(ratioStr: String): Float {
        return if (ratioStr.contains(":")) {
            val parts = ratioStr.split(":")
            if (parts.size == 2) {
                val w = parts[0].toFloatOrNull() ?: 1f
                val h = parts[1].toFloatOrNull() ?: 1f
                w / h
            } else 0f
        } else {
            ratioStr.toFloatOrNull() ?: 0f
        }
    }

    fun isEmptyImageViewSelected(): Boolean = selectedViews.isEmpty()
    fun getCurrentImageViewSelected(): StrokeImageView? = selectedView
}
