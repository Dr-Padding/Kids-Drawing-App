package com.drawing.kidsdrawingapp


import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View


class DrawingView(context: Context, attrs: AttributeSet): View(context, attrs) {

    private var drawPaint: Paint? = null
    private var mBrushSize: Float = 0.toFloat()
    private var penSelected = true
    private var eraserSelected = false
    private var mBitmap: Bitmap? = null
    private var mCanvas: Canvas? = null
    private val mBitmapPaint: Paint = Paint(Paint.DITHER_FLAG)
    var eraserOn = false
    var newAdded = false
    var allClear = false
    private val drawPath: Path = Path()
    private val bitmap = ArrayList<Bitmap>()
    private val undoBitmap = ArrayList<Bitmap>()
    var tappedOnce = false


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
            penSelected = false
            eraserSelected = true
        } else {
            eraserOn = false
            drawPaint!!.color = drawPaint!!.color
            drawPaint!!.xfermode = null
            eraserSelected = false
            penSelected = true
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
        if (penSelected || eraserSelected) {
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

//                    if (eraserSelected){
//                        tappedOnce = true
//                    }

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
                }
                MotionEvent.ACTION_CANCEL -> return false
                else -> return false
            }
            invalidate()
            return true
        }
        return false
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

    fun setColor(color: Int) {
        drawPaint!!.color = color
    }

    fun allCleared(allClearSelected: Boolean){
        if (allClearSelected && tappedOnce){
            eraserOn = false
        }
    }

}