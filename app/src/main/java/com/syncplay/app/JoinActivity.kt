package com.syncplay.app

import android.app.Activity
import android.graphics.Color
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView

class JoinActivity : Activity() {

    private lateinit var status: TextView

    private var networkClient: NetworkClient? = null
    private var audioTrack: AudioTrack? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this)

        layout.orientation =
            LinearLayout.VERTICAL

        layout.gravity =
            Gravity.CENTER

        layout.setPadding(
            40,
            40,
            40,
            40
        )

        val title = TextView(this)

        title.text = "JOIN ROOM"
        title.textSize = 28f
        title.setTextColor(Color.BLACK)
        title.gravity = Gravity.CENTER

        val ipInput = EditText(this)

        ipInput.hint =
            "Host IP address"

        ipInput.textSize = 18f

        val joinButton = Button(this)

        joinButton.text =
            "🔗 JOIN"

        val disconnectButton = Button(this)

        disconnectButton.text =
            "⏹ DISCONNECT"

        status = TextView(this)

        status.text =
            "Not connected"

        status.textSize = 18f
        status.gravity = Gravity.CENTER

        layout.addView(title)
        layout.addView(ipInput)
        layout.addView(joinButton)
        layout.addView(disconnectButton)
        layout.addView(status)

        setContentView(layout)

        joinButton.setOnClickListener {

            val ip =
                ipInput.text
                    .toString()
                    .trim()

            if (ip.isEmpty()) {

                status.text =
                    "Enter Host IP address"

                return@setOnClickListener
            }

            status.text =
                "Connecting..."

            setupAudioTrack()

            networkClient =
                NetworkClient(

                    hostIp = ip,

                    onConnected = {

                        runOnUiThread {

                            status.text =
                                "✓ Connected\nWaiting for audio..."
                        }
                    },

                    onAudioReceived = {
                            buffer,
                            length ->

                        audioTrack?.write(
                            buffer,
                            0,
                            length
                        )
                    },

                    onError = {

                        runOnUiThread {

                            status.text =
                                "Connection lost"
                        }
                    }
                )

            networkClient?.connect()
        }

        disconnectButton.setOnClickListener {

            disconnect()
        }
    }

    private fun setupAudioTrack() {

        val sampleRate =
            48000

        val channelConfig =
            AudioFormat.CHANNEL_OUT_MONO

        val audioEncoding =
            AudioFormat.ENCODING_PCM_16BIT

        val bufferSize =
            AudioTrack.getMinBufferSize(
                sampleRate,
                channelConfig,
                audioEncoding
            )

        audioTrack =
            AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(
                            AudioAttributes.USAGE_MEDIA
                        )
                        .setContentType(
                            AudioAttributes.CONTENT_TYPE_MUSIC
                        )
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setSampleRate(
                            sampleRate
                        )
                        .setEncoding(
                            audioEncoding
                        )
                        .setChannelMask(
                            channelConfig
                        )
                        .build()
                )
                .setBufferSizeInBytes(
                    bufferSize * 2
                )
                .setTransferMode(
                    AudioTrack.MODE_STREAM
                )
                .build()

        audioTrack?.play()
    }

    private fun disconnect() {

        networkClient?.disconnect()
        networkClient = null

        try {
            audioTrack?.stop()
        } catch (_: Exception) {
        }

        audioTrack?.release()
        audioTrack = null

        status.text =
            "Disconnected"
    }

    override fun onDestroy() {

        disconnect()

        super.onDestroy()
    }
}
