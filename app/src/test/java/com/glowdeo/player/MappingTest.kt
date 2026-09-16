package com.glowdeo.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MappingTest {
    @Test fun defaultsAreValid() {
        Surface.entries.forEach { assertTrue(it.default.isValid()) }
    }

    @Test fun serializationRoundTrips() {
        val value = Surface.WALL.default.moved(0, .08f, .04f)
        assertEquals(value, Quad.decode(value.encode(), Surface.WALL.default))
    }

    @Test fun invalidSavedDataFallsBack() {
        val fallback = Surface.TV.default
        listOf(null, "", "NaN,0,1,0,1,1,0,1", "0,0,1,1,1,0,0,1", "-1,0,1,0,1,1,0,1", "0,0").forEach {
            assertEquals(fallback, Quad.decode(it, fallback))
        }
    }

    @Test fun rejectsCrossedOrCollapsedCorners() {
        assertFalse(Quad(listOf(Corner(0f, 0f), Corner(1f, 1f), Corner(1f, 0f), Corner(0f, 1f))).isValid())
        assertFalse(Quad.rectangle(.5f, .5f, .51f, .51f).isValid())
    }

    @Test fun movesStayOnscreenAndCannotInvert() {
        val q = Surface.WALL.default
        assertEquals(0f, q.moved(0, -4f, 0f).points[0].x)
        assertEquals(q, q.moved(0, 1f, 1f))
    }
}
