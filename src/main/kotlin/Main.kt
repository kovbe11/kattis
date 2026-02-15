package com.softpaw.systems

import com.softpaw.systems.command.KattisCommandDispatcher
import com.softpaw.systems.store.BasicKeyValueStore
import kotlinx.coroutines.runBlocking
import java.util.concurrent.ConcurrentHashMap

fun main() = runBlocking {
    println("Starting Redis-compatible server...")

    val store = BasicKeyValueStore(ConcurrentHashMap())
    val dispatcher = KattisCommandDispatcher(store)

    startTCPServer(dispatcher)
}