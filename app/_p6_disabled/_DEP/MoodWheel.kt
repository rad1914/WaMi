// @path: app/_p6_disabled/_DEP/MoodWheel.kt
// @path: app/src/main/java/com/radwrld/resonance/MoodWheel.kt
package com.radwrld.resonance

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.MotionEvent
import android.view.View
import kotlin.math.max
import kotlin.math.min

class MoodWheel(
    ctx: Context,
    private val onChange: (valence: Float, arousal: Float) -> Unit = { _, _ -> }
) : View(ctx) {

    private val bg = Paint().apply {
        color = Color.DKGRAY
        style = Paint.Style.FILL
    }

    private val puck = Paint().apply {
        color = Color.WHITE
        style = Paint.Style.FILL
    }

    private var px = 0.5f
    private var py = 0.5f

    override fun onDraw(c: Canvas) {
        super.onDraw(c)
        val w = width.toFloat()
        val h = height.toFloat()

        c.drawRect(0f, 0f, w, h, bg)
        c.drawCircle(px * w, py * h, 24f, puck)
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        if (e.action == MotionEvent.ACTION_DOWN || e.action == MotionEvent.ACTION_MOVE) {
            px = (e.x / width).coerce()
            py = (e.y / height).coerce()

            val valence = px * 2f - 1f
            val arousal = 1f - py * 2f

            onChange(valence, arousal)
            invalidate()
            return true
        }
        return super.onTouchEvent(e)
    }

 private fun Float.coerce() = max(0f, min(1f, this))
}
