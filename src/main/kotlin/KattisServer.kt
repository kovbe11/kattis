package com.softpaw.systems

import com.softpaw.systems.command.KattisCommandDispatcher
import com.softpaw.systems.store.BasicKeyValueStore
import com.softpaw.systems.task.ProactiveExpiredKeyCleanupTask
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

fun main() = runBlocking {
    println("Starting Redis-compatible server...")

    val store = BasicKeyValueStore.default()
    val dispatcher = KattisCommandDispatcher(store)

    val backgroundTasks = listOf(
        ProactiveExpiredKeyCleanupTask(store)
    )

    backgroundTasks.forEach { launch { it.start() } }

    startTCPServer(dispatcher)
}