package com.softpaw.systems.task

import com.softpaw.systems.store.KeyExpiryCleanupPort
import kotlinx.coroutines.delay

class ProactiveExpiredKeyCleanupTask(private val keyExpiryCleanupPort: KeyExpiryCleanupPort) : BackgroundTask {

    companion object {
        private const val CLEANUP_INTERVAL_MS = 100L
        private const val MAX_TIME = 25
        private const val KEYS_TO_CLEAN = 20
    }

    override suspend fun start(): Nothing {
        while (true) {
            delay(CLEANUP_INTERVAL_MS)
            val startTime = System.currentTimeMillis()
            do {
                val removedItemCount = keyExpiryCleanupPort.proactiveExpireCleanup(KEYS_TO_CLEAN)
            } while (removedItemCount > KEYS_TO_CLEAN / 4 && (System.currentTimeMillis() - startTime) < MAX_TIME)
        }
    }
}