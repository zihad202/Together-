package com.syncplay.app

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this)

        layout.orientation = LinearLayout.VERTICAL
        layout.gravity = Gravity.CENTER

        layout.setPadding(50, 50, 50, 50)

        val title = TextView(this)

        title.text = "SYNCPLAY"
        title.textSize = 32f
        title.setTextColor(Color.BLACK)
        title.gravity = Gravity.CENTER

        val subtitle = TextView(this)

        subtitle.text = "Play audio together"
        subtitle.textSize = 16f
        subtitle.gravity = Gravity.CENTER

        val hostButton = Button(this)

        hostButton.text = "CREATE ROOM"

        val joinButton = Button(this)

        joinButton.text = "JOIN ROOM"

        layout.addView(title)
        layout.addView(subtitle)
        layout.addView(hostButton)
        layout.addView(joinButton)

        hostButton.setOnClickListener {
            Toast.makeText(
                this,
                "Host mode selected",
                Toast.LENGTH_SHORT
            ).show()
        }

        joinButton.setOnClickListener {
            Toast.makeText(
                this,
                "Join mode selected",
                Toast.LENGTH_SHORT
            ).show()
        }

        setContentView(layout)
    }
}
