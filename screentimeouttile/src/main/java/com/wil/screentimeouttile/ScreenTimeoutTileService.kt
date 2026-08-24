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
        private const val KEY_PREVIOUS_TIMEOUT = "previous_timeout"
        private const val KEY_ALWAYS_ON = "always_on_active"
        private const val ALWAYS_ON_VALUE = Int.MAX_VALUE
        private const val DEFAULT_TIMEOUT_FALLBACK = 30000
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

        if (!Settings.System.canWrite(applicationContext)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_WRITE_SETTINGS,
                Uri.parse("package:$packageName")
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivityAndCollapse(intent)
            return
        }

        val isAlwaysOnActive = prefs.getBoolean(KEY_ALWAYS_ON, false)

        if (isAlwaysOnActive) {
            val previousTimeout = prefs.getInt(KEY_PREVIOUS_TIMEOUT, DEFAULT_TIMEOUT_FALLBACK)
            Settings.System.putInt(
                contentResolver,
                Settings.System.SCREEN_OFF_TIMEOUT,
                previousTimeout
            )
            prefs.edit().putBoolean(KEY_ALWAYS_ON, false).apply()
        } else {
            val currentTimeout = Settings.System.getInt(
                contentResolver,
                Settings.System.SCREEN_OFF_TIMEOUT,
                DEFAULT_TIMEOUT_FALLBACK
            )
            prefs.edit()
                .putInt(KEY_PREVIOUS_TIMEOUT, currentTimeout)
                .putBoolean(KEY_ALWAYS_ON, true)
                .apply()

            Settings.System.putInt(
                contentResolver,
                Settings.System.SCREEN_OFF_TIMEOUT,
                ALWAYS_ON_VALUE
            )
        }

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
