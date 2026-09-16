package com.glowdeo.player

import android.app.Activity
import android.app.AlertDialog
import android.os.Bundle
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

@Suppress("TooManyFunctions")
class MainActivity : Activity() {
    private lateinit var settings: PlayerSettings
    private lateinit var player: PlayerView
    private lateinit var controls: LinearLayout
    private lateinit var hint: TextView
    private var original: Quad? = null
    private var step = .002f
    private var dialog: AlertDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        settings = PlayerSettings(this)
        player = PlayerView(this, settings)
        val root = android.widget.FrameLayout(this)
        root.addView(player)
        controls = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 8, 16, 8)
            setBackgroundColor(0xEE132119.toInt())
            visibility = View.GONE
        }
        hint = TextView(this).apply { setTextColor(android.graphics.Color.WHITE); textSize = 16f }
        controls.addView(hint)
        val row = LinearLayout(this)
        listOf("←" to Pair(-1, 0), "↑" to Pair(0, -1), "↓" to Pair(0, 1), "→" to Pair(1, 0)).forEach { (label, direction) ->
            row.addView(button(label) { move(direction.first, direction.second) }, LinearLayout.LayoutParams(0, 48, 1f))
        }
        row.addView(button("Next corner") { nextCorner() }, LinearLayout.LayoutParams(0, 48, 2f))
        row.addView(button("Save / options") { editMenu() }, LinearLayout.LayoutParams(0, 48, 2f))
        controls.addView(row)
        root.addView(controls, android.widget.FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM))
        setContentView(root)
        player.setOnClickListener { if (player.editing == null) showMenu() else nextCorner() }
        player.requestFocus()
        immersive()
        showMenu()
    }

    private fun button(label: String, action: () -> Unit): Button = Button(this).apply {
        text = label
        contentDescription = label
        setOnClickListener { action() }
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

    override fun onResume() {
        super.onResume()
        if (::player.isInitialized) { player.running = true; player.invalidate() }
    }

    override fun onPause() {
        if (::player.isInitialized) player.running = false
        super.onPause()
    }

    override fun onDestroy() {
        dialog?.dismiss()
        super.onDestroy()
    }

    private fun showMenu() {
        if (dialog?.isShowing == true) return
        val choices = arrayOf(
            "Play offline demo", "Show alignment grid", "Map wall boundary", "Map TV blackout", "Map featured panel",
            if (settings.motion) "Pause animation" else "Resume animation", "Reset mapping…", "About / controls", "Exit Glowdeo",
        )
        dialog = AlertDialog.Builder(this).setTitle("Glowdeo · offline alpha").setItems(choices) { _, which ->
            dialog = null
            when (which) {
                0 -> { settings.grid = false; settings.save(); player.invalidate() }
                1 -> { settings.grid = true; settings.save(); player.invalidate() }
                2 -> beginEditing(Surface.WALL)
                3 -> beginEditing(Surface.TV)
                4 -> beginEditing(Surface.FEATURE)
                5 -> { settings.motion = !settings.motion; settings.save(); player.invalidate() }
                6 -> confirmReset()
                7 -> about()
                8 -> finish()
            }
        }.setNegativeButton("Close", null).create()
        dialog?.show()
    }

    private fun beginEditing(surface: Surface) {
        original = settings.quads.getValue(surface)
        player.editing = surface
        player.corner = 0
        controls.visibility = View.VISIBLE
        updateHint()
        player.requestFocus()
        player.invalidate()
    }

    private fun updateHint() {
        hint.text = "${player.editing?.label} · corner ${player.corner + 1}/4 · arrows move · OK next · Back save/options"
    }

    private fun move(dx: Int, dy: Int) {
        val surface = player.editing ?: return
        settings.quads[surface] = settings.quads.getValue(surface).moved(player.corner, dx * step, dy * step)
        player.invalidate()
    }

    private fun nextCorner() {
        player.corner = (player.corner + 1) % 4
        updateHint()
        player.invalidate()
    }

    private fun editMenu() {
        if (dialog?.isShowing == true) return
        dialog = AlertDialog.Builder(this).setTitle("Mapping options").setItems(
            arrayOf("Save mapping", "Keep adjusting", if (step < .005f) "Use coarse steps" else "Use fine steps", "Discard changes"),
        ) { _, which ->
            dialog = null
            when (which) {
                0 -> finishEditing(true)
                1 -> player.requestFocus()
                2 -> { step = if (step < .005f) .01f else .002f; player.requestFocus() }
                3 -> finishEditing(false)
            }
        }.create()
        dialog?.show()
    }

    private fun finishEditing(save: Boolean) {
        val surface = player.editing ?: return
        if (save) settings.save() else original?.let { settings.quads[surface] = it }
        player.editing = null
        original = null
        controls.visibility = View.GONE
        player.invalidate()
        player.requestFocus()
    }

    private fun confirmReset() {
        dialog = AlertDialog.Builder(this).setTitle("Reset all mapping?")
            .setMessage("This replaces your saved wall, TV blackout and featured-panel corners with the defaults.")
            .setPositiveButton("Reset") { _, _ ->
                Surface.entries.forEach { settings.quads[it] = it.default }
                settings.save(); player.invalidate()
            }.setNegativeButton("Cancel", null).create()
        dialog?.show()
    }

    private fun about() {
        dialog = AlertDialog.Builder(this).setTitle("Glowdeo 0.1.0 alpha")
            .setMessage("Standalone offline demo. All sports figures are sample data.\n\nPress OK or tap for controls. Map wall first, then TV blackout and featured panel. Arrows move corners; OK selects the next corner; Back opens Save/options.\n\nNo account, network, camera, or developer mode is needed to run this app. Phone pairing and live stats are planned, not connected.")
            .setPositiveButton("Got it", null).create()
        dialog?.show()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (player.editing != null) {
            when (keyCode) {
                KeyEvent.KEYCODE_DPAD_LEFT -> move(-1, 0)
                KeyEvent.KEYCODE_DPAD_RIGHT -> move(1, 0)
                KeyEvent.KEYCODE_DPAD_UP -> move(0, -1)
                KeyEvent.KEYCODE_DPAD_DOWN -> move(0, 1)
                KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> nextCorner()
                KeyEvent.KEYCODE_BACK, KeyEvent.KEYCODE_MENU -> editMenu()
                else -> return super.onKeyDown(keyCode, event)
            }
            return true
        }
        if (keyCode in listOf(KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_MENU, KeyEvent.KEYCODE_BACK)) {
            showMenu()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }
}
