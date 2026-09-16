package com.glowdeo.player

import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import java.io.Closeable
import java.io.IOException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/** Owns the native camera on one worker; cancellation closes it on that same worker. */
class CameraScan(
    private val view: CameraCanvas,
) : Closeable {
    private val cancelled = AtomicBoolean(false)
    private val executor = Executors.newSingleThreadExecutor()
    private val main = Handler(Looper.getMainLooper())

    fun start(
        scan: Boolean,
        complete: (Bitmap, CameraMap?) -> Unit,
        failed: (String) -> Unit,
    ) {
        val projectorWidth = view.width
        val projectorHeight = view.height
        executor.execute {
            try {
                require(projectorWidth > 1 && projectorHeight > 1) { "Projector display is not ready. Please try again." }
                for (number in 3 downTo 1) {
                    display {
                        countdown = number.toString()
                        pattern = null
                        photo = null
                        quad = null
                    }
                    delay(1000)
                }
                val result =
                    Hy310xCamera().use { camera ->
                        camera.open()
                        if (scan) {
                            calibrate(camera, projectorWidth, projectorHeight)
                        } else {
                            display {
                                countdown = null
                                pattern = ScanPattern.WHITE
                            }
                            delay(1000)
                            val photo = camera.capture()
                            Pair(photo, null)
                        }
                    }
                main.post {
                    if (cancelled.get()) result.first.recycle() else complete(result.first, result.second)
                }
            } catch (_: InterruptedException) {
                // Leaving the activity cancels the scan and preserves the previous room.
            } catch (error: IOException) {
                deliver { failed(error.message ?: "Camera I/O failed.") }
            } catch (error: IllegalArgumentException) {
                deliver { failed(error.message ?: "Invalid camera calibration data.") }
            } catch (error: IllegalStateException) {
                deliver { failed(error.message ?: "Camera capture was interrupted.") }
            } finally {
                executor.shutdown()
            }
        }
    }

    private fun calibrate(
        camera: Hy310xCamera,
        width: Int,
        height: Int,
    ): Pair<Bitmap, CameraMap?> {
        var decoder: StructuredLight? = null
        var roomPhoto: Bitmap? = null
        var transferred = false
        try {
            for (pattern in ScanPattern.sequence) {
                display {
                    countdown = null
                    this.pattern = pattern
                }
                delay(650)
                val frame = camera.capture()
                if (decoder == null) decoder = StructuredLight(frame.width, frame.height)
                val reference = decodeFrame(frame, pattern, decoder)
                if (reference != null) roomPhoto = reference
            }
            val map = checkNotNull(decoder).finish(width, height)
            val photo = checkNotNull(roomPhoto)
            transferred = true
            return Pair(photo, map)
        } finally {
            if (!transferred) roomPhoto?.recycle()
        }
    }

    private fun decodeFrame(
        frame: Bitmap,
        pattern: ScanPattern,
        decoder: StructuredLight,
    ): Bitmap? =
        try {
            decoder.add(pattern, luminance(frame))
            if (pattern == ScanPattern.WHITE) frame.copy(Bitmap.Config.ARGB_8888, false) else null
        } finally {
            frame.recycle()
        }

    private fun luminance(bitmap: Bitmap): IntArray {
        val data = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(data, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        for (i in data.indices) {
            val pixel = data[i]
            data[i] = (77 * ((pixel shr 16) and 255) + 150 * ((pixel shr 8) and 255) + 29 * (pixel and 255)) shr 8
        }
        return data
    }

    private fun display(action: CameraCanvas.() -> Unit) {
        checkCancelled()
        val drawn = CountDownLatch(1)
        main.post {
            if (!cancelled.get()) {
                view.action()
                view.invalidate()
                view.postOnAnimation { view.postOnAnimation { drawn.countDown() } }
            } else {
                drawn.countDown()
            }
        }
        if (!drawn.await(3, TimeUnit.SECONDS)) throw IOException("The projection was interrupted. Please retry.")
        checkCancelled()
    }

    private fun deliver(action: () -> Unit) {
        main.post { if (!cancelled.get()) action() }
    }

    private fun checkCancelled() {
        if (cancelled.get()) throw InterruptedException()
    }

    private fun delay(milliseconds: Long) {
        checkCancelled()
        Thread.sleep(milliseconds)
        checkCancelled()
    }

    override fun close() {
        cancelled.set(true)
        executor.shutdownNow()
    }
}
