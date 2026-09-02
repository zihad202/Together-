package com.syncplay.app

import java.net.ServerSocket
import java.net.Socket
import kotlin.concurrent.thread

class NetworkServer(
    private val onDeviceConnected: (Socket) -> Unit
) {

    private var serverSocket: ServerSocket? = null

    fun start() {

        thread {

            try {

                serverSocket = ServerSocket(8988)

                while (true) {

                    val socket = serverSocket!!.accept()

                    onDeviceConnected(socket)
                }

            } catch (e: Exception) {

                e.printStackTrace()
            }
        }
    }

    fun stop() {

        try {

            serverSocket?.close()

        } catch (e: Exception) {

            e.printStackTrace()
        }
    }
}
