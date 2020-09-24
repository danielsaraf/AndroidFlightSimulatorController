package services.joystick

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import android.view.View.OnTouchListener
import kotlin.math.*


class Joystick : SurfaceView,
    SurfaceHolder.Callback, OnTouchListener {
    private var centerPositionX = 0f
    private var centerPositionY = 0f
    private var baseCircleRadius = 0f
    private var innerCircleRadius = 0f
    private var actionDown: Boolean = false
    private var joystickCallback: JoystickListener? = null
    private var color: Paint? = null
    private val shade = 2

    constructor(context: Context?) : super(context) {
        holder.addCallback(this)
        setOnTouchListener(this)
        if (context is JoystickListener) joystickCallback = context
    }

    constructor(
        context: Context?, attr:
        AttributeSet?
    ) : super(context, attr) {
        if (context is JoystickListener) joystickCallback = context
        holder.addCallback(this)
        setOnTouchListener(this)
    }


    constructor(
        context: Context?, attr: AttributeSet?,
        style: Int
    ) : super(context, attr, style) {
        if (context is JoystickListener) joystickCallback = context
        holder.addCallback(this)
        setOnTouchListener(this)
    }


    override fun surfaceCreated(sufaceHolder: SurfaceHolder) {
        setupDimensions()
        color = Paint()
        drawJoystick(centerPositionX, centerPositionY)
    }

    override fun surfaceChanged(
        sufaceHolder: SurfaceHolder,
        format: Int, width: Int, height: Int
    ) {
    }

    override fun surfaceDestroyed(sufaceHolder: SurfaceHolder) {}

    /*
     * Separate the touch event to 3 different events.
     * Action Up, Action Down, Action Move.
     */
    override fun onTouch(view: View, e: MotionEvent): Boolean {
        if (view != this) {
            return true
        }
        when (e.action) {
            MotionEvent.ACTION_UP -> {
                motionUp(e)
            }
            MotionEvent.ACTION_DOWN -> {
                motionDown(e)
            }
            else -> {
                motionMove(e)
            }
        }
        return true
    }

    /* calculate the location of the inner circle of the joystick
     based on the location of the finger in the screen relative
      to the center position of the joystick.
     */
    private fun motionMove(e: MotionEvent) {
        if (!actionDown) return
        val deltaX = e.x - centerPositionX.toDouble()
        val deltaY = e.y - centerPositionY.toDouble()
        val distance = sqrt(deltaX.pow(2.0) + deltaY.pow(2.0))
        // if the location of the finger is inside the joystick base circle.
        if (distance < baseCircleRadius) {
            val xFraction = (deltaX / baseCircleRadius).toFloat()
            val yFraction = (deltaY / baseCircleRadius).toFloat()
            joystickCallback!!.onJoystickMoved(xFraction, yFraction, 2F)
            drawJoystick(e.x, e.y)
        }
        //if the location of the finger is outside to the joystick base circle.
        else {
            val angle = abs(atan(deltaX / deltaY))
            var x = baseCircleRadius * sin(angle).toFloat()
            var y = baseCircleRadius * cos(angle).toFloat()
            if (deltaX < 0) x *= -1
            if (deltaY < 0) y *= -1
            val xFraction = (x / baseCircleRadius)
            val yFraction = (y / baseCircleRadius)
            joystickCallback!!.onJoystickMoved(xFraction, yFraction, 2F)
            drawJoystick(
                centerPositionX + x,
                centerPositionY + y
            )
        }
    }

    /* assign a true value to the actionDown if the touch event
     * occurred in the joystick. and then any move event will affect
     * the joystick until actionUp event.
     */
    private fun motionDown(e: MotionEvent) {
        if (!actionDown) {
            val deltaX = e.x - centerPositionX.toDouble()
            val deltaY = e.y - centerPositionY.toDouble()
            val distance = sqrt(deltaX.pow(2.0) + deltaY.pow(2.0))
            if (distance < innerCircleRadius) {
                actionDown = true
            }
        }
    }

    /*
     * When the action is up, reset the coordinates
     * and start a nice animation that acts like pendulum that stops
     * in the center of the joystick.
     */
    private fun motionUp(e: MotionEvent) {
        if (!actionDown) return
        // some calculation before starting the animation.
        val deltaX = e.x - centerPositionX.toDouble()
        val deltaY = e.y - centerPositionY.toDouble()
        val distance =
            (sqrt(deltaX.pow(2.0) + deltaY.pow(2.0))).toFloat()
                .coerceAtMost(baseCircleRadius)
        val angle = abs(atan(deltaX / deltaY))
        var x = distance * sin(angle).toFloat()
        var y = distance * cos(angle).toFloat()
        if (deltaX < 0) x *= -1
        if (deltaY < 0) y *= -1
        var newX = x
        var newY = y
        var delayTime = 50L
        joystickCallback!!.onJoystickMoved(0F, 0F, 2F)
        // Starting an animation that acts like pendulum.
        while (abs(newX) > abs(x / 100) && abs(newY) > abs(y / 100)) {
            newX *= 0.5F
            newY *= 0.5F
            for (i in 1 downTo -1) {
                Thread.sleep(delayTime)
                drawJoystick(centerPositionX + newX * i, centerPositionY + newY * i)
                delayTime = (delayTime / 2)
            }
            delayTime = (delayTime / 2)
        }
        drawJoystick(centerPositionX, centerPositionY)
        actionDown = false
    }

    /*
     * Draw the joystick on the surface according to the location of
     *  the inner circle and the base circle.
     */
    private fun drawJoystick(newXInnerCircle: Float, newYInnerCircle: Float) {
        if (holder.surface.isValid) {
            val myCanvas: Canvas = this.holder.lockCanvas()
            myCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
            val fillPaint = Paint()
            fillPaint.style = Paint.Style.FILL
            fillPaint.color = Color.parseColor("#293887")
            val r = RectF(0F, 0F, width.toFloat(), height.toFloat())
            myCanvas.drawRect(r, fillPaint)
            color!!.setARGB(250, 0, 0, 0)
            myCanvas.drawCircle(
                centerPositionX, centerPositionY,
                baseCircleRadius, color!!
            )
            // some shading.
            for (i in 0..(innerCircleRadius / shade).toInt()) {
                color!!.setARGB(
                    255,
                    (50 + i * (200 * shade / innerCircleRadius)).toInt(),
                    (50 + i * (200 * shade / innerCircleRadius)).toInt(),
                    50 + i * (200 * shade / innerCircleRadius).toInt()
                )
                myCanvas.drawCircle(
                    newXInnerCircle, newYInnerCircle,
                    innerCircleRadius - i.toFloat() * 5 / 2,
                    color!!
                )
            }
            holder.unlockCanvasAndPost(myCanvas)
        }
    }

    private fun setupDimensions() {
        centerPositionX = width / 2.toFloat()
        centerPositionY = height / 2.toFloat()
        baseCircleRadius = width.coerceAtMost(height) / 3.toFloat()
        innerCircleRadius = width.coerceAtMost(height) / 7.toFloat()
    }
}