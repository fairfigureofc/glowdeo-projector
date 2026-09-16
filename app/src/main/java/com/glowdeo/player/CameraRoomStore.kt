package com.glowdeo.player

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.AtomicFile
import java.io.BufferedOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.IOException

data class CameraRoom(
    val map: CameraMap,
    val photo: Bitmap,
)

class CameraRoomStore(
    context: Context,
) {
    private val file = AtomicFile(File(context.filesDir, "hy310x-room-v1.bin"))

    fun save(room: CameraRoom) {
        val stream = file.startWrite()
        var committed = false
        try {
            val output = DataOutputStream(BufferedOutputStream(stream))
            with(room.map) {
                output.writeInt(0x474c4f31)
                listOf(width, height, projectorWidth, projectorHeight).forEach { output.writeInt(it) }
                for (i in x.indices) {
                    output.writeShort(x[i])
                    output.writeShort(y[i])
                }
            }
            check(room.photo.compress(Bitmap.CompressFormat.PNG, 100, output))
            output.flush()
            file.finishWrite(stream)
            committed = true
        } finally {
            if (!committed) file.failWrite(stream)
        }
    }

    fun load(): CameraRoom? =
        try {
            DataInputStream(file.openRead().buffered()).use { input ->
                require(input.readInt() == 0x474c4f31)
                val w = input.readInt()
                val h = input.readInt()
                val pw = input.readInt()
                val ph = input.readInt()
                require(w in 2..2048 && h in 2..2048 && pw in 2..16384 && ph in 2..16384)
                val x = IntArray(w * h)
                val y = IntArray(w * h)
                for (i in x.indices) {
                    x[i] = input.readShort().toInt()
                    y[i] = input.readShort().toInt()
                }
                val photo = BitmapFactory.decodeStream(input) ?: throw IOException("Room photo is missing")
                require(photo.width == w && photo.height == h)
                CameraRoom(CameraMap(w, h, pw, ph, x, y), photo)
            }
        } catch (_: IOException) {
            null
        } catch (_: IllegalArgumentException) {
            null
        }

    fun clear() {
        file.delete()
    }
}
