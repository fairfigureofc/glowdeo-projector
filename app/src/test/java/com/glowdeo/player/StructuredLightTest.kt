package com.glowdeo.player

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class StructuredLightTest {
    @Test fun decodesAllGrayCodeCellsIncludingHighestBits() {
        val width = 512
        val height = 4
        val decoder = StructuredLight(width, height)
        for (pattern in ScanPattern.sequence) {
            decoder.add(
                pattern,
                IntArray(width * height) { i ->
                    val coordinate = if (pattern.axis == 1) (i / width) * 170 else i % width
                    if (pattern.bright(coordinate)) 220 else 20
                },
            )
        }
        val result = decoder.finish(1920, 1080)
        assertArrayEquals(IntArray(width * height) { it % width }, result.x)
        assertArrayEquals(IntArray(width * height) { it / width * 170 }, result.y)
    }

    @Test fun lowContrastAndAmbiguousPixelsAreExcluded() {
        val decoder = StructuredLight(32, 32)
        for (pattern in ScanPattern.sequence) {
            decoder.add(
                pattern,
                IntArray(1024) { i ->
                    when {
                        i == 0 -> 40
                        i == 1 && pattern.axis == 0 && pattern.bit == 3 -> 80
                        else -> if (pattern.bright(i % 512)) 220 else 20
                    }
                },
            )
        }
        val map = decoder.finish(1280, 720)
        assertEquals(-1, map.x[0])
        assertEquals(-1, map.y[1])
        assertEquals(1022, map.validCount)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsBlackRoomRatherThanInventingCalibration() {
        val decoder = StructuredLight(16, 16)
        ScanPattern.sequence.forEach { decoder.add(it, IntArray(256) { 15 }) }
        decoder.finish(1280, 720)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsMissingOrOutOfOrderPatterns() {
        StructuredLight(16, 16).add(ScanPattern.WHITE, IntArray(256))
    }

    @Test(expected = IllegalStateException::class)
    fun incompleteScanCannotReplaceSavedMap() {
        StructuredLight(16, 16).finish(1280, 720)
    }

    @Test fun photoCoordinatesMapToProjectorAndRejectUnseenRegions() {
        val x = IntArray(10000) { it % 100 * 5 }
        val y = IntArray(10000) { it / 100 * 5 }
        for (i in x.indices) {
            if (i % 100 in 40..60) {
                x[i] = -1
                y[i] = -1
            }
        }
        val map = CameraMap(100, 100, 1920, 1080, x, y)
        assertNull(map.projectorPoint(Corner(.5f, .5f)))
        val point = map.projectorPoint(Corner(.2f, .3f))!!
        assertEquals(100.5f / 512, point.x, .001f)
        assertEquals(150.5f / 512, point.y, .001f)
        assertNotNull(map.projectorQuad(Quad.rectangle(.1f, .1f, .9f, .9f)))
        assertNull(map.projectorQuad(Quad.rectangle(.1f, .1f, .5f, .9f)))
    }
}
