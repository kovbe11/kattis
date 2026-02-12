package com.softpaw.systems

import kotlinx.coroutines.runBlocking

fun main() = runBlocking {
    println("Starting Redis-compatible server...")
    startTCPServer()
}