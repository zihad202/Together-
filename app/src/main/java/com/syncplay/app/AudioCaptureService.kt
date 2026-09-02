package com.syncplay.app

import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioPlaybackCaptureConfiguration
import android.media.AudioRecord
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import kotlin.concurrent.thread

class AudioCaptureService : Service() {

    private var mediaProjection: MediaProjection? = null
    private var audioRecord: AudioRecord? = null
    private var captureThread: Thread? = null

    @Volatile
    private var isCapturing = false

    companion object {
        private const val CHANNEL_ID = "syncplay_capture"
        private const val NOTIFICATION_ID = 1001

        const val RESULT_CODE = "resultCode"
        const val PROJECTION_DATA = "projectionData"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {

        if (intent == null) {
            stopSelf()
            return START_NOT_STICKY
        }

        val resultCode =
            intent.getIntExtra(RESULT_CODE, Activity.RESULT_CANCELED)

        val projectionData: Intent? =
            if (Build.VERSION.SDK_INT >= 33) {
                intent.getParcelableExtra(
                    PROJECTION_DATA,
                    Intent::class.java
                )
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra(PROJECTION_DATA)
            }

        if (resultCode != Activity.RESULT_OK || projectionData == null) {
            stopSelf()
            return START_NOT_STICKY
        }

        startCapture(resultCode, projectionData)

        return START_NOT_STICKY
    }

    private fun startCapture(
        resultCode: Int,
        projectionData: Intent
    ) {

        if (isCapturing) return

        val notification = createNotification(
            "Capturing Host audio..."
        )

        if (Build.VERSION.SDK_INT >= 29) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
            )
        } else {
            startForeground(
                NOTIFICATION_ID,
                notification
            )
        }

        val projectionManager =
            getSystemService(
                Context.MEDIA_PROJECTION_SERVICE
            ) as MediaProjectionManager

        mediaProjection =
            projectionManager.getMediaProjection(
                resultCode,
                projectionData
            )

        if (mediaProjection == null) {
            stopSelf()
            return
        }

        setupAudioCapture()
    }

    private fun setupAudioCapture() {

        if (Build.VERSION.SDK_INT < 29) {
            stopSelf()
            return
        }

        try {

            val captureConfig =
                AudioPlaybackCaptureConfiguration.Builder(
                    mediaProjection!!
                )
                    .addMatchingUsage(
                        AudioAttributes.USAGE_MEDIA
                    )
                    .addMatchingUsage(
                        AudioAttributes.USAGE_GAME
                    )
                    .addMatchingUsage(
                        AudioAttributes.USAGE_UNKNOWN
                    )
                    .build()

            val audioFormat =
                AudioFormat.Builder()
                    .setEncoding(
                        AudioFormat.ENCODING_PCM_16BIT
                    )
                    .setSampleRate(48000)
                    .setChannelMask(
                        AudioFormat.CHANNEL_IN_MONO
                    )
                    .build()

            val minBufferSize =
                AudioRecord.getMinBufferSize(
                    48000,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
                )

            if (minBufferSize <= 0) {
                stopSelf()
                return
            }

            audioRecord =
                AudioRecord.Builder()
                    .setAudioFormat(audioFormat)
                    .set
