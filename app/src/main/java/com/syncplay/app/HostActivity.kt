package com.syncplay.app

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import java.net.Socket

class HostActivity : Activity() {

    private lateinit var status: TextView

    private var server: NetworkServer? = null

    private var connectedDevices = 0

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this)

        layout.orientation = LinearLayout.VERTICAL
        layout.gravity = Gravity.CENTER
        layout.setPadding(40, 40, 40, 40)

        val title = TextView(this)

        title.text = "HOST MODE"
        title.textSize = 28f
        title.setTextColor(Color.BLACK)
        title.gravity = Gravity.CENTER

        val room = TextView(this)

        room.text = "SYNCPLAY ROOM"
        room.textSize = 20f
        room.gravity = Gravity.CENTER

        status = TextView(this)

        status.text = "Starting local server..."
        status.textSize = 18f
        status.gravity = Gravity.CENTER

        val selectAudio = Button(this)

        selectAudio.text = "SELECT AUDIO"

        val playButton = Button(this)

        playButton.text = "▶ PLAY"

        val pauseButton = Button(this)

        pauseButton.text = "⏸ PAUSE"

        layout.addView(title)
        layout.addView(room)
        layout.addView(status)
        layout.addView(selectAudio)
        layout.addView(playButton)
        layout.addView(pauseButton)

        setContentView(layout)

        startServer()
    }

    private fun startServer() {

        server = NetworkServer { socket: Socket ->

            connectedDevices++

            runOnUiThread {

                status.text =
                    "Connected devices: $connectedDevices"
            }
        }

        server?.start()

        status.text =
            "Server started\nPort: 8988\nWaiting for phones..."
    }

    override fun onDestroy() {

        server?.stop()

        super.onDestroy()
    }
}
