package com.drawing.kidsdrawingapp

import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.content.Context
import android.graphics.*
import android.os.Build
import android.util.AttributeSet
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import android.widget.Toast
import kotlin.math.abs



class DrawingView(context: Context, attrs: AttributeSet) : View(context, attrs) {

    private var mDrawPath: CustomPath? = null
    private var mCanvasBitmap: Bitmap? = null
    private var mDrawPaint: Paint? = null
    private var mCanvasPaint: Paint? = null
    private var mBrushSize: Float = 0.toFloat()
    private var color: Int = Color.GREEN
    private var canvas: Canvas? = null
    private val mPaths = ArrayList<CustomPath>()
    private val mUndoPaths = ArrayList<CustomPath>()
    // Eraser mode
    private val mEraserPaths = ArrayList<CustomPath>()
    private var mDrawEraserPaint: Paint? = null
    private var erase = false


    init {
        setUpDrawing()
    }

    fun onClickUndo() {

//            erase = false


        if (mPaths.size > 0) {
            mUndoPaths.add(mPaths.removeAt(mPaths.size - 1))
            invalidate()
        }else{
            Toast.makeText(context, "There is nothing to Undo", Toast.LENGTH_SHORT).show()
        }
    }

    fun onClickRedo() {
        //erase = true
        if (mUndoPaths.size > 0) {
            mPaths.add(mUndoPaths.removeAt(mUndoPaths.size - 1))
            invalidate()
        }else{
            Toast.makeText(context, "There is nothing to Redo", Toast.LENGTH_SHORT).show()
        }
    }

    fun onClickEraser(isErase: Boolean) {
        erase = isErase
        if (erase) {

            mDrawPaint!!.apply {

                maskFilter = null
                alpha = 0xFF

                isAntiAlias = true
                // destination pixels covered by the source are cleared to 0
                xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
            }
        } else {
            mDrawPaint!!.xfermode = null
        }
    }





    @TargetApi(Build.VERSION_CODES.Q)
    private fun setUpDrawing() {
        mDrawPaint = Paint()
        mDrawPaint!!.color = color
        mDrawPaint!!.style = Paint.Style.STROKE
        mDrawPaint!!.strokeJoin = Paint.Join.ROUND
        mDrawPaint!!.strokeCap = Paint.Cap.ROUND
        mDrawPaint!!.isDither = true
        mDrawPath = CustomPath(color, mBrushSize)
        mCanvasPaint = Paint(Paint.DITHER_FLAG)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        mCanvasBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        canvas = Canvas(mCanvasBitmap!!)
    }

    // Change Canvas to Canvas? if fails

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        canvas.drawBitmap(mCanvasBitmap!!, 0f, 0f, mCanvasPaint)


        //if (!erase) {

            for (path in mPaths) {
                mDrawPaint!!.strokeWidth = path.brushThickness
                mDrawPaint!!.color = path.color
                canvas.drawPath(path, mDrawPaint!!)

            }

            if (!mDrawPath!!.isEmpty) {
                mDrawPaint!!.strokeWidth = mDrawPath!!.brushThickness
                mDrawPaint!!.color = mDrawPath!!.color
                canvas.drawPath(mDrawPath!!, mDrawPaint!!)
            }

        //}



    }
//
//
//    private var mX = 0f
//    private var mY = 0f
//    private fun touchStart(x: Float, y: Float) {
//        mDrawPath!!.reset()
//        mDrawPath!!.moveTo(x, y)
//        mX = x
//        mY = y
//    }
//
//    private fun touchMove(x: Float, y: Float) {
//
//        val dx = abs(x - mX)
//        val dy = abs(y - mY)
//        if (dx >= TOUCH_TOLERANCE || dy >= TOUCH_TOLERANCE) {
//            mDrawPath!!.quadTo(mX, mY, (x + mX) / 2, (y + mY) / 2)
//            mX = x
//            mY = y
//        }
//    }
//
//    private fun touchUp() {
//
//
//
//
//    }
//
//    @SuppressLint("ClickableViewAccessibility")
//    override fun onTouchEvent(event: MotionEvent?): Boolean {
//        val x = event?.x
//        val y = event?.y
//
//        if (event != null) {
//            when (event.action) {
//                MotionEvent.ACTION_DOWN -> {
//
//                    mDrawPath!!.color = color
//                    mDrawPath!!.brushThickness = mBrushSize
//
//                    if (x != null) {
//                        if (y != null) {
//                            touchStart(x, y)
//                        }
//                    }
//                    invalidate()
//                }
//                MotionEvent.ACTION_MOVE -> {
//                    if (x != null) {
//                        if (y != null) {
//                            touchMove(x, y)
//                        }
//                    }
//                    invalidate()
//                }
//                MotionEvent.ACTION_UP -> {
//                    touchUp()
//                    invalidate()
//                }
//            }
//        }
//        return true
//    }
//
//    companion object {
//        private const val TOUCH_TOLERANCE = 4f
//    }


    @SuppressLint("ClickableViewAccessibility")
       override fun onTouchEvent(event: MotionEvent?): Boolean {
           val touchX = event?.x
           val touchY = event?.y

           when(event?.action){
               MotionEvent.ACTION_DOWN ->{

//                   if (erase){
//                       for (i in mPaths.indices - 2)
//                       mDrawPaint!!.xfermode = null
//                   }

                   mDrawPath!!.color = color
                   mDrawPath!!.brushThickness = mBrushSize


                   mDrawPath!!.reset()

                   if (touchX != null) {
                       if (touchY != null) {
                           mDrawPath!!.moveTo(touchX, touchY)
                       }
                   }
               }
               MotionEvent.ACTION_MOVE ->{
                   if (touchX != null) {
                       if (touchY != null) {
                           mDrawPath!!.lineTo(touchX, touchY)
                       }
                   }
               }
               MotionEvent.ACTION_UP ->{

                   if (touchX != null) {
                       if (touchY != null) {
                           mDrawPath!!.lineTo(touchX, touchY)
                       }
                   }

                if (erase) {
                    // commit the path to our offscreen
                    canvas!!.drawPath(mDrawPath!!, mDrawPaint!!)

                    // kill this so we don't double draw
                    mDrawPath!!.reset()
                }


            mPaths.add(mDrawPath!!)
            mDrawPath = CustomPath(color, mBrushSize)







               }
               else -> return false
           }
           invalidate()

           return true
       }

    fun setSizeForBrush(newSize: Float) {
        mBrushSize = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            newSize,
            resources.displayMetrics
        )
        mDrawPaint!!.strokeWidth = mBrushSize
    }

    fun setColor(color: Int) {
        this.color = color
    }

    internal inner class CustomPath(
        var color: Int,
        var brushThickness: Float
    ) : android.graphics.Path()
}