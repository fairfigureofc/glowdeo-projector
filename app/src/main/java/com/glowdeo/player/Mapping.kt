package com.glowdeo.player

import kotlin.math.abs

data class Corner(val x: Float, val y: Float)

data class Quad(val points: List<Corner>) {
    fun isValid(): Boolean {
        if (points.size != 4 || points.any { !it.x.isFinite() || !it.y.isFinite() || it.x !in 0f..1f || it.y !in 0f..1f }) return false
        val crosses = points.indices.map { i ->
            val a = points[i]
            val b = points[(i + 1) % 4]
            val c = points[(i + 2) % 4]
            (b.x - a.x) * (c.y - b.y) - (b.y - a.y) * (c.x - b.x)
        }
        val area = abs(points.indices.sumOf { i ->
            val a = points[i]
            val b = points[(i + 1) % 4]
            (a.x * b.y - b.x * a.y).toDouble()
        }) / 2
        return crosses.all { it > 0.00001f } && area > 0.002
    }

    fun moved(index: Int, dx: Float, dy: Float): Quad {
        val next = points.toMutableList()
        val old = next[index]
        next[index] = Corner((old.x + dx).coerceIn(0f, 1f), (old.y + dy).coerceIn(0f, 1f))
        return Quad(next).takeIf { it.isValid() } ?: this
    }

    fun encode(): String = points.joinToString(",") { "${it.x},${it.y}" }

    companion object {
        fun rectangle(left: Float, top: Float, right: Float, bottom: Float): Quad =
            Quad(listOf(Corner(left, top), Corner(right, top), Corner(right, bottom), Corner(left, bottom)))

        fun decode(text: String?, fallback: Quad): Quad {
            val values = text?.split(',')?.map { it.toFloatOrNull() ?: return fallback } ?: return fallback
            if (values.size != 8) return fallback
            return Quad(values.chunked(2).map { Corner(it[0], it[1]) }).takeIf { it.isValid() } ?: fallback
        }
    }
}

enum class Surface(val label: String, val default: Quad) {
    WALL("Wall boundary", Quad.rectangle(.025f, .04f, .975f, .96f)),
    TV("TV blackout", Quad.rectangle(.30f, .28f, .78f, .74f)),
    FEATURE("Featured panel", Quad.rectangle(.09f, .74f, .25f, .94f)),
}
