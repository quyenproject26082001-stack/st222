package com.piratemaker.postermaker.core.custom.drawview

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.TypedArray
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.DashPathEffect
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PointF
import android.graphics.RectF
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.widget.FrameLayout
import androidx.annotation.IntDef
import androidx.core.content.ContextCompat
import androidx.core.view.MotionEventCompat
import androidx.core.view.ViewCompat
import com.ocmaker.pixcel.maker.data.model.draw.Draw
import com.ocmaker.pixcel.maker.data.model.draw.DrawDraw
import com.ocmaker.pixcel.maker.data.model.draw.DrawableDraw
import com.ocmaker.pixcel.maker.data.model.draw.TextDraw
import poster.maker.core.listener.listenerdraw.BitmapDrawIcon
import com.piratemaker.postermaker.listener.listenerdraw.DeleteEvent
import com.piratemaker.postermaker.listener.listenerdraw.FlipEvent
import com.piratemaker.postermaker.listener.listenerdraw.OnDrawListener
import com.piratemaker.postermaker.listener.listenerdraw.ZoomEvent
import poster.maker.R
import poster.maker.core.utils.key.DrawKey
import java.util.Collections
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.pow
import kotlin.math.sqrt


@SuppressLint("CustomViewStyleable")
open class DrawView(context: Context, attrs: AttributeSet?, defStyleAttr: Int) :
    FrameLayout(context, attrs, defStyleAttr) {

    companion object {
        private const val MIN_SCALE_PERCENT = 0.25f
        private const val MAX_SCALE_MULTIPLIER = 10f
    }

    @IntDef(
        DrawKey.NONE, DrawKey.DRAG, DrawKey.ZOOM_WITH_TWO_FINGER, DrawKey.ICON, DrawKey.CLICK
    )
    @Retention(AnnotationRetention.SOURCE)
    protected annotation class ActionMode

    @IntDef(flag = true, value = [DrawKey.FLIP_HORIZONTALLY, DrawKey.FLIP_VERTICALLY])
    @Retention(AnnotationRetention.SOURCE)
    protected annotation class Flip

    val drawList = ArrayList<DrawableDraw>()
    private val iconList = ArrayList<BitmapDrawIcon>()

    private val initialScaleMap = HashMap<DrawableDraw, Float>()
    private var lastCheckedScale = -1f
    private var lastScaleValid = true

    public val undoList = ArrayList<List<DrawableDraw>>()
    public val undoTempList = ArrayList<DrawableDraw>()

    private val borderPaint = Paint()
    private val stickerRect = RectF()

    private val bitmapPoints = FloatArray(8)
    private val bounds = FloatArray(8)
    private val point = FloatArray(2)
    private val currentCenterPoint = PointF()
    private val temp = FloatArray(2)
    private var midPoint = PointF()

    private val sizeMatrix = Matrix()
    private val downMatrix = Matrix()
    private val moveMatrix = Matrix()

    private var isLocked: Boolean = false
    private var constrained: Boolean = false

    private var OnDrawListener: OnDrawListener? = null

    private val isShowIcons: Boolean
    private val isShowBorder: Boolean
    private val bringToFrontCurrentSticker: Boolean

    private val touchSlop: Int

    @ActionMode
    private var currentMode = DrawKey.NONE

    private var handlingDraw: DrawableDraw? = null

    private var lastClickTime: Long = 0L
    private var minClickDelayTime = DrawKey.SINGLE_CLICK_TIME - 300

    private var currentIcon: BitmapDrawIcon? = null
    private var downX: Float = 0f
    private var downY: Float = 0f

    private var oldDistance: Float = 0f
    private var oldRotation: Float = 0f

    private val handler = Handler(Looper.getMainLooper())

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)

    init {
        touchSlop = ViewConfiguration.get(context).scaledTouchSlop
        val typedArray: TypedArray = context.obtainStyledAttributes(attrs, R.styleable.StickerView)
        try {
            isShowIcons = typedArray.getBoolean(R.styleable.StickerView_showIcons, true)
            isShowBorder = typedArray.getBoolean(R.styleable.StickerView_showBorder, true)
            bringToFrontCurrentSticker =
                typedArray.getBoolean(R.styleable.StickerView_bringToFrontCurrentSticker, false)

            borderPaint.isAntiAlias = true
            borderPaint.color = typedArray.getColor(
                R.styleable.StickerView_borderColor, ContextCompat.getColor(context, R.color.white)
            )
            borderPaint.alpha = typedArray.getInteger(R.styleable.StickerView_borderAlpha, 128)
            borderPaint.strokeWidth = 10.0f
            borderPaint.pathEffect = DashPathEffect(floatArrayOf(28f, 18f), 0f)

            setupDefaultIcons()
        } finally {
            typedArray.recycle()
        }
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        if (changed) {
            stickerRect.left = left.toFloat()
            stickerRect.top = top.toFloat()
            stickerRect.right = right.toFloat()
            stickerRect.bottom = bottom.toFloat()
        }
    }

    override fun dispatchDraw(canvas: Canvas) {
        super.dispatchDraw(canvas)
        drawDraws(canvas)
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        if (isLocked) {
            Log.d("onInterceptTouchEvent", "Locking")
            return super.onInterceptTouchEvent(ev)
        }
        Log.d("onInterceptTouchEvent", "Unlocking ")

        when (ev.action) {
            MotionEvent.ACTION_DOWN -> {
                downX = ev.x
                downY = ev.y

                val hasIcon = targetCurrentDraw() != null
                val hasSticker = targetHandlingDraw() != null
                val shouldIntercept = hasIcon || hasSticker

                Log.d("DrawView_Intercept", "ACTION_DOWN: hasIcon=$hasIcon, hasSticker=$hasSticker → shouldIntercept=$shouldIntercept")

                return shouldIntercept
            }
        }

        return super.onInterceptTouchEvent(ev)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (isLocked) {
            Log.d("onTouchEvent", "Locking")
            return super.onTouchEvent(event)
        }
        Log.d("onTouchEvent", "Unlocking")
        when (MotionEventCompat.getActionMasked(event)) {
            MotionEvent.ACTION_DOWN -> {
                if (!touchDown(event)) {
                    return false
                }
            }

            MotionEvent.ACTION_POINTER_DOWN -> {
                oldDistance = calcDistance(event)
                oldRotation = calcRotation(event)

                midPoint = calcMidPoint(event)

                if (handlingDraw != null && isFocusDraw(
                        handlingDraw!!, event.getX(1), event.getY(1)
                    ) && targetCurrentDraw() == null
                ) {
                    currentMode = DrawKey.ZOOM_WITH_TWO_FINGER
                }
                Log.d("MotionEvent.ACTION_POINTER_DOWN", "ACTION_POINTER_DOWN")
            }

            MotionEvent.ACTION_MOVE -> {
                if (handlingDraw != null) {
                    if (!isLocking()) {
                        handleCurrentMode(event)
                        invalidate()
                    }
                }
            }

            MotionEvent.ACTION_UP -> touchUp(event)
            MotionEvent.ACTION_POINTER_UP -> {
                if (currentMode == DrawKey.ZOOM_WITH_TWO_FINGER && handlingDraw != null) {
                    if (OnDrawListener != null) {
                        OnDrawListener!!.onZoomFinishedDraw(handlingDraw!!)

                        saveDrawState()
                    }
                }
                currentMode = DrawKey.NONE
                Log.d("MotionEvent.ACTION_POINTER_UP", "ACTION_POINTER_UP")
            }
        }
        return true
    }

    override fun onSizeChanged(w: Int, h: Int, oldW: Int, oldH: Int) {
        super.onSizeChanged(w, h, oldW, oldH)
    }

    fun removeDrawCurrent(): Boolean {
        OnDrawListener?.onHideOptionIconDraw()
        return remove(handlingDraw)
    }

    fun hideSelect() {
        OnDrawListener?.onHideOptionIconDraw()
        handlingDraw = null
        invalidate()
    }

    fun setOnDrawListener(OnDrawListener: OnDrawListener?): DrawView {
        this.OnDrawListener = OnDrawListener
        return this
    }

    fun getOnDrawListener(): OnDrawListener? {
        return OnDrawListener
    }

    fun getStickerCount(): Int {
        return drawList.size
    }

    fun getDraws(): List<DrawableDraw> {
        return drawList
    }

    fun rotateZoomCurrentDraw(event: MotionEvent) {
        rotateZoomDraw(handlingDraw, event)
    }

    fun flipCurrentDraw(direction: Int) {
        flip(handlingDraw, direction)
    }

    fun remove(draw: Draw?): Boolean {
        if (drawList.contains(draw)) {
            // Bảo vệ character - không cho xóa
            if (draw is DrawableDraw && draw.isCharacter) {
                Log.d("Draw View", "remove: cannot remove character sticker")
                return false
            }

            drawList.remove(draw)

            // Clear khỏi initialScaleMap để tránh memory leak
            if (draw is DrawableDraw) {
                initialScaleMap.remove(draw)
            }

            OnDrawListener?.onDeletedDraw(draw!!)
            if (handlingDraw == draw) {
                handlingDraw = null
            }

            // save undo khi xóa xong
            saveDrawState()

            invalidate()

            return true
        } else {
            Log.d("Draw View", "remove: the sticker is not in this StickerView")
            return false
        }
    }

    fun exchangeLayers(oldP: Int, newP: Int) {
        if (drawList.size >= oldP && drawList.size >= newP) {
            Collections.swap(drawList, oldP, newP)

            saveDrawState()
            Log.d("Function: SwapLayers", "oldPosition = $oldP, newPosition = $newP")
            invalidate()
        }
    }

    fun showOrHideDraw(draw: Draw, pos: Int) {
        if (drawList.size > 0) {
            draw.setHide(!draw.isHide)

            saveDrawState()
            invalidate()
        }
    }

    fun addDraw(draw: DrawableDraw): DrawView {
        return addDraw(draw, DrawKey.CENTER)
    }

    fun fillData(draw: ArrayList<DrawableDraw>) {
        this.drawList.clear()
        this.drawList.addAll(draw)

        // Initialize initialScaleMap cho tất cả stickers
        for (d in draw) {
            initialScaleMap[d] = d.currentScale
        }

        saveDrawState()
        postInvalidate()
    }

    private var bitm: Bitmap? = null
    fun setBitm(bitmap: Bitmap) {
        bitm = bitmap
    }

    fun save(): Bitmap {
        handlingDraw = null
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        this.draw(canvas)
        return bitmap
    }

    fun saveFirst(): Bitmap {
        handlingDraw = null
        val bitmap = Bitmap.createBitmap(70, 70, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        this.draw(canvas)
        return bitmap
    }

    fun isLocking(): Boolean {
        return handlingDraw?.isLock ?: false
    }

    fun setConstrained(constrained: Boolean): DrawView {
        this.constrained = constrained
        postInvalidate()
        return this
    }

    fun unSelectCurrentDraw() {
        handlingDraw = null
        invalidate()
    }

    fun selectCurrentDraw(draw: DrawableDraw) {
        handlingDraw = draw
        invalidate()
    }

    // Auto select first draw to show border immediately
    fun autoSelectFirstDraw() {
        if (drawList.isNotEmpty()) {
            handlingDraw = drawList[0]
            OnDrawListener?.onTouchedDownDraw(handlingDraw!!)
            invalidate()
        }
    }

    fun setLocked(locked: Boolean): DrawView {
        this.isLocked = locked
        invalidate()
        return this
    }

    fun getCurrentDraw(): DrawableDraw? {
        return handlingDraw
    }

    public val redoList = ArrayList<List<DrawableDraw>>()

//    fun undo() {
//        if (undoList.size > 1) {
//            undoTempList.clear()
//            undoTempList.addAll(undoList[undoList.size - 1])
//            redoList.add(undoList.removeAt(undoList.size - 1))
//            val previousDraw = undoList[undoList.size - 1]
//            removeAllDraw()
//            for (draw in previousDraw) {
//                when (draw) {
//                    is DrawableDraw -> {
//                        val drawableStickerNew = configDrawableDraw(draw, false)
//                        drawList.add(drawableStickerNew)
//                    }
//
//                    is TextDraw -> {
//                        val textStickerNew = configTextDraw(draw, false)
//                        drawList.add(textStickerNew)
//                    }
//
//                    is DrawDraw -> {
//                        val drawStickerNew = configDrawDraw(draw, false)
//                        drawList.add(drawStickerNew)
//                    }
//                }
//                invalidate()
//            }
//        } else {
//            removeAllDraw()
//            redoList.add(undoList.removeAt(undoList.size - 1))
//            undoList.clear()
//            if (OnDrawListener != null) {
//                OnDrawListener!!.onUndoDeleteAll()
//            }
//        }
//    }
//
//    fun redo() {
//        if (redoList.size > 1) {
//            removeAllDraw()
//            val redoTempList = redoList[redoList.size - 1]
//            undoList.add(redoTempList)
//            redoList.removeAt(redoList.size - 1)
//            for (draw in redoTempList) {
//                when (draw) {
//                    is DrawableDraw -> {
//                        val drawableStickerNew = configDrawableDraw(draw, false)
//                        drawList.add(drawableStickerNew)
//                    }
//
//                    is TextDraw -> {
//                        val textStickerNew = configTextDraw(draw, false)
//                        drawList.add(textStickerNew)
//                    }
//
//                    is DrawDraw -> {
//                        val drawStickerNew = configDrawDraw(draw, false)
//                        drawList.add(drawStickerNew)
//                    }
//                }
//                invalidate()
//            }
//        } else {
//            removeAllDraw()
//            val redoTempList = redoList[redoList.size - 1]
//            undoList.add(redoTempList)
//            redoList.removeAt(redoList.size - 1)
//            for (draw in redoTempList) {
//                when (draw) {
//                    is DrawableDraw -> {
//                        val drawableStickerNew = configDrawableDraw(draw, false)
//                        drawList.add(drawableStickerNew)
//                    }
//
//                    is TextDraw -> {
//                        val textStickerNew = configTextDraw(draw, false)
//                        drawList.add(textStickerNew)
//                    }
//
//                    is DrawDraw -> {
//                        val drawStickerNew = configDrawDraw(draw, false)
//                        drawList.add(drawStickerNew)
//                    }
//                }
//                invalidate()
//            }
//            OnDrawListener!!.onRedoAll()
//        }
//    }

    fun removeAllDraw() {
        drawList.clear()
        initialScaleMap.clear()  // Clear để tránh memory leak
        handlingDraw?.release()
        handlingDraw = null
        invalidate()
    }

    private fun saveDrawState() {
        val drawCopy = ArrayList<DrawableDraw>()
        for (draw in drawList) {
            when (draw) {
                is DrawableDraw -> {
                    val drawableStickerNew = configDrawableDraw(draw, false)
                    drawCopy.add(drawableStickerNew)
                }

//                is TextDraw -> {
//                    val textStickerNew = configTextDraw(draw, false)
//                    drawCopy.add(textStickerNew)
//                }
//
//                is DrawDraw -> {
//                    val drawStickerNew = configDrawDraw(draw, false)
//                    drawCopy.add(drawStickerNew)
//                }
            }
        }
        undoList.add(drawCopy)
    }

    private fun findDrawNotInList2(list1: List<DrawableDraw>, list2: List<DrawableDraw>): List<DrawableDraw> {
        val result = ArrayList<DrawableDraw>()

        for (model in list1) {
            var found = false
            for (model2 in list2) {
                if (model.pagerSelected == model2.pagerSelected && model.positionSelected == model2.positionSelected) {
                    found = true
                    break
                }
            }
            if (!found) {
                result.add(model)
            }
        }

        return result
    }

    private fun configTextDraw(textDraw: TextDraw, checkClone: Boolean): TextDraw {
        val drawableNew = textDraw.drawable.constantState!!.newDrawable().mutate()
        val textDrawNew = TextDraw(context, drawableNew, textDraw.drawablePath)
        val matrix = Matrix(textDraw.getMatrix())
        if (checkClone) {
            matrix.postTranslate(0.0F, 30.0F)
        }
        textDrawNew.setMatrix(matrix)
        textDrawNew.setText(textDraw.text)
        textDrawNew.setHide(textDraw.isHide)
        textDrawNew.setLock(textDraw.isLock)
        textDrawNew.setFlippedH(textDraw.isFlippedH)
        textDrawNew.setFlippedV(textDraw.isFlippedV)
        textDrawNew.setTextColor(textDraw.textColor1)
        textDrawNew.textCheckAlign = (textDraw.textCheckAlign)
        textDrawNew.setTextAlign(textDraw.textAlign)
        textDrawNew.idTypeFace = (textDraw.idTypeFace)
        textDrawNew.setTypeface(textDraw.typeface)
        textDrawNew.resizeText()
        return textDrawNew
    }

    private fun configDrawDraw(drawDraw: DrawDraw, checkClone: Boolean): DrawDraw {
        val drawable = drawDraw.drawable.constantState!!.newDrawable().mutate()
        val drawDrawNew = DrawDraw(drawable, drawDraw.drawablePath)
        val matrix = Matrix(drawDraw.getMatrix())
        if (checkClone) {
            matrix.postTranslate(0.0F, 30.0F)
        }
        drawDrawNew.setMatrix(matrix)
        drawDrawNew.setHide(drawDraw.isHide)
        drawDrawNew.setLock(drawDraw.isLock)
        drawDrawNew.setFlippedH(drawDraw.isFlippedH)
        drawDrawNew.setFlippedV(drawDraw.isFlippedV)
        return drawDrawNew
    }

    private fun addDraw(draw: DrawableDraw, position: Int): DrawView {
        if (ViewCompat.isLaidOut(this)) {
            addDrawImmediately(draw, position)
        } else {
            post {
                addDrawImmediately(draw, position)
            }
        }
        return this
    }

    private fun addDrawImmediately(draw: DrawableDraw, position: Int) {
        setDrawPosition(draw, position)

        val widthScaleFactor = width.toFloat() / draw.drawable.intrinsicWidth
        val heightScaleFactor = height.toFloat() / draw.drawable.intrinsicHeight
        val scaleFactor = if (widthScaleFactor > heightScaleFactor) heightScaleFactor else widthScaleFactor

        if (draw.isText) {
            draw.getMatrix().postScale(scaleFactor / 1.2f, scaleFactor / 1.2f, width / 2f, height / 2f)
        } else if (draw.isCharacter) {
            draw.getMatrix().postScale(scaleFactor / 1.2f, scaleFactor / 1.2f, width / 2f, height / 2f)
        } else {
            draw.getMatrix().postScale(scaleFactor / 2f, scaleFactor / 2f, width / 2f, height / 2f)
        }

        // Lưu initial scale để dùng cho validation sau này
        initialScaleMap[draw] = draw.currentScale

        handlingDraw = draw
        drawList.add(draw)

        // save undo khi add xong
        saveDrawState()

        OnDrawListener?.onAddedDraw(draw)
        invalidate()
    }

    private fun touchDown(event: MotionEvent): Boolean {
        currentMode = DrawKey.DRAG

        downX = event.x
        downY = event.y

        midPoint = calcMidPoint()
        oldDistance = calcDistance(midPoint.x, midPoint.y, downX, downY)
        oldRotation = calcRotation(midPoint.x, midPoint.y, downX, downY)

        Log.d("DrawView_TouchDown", "========== TOUCH DOWN START ==========")
        Log.d("DrawView_TouchDown", "Touch position: x=$downX, y=$downY")
        Log.d("DrawView_TouchDown", "Total stickers in drawList: ${drawList.size}")

        // ✅ FIX: Tìm icon TRƯỚC (dựa trên handlingDraw CŨ)
        currentIcon = targetCurrentDraw()

        // Nếu KHÔNG có icon → Tìm sticker mới tại touch position
        if (currentIcon == null) {
            Log.d("DrawView_TouchDown", "No icon touched, finding sticker at touch position...")
            handlingDraw = targetHandlingDraw()
        } else {
            // Có icon → GIỮ NGUYÊN handlingDraw (không update)
            currentMode = DrawKey.ICON
            currentIcon!!.onActionDown(this, event)
            Log.d("DrawView_TouchDown", "✓ Icon touched: ${currentIcon} → Keep current handlingDraw")
        }
        if (handlingDraw != null) {
            Log.d("DrawView_TouchDown", "✓ Sticker FOUND! handlingDraw = ${handlingDraw}")
            OnDrawListener!!.onTouchedDownDraw(handlingDraw!!)
            downMatrix.set(handlingDraw!!.getMatrix())

            if (bringToFrontCurrentSticker) {
                drawList.remove(handlingDraw)
                drawList.add(handlingDraw!!)
                Log.d("DrawView_TouchDown", "Brought sticker to front")
            }
        } else {
            Log.e("DrawView_TouchDown", "✗ NO STICKER FOUND at x=$downX, y=$downY")
            Log.e("DrawView_TouchDown", "DrawList has ${drawList.size} stickers but none matched")
        }

        if (currentIcon == null && handlingDraw == null) {
            Log.e("DrawView_TouchDown", "✗✗ FAILED: No icon and no sticker found → returning FALSE")
            Log.d(
                "Function: onTouchDown", "CurrentIcon: $currentIcon, HandingDraw: $handlingDraw"
            )
            return false
        }
        Log.d("DrawView_TouchDown", "========== TOUCH DOWN SUCCESS → invalidate() ==========")
        invalidate()
        return true
    }

    private fun touchUp(event: MotionEvent) {
        val currentTime = SystemClock.uptimeMillis()

        if (currentMode == DrawKey.ICON && currentIcon != null && handlingDraw != null) {
            currentIcon!!.onActionUp(this, event)
        }

        if (currentMode == DrawKey.DRAG && Math.abs(event.x - downX) < touchSlop && Math.abs(
                event.y - downY
            ) < touchSlop && handlingDraw != null
        ) {
            currentMode = DrawKey.CLICK
            if (OnDrawListener != null) {
                OnDrawListener!!.onClickedDraw(handlingDraw!!)
            }
            if (currentTime - lastClickTime < minClickDelayTime) {
                if (OnDrawListener != null) {
                    OnDrawListener!!.onDoubleTappedDraw(handlingDraw!!)
                }
            }
        }

        if (currentMode == DrawKey.DRAG && handlingDraw != null) {
            if (OnDrawListener != null) {
                OnDrawListener!!.onDragFinishedDraw(handlingDraw!!)

                saveDrawState()
            }
        }

        currentMode = DrawKey.NONE
        lastClickTime = currentTime
    }

    private fun drawDraws(canvas: Canvas) {
        for (i in 0 until drawList.size) {
            val draw = drawList[i]
            if (!draw.isHide) {
                draw.draw(canvas)
            }
        }

        if (handlingDraw != null && !isLocked && (isShowBorder || isShowIcons)) {
            Log.d("DrawView_Draw", "✓ Drawing handle box for handlingDraw")
            getDrawPoints(handlingDraw, bitmapPoints)

            val x1 = bitmapPoints[0]
            val y1 = bitmapPoints[1]
            val x2 = bitmapPoints[2]
            val y2 = bitmapPoints[3]
            val x3 = bitmapPoints[4]
            val y3 = bitmapPoints[5]
            val x4 = bitmapPoints[6]
            val y4 = bitmapPoints[7]

            if (isShowBorder) {
                canvas.drawLine(x1, y1, x2, y2, borderPaint)
                canvas.drawLine(x1, y1, x3, y3, borderPaint)
                canvas.drawLine(x2, y2, x4, y4, borderPaint)
                canvas.drawLine(x4, y4, x3, y3, borderPaint)
            }

            // draw icons
            if (isShowIcons) {
                val rotation = calcRotation(x4, y4, x3, y3)
                for (i in 0 until iconList.size) {
                    val icon = iconList[i]
                    when (icon.positionDefault) {
                        DrawKey.TOP_LEFT -> setupMatrix(icon, x1, y1, rotation)
                        DrawKey.RIGHT_TOP -> setupMatrix(icon, x2, y2, rotation)
                        DrawKey.LEFT_BOTTOM -> setupMatrix(icon, x3, y3, rotation)
                        DrawKey.RIGHT_BOTTOM -> setupMatrix(icon, x4, y4, rotation)
                    }
                    if (icon.positionDefault == DrawKey.RIGHT_TOP) {
                        // Don't draw delete icon for character
                        if (!handlingDraw!!.isCharacter) {
                            icon.draw(canvas, borderPaint)
                        }
                    } else {
                        // Draw all other icons (zoom, flip, etc.)
                        icon.draw(canvas, borderPaint)
                    }
                }
            }
        } else {
            if (handlingDraw == null) {
                Log.w("DrawView_Draw", "✗ NOT drawing handle box: handlingDraw is NULL")
            } else if (isLocked) {
                Log.w("DrawView_Draw", "✗ NOT drawing handle box: isLocked=$isLocked")
            } else if (!isShowBorder && !isShowIcons) {
                Log.w("DrawView_Draw", "✗ NOT drawing handle box: isShowBorder=$isShowBorder, isShowIcons=$isShowIcons")
            }
        }
    }

    private fun setupMatrix(icon: BitmapDrawIcon, x: Float, y: Float, rotation: Float) {
        icon.x = x
        icon.y = y
        icon.getMatrix().reset()

        icon.getMatrix().postRotate(rotation, icon.width / 2f, icon.height / 2f)
        icon.getMatrix().postTranslate(x - icon.width / 2f, y - icon.height / 2f)
    }

    private fun handleCurrentMode(event: MotionEvent) {
        when (currentMode) {
            DrawKey.NONE, DrawKey.CLICK -> {
                Log.d("handleCurrentMode", "CLICK")
            }

            DrawKey.DRAG -> {
                if (handlingDraw != null) {
                    moveMatrix.set(downMatrix)
                    moveMatrix.postTranslate(event.x - downX, event.y - downY)
                    handlingDraw!!.setMatrix(moveMatrix)
                    if (constrained) {
                        constrainDraw(handlingDraw!!)
                        Log.d("Action: DRAG", "DRAGGING")
                    }
                }
                Log.d("Action: DRAG", "DRAG")
            }

            DrawKey.ZOOM_WITH_TWO_FINGER -> {
                if (handlingDraw != null) {
                    val newDistance = calcDistance(event)
                    val newRotation = calcRotation(event)

                    moveMatrix.set(downMatrix)
                    moveMatrix.postScale(
                        newDistance / oldDistance, newDistance / oldDistance, midPoint.x, midPoint.y
                    )
                    moveMatrix.postRotate(newRotation - oldRotation, midPoint.x, midPoint.y)

                    // Chỉ apply matrix nếu scale hợp lệ
                    if (isScaleValid(handlingDraw!!, moveMatrix)) {
                        handlingDraw!!.setMatrix(moveMatrix)
                    }
                }
                Log.d("Action: ZOOM_WITH_TWO_FINGER", "ZOOM_WITH_TWO_FINGER")
            }

            DrawKey.ICON -> {
                if (handlingDraw != null && currentIcon != null) {
                    currentIcon!!.onActionMove(this, event)
                }
                Log.d("Action: Icon", "ICON")
            }
        }
    }

    private fun rotateZoomDraw(draw: Draw?, event: MotionEvent) {
        if (draw != null) {
            val newDistance = calcDistance(midPoint.x, midPoint.y, event.x, event.y)
            val newRotation = calcRotation(midPoint.x, midPoint.y, event.x, event.y)
            Log.e("HVV1312", "OK ???: $midPoint.x va ${event.x}")

            moveMatrix.set(downMatrix)
            moveMatrix.postScale(
                newDistance / oldDistance, newDistance / oldDistance, midPoint.x, midPoint.y
            )
            moveMatrix.postRotate(newRotation - oldRotation, midPoint.x, midPoint.y)

            // Chỉ apply matrix nếu scale hợp lệ
            if (draw is DrawableDraw && isScaleValid(draw, moveMatrix)) {
                draw.setMatrix(moveMatrix)
            }
        }
    }

    private fun constrainDraw(draw: Draw) {
        var moveX = 0f
        var moveY = 0f
        val width = width
        val height = height
        draw.getMappedCenterPoint(currentCenterPoint, point, temp)
        if (currentCenterPoint.x < 0) {
            moveX = -currentCenterPoint.x
        }
        if (currentCenterPoint.x > width) {
            moveX = width - currentCenterPoint.x
        }
        if (currentCenterPoint.y < 0) {
            moveY = -currentCenterPoint.y
        }
        if (currentCenterPoint.y > height) {
            moveY = height - currentCenterPoint.y
        }
        draw.getMatrix().postTranslate(moveX, moveY)
    }

    private fun targetCurrentDraw(): BitmapDrawIcon? {
        // ✅ FIX: Icon chỉ detect khi có sticker được chọn
        if (handlingDraw == null) {
            Log.d("DrawView_TargetIcon", "No handlingDraw → skip icon detection")
            return null
        }

        // Check từng icon xem có bị touch không
        for (icon in iconList) {
            // Skip delete icon for character
            if (handlingDraw?.isCharacter == true) {
                if (icon.positionDefault == DrawKey.RIGHT_TOP) {
                    continue
                }
            }

            val x = icon.x - downX
            val y = icon.y - downY
            val distance = x * x + y * y
            if (distance <= (icon.radius + icon.radius).toDouble().pow(2.0)) {
                Log.d("DrawView_TargetIcon", "✓ Icon touched at distance=${sqrt(distance.toDouble())}")
                return icon
            }
        }
        Log.d("DrawView_TargetIcon", "No icon matched at touch position")
        return null
    }

    private fun targetHandlingDraw(): DrawableDraw? {
        Log.d("DrawView_TargetDraw", "--- Searching for sticker at x=$downX, y=$downY ---")
        for (i in drawList.indices.reversed()) {
            val draw = drawList[i]
            val isFocused = isFocusDraw(draw, downX, downY)
            Log.d("DrawView_TargetDraw", "Sticker[$i]: isFocused=$isFocused, isHide=${draw.isHide}")
            if (isFocused) {
                Log.d("DrawView_TargetDraw", "✓ Found target sticker at index $i")
                return draw
            }
        }
        Log.e("DrawView_TargetDraw", "✗ No sticker matched the touch position!")
        return null
    }

    private fun isFocusDraw(draw: DrawableDraw, downX: Float, downY: Float): Boolean {
        temp[0] = downX
        temp[1] = downY
        val contains = draw.contains(temp)
        if (!contains) {
            Log.d("DrawView_IsFocus", "  → Sticker bounds check: touch($downX, $downY) NOT in sticker bounds")
        } else {
            Log.d("DrawView_IsFocus", "  → ✓ Touch IS inside sticker bounds!")
        }
        return contains
    }

    private fun calcMidPoint(event: MotionEvent?): PointF {
        if (event == null || event.pointerCount < 2) {
            midPoint[0f] = 0f
            return midPoint
        }
        val x = (event.getX(0) + event.getX(1)) / 2
        val y = (event.getY(0) + event.getY(1)) / 2
        midPoint[x] = y
        return midPoint
    }

    private fun calcMidPoint(): PointF {
        if (handlingDraw == null) {
            midPoint[0f] = 0f
            return midPoint
        }
        handlingDraw!!.getMappedCenterPoint(midPoint, point, temp)
        return midPoint
    }

    private fun setDrawPosition(draw: Draw, position: Int) {
        val width = width.toFloat()
        val height = height.toFloat()
        var offsetX = width - draw.width
        var offsetY = height - draw.height
        if (position and DrawKey.TOP > 0) {
            offsetY /= 4f
        } else if (position and DrawKey.BOTTOM > 0) {
            offsetY *= 3f / 4f
        } else {
            offsetY /= 2f
        }
        if (position and DrawKey.LEFT > 0) {
            offsetX /= 4f
        } else if (position and DrawKey.RIGHT > 0) {
            offsetX *= 3f / 4f
        } else {
            offsetX /= 2f
        }
        draw.getMatrix().postTranslate(offsetX, offsetY)
    }

    private fun getDrawPoints(draw: Draw?, dst: FloatArray) {
        if (draw == null) {
            dst.fill(0f)
            return
        }
        draw.getBoundPoints(bounds)
        draw.getMappedPoints(dst, bounds)
    }

    private fun findDrawInList2NotInList1(list1: List<DrawableDraw>, list2: List<DrawableDraw>): List<DrawableDraw> {
        val result = ArrayList<DrawableDraw>()

        for (model2 in list2) {
            var found = false

            for (model in list1) {
                if (model2.pagerSelected == model.pagerSelected && model2.positionSelected == model.positionSelected) {
                    found = true
                    break
                }
            }

            if (!found) {
                result.add(model2)
            }
        }

        return result
    }

    private fun configDrawableDraw(
        drawableDraw: DrawableDraw, checkClone: Boolean
    ): DrawableDraw {
        val drawable = drawableDraw.getOriginalDrawable().constantState!!.newDrawable().mutate()
        val drawableDrawNew = DrawableDraw(drawable, drawableDraw.drawablePath)
        val matrix = Matrix(drawableDraw.getMatrix())
        if (checkClone) {
            matrix.postTranslate(0.0F, 30.0F)
        }
        drawableDrawNew.setMatrix(matrix)
        drawableDrawNew.setPagerSelected(drawableDraw.pagerSelected)
        drawableDrawNew.setPositionSelected(drawableDraw.positionSelected)
        drawableDrawNew.setHide(drawableDraw.isHide)
        drawableDrawNew.setLock(drawableDraw.isLock)
        drawableDrawNew.setFlippedH(drawableDraw.isFlippedH)
        drawableDrawNew.setFlippedV(drawableDraw.isFlippedV)
        return drawableDrawNew
    }

    private fun calcRotation(event: MotionEvent?): Float {
        return if (event == null || event.pointerCount < 2) {
            0f
        } else calcRotation(event.getX(0), event.getY(0), event.getX(1), event.getY(1))
    }

    private fun calcRotation(x1: Float, y1: Float, x2: Float, y2: Float): Float {
        val x = x1 - x2
        val y = y1 - y2
        val radians = atan2(y.toDouble(), x.toDouble())
        return Math.toDegrees(radians).toFloat()
    }

    private fun calcDistance(event: MotionEvent?): Float {
        if (event == null || event.pointerCount < 2) {
            return 0f
        }
        return calcDistance(event.getX(0), event.getY(0), event.getX(1), event.getY(1))
    }

    private fun calcDistance(x1: Float, y1: Float, x2: Float, y2: Float): Float {
        val x = x1 - x2
        val y = y1 - y2
        return sqrt(x * x + y * y.toDouble()).toFloat()
    }

    fun editText() {
        OnDrawListener?.onEditText(handlingDraw!!)
    }

    private fun setupDefaultIcons() {
        val deleteIcon = BitmapDrawIcon(
            ContextCompat.getDrawable(context, R.drawable.ic_delete_sticker), DrawKey.LEFT_BOTTOM
        )
        deleteIcon.event = DeleteEvent()

        val zoomIcon = BitmapDrawIcon(
            ContextCompat.getDrawable(context, R.drawable.ic_rotate_scale), DrawKey.RIGHT_TOP
        )
        zoomIcon.event = ZoomEvent()

        val flipIcon = BitmapDrawIcon(
            ContextCompat.getDrawable(context, R.drawable.ic_flip_sticker), DrawKey.RIGHT_BOTTOM
        )
        flipIcon.event = FlipEvent()

        val editIcon = BitmapDrawIcon(
            ContextCompat.getDrawable(context, R.drawable.ic_flip_sticker), DrawKey.TOP_LEFT
        )
        editIcon.event = FlipEvent()

        iconList.clear()
        iconList.add(deleteIcon)
        iconList.add(flipIcon)
        iconList.add(zoomIcon)
    }

    private fun flip(draw: Draw?, direction: Int) {
        if (draw != null) {
            if (!draw.isLock) {
                draw.getCenterPoint(midPoint)
                if ((direction and DrawKey.FLIP_HORIZONTALLY) > 0) {
                    draw.getMatrix().preScale(-1f, 1f, midPoint.x, midPoint.y)
                    draw.setFlippedH(!draw.isFlippedH)
                }
                if ((direction and DrawKey.FLIP_VERTICALLY) > 0) {
                    draw.getMatrix().preScale(1f, -1f, midPoint.x, midPoint.y)
                    draw.setFlippedV(!draw.isFlippedV)
                }

                if (OnDrawListener != null) {
                    OnDrawListener!!.onFlippedDraw(draw)
                }

                saveDrawState()

                invalidate()
            }
        }
    }

    fun replace(stickerOld: DrawableDraw, stickerNew: DrawableDraw) {
        val index = drawList.indexOfFirst { it == stickerOld }
        if (index >= 0) {
            drawList.removeAt(index)
            replaceSticker(stickerNew, index)
            invalidate()
        }
    }

    private fun replaceSticker(sticker: DrawableDraw, position: Int): DrawView {
        if (ViewCompat.isLaidOut(this)) {
            replaceStickerImmediately(sticker, position)
        } else {
            post {
                replaceStickerImmediately(sticker, position)
            }
        }
        return this
    }

    private fun replaceStickerImmediately(sticker: DrawableDraw, position: Int) {
        setDrawPosition(sticker, DrawKey.CENTER)

        val widthScaleFactor = width.toFloat() / sticker.drawable.intrinsicWidth
        val heightScaleFactor = height.toFloat() / sticker.drawable.intrinsicHeight
        val scaleFactor = if (widthScaleFactor > heightScaleFactor) heightScaleFactor else widthScaleFactor

        sticker.getMatrix().postScale(scaleFactor / 2f, scaleFactor / 2f, width / 2f, height / 2f)

        handlingDraw = sticker
        drawList.add(position, sticker)

        // save undo khi add xong
//        saveStickerState()

        OnDrawListener?.onReplace(sticker)
        invalidate()
    }

    private fun isScaleValid(draw: Draw, matrix: Matrix): Boolean {
        val initialScale = initialScaleMap[draw] ?: return true
        val minAllowedScale = initialScale * MIN_SCALE_PERCENT
        val maxAllowedScale = initialScale * MAX_SCALE_MULTIPLIER

        // Tính scale hiện tại từ matrix
        val values = FloatArray(9)
        matrix.getValues(values)
        val scaleX = values[Matrix.MSCALE_X]
        val skewY = values[Matrix.MSKEW_Y]
        val currentScale = sqrt(scaleX * scaleX + skewY * skewY)

        // Cache kết quả nếu scale không thay đổi nhiều (tối ưu performance)
        if (abs(currentScale - lastCheckedScale) < 0.001f) {
            return lastScaleValid
        }

        lastCheckedScale = currentScale
        lastScaleValid = currentScale in minAllowedScale..maxAllowedScale
        return lastScaleValid
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        // Clear tất cả pending tasks
        handler.removeCallbacksAndMessages(null)
    }
}

