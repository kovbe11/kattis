package com.softpaw.systems

import arrow.core.Either
import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import io.ktor.utils.io.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope


suspend fun startTCPServer(port: Int = 6379) {
    SelectorManager(Dispatchers.IO).use { selector ->
        val serverSocket = aSocket(selector).tcp().bind(port = port)
        println("Redis-compatible server listening on port $port")
        serverSocket.use { listener ->
            startAcceptingConnections(listener)
        }
        println("Server socket closed, shutting down server.")
    }
}

private fun ServerSocket.isActive(): Boolean = this.socketContext.isActive

suspend fun startAcceptingConnections(listener: ServerSocket) = supervisorScope {
    while (listener.isActive()) {
        Either.catch { listener.accept() }
            .fold(
                ifLeft = { e -> println("Error accepting connection: ${e.message}") },
                ifRight = { connection ->
                    launch {
                        connection.use { connection ->
                            val inputChannel = connection.openReadChannel()
                            val outputChannel = connection.openWriteChannel(autoFlush = true)
                            println("Client connected: ${connection.remoteAddress}")

                            Either.catch { handleConnection(inputChannel, outputChannel) }
                                .onLeft { e -> println("Client error: ${e.message}") }
                        }
                    }
                }
            )
    }
    println("Server socket is no longer active, stopping accept loop.")
}

suspend fun handleConnection(inputChannel: ByteReadChannel, outputChannel: ByteWriteChannel) =
    coroutineScope {
        while (!inputChannel.isClosedForRead) {
            val line = inputChannel.readUTF8Line() ?: break
            println("Received: `$line`")
            outputChannel.writeStringUtf8("+PONG\r\n")
        }
        println("Connection is closed, stopping processing loop.")
    }