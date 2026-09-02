package com.syncplay.app

import java.net.Socket
import kotlin.concurrent.thread

class NetworkClient(
    private val hostIp: String,
    private val onConnected: (Socket) -> Unit,
    private val onError: (Exception) -> Unit
) {

    fun connect() {

        thread {

            try {

                val socket = Socket(hostIp, 8988)

                onConnected(socket)

            } catch (e: Exception) {

                onError(e)
            }
        }
    }
}
