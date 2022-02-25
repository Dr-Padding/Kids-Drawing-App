package com.drawing.kidsdrawingapp

import android.R
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.widget.Toast
import com.google.android.material.snackbar.Snackbar


//class DrawingView(context: Context, attrs: AttributeSet): View(context, attrs) {
//
//    private var mDrawPath: CustomPath? = null
//    private var mCanvasBitmap: Bitmap? = null
//    private var mDrawPaint: Paint? = null
//    private var mCanvasPaint: Paint? = null
//    private var mBrushSize: Float = 0.toFloat()
//    private var color: Int = Color.GREEN
//    private var canvas: Canvas? = null
//    private val mPaths = ArrayList<CustomPath>()
//    private val mUndoPaths = ArrayList<CustomPath>()
//
//    // Eraser mode
//    private var erase = false
//
//
//    init {
//        setUpDrawing()
//    }
//
//    fun onClickUndo() {
//        if (mPaths.size > 0) {
//            mUndoPaths.add(mPaths.removeAt(mPaths.size - 1))
//            invalidate()
//        }
//    }
//
//    fun onClickRedo() {
//        if (mUndoPaths.size > 0) {
//            mPaths.add(mUndoPaths.removeAt(mUndoPaths.size - 1))
//            invalidate()
//        }
//    }
//
//    fun onClickEraser(isErase: Boolean) {
//
//        erase = isErase
//
//        if (erase) {
//            mDrawPaint!!.xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
//        } else {
//            mDrawPaint!!.xfermode = null
//        }
//
//    }
//
//
//    private fun setUpDrawing() {
//
//        mDrawPaint = Paint()
//        mDrawPath = CustomPath(color, mBrushSize)
//        mDrawPaint!!.color = color
//        mDrawPaint!!.style = Paint.Style.STROKE
//        mDrawPaint!!.strokeJoin = Paint.Join.ROUND
//        mDrawPaint!!.strokeCap = Paint.Cap.ROUND
//        mCanvasPaint = Paint(Paint.DITHER_FLAG)
//        mDrawPaint!!.isAntiAlias = true
//        mDrawPaint!!.isDither = true
//        mDrawPaint!!.xfermode = null
//        mDrawPaint!!.alpha = 0xFF
//    }
//
//    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
//        super.onSizeChanged(w, h, oldw, oldh)
//        mCanvasBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
//        canvas = Canvas(mCanvasBitmap!!)
//    }
//
//    // Change Canvas to Canvas? if fails
//    override fun onDraw(canvas: Canvas) {
//        super.onDraw(canvas)
//
//        canvas.drawBitmap(mCanvasBitmap!!, 0f, 0f, mCanvasPaint)
//
//        if (!erase) {
//
//            for (path in mPaths) {
//                mDrawPaint!!.strokeWidth = path.brushThickness
//                mDrawPaint!!.color = path.color
//                canvas.drawPath(path, mDrawPaint!!)
//            }
//
//
//            if (!mDrawPath!!.isEmpty) {
//                mDrawPaint!!.strokeWidth = mDrawPath!!.brushThickness
//                mDrawPaint!!.color = mDrawPath!!.color
//                canvas.drawPath(mDrawPath!!, mDrawPaint!!)
//            }
//        }
//    }
//
//    @SuppressLint("ClickableViewAccessibility")
//    override fun onTouchEvent(event: MotionEvent?): Boolean {
//        val touchX = event?.x
//        val touchY = event?.y
//
//        when (event?.action) {
//            MotionEvent.ACTION_DOWN -> {
//
//                mDrawPath!!.color = color
//                mDrawPath!!.brushThickness = mBrushSize
//
//                mDrawPath!!.reset()
//                if (touchX != null) {
//                    if (touchY != null) {
//                        mDrawPath!!.moveTo(touchX, touchY)
//                    }
//                }
//            }
//            MotionEvent.ACTION_MOVE -> {
//                if (touchX != null) {
//                    if (touchY != null) {
//                        mDrawPath!!.lineTo(touchX, touchY)
//                    }
//                }
//            }
//            MotionEvent.ACTION_UP -> {
//                mDrawPath!!.lineTo(touchX!!, touchY!!)
//
//                //THESE 2 LINES OF CODE
//                // commit the path to our offscreen
//                canvas?.drawPath(mDrawPath!!, mDrawPaint!!)
//                //kill this so we don't double draw
//                mDrawPath!!.reset()
//
//                mPaths.add(mDrawPath!!)
//                mDrawPath = CustomPath(color, mBrushSize)
//            }
//            else -> return false
//        }
//        invalidate()
//
//        return true
//    }
//
//
//    fun setSizeForBrush(newSize: Float) {
//        mBrushSize = TypedValue.applyDimension(
//            TypedValue.COMPLEX_UNIT_DIP,
//            newSize,
//            resources.displayMetrics
//        )
//        mDrawPaint!!.strokeWidth = mBrushSize
//    }
//
//    fun setColor(color: Int) {
//        this.color = color
//    }
//
//    internal inner class CustomPath(
//        var color: Int,
//        var brushThickness: Float
//    ) : android.graphics.Path()
//}

class DrawingView(context: Context, attrs: AttributeSet): View(context, attrs) {

    private var drawPaint: Paint? = null
    private var mBrushSize: Float = 0.toFloat()
    private var penSelected = true
    private var eraserSelected =false
    private var mBitmap: Bitmap? = null
    private var mCanvas: Canvas? = null
    private val mBitmapPaint: Paint = Paint(Paint.DITHER_FLAG)
    private val circlePaint: Paint = Paint()
    private val circlePath: Path = Path()
    var eraserOn = false
    var newAdded = false
    var allClear = false
    private val drawPath: Path = Path()
    private val bitmap = ArrayList<Bitmap>()
    private val undoBitmap = ArrayList<Bitmap>()


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
        canvas.drawPath(circlePath, circlePaint)
    }

    fun onClickEraser(isEraserOn: Boolean) {
        if (isEraserOn) {
            eraserOn = true
            drawPaint!!.setColor(resources.getColor(R.color.transparent))
            drawPaint!!.xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
            penSelected = false
            eraserSelected = true
        } else {
            eraserOn = false
            //drawPaint!!.setColor(drawPaint!!.color) //??
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
        } else {
            val toast = Toast.makeText(context.applicationContext, "nothing to Undo", Toast.LENGTH_SHORT)
            toast.setGravity(Gravity.CENTER, 0, 0)
            toast.show()
        }
    }

    fun onClickRedo() {
        if (undoBitmap.size > 0) {
            bitmap.add(undoBitmap.removeAt(undoBitmap.size - 1))
            mBitmap = bitmap[bitmap.size - 1].copy(mBitmap!!.config, mBitmap!!.isMutable)
            mCanvas = Canvas(mBitmap!!)
            invalidate()
        } else {
            val toast = Toast.makeText(context, "nothing to Redo", Toast.LENGTH_SHORT)
            toast.setGravity(Gravity.CENTER, 0, 0)
            toast.show()
        }
    }

    override fun performClick(): Boolean {
        return super.performClick()
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
        circlePaint.isAntiAlias = true
        circlePaint.color = Color.BLUE
        circlePaint.style = Paint.Style.STROKE
        circlePaint.strokeJoin = Paint.Join.MITER
        circlePaint.strokeWidth = 4f
        drawPaint = Paint()
        drawPaint!!.setAntiAlias(true)
        drawPaint!!.setDither(true)
        drawPaint!!.setColor(Color.BLACK)
        drawPaint!!.setStyle(Paint.Style.STROKE)
        drawPaint!!.setStrokeJoin(Paint.Join.ROUND)
        drawPaint!!.setStrokeCap(Paint.Cap.ROUND)
        drawPaint!!.setStrokeWidth(20F)
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

}