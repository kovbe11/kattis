package com.softpaw.systems

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.merge
import com.softpaw.systems.command.KattisCommand
import com.softpaw.systems.command.KattisCommandDispatcher
import com.softpaw.systems.resp.RespArray
import com.softpaw.systems.resp.RespProtocol.deserialize
import com.softpaw.systems.resp.RespProtocol.serialize
import com.softpaw.systems.resp.RespSimpleError
import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import io.ktor.utils.io.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope


suspend fun startTCPServer(dispatcher: KattisCommandDispatcher, port: Int = 6379) {
    SelectorManager(Dispatchers.IO).use { selector ->
        val serverSocket = aSocket(selector).tcp().bind(port = port)
        println("Redis-compatible server listening on port $port")
        serverSocket.use { listener ->
            startAcceptingConnections(listener, dispatcher)
        }
        println("Server socket closed, shutting down server.")
    }
}

private fun ServerSocket.isActive(): Boolean = this.socketContext.isActive

suspend fun startAcceptingConnections(listener: ServerSocket, dispatcher: KattisCommandDispatcher) = supervisorScope {
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

                            Either.catch { handleConnection(inputChannel, outputChannel, dispatcher) }
                                .onLeft { e -> println("Client error: ${e.message}") }
                        }
                    }
                }
            )
    }
    println("Server socket is no longer active, stopping accept loop.")
}

suspend fun handleConnection(
    inputChannel: ByteReadChannel,
    outputChannel: ByteWriteChannel,
    dispatcher: KattisCommandDispatcher
) = coroutineScope {
    while (!inputChannel.isClosedForRead) {
        val respValue = deserialize(inputChannel)
        if (respValue !is RespArray) {
            val errorResponse = serialize(RespSimpleError("ERR expected array"))
            outputChannel.writeStringUtf8(errorResponse)
            continue
        }
        val response = KattisCommand.resolve(respValue)
            .flatMap { dispatcher.execute(it) }
            .merge()
            .let { serialize(it) }

        outputChannel.writeStringUtf8(response)
    }
    println("Connection is closed, stopping processing loop.")
}