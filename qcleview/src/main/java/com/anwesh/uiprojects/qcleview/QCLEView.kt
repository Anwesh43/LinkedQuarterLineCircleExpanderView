package com.anwesh.uiprojects.qcleview

/**
 * Created by anweshmishra on 26/01/19.
 */

import android.app.Activity
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Color
import android.graphics.RectF
import android.view.View
import android.view.MotionEvent

val nodes : Int = 5
val circles : Int = 4
val scGap : Float = 0.05f
val scDiv : Double = 0.51
val sizeFactor : Float = 2.8f
val strokeFactor : Int = 90
val foreColor : Int = Color.parseColor("#01579B")
val backColor : Int = Color.parseColor("#BDBDBD")
val offset : Float = 10f
val delay : Long = 10
val rotDeg : Float = 90f
val halfFactor : Int = 2
val fullDeg : Float = 360f
val sizeOffset : Int = 1

fun Int.inverse() : Float = 1f / this
fun Float.maxScale(i : Int, n : Int) : Float = Math.max(0f, this - i * n.inverse())
fun Float.divideScale(i : Int, n : Int) : Float = Math.min(n.inverse(), maxScale(i, n)) * n
fun Float.scaleFactor() : Float = Math.floor(this / scDiv).toFloat()
fun Float.mirrorValue(a : Int, b : Int) : Float = (1 - scaleFactor()) * a.inverse() + scaleFactor() * b.inverse()
fun Float.updateValue(dir : Float, a : Int, b : Int) : Float = mirrorValue(a, b) * dir * scGap

fun Canvas.drawQCLENode(i : Int, scale : Float, paint : Paint) {
    val w : Float = width.toFloat()
    val h : Float = height.toFloat()
    val gap : Float = h / (nodes + sizeOffset)
    val size : Float = gap / sizeFactor
    val r : Float = size / halfFactor
    paint.color = foreColor
    paint.strokeWidth = Math.min(w, h) / strokeFactor
    paint.strokeCap = Paint.Cap.ROUND
    paint.style = Paint.Style.STROKE
    val sc1 : Float = scale.divideScale(0, 2)
    val sc2 : Float = scale.divideScale(1, 2)
    val sc21 : Float = sc2.divideScale(0, 2)
    val sc22 : Float = sc2.divideScale(1, 2)
    val deg : Float = fullDeg / circles
    save()
    translate(w/ halfFactor, gap * (i + 1))
    rotate(rotDeg * sc21)
    for (j in 0..(circles - 1)) {
        val scj1 : Float = sc1.divideScale(j, circles)
        val scj2 : Float = sc22.divideScale(j, circles)
        save()
        rotate(deg * j)
        translate(r * scj2, r * scj2)
        drawArc(RectF(-r, -r, r, r), offset, (deg - 2 * offset) * scj1, false, paint)
        restore()
    }
    restore()
}

class QCLEView(ctx : Context) : View(ctx) {

    private val paint : Paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val renderer : Renderer = Renderer(this)

    override fun onDraw(canvas : Canvas) {
        renderer.render(canvas, paint)
    }

    override fun onTouchEvent(event : MotionEvent) : Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                renderer.handleTap()
            }
        }
        return true
    }

    data class State(var scale : Float = 0f, var dir : Float = 0f, var prevScale : Float = 0f) {

        fun update(cb : (Float) -> Unit) {
            scale += scale.updateValue(dir, circles, circles * 2)
            if (Math.abs(scale - prevScale) > 1) {
                scale = prevScale + dir
                dir = 0f
                prevScale = scale
                cb(prevScale)
            }
        }

        fun startUpdating(cb : () -> Unit) {
            if (dir == 0f) {
                dir = 1f - 2 * prevScale
                cb()
            }
        }
    }

    data class Animator(var view : View, var animated : Boolean = false) {

        fun animate(cb : () -> Unit) {
            if (animated) {
                cb()
                try {
                    Thread.sleep(delay)
                    view.invalidate()
                } catch(ex : Exception) {

                }
            }
        }

        fun start() {
            if (!animated) {
                animated = true
                view.postInvalidate()
            }
        }

        fun stop() {
            if (animated) {
                animated = false
            }
        }
    }

    data class QCLENode(var i : Int = 0, private val state : State = State()) {

        private var next : QCLENode? = null
        private var prev : QCLENode? = null

        init {
            addNeighbor()
        }

        fun addNeighbor() {
            if (i < nodes - 1) {
                next = QCLENode(i + 1)
                next?.prev = this
            }
        }

        fun draw(canvas : Canvas, paint : Paint) {
            canvas.drawQCLENode(i, state.scale, paint)
            prev?.draw(canvas, paint)
        }

        fun update(cb : (Int, Float) -> Unit) {
            state.update {
                cb(i, it)
            }
        }

        fun startUpdating(cb : () -> Unit) {
            state.startUpdating(cb)
        }

        fun getNext(dir : Int, cb : () -> Unit) : QCLENode {
            var curr : QCLENode? = prev
            if (dir == 1) {
                curr = next
            }
            if (curr != null) {
                return curr
            }
            cb()
            return this
        }
    }

    data class QCLE(var i : Int) {
        private var curr : QCLENode = QCLENode(0)
        private var dir : Int = 1

        fun draw(canvas : Canvas, paint : Paint) {
            curr.draw(canvas, paint)
        }

        fun update(cb : (Int, Float) -> Unit) {
            curr.update {i, scl ->
                curr = curr.getNext(dir) {
                    dir *= -1
                }
                cb(i, scl)
            }
        }

        fun startUpdating(cb : () -> Unit) {
            curr.startUpdating(cb)
        }
    }

    data class Renderer(var view : View) {

        private val animator : Animator = Animator(view)
        private val qcle : QCLE = QCLE(0)

        fun render(canvas : Canvas, paint : Paint) {
            canvas.drawColor(backColor)
            qcle.draw(canvas, paint)
            animator.animate {
                qcle.update {i, scl ->
                    animator.stop()
                }
            }
        }

        fun handleTap() {
            qcle.startUpdating {
                animator.start()
            }
        }
    }

    companion object {

        fun create(activity : Activity) : QCLEView {
            val view : QCLEView = QCLEView(activity)
            activity.setContentView(view)
            return view
        }
    }
}