package com.glowdeo.player

import android.content.Context
import android.view.KeyEvent
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RemoteMappingTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()

    @Before fun resetSettings() {
        instrumentation.targetContext
            .getSharedPreferences("glowdeo.mapping.v1", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
    }

    @Test fun remoteMovesCornerAndSavesAcrossActivityRecreation() {
        ActivityScenario.launch(MainActivity::class.java).use { activity ->
            onView(withText("Map TV blackout")).perform(click())
            instrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_DPAD_RIGHT)
            instrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
            onView(withText("Save mapping")).perform(click())
            activity.recreate()
            assertEquals(
                Surface.TV.default.moved(0, .002f, 0f),
                PlayerSettings(instrumentation.targetContext).quads[Surface.TV],
            )
        }
    }

    @Test fun discardDoesNotOverwriteSavedMapping() {
        ActivityScenario.launch(MainActivity::class.java).use {
            onView(withText("Map wall boundary")).perform(click())
            instrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_DPAD_RIGHT)
            instrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
            onView(withText("Discard changes")).perform(click())
            assertEquals(Surface.WALL.default, PlayerSettings(instrumentation.targetContext).quads[Surface.WALL])
        }
    }
}
