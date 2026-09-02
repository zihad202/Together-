package com.syncplay.app

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.view.Gravity
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView

class JoinActivity : Activity() {

    private lateinit var status: TextView

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this)

        layout.orientation = LinearLayout.VERTICAL
        layout.gravity = Gravity.CENTER
        layout.setPadding(40, 40, 40, 40)

        val title = TextView(this)

        title.text = "JOIN ROOM"
        title.textSize = 28f
        title.setTextColor(Color.BLACK)
        title.gravity = Gravity.CENTER

        val ipInput = EditText(this)

        ipInput.hint = "Host IP address"
        ipInput.textSize = 18f

        val joinButton = Button(this)

        joinButton.text = "JOIN"

        status = TextView(this)

        status.text = "Not connected"
        status.textSize = 18f
        status.gravity = Gravity.CENTER

        layout.addView(title)
        layout.addView(ipInput)
        layout.addView(joinButton)
        layout.addView(status)

        setContentView(layout)

        joinButton.setOnClickListener {

            val ip = ipInput.text.toString().trim()

            if (ip.isEmpty()) {

                status.text = "Enter Host IP address"

                return@setOnClickListener
            }

            status.text = "Connecting..."

            NetworkClient(
                hostIp = ip,

                onConnected = {

                    runOnUiThread {

                        status.text =
                            "✓ Connected to Host"
                    }
                },

                onError = {

                    runOnUiThread {

                        status.text =
                            "Connection failed"
                    }
                }

            ).connect()
        }
    }
}
