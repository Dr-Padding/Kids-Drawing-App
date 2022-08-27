package com.drawing.paint

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.Base64
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View


class DrawingView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var drawPaint: Paint? = null
    private var mBrushSize: Float = 0.toFloat()
    private var mEraserSize: Float = 0.toFloat()
    private var mBitmap: Bitmap? = null
    private var mCanvas: Canvas? = null
    private val mBitmapPaint: Paint = Paint(Paint.DITHER_FLAG)
    var eraserOn = false
    var newAdded = false
    var allClear = false
    private val drawPath: Path = Path()
    private val bitmap = mutableListOf<Bitmap>()
    private val undoBitmap = mutableListOf<Bitmap>()

    init {
        setUpDrawing()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (mBitmap == null) {
            mBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            mCanvas = Canvas(mBitmap!!)
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawBitmap(mBitmap!!, 0f, 0f, mBitmapPaint)
        canvas.drawPath(drawPath, drawPaint!!)
    }

    fun onClickEraser(isEraserOn: Boolean) {
        if (isEraserOn) {
            eraserOn = true
            drawPaint!!.xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
        } else {
            eraserOn = false
            drawPaint!!.color = drawPaint!!.color
            drawPaint!!.strokeWidth = mBrushSize
            drawPaint!!.xfermode = null
        }
    }


    fun onClickUndo() {
        if (newAdded) {
            bitmap.add(mBitmap!!.copy(mBitmap!!.config, mBitmap!!.isMutable))
            newAdded = false
        }
        if (bitmap.size > 1) {
            undoBitmap.add(bitmap.removeAt(bitmap.size - 1))
            mBitmap = bitmap[bitmap.size - 1].copy(mBitmap!!.config, mBitmap!!.isMutable)
            mCanvas = Canvas(mBitmap!!)
            invalidate()
            if (bitmap.size == 1) allClear = true
        }
    }

    fun onClickRedo() {
        if (undoBitmap.size > 0) {
            bitmap.add(undoBitmap.removeAt(undoBitmap.size - 1))
            mBitmap = bitmap[bitmap.size - 1].copy(mBitmap!!.config, mBitmap!!.isMutable)
            mCanvas = Canvas(mBitmap!!)
            invalidate()
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val touchX = event.x
        val touchY = event.y
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                newAdded = true
                if (!allClear) bitmap.add(
                    mBitmap!!.copy(
                        mBitmap!!.config,
                        mBitmap!!.isMutable
                    )
                ) else allClear = false
                drawPath.moveTo(touchX, touchY)
            }
            MotionEvent.ACTION_MOVE -> if (eraserOn) {
                drawPath.lineTo(touchX, touchY)
                mCanvas!!.drawPath(drawPath, drawPaint!!)
                drawPath.reset()
                drawPath.moveTo(touchX, touchY)
            } else {
                drawPath.lineTo(touchX, touchY)
            }
            MotionEvent.ACTION_UP -> {
                drawPath.lineTo(touchX, touchY)
                mCanvas!!.drawPath(drawPath, drawPaint!!)
                drawPath.reset()
                if (drawPaint!!.strokeWidth >= 9999.toFloat()) {
                    onClickEraser(false)
                }
            }
            else -> return false
        }
        invalidate()
        return true
    }

    private fun setUpDrawing() {
        drawPaint = Paint()
        drawPaint!!.apply {
            isAntiAlias = true
            isDither = true
            color = Color.BLACK
            style = Paint.Style.STROKE
            strokeJoin = Paint.Join.ROUND
            strokeCap = Paint.Cap.ROUND
        }
    }

    fun setSizeForBrush(newSize: Float) {
        mBrushSize = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            newSize,
            resources.displayMetrics
        )
        drawPaint!!.strokeWidth = mBrushSize
    }

    fun setSizeForEraser(newSize: Float) {
        mEraserSize = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            newSize,
            resources.displayMetrics
        )
        drawPaint!!.strokeWidth = mEraserSize
    }

    fun setColor(newColor: String) {
        val color = Color.parseColor(newColor)
        drawPaint!!.color = color
    }

    fun revertBase64toBitmap(string: String): Bitmap {
        return string.toBitmap()
    }

    // extension function to decode base64 string to bitmap
    private fun String.toBitmap(): Bitmap {
        Base64.decode(this, Base64.DEFAULT).apply {
            return BitmapFactory.decodeByteArray(this, 0, size)
        }
    }

    fun restoreLastDrawing(revertedBitmap: Bitmap) {
        if (bitmap.isEmpty()) {
            bitmap.add(revertedBitmap)
        }
        for (i in bitmap) {
            mCanvas!!.drawBitmap(i, 0f, 0f, drawPaint)
        }
    }
}