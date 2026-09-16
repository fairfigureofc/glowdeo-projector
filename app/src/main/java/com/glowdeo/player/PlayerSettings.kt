package com.glowdeo.player

import android.content.Context

class PlayerSettings(
    context: Context,
) {
    private val preferences = context.getSharedPreferences("glowdeo.mapping.v1", Context.MODE_PRIVATE)
    val quads = Surface.entries.associateWith { Quad.decode(preferences.getString(it.name, null), it.default) }.toMutableMap()
    var grid = preferences.getBoolean("grid", false)
    var motion = preferences.getBoolean("motion", true)

    fun save() {
        preferences
            .edit()
            .apply {
                quads.forEach { (surface, quad) -> putString(surface.name, quad.encode()) }
                putBoolean("grid", grid)
                putBoolean("motion", motion)
            }.apply()
    }
}
