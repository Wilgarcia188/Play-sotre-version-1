package com.wil.screentimeouttile

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var status: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        status = TextView(this)
        val button = Button(this).apply {
            text = getString(R.string.grant_permission_button)
            setOnClickListener {
                if (!Settings.System.canWrite(this@MainActivity)) {
                    val intent = Intent(
                        Settings.ACTION_MANAGE_WRITE_SETTINGS,
                        Uri.parse("package:$packageName")
                    )
                    startActivity(intent)
                } else {
                    status.text = getString(R.string.permission_granted_message)
                }
            }
        }

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 100, 40, 40)
            addView(status)
            addView(button)
        }
        setContentView(layout)
    }

    override fun onResume() {
        super.onResume()
        if (Settings.System.canWrite(this)) {
            status.text = getString(R.string.permission_granted_message)
        }
    }
}
