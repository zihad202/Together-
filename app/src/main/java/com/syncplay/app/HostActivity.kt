package com.syncplay.app

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class HostActivity : Activity() {

    private lateinit var status: TextView

    companion object {
        private const val RECORD_AUDIO_REQUEST = 2001
        private const val MEDIA_PROJECTION_REQUEST = 2002
    }

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
        status.text = "Ready"
        status.textSize = 18f
        status.gravity = Gravity.CENTER

        val startButton = Button(this)
        startButton.text = "🎵 START AUDIO SHARING"

        val stopButton = Button(this)
        stopButton.text = "⏹ STOP SHARING"

        layout.addView(title)
        layout.addView(room)
        layout.addView(status)
        layout.addView(startButton)
        layout.addView(stopButton)

        setContentView(layout)

        startButton.setOnClickListener {
            requestAudioPermission()
        }

        stopButton.setOnClickListener {
            stopAudioSharing()
        }
    }

    private fun requestAudioPermission() {

        if (
            Build.VERSION.SDK_INT >= 23 &&
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {

            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.RECORD_AUDIO),
                RECORD_AUDIO_REQUEST
            )

        } else {

            requestMediaProjection()
        }
    }

    private fun requestMediaProjection() {

        if (Build.VERSION.SDK_INT < 29) {

            status.text =
                "Android 10 or newer is required"

            return
        }

        val manager =
            getSystemService(
                MEDIA_PROJECTION_SERVICE
            ) as MediaProjectionManager

        val captureIntent =
            manager.createScreenCaptureIntent()

        startActivityForResult(
            captureIntent,
            MEDIA_PROJECTION_REQUEST
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {

        super.onRequestPermissionsResult(
            requestCode,
            permissions,
            grantResults
        )

        if (requestCode == RECORD_AUDIO_REQUEST) {

            if (
                grantResults.isNotEmpty() &&
                grantResults[0] ==
                PackageManager.PERMISSION_GRANTED
            ) {

                requestMediaProjection()

            } else {

                status.text =
                    "Microphone permission denied"
            }
        }
    }

    @Suppress("DEPRECATION")
    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {

        super.onActivityResult(
            requestCode,
            resultCode,
            data
        )

        if (requestCode == MEDIA_PROJECTION_REQUEST) {

            if (
                resultCode == RESULT_OK &&
                data != null
            ) {

                startAudioSharing(
                    resultCode,
                    data
                )

            } else {

                status.text =
                    "Audio capture permission denied"
            }
        }
    }

    private fun startAudioSharing(
        resultCode: Int,
        data: Intent
    ) {

        val serviceIntent =
            Intent(
                this,
                AudioCaptureService::class.java
            ).apply {

                putExtra(
                    AudioCaptureService.RESULT_CODE,
                    resultCode
                )

                putExtra(
                    AudioCaptureService.PROJECTION_DATA,
                    data
                )
            }

        if (Build.VERSION.SDK_INT >= 26) {

            ContextCompat.startForegroundService(
                this,
                serviceIntent
            )

        } else {

            startService(serviceIntent)
        }

        status.text =
            "✓ Audio sharing started\nPort: 8988"
    }

    private fun stopAudioSharing() {

        val serviceIntent =
            Intent(
                this,
                AudioCaptureService::class.java
            )

        stopService(serviceIntent)

        status.text =
            "Audio sharing stopped"
    }
}
