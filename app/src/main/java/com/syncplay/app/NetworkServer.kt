package com.syncplay.app

import java.io.DataOutputStream
import java.net.ServerSocket
import java.net.Socket
import kotlin.concurrent.thread

class NetworkServer {

    private var serverSocket: ServerSocket? = null

    private val clients =
        mutableListOf<DataOutputStream>()

    @Volatile
    private var running = false

    fun start() {

        thread {

            try {

                serverSocket = ServerSocket(8988)
                running = true

                while (running) {

                    val socket =
                        serverSocket!!.accept()

                    addClient(socket)
                }

            } catch (e: Exception) {

                if (running) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun addClient(socket: Socket) {

        try {

            socket.tcpNoDelay = true

            val output =
                DataOutputStream(
                    socket.getOutputStream()
                )

            synchronized(clients) {
                clients.add(output)
            }

        } catch (e: Exception) {

            try {
                socket.close()
            } catch (_: Exception) {
            }
        }
    }

    fun sendAudio(
        buffer: ByteArray,
        length: Int
    ) {

        synchronized(clients) {

            val iterator =
                clients.iterator()

            while (iterator.hasNext()) {

                val output =
                    iterator.next()

                try {

                    output.writeInt(length)
                    output.write(buffer, 0, length)
                    output.flush()

                } catch (e: Exception) {

                    iterator.remove()

                    try {
                        output.close()
                    } catch (_: Exception) {
                    }
                }
            }
        }
    }

    fun stop() {

        running = false

        try {
            serverSocket?.close()
        } catch (_: Exception) {
        }

        synchronized(clients) {

            clients.forEach {

                try {
                    it.close()
                } catch (_: Exception) {
                }
            }

            clients.clear()
        }
    }

    fun getClientCount(): Int {

        synchronized(clients) {
            return clients.size
        }
    }
}
