package com.glowdeo.player

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import java.io.File
import java.io.IOException

@Suppress("TooManyFunctions")
class CameraActivity : Activity() {
    private lateinit var store: CameraRoomStore
    private var room: CameraRoom? = null
    private var scan: CameraScan? = null
    private var canvas: CameraCanvas? = null
    private var editing: Surface? = null
    private var hint: TextView? = null
    private var pendingScan: Boolean? = null
    private var foreground = false
    private var dialog: AlertDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        store = CameraRoomStore(this)
        room = store.load()
        immersive()
        menu()
    }

    @Suppress("DEPRECATION")
    private fun immersive() {
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) immersive()
    }

    private fun column(): LinearLayout =
        LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 16, 24, 16)
            setBackgroundColor(Color.BLACK)
        }

    private fun label(
        value: String,
        size: Float = 18f,
    ): TextView =
        TextView(this).apply {
            text = value
            textSize = size
            setTextColor(Color.WHITE)
            setPadding(8, 8, 8, 8)
        }

    private fun button(
        value: String,
        action: () -> Unit,
    ): Button =
        Button(this).apply {
            text = value
            setOnClickListener { action() }
        }

    private fun menu() {
        editing = null
        canvas = null
        val column = column()
        column.addView(label("Magcubic HY310X · camera & room", 26f))
        column.addView(label("Photos stay on this projector. Close other camera apps and let autofocus finish first."))
        column.addView(button("Take camera photo · 3 2 1") { requestCapture(false) })
        column.addView(button("Calibrate room with projected patterns") { explainScan() })
        val saved = room
        if (saved != null) {
            column.addView(label("Saved room · ${saved.map.width} × ${saved.map.height} camera · ${saved.map.validCount} mapped points"))
            Surface.entries.forEach { surface ->
                column.addView(button("Mark ${surface.label.lowercase()} on photo") { edit(surface) })
            }
        }
        if (saved != null || File(filesDir, "hy310x-last-photo.png").exists()) {
            column.addView(button("Delete saved room photo and calibration") { confirmDelete() })
        }
        column.addView(label("Recalibrate after moving the projector or changing keystone. Manual mapping is still available."))
        column.addView(button("Return to projection") { finish() })
        setContentView(ScrollView(this).apply { addView(column) })
        column.getChildAt(2).requestFocus()
    }

    private fun explainScan() {
        dialog =
            AlertDialog
                .Builder(this)
                .setTitle("Scan this room?")
                .setMessage(
                    "Pause the TV on a black screen, dim the room and keep the projector still. " +
                        "After 3 2 1, Glowdeo projects bright stripes across the full image for about a minute. " +
                        "Press Back to cancel. A failed scan keeps your previous calibration.",
                ).setPositiveButton("Start calibration") { _, _ -> requestCapture(true) }
                .setNegativeButton("Cancel", null)
                .show()
    }

    private fun requestCapture(calibrate: Boolean) {
        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            pendingScan = calibrate
            requestPermissions(arrayOf(Manifest.permission.CAMERA), 10)
        } else {
            beginCapture(calibrate)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        results: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, results)
        val action = pendingScan
        pendingScan = null
        if (requestCode == 10 && action != null) {
            if (results.firstOrNull() == PackageManager.PERMISSION_GRANTED) {
                beginCapture(action)
            } else {
                message(
                    "Camera access is off",
                    "Allow Camera in Glowdeo's app settings to capture or calibrate. Manual mapping still works.",
                )
            }
        }
    }

    private fun beginCapture(calibrate: Boolean) {
        editing = null
        val view = CameraCanvas(this)
        canvas = view
        setContentView(view)
        view.addOnLayoutChangeListener(
            object : View.OnLayoutChangeListener {
                override fun onLayoutChange(
                    v: View,
                    left: Int,
                    top: Int,
                    right: Int,
                    bottom: Int,
                    oldLeft: Int,
                    oldTop: Int,
                    oldRight: Int,
                    oldBottom: Int,
                ) {
                    if (view.width > 1 && view.height > 1) {
                        view.removeOnLayoutChangeListener(this)
                        if (foreground && !isFinishing && !isDestroyed) {
                            scan = CameraScan(view).also { it.start(calibrate, ::captured, ::captureFailed) }
                        }
                    }
                }
            },
        )
    }

    private fun captured(
        photo: Bitmap,
        map: CameraMap?,
    ) {
        scan = null
        try {
            if (map != null) {
                val next = CameraRoom(map, photo)
                store.save(next)
                room = next
                menu()
                message(
                    "Room calibrated",
                    "Choose a surface to mark on the photo. Save it, then check the projected grid and fine-tune if needed.",
                )
            } else {
                File(filesDir, "hy310x-last-photo.png").outputStream().use { photo.compress(Bitmap.CompressFormat.PNG, 100, it) }
                preview(photo)
            }
        } catch (error: IOException) {
            captureFailed("Could not save capture: ${error.message}")
        } catch (error: IllegalStateException) {
            captureFailed("Could not encode capture: ${error.message}")
        }
    }

    private fun preview(photo: Bitmap) {
        val column = column()
        column.addView(label("HY310X camera · saved on this device", 24f))
        val view = CameraCanvas(this).apply { this.photo = photo }
        column.addView(view, LinearLayout.LayoutParams(-1, 0, 1f))
        column.addView(button("Back to camera controls") { menu() })
        canvas = view
        setContentView(column)
    }

    private fun captureFailed(detail: String) {
        scan = null
        menu()
        message(
            "HY310X camera unavailable",
            "$detail\n\nClose Projection Player or autofocus if they are using the camera, then retry. " +
                "This firmware may restrict the camera for this app. Glowdeo does not change device permissions or require ADB. " +
                "Your saved manual mapping is unchanged.",
        )
    }

    private fun edit(surface: Surface) {
        val saved = room ?: return
        if (saved.map.projectorWidth != window.decorView.width || saved.map.projectorHeight != window.decorView.height) {
            message("Recalibration needed", "The projector display size has changed. Run a new room calibration before marking the photo.")
            return
        }
        val projected = PlayerSettings(this).quads.getValue(surface)
        val points = projected.points.mapNotNull { saved.map.cameraPoint(it) }
        val initial = Quad(points).takeIf { it.isValid() } ?: Quad.rectangle(.2f, .2f, .8f, .8f)
        editing = surface
        val column = column()
        hint = label("", 17f)
        column.addView(hint)
        val view =
            CameraCanvas(this).apply {
                photo = saved.photo
                quad = initial
                onPoint = { point -> selectPoint(point) }
            }
        canvas = view
        column.addView(view, LinearLayout.LayoutParams(-1, 0, 1f))
        val row = LinearLayout(this)
        listOf("←" to Pair(-1, 0), "↑" to Pair(0, -1), "↓" to Pair(0, 1), "→" to Pair(1, 0)).forEach { (text, delta) ->
            row.addView(button(text) { move(delta.first, delta.second) }, LinearLayout.LayoutParams(0, -2, 1f))
        }
        row.addView(button("Next") { nextCorner() }, LinearLayout.LayoutParams(0, -2, 1f))
        row.addView(button("Save / grid") { saveSurface() }, LinearLayout.LayoutParams(0, -2, 2f))
        column.addView(row)
        column.addView(button("Discard and return") { menu() })
        setContentView(column)
        view.requestFocus()
        updateHint()
    }

    private fun selectPoint(point: Corner) {
        val view = canvas ?: return
        val old = view.quad ?: return
        val delta = old.points[view.selected]
        view.quad = old.moved(view.selected, point.x - delta.x, point.y - delta.y)
        view.invalidate()
    }

    private fun move(
        dx: Int,
        dy: Int,
    ) {
        val view = canvas ?: return
        val old = view.quad ?: return
        val map = checkNotNull(room).map
        view.quad = old.moved(view.selected, dx.toFloat() / map.width, dy.toFloat() / map.height)
        view.invalidate()
    }

    private fun nextCorner() {
        canvas?.let {
            it.selected = (it.selected + 1) % 4
            it.invalidate()
        }
        updateHint()
    }

    private fun updateHint() {
        hint?.text = "${editing?.label} · corner ${(canvas?.selected ?: 0) + 1}/4 · arrows move · OK next · Menu save"
    }

    private fun saveSurface() {
        val surface = editing ?: return
        val saved = checkNotNull(room)
        val quad = canvas?.quad ?: return
        val projected = saved.map.projectorQuad(quad)
        if (projected == null) {
            message(
                "Corner not mapped",
                "Place every corner on a visible, illuminated edge. Dark or unseen areas cannot be mapped reliably. " +
                    "You can also use manual mapping for this surface.",
            )
        } else {
            PlayerSettings(this).apply {
                quads[surface] = projected
                grid = true
                save()
            }
            finish()
        }
    }

    private fun confirmDelete() {
        dialog =
            AlertDialog
                .Builder(this)
                .setTitle("Delete camera data?")
                .setMessage(
                    "Deletes the saved room photo, calibration scan and last camera photo. Your manual projection corners stay saved.",
                ).setPositiveButton("Delete") { _, _ ->
                    store.clear()
                    File(filesDir, "hy310x-last-photo.png").delete()
                    room = null
                    menu()
                }.setNegativeButton("Cancel", null)
                .show()
    }

    private fun message(
        title: String,
        body: String,
    ) {
        dialog =
            AlertDialog
                .Builder(this)
                .setTitle(title)
                .setMessage(body)
                .setPositiveButton("OK", null)
                .show()
    }

    override fun onKeyDown(
        keyCode: Int,
        event: KeyEvent,
    ): Boolean {
        val action: (() -> Unit)? =
            when {
                scan != null && keyCode == KeyEvent.KEYCODE_BACK -> ({ cancelScan() })
                editing != null -> editAction(keyCode)
                keyCode == KeyEvent.KEYCODE_BACK -> ({ finish() })
                else -> null
            }
        action?.invoke()
        return action != null || super.onKeyDown(keyCode, event)
    }

    private fun editAction(keyCode: Int): (() -> Unit)? =
        when (keyCode) {
            KeyEvent.KEYCODE_DPAD_LEFT -> ({ move(-1, 0) })
            KeyEvent.KEYCODE_DPAD_RIGHT -> ({ move(1, 0) })
            KeyEvent.KEYCODE_DPAD_UP -> ({ move(0, -1) })
            KeyEvent.KEYCODE_DPAD_DOWN -> ({ move(0, 1) })
            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> ::nextCorner
            KeyEvent.KEYCODE_MENU -> ::saveSurface
            KeyEvent.KEYCODE_BACK -> ({ menu() })
            else -> null
        }

    private fun cancelScan() {
        scan?.close()
        scan = null
        menu()
    }

    override fun onResume() {
        super.onResume()
        foreground = true
    }

    override fun onPause() {
        foreground = false
        if (scan != null) cancelScan()
        super.onPause()
    }

    override fun onDestroy() {
        scan?.close()
        dialog?.dismiss()
        super.onDestroy()
    }
}
