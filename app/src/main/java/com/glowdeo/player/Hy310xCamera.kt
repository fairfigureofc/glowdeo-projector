package com.glowdeo.player

import android.graphics.Bitmap
import java.io.Closeable
import java.io.IOException

/** Only opened by a user-triggered foreground capture after CAMERA permission. */
class Hy310xCamera : Closeable {
    private var handle = 0L

    fun open() {
        check(handle == 0L)
        try {
            System.loadLibrary("glowdeo_camera")
            handle = openNative()
        } catch (error: UnsatisfiedLinkError) {
            throw IOException("Camera module is unavailable for this processor.", error)
        }
    }

    fun capture(): Bitmap {
        check(handle != 0L)
        val data = captureNative(handle)
        return Bitmap.createBitmap(data, 2, data[0], data[0], data[1], Bitmap.Config.ARGB_8888)
    }

    override fun close() {
        if (handle != 0L) closeNative(handle)
        handle = 0L
    }

    private external fun openNative(): Long

    private external fun captureNative(handle: Long): IntArray

    private external fun closeNative(handle: Long)
}
