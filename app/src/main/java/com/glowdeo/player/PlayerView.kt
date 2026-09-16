package com.glowdeo.player

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PathMeasure
import android.graphics.RectF
import android.os.SystemClock
import android.view.View
import kotlin.math.cos
import kotlin.math.sin

@Suppress("TooManyFunctions")
class PlayerView(context: Context, val settings: PlayerSettings) : View(context) {
    var editing: Surface? = null
    var corner = 0
    var running = true
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val lime = Color.rgb(165, 250, 84)
    private val cyan = Color.rgb(88, 220, 230)
    private val start = SystemClock.uptimeMillis()

    init {
        contentDescription = "Glowdeo offline projection demo. Press OK or tap to open controls."
        isFocusable = true
        isFocusableInTouchMode = true
    }

    private fun path(surface: Surface): Path = Path().apply {
        settings.quads.getValue(surface).points.forEachIndexed { index, p ->
            if (index == 0) moveTo(p.x * width, p.y * height) else lineTo(p.x * width, p.y * height)
        }
        close()
    }

    private fun transform(surface: Surface, w: Float, h: Float): Matrix = Matrix().apply {
        val target = settings.quads.getValue(surface).points.flatMap { listOf(it.x * width, it.y * height) }.toFloatArray()
        setPolyToPoly(floatArrayOf(0f, 0f, w, 0f, w, h, 0f, h), 0, target, 0, 4)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawColor(Color.BLACK)
        val t = if (settings.motion) (SystemClock.uptimeMillis() - start) / 1000f else 0f
        canvas.save()
        canvas.clipPath(path(Surface.WALL))
        canvas.concat(transform(Surface.WALL, 1000f, 600f))
        if (settings.grid || editing != null) drawGrid(canvas) else drawDemo(canvas, t)
        canvas.restore()
        if (!settings.grid && editing == null) {
            canvas.save()
            canvas.clipPath(path(Surface.WALL))
            canvas.clipPath(path(Surface.FEATURE))
            canvas.concat(transform(Surface.FEATURE, 200f, 200f))
            drawFeature(canvas, t)
            canvas.restore()
        }
        if (editing == null) {
            canvas.save()
            canvas.clipPath(path(Surface.WALL))
            val outline = path(Surface.TV)
            val measure = PathMeasure(outline, true)
            val distance = measure.length
            val head = (t % 12f) / 12f * distance
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 5f
            paint.color = lime
            val segment = Path()
            measure.getSegment(head, (head + distance * .16f).coerceAtMost(distance), segment, true)
            if (head + distance * .16f > distance) {
                measure.getSegment(0f, head + distance * .16f - distance, segment, true)
            }
            canvas.drawPath(segment, paint)
            canvas.restore()
        }
        // Blackout is applied last, so even overlapping panels cannot light the TV picture.
        paint.style = Paint.Style.FILL
        paint.color = Color.BLACK
        canvas.drawPath(path(Surface.TV), paint)
        if (editing != null) drawHandles(canvas)
        if (running && (settings.motion || editing != null)) postInvalidateDelayed(33)
    }

    private fun text(canvas: Canvas, value: String, x: Float, y: Float, size: Float, color: Int = Color.WHITE) {
        paint.style = Paint.Style.FILL
        paint.color = color
        paint.textSize = size
        paint.typeface = android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL)
        canvas.drawText(value, x, y, paint)
    }

    private fun line(canvas: Canvas, x1: Float, y1: Float, x2: Float, y2: Float, color: Int, stroke: Float = 1f) {
        paint.color = color
        paint.strokeWidth = stroke
        canvas.drawLine(x1, y1, x2, y2, paint)
    }

    private fun drawGrid(canvas: Canvas) {
        for (x in 0..1000 step 50) line(canvas, x.toFloat(), 0f, x.toFloat(), 600f, cyan)
        for (y in 0..600 step 50) line(canvas, 0f, y.toFloat(), 1000f, y.toFloat(), cyan)
        text(canvas, "GLOWDEO / ALIGNMENT GRID", 20f, 42f, 24f, lime)
    }

    private fun drawDemo(canvas: Canvas, t: Float) {
        text(canvas, "GLOWDEO", 25f, 56f, 35f, lime)
        text(canvas, "YOUR TEAM. YOUR ROOM.", 275f, 56f, 25f)
        text(canvas, "OFFLINE DEMO / SAMPLE DATA", 25f, 94f, 17f, cyan)
        line(canvas, 25f, 112f, 975f, 112f, lime)
        text(canvas, "HOME", 55f, 185f, 19f, cyan)
        text(canvas, "24", 50f, 240f, 48f)
        text(canvas, "AWAY", 820f, 185f, 19f, cyan)
        text(canvas, "17", 820f, 240f, 48f)
        text(canvas, "Q3 / 08:42", 810f, 290f, 18f)
        text(canvas, "FEATURED", 35f, 350f, 18f, lime)
        text(canvas, "PLAYER 11", 35f, 385f, 22f)
        text(canvas, "6 REC", 820f, 355f, 22f)
        text(canvas, "94 YDS", 820f, 395f, 22f)
        text(canvas, "1 TD", 820f, 435f, 22f)
        text(canvas, "TEAM MODE", 310f, 535f, 25f, lime)
        text(canvas, "Your stream stays on your TV", 310f, 568f, 19f, cyan)
        val x = 25f + (t % 12f) / 12f * 900f
        line(canvas, x, 117f, x + 50f, 117f, lime, 3f)
    }

    private fun drawFeature(canvas: Canvas, t: Float) {
        val bob = sin(t * .8f) * 5f
        paint.color = Color.rgb(3, 15, 18)
        paint.style = Paint.Style.FILL
        canvas.drawRect(0f, 0f, 200f, 200f, paint)
        paint.color = cyan
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2f
        canvas.drawOval(RectF(15f, 150f, 185f, 179f), paint)
        canvas.drawCircle(100f, 55f + bob, 24f, paint)
        val torso = Path().apply {
            moveTo(72f, 89f + bob); lineTo(48f, 109f + bob); lineTo(59f, 130f + bob)
            lineTo(70f, 122f + bob); lineTo(70f, 154f + bob); lineTo(130f, 154f + bob)
            lineTo(130f, 122f + bob); lineTo(141f, 130f + bob); lineTo(152f, 109f + bob)
            lineTo(128f, 89f + bob); close()
        }
        canvas.drawPath(torso, paint)
        text(canvas, "11", 83f, 137f + bob, 35f, lime)
        for (i in 0..11) {
            val angle = t + i * Math.PI.toFloat() / 6
            paint.color = lime
            canvas.drawCircle(100f + cos(angle) * 80f, 165f + sin(angle) * 12f, 2f, paint)
        }
        text(canvas, "DEMO PLAYER", 30f, 194f, 17f)
    }

    private fun drawHandles(canvas: Canvas) {
        val surface = editing ?: return
        paint.color = lime
        paint.strokeWidth = 2f
        paint.style = Paint.Style.STROKE
        canvas.drawPath(path(surface), paint)
        settings.quads.getValue(surface).points.forEachIndexed { i, point ->
            paint.color = if (i == corner) Color.YELLOW else Color.WHITE
            paint.style = Paint.Style.FILL
            canvas.drawCircle(point.x * width, point.y * height, if (i == corner) 11f else 6f, paint)
            text(canvas, "${i + 1}", (point.x * width + 14).coerceAtMost(width - 24f), (point.y * height + 24).coerceAtMost(height - 8f), 20f)
        }
    }
}
