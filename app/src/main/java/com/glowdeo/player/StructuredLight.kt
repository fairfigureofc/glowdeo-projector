package com.glowdeo.player

import kotlin.math.abs
import kotlin.math.roundToInt

data class ScanPattern(
    val axis: Int,
    val bit: Int = 0,
    val inverse: Boolean = false,
) {
    fun bright(cell: Int): Boolean =
        when (axis) {
            -2 -> false
            -1 -> true
            else -> (((cell xor (cell shr 1)) shr bit) and 1 == 1) xor inverse
        }

    companion object {
        const val BITS = 9
        const val CELLS = 1 shl BITS
        val BLACK = ScanPattern(-2)
        val WHITE = ScanPattern(-1)
        val sequence =
            listOf(BLACK, WHITE) +
                (0..1).flatMap { axis ->
                    (0 until BITS).flatMap { bit -> listOf(ScanPattern(axis, bit), ScanPattern(axis, bit, true)) }
                }
    }
}

/** Incremental decode bounds memory to a few camera frames, not the whole scan. */
class StructuredLight(
    private val width: Int,
    private val height: Int,
) {
    private val size = width * height
    private var black = IntArray(0)
    private var positive = IntArray(0)
    private val valid = BooleanArray(size)
    private val x = IntArray(size)
    private val y = IntArray(size)
    private var next = 0

    init {
        require(width in 2..2048 && height in 2..2048)
    }

    fun add(
        pattern: ScanPattern,
        luminance: IntArray,
    ) {
        require(luminance.size == size && next < ScanPattern.sequence.size && ScanPattern.sequence[next] == pattern)
        next++
        when (pattern.axis) {
            -2 -> black = luminance.copyOf()
            -1 -> for (i in 0 until size) valid[i] = luminance[i] - black[i] >= 24
            else -> addBit(pattern, luminance)
        }
    }

    private fun addBit(
        pattern: ScanPattern,
        luminance: IntArray,
    ) {
        if (!pattern.inverse) {
            positive = luminance.copyOf()
            return
        }
        val axis = if (pattern.axis == 0) x else y
        for (i in 0 until size) {
            val delta = positive[i] - luminance[i]
            if (abs(delta) < 12) valid[i] = false
            if (delta > 0) axis[i] = axis[i] or (1 shl pattern.bit)
        }
    }

    fun finish(
        projectorWidth: Int,
        projectorHeight: Int,
    ): CameraMap {
        check(next == ScanPattern.sequence.size) { "Calibration scan is incomplete." }
        val decodedX = IntArray(size) { if (valid[it]) decode(x[it]) else -1 }
        val decodedY = IntArray(size) { if (valid[it]) decode(y[it]) else -1 }
        require(valid.count { it } >= maxOf(64, size / 100)) {
            "Too little reflected light to map the room. Dim the room, aim at the wall and try again."
        }
        return CameraMap(width, height, projectorWidth, projectorHeight, decodedX, decodedY)
    }

    private fun decode(gray: Int): Int {
        var value = gray
        var shift = 1
        while (shift < ScanPattern.BITS) {
            value = value xor (value shr shift)
            shift *= 2
        }
        return value
    }
}

data class CameraMap(
    val width: Int,
    val height: Int,
    val projectorWidth: Int,
    val projectorHeight: Int,
    val x: IntArray,
    val y: IntArray,
) {
    init {
        require(width in 2..2048 && height in 2..2048 && projectorWidth in 2..16384 && projectorHeight in 2..16384)
        require(x.size == width * height && y.size == x.size)
        require(x.indices.all { (x[it] == -1 && y[it] == -1) || (x[it] in 0 until 512 && y[it] in 0 until 512) })
    }

    val validCount: Int get() = x.count { it >= 0 }

    /** Never extrapolate a plane through a dark TV or unseen part of the room. */
    fun projectorPoint(cameraPoint: Corner): Corner? {
        val cx = (cameraPoint.x * (width - 1)).roundToInt()
        val cy = (cameraPoint.y * (height - 1)).roundToInt()
        val radius = maxOf(2, minOf(width, height) / 40)
        var best = -1
        var distance = radius * radius + 1
        for (py in maxOf(0, cy - radius)..minOf(height - 1, cy + radius)) {
            for (px in maxOf(0, cx - radius)..minOf(width - 1, cx + radius)) {
                val i = py * width + px
                val d = (px - cx) * (px - cx) + (py - cy) * (py - cy)
                if (x[i] >= 0 && d < distance) {
                    best = i
                    distance = d
                }
            }
        }
        return if (best < 0) null else Corner((x[best] + .5f) / 512f, (y[best] + .5f) / 512f)
    }

    fun cameraPoint(projectorPoint: Corner): Corner? {
        var best = -1
        var distance = Float.MAX_VALUE
        for (i in x.indices) {
            if (x[i] < 0) continue
            val dx = (x[i] + .5f) / 512f - projectorPoint.x
            val dy = (y[i] + .5f) / 512f - projectorPoint.y
            val d = dx * dx + dy * dy
            if (d < distance) {
                best = i
                distance = d
            }
        }
        return if (best < 0) null else Corner((best % width).toFloat() / (width - 1), (best / width).toFloat() / (height - 1))
    }

    fun projectorQuad(cameraQuad: Quad): Quad? {
        val points = cameraQuad.points.map { projectorPoint(it) ?: return null }
        return Quad(points).takeIf { it.isValid() }
    }
}
