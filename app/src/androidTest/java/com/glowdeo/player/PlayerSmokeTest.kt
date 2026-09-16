package com.glowdeo.player

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PlayerSmokeTest {
    @Test fun mappingSurvivesReload() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        context
            .getSharedPreferences("glowdeo.mapping.v1", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
        val settings = PlayerSettings(context)
        val changed = Surface.TV.default.moved(0, .02f, .01f)
        settings.quads[Surface.TV] = changed
        settings.motion = false
        settings.save()
        val loaded = PlayerSettings(context)
        assertEquals(changed, loaded.quads[Surface.TV])
        assertEquals(false, loaded.motion)
        context
            .getSharedPreferences("glowdeo.mapping.v1", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
    }
}
