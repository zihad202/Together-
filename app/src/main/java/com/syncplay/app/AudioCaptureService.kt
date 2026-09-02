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

    private var networkServer: NetworkServer? = null

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

        // Start local audio server
        networkServer = NetworkServer()
        networkServer?.start()
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
            intent.getIntExtra(
                RESULT_CODE,
                Activity.RESULT_CANCELED
            )

        val projectionData: Intent? =
            if (Build.VERSION.SDK_INT >= 33) {

                intent.getParcelableExtra(
                    PROJECTION_DATA,
                    Intent::class.java
                )

            } else {

                @Suppress("DEPRECATION")
                intent.getParcelableExtra(
                    PROJECTION_DATA
                )
            }

        if (
            resultCode != Activity.RESULT_OK ||
            projectionData == null
        ) {

            stopSelf()
            return START_NOT_STICKY
        }

        startCapture(
            resultCode,
            projectionData
        )

        return START_NOT_STICKY
    }

    private fun startCapture(
        resultCode: Int,
        projectionData: Intent
    ) {

        if (isCapturing) return

        val notification =
            createNotification(
                "Capturing live audio..."
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

            val sampleRate = 48000

            val channelMask =
                AudioFormat.CHANNEL_IN_MONO

            val encoding =
                AudioFormat.ENCODING_PCM_16BIT

            val audioFormat =
                AudioFormat.Builder()
                    .setEncoding(encoding)
                    .setSampleRate(sampleRate)
                    .setChannelMask(channelMask)
                    .build()

            val minBufferSize =
                AudioRecord.getMinBufferSize(
                    sampleRate,
                    channelMask,
                    encoding
                )

            if (minBufferSize <= 0) {

                stopSelf()
                return
            }

            audioRecord =
                AudioRecord.Builder()
                    .setAudioFormat(audioFormat)
                    .setBufferSizeInBytes(
                        minBufferSize * 2
                    )
                    .setAudioPlaybackCaptureConfig(
                        captureConfig
                    )
                    .build()

            audioRecord?.startRecording()

            isCapturing = true

            captureThread = thread(
                start = true,
                name = "SyncPlayAudioCapture"
            ) {

                val buffer =
                    ByteArray(minBufferSize)

                while (isCapturing) {

                    val bytesRead =
                        audioRecord?.read(
                            buffer,
                            0,
                            buffer.size
                        ) ?: 0

                    if (bytesRead > 0) {

                        // Captured live PCM audio
                        // is sent to connected receivers.

                        networkServer?.sendAudio(
                            buffer,
                            bytesRead
                        )
                    }
                }
            }

        } catch (e: Exception) {

            e.printStackTrace()

            stopCapture()
        }
    }

    private fun createNotification(
        text: String
    ) =
        NotificationCompat.Builder(
            this,
            CHANNEL_ID
        )
            .setContentTitle("SyncPlay")
            .setContentText(text)
            .setSmallIcon(
                android.R.drawable.ic_media_play
            )
            .setOngoing(true)
            .build()

    private fun createNotificationChannel() {

        if (Build.VERSION.SDK_INT >= 26) {

            val channel =
                NotificationChannel(
                    CHANNEL_ID,
                    "SyncPlay Audio Capture",
                    NotificationManager.IMPORTANCE_LOW
                )

            val manager =
                getSystemService(
                    NotificationManager::class.java
                )

            manager.createNotificationChannel(
                channel
            )
        }
    }

    private fun stopCapture() {

        isCapturing = false

        try {
            audioRecord?.stop()
        } catch (_: Exception) {
        }

        try {
            audioRecord?.release()
        } catch (_: Exception) {
        }

        audioRecord = null

        try {
            mediaProjection?.stop()
        } catch (_: Exception) {
        }

        mediaProjection = null
    }

    override fun onDestroy() {

        stopCapture()

        networkServer?.stop()
        networkServer = null

        captureThread = null

        super.onDestroy()
    }

    override fun onBind(
        intent: Intent?
    ): IBinder? {
        return null
    }
}
