package com.wil.screentimeouttile

import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.provider.Settings
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService

class ScreenTimeoutTileService : TileService() {

    companion object {
        private const val PREFS = "screen_timeout_prefs"
        private const val KEY_ALWAYS_ON = "always_on_active"
    }

    private lateinit var prefs: SharedPreferences

    override fun onCreate() {
        super.onCreate()
        prefs = getSharedPreferences(PREFS, MODE_PRIVATE)
    }

    override fun onStartListening() {
        super.onStartListening()
        updateTileState()
    }

    override fun onClick() {
        super.onClick()

        if (!Settings.canDrawOverlays(applicationContext)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivityAndCollapse(intent)
            return
        }

        val isAlwaysOnActive = prefs.getBoolean(KEY_ALWAYS_ON, false)
        val nowActive = !isAlwaysOnActive

        val serviceIntent = Intent(this, StayAwakeService::class.java).apply {
            action = if (nowActive) StayAwakeService.ACTION_START else StayAwakeService.ACTION_STOP
        }
        startForegroundService(serviceIntent)

        prefs.edit().putBoolean(KEY_ALWAYS_ON, nowActive).apply()
        updateTileState()
    }

    private fun updateTileState() {
        val isAlwaysOnActive = prefs.getBoolean(KEY_ALWAYS_ON, false)
        qsTile?.apply {
            state = if (isAlwaysOnActive) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
            label = getString(
                if (isAlwaysOnActive) R.string.tile_label_active else R.string.tile_label_inactive
            )
            updateTile()
        }
    }
}
