package com.syncplay.app

import java.io.BufferedInputStream
import java.io.DataInputStream
import java.net.Socket
import kotlin.concurrent.thread

class NetworkClient(
    private val hostIp: String,
    private val onConnected: () -> Unit,
    private val onAudioReceived: (ByteArray, Int) -> Unit,
    private val onError: (Exception) -> Unit
) {

    private var socket: Socket? = null
    private var input: DataInputStream? = null

    @Volatile
    private var running = false

    fun connect() {

        thread {

            try {

                socket = Socket(hostIp, 8988)

                socket?.tcpNoDelay = true

                input =
                    DataInputStream(
                        BufferedInputStream(
                            socket!!.getInputStream()
                        )
                    )

                running = true

                onConnected()

                receiveAudio()

            } catch (e: Exception) {

                onError(e)
            }
        }
    }

    private fun receiveAudio() {

        try {

            while (running) {

                val length =
                    input?.readInt()
                        ?: break

                if (length <= 0 || length > 1_000_000) {
                    break
                }

                val buffer =
                    ByteArray(length)

                input?.readFully(buffer)

                onAudioReceived(
                    buffer,
                    length
                )
            }

        } catch (e: Exception) {

            if (running) {
                onError(e)
            }
        }
    }

    fun disconnect() {

        running = false

        try {
            input?.close()
        } catch (_: Exception) {
        }

        try {
            socket?.close()
        } catch (_: Exception) {
        }

        input = null
        socket = null
    }
}
