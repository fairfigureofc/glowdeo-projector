package com.glowdeo.player

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.view.MotionEvent
import android.view.View

class CameraCanvas(
    context: Context,
) : View(context) {
    var pattern: ScanPattern? = null
    var countdown: String? = null
    var photo: Bitmap? = null
    var quad: Quad? = null
    var selected = 0
    var onPoint: ((Corner) -> Unit)? = null
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val imageRect = RectF()

    init {
        isFocusable = true
        isFocusableInTouchMode = true
        contentDescription = "HY310X room photo. Arrows move the selected corner; OK selects the next corner."
    }

    override fun onDraw(canvas: Canvas) {
        canvas.drawColor(Color.BLACK)
        val active = pattern
        if (active != null) {
            drawPattern(canvas, active)
        } else {
            photo?.let { drawPhoto(canvas, it) }
            quad?.let { drawCorners(canvas, it) }
            countdown?.let {
                paint.color = Color.WHITE
                paint.style = Paint.Style.FILL
                paint.textSize = height * .35f
                paint.textAlign = Paint.Align.CENTER
                canvas.drawText(it, width / 2f, height * .6f, paint)
            }
        }
    }

    private fun drawPattern(
        canvas: Canvas,
        active: ScanPattern,
    ) {
        paint.style = Paint.Style.FILL
        paint.color = Color.WHITE
        if (active.axis == -1) canvas.drawColor(Color.WHITE)
        if (active.axis < 0) return
        val size = if (active.axis == 0) width else height
        for (cell in 0 until ScanPattern.CELLS) {
            if (!active.bright(cell)) continue
            val a = cell * size.toFloat() / ScanPattern.CELLS
            val b = (cell + 1) * size.toFloat() / ScanPattern.CELLS
            if (active.axis == 0) {
                canvas.drawRect(a, 0f, b, height.toFloat(), paint)
            } else {
                canvas.drawRect(0f, a, width.toFloat(), b, paint)
            }
        }
    }

    private fun drawPhoto(
        canvas: Canvas,
        bitmap: Bitmap,
    ) {
        val scale = minOf(width.toFloat() / bitmap.width, height.toFloat() / bitmap.height)
        val w = bitmap.width * scale
        val h = bitmap.height * scale
        imageRect.set((width - w) / 2, (height - h) / 2, (width + w) / 2, (height + h) / 2)
        paint.style = Paint.Style.FILL
        paint.isFilterBitmap = true
        canvas.drawBitmap(bitmap, null, imageRect, paint)
    }

    private fun drawCorners(
        canvas: Canvas,
        value: Quad,
    ) {
        val outline = Path()
        value.points.forEachIndexed { index, p ->
            val x = imageRect.left + p.x * imageRect.width()
            val y = imageRect.top + p.y * imageRect.height()
            if (index == 0) outline.moveTo(x, y) else outline.lineTo(x, y)
        }
        outline.close()
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 3f
        paint.color = Color.GREEN
        canvas.drawPath(outline, paint)
        value.points.forEachIndexed { index, p ->
            val x = imageRect.left + p.x * imageRect.width()
            val y = imageRect.top + p.y * imageRect.height()
            paint.style = Paint.Style.FILL
            paint.color = if (index == selected) Color.YELLOW else Color.WHITE
            canvas.drawCircle(x, y, if (index == selected) 12f else 7f, paint)
            paint.textAlign = Paint.Align.LEFT
            paint.textSize = 24f
            canvas.drawText("${index + 1}", x + 14, y + 12, paint)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_UP) {
            performClick()
            if (quad != null && imageRect.contains(event.x, event.y)) {
                onPoint?.invoke(Corner((event.x - imageRect.left) / imageRect.width(), (event.y - imageRect.top) / imageRect.height()))
            }
        }
        return true
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }
}
