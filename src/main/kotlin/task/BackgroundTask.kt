package com.softpaw.systems.task

interface BackgroundTask {
    suspend fun start(): Nothing
}