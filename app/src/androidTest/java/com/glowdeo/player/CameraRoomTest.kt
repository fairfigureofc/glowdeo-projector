package com.glowdeo.player

import android.graphics.Bitmap
import android.os.ParcelFileDescriptor
import android.view.KeyEvent
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeFalse
import org.junit.Before
import org.junit.Test
import java.io.File
import java.io.IOException

class CameraRoomTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Before fun resetRoom() {
        CameraRoomStore(context).clear()
        PlayerSettings(context).apply {
            Surface.entries.forEach { quads[it] = it.default }
            motion = false
            grid = false
            save()
        }
    }

    @Test fun nativeCameraModuleLoadsOnPackagedArchitecture() {
        System.loadLibrary("glowdeo_camera")
    }

    @Test fun missingCameraReportsIoErrorWithoutRootOrAdb() {
        assumeFalse(File("/dev/video0").exists())
        Hy310xCamera().use { camera -> assertThrows(IOException::class.java) { camera.open() } }
    }

    @Test fun savedRoomRoundTripsPhotoAndCoordinatesAndCanBeDeleted() {
        val store = CameraRoomStore(context)
        val photo = Bitmap.createBitmap(32, 24, Bitmap.Config.ARGB_8888)
        photo.eraseColor(0xff558899.toInt())
        val map = CameraMap(32, 24, 1280, 720, IntArray(768) { it % 32 }, IntArray(768) { it / 32 })
        store.save(CameraRoom(map, photo))
        val loaded = store.load()!!
        assertArrayEquals(map.x, loaded.map.x)
        assertArrayEquals(map.y, loaded.map.y)
        assertEquals(photo.getPixel(10, 10), loaded.photo.getPixel(10, 10))
        store.clear()
        assertNull(store.load())
    }

    @Test fun corruptRoomFallsBackWithoutChangingManualMapping() {
        val original = PlayerSettings(context).quads.toMap()
        File(context.filesDir, "hy310x-room-v1.bin").writeBytes(byteArrayOf(1, 2, 3))
        assertNull(CameraRoomStore(context).load())
        assertEquals(original, PlayerSettings(context).quads)
        CameraRoomStore(context).clear()
    }

    @Test fun cameraControlsOpenWithoutCameraPermissionOrHardware() {
        ActivityScenario.launch(CameraActivity::class.java).use {
            onView(withText("Take camera photo · 3 2 1")).check(matches(isDisplayed()))
            screenshot("glowdeo-camera-controls.png")
            onView(withText("Return to projection")).perform(scrollTo(), click())
        }
    }

    @Test fun photoCornerCanBeAdjustedAndSavedAsProjectionGrid() {
        val original = PlayerSettings(context).quads.getValue(Surface.TV)
        ActivityScenario.launch(CameraActivity::class.java).use { activity ->
            onView(withText("Take camera photo · 3 2 1")).check(matches(isDisplayed()))
            activity.onActivity { screen ->
                val w = 320
                val h = 240
                val pixels =
                    IntArray(w * h) { i ->
                        if ((i % w / 20 + i / w / 20) % 2 == 0) 0xff143444.toInt() else 0xff315566.toInt()
                    }
                val bitmap = Bitmap.createBitmap(pixels, w, h, Bitmap.Config.ARGB_8888)
                val map =
                    CameraMap(
                        w,
                        h,
                        screen.window.decorView.width,
                        screen.window.decorView.height,
                        IntArray(w * h) { it % w * 512 / w },
                        IntArray(w * h) { it / w * 512 / h },
                    )
                CameraRoomStore(context).save(CameraRoom(map, bitmap))
            }
            activity.recreate()
            onView(withText("Mark tv blackout on photo")).perform(scrollTo(), click())
            val instrumentation = InstrumentationRegistry.getInstrumentation()
            repeat(4) { instrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_DPAD_RIGHT) }
            screenshot("glowdeo-photo-mapping.png")
            onView(withText("Save / grid")).perform(click())
            val saved = PlayerSettings(context)
            assertTrue(saved.grid)
            assertTrue(
                saved.quads
                    .getValue(Surface.TV)
                    .points[0]
                    .x > original.points[0].x,
            )
            assertEquals(Surface.WALL.default, saved.quads[Surface.WALL])
        }
    }

    private fun screenshot(name: String) {
        val descriptor =
            InstrumentationRegistry
                .getInstrumentation()
                .uiAutomation
                .executeShellCommand("screencap -p /sdcard/$name")
        ParcelFileDescriptor.AutoCloseInputStream(descriptor).use { it.readBytes() }
    }
}
