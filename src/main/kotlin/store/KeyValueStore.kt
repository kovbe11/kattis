package com.softpaw.systems.store

import com.softpaw.systems.resp.RespValue
import java.time.Instant


interface KeyValueGetPort {
    fun get(key: String): RespValue<*>?
}

interface KeyValueSetPort {
    fun set(key: String, value: RespValue<*>)
}

interface KeyValueDeletePort {
    fun delete(key: String): Boolean
}

interface KeyExistsPort {
    fun exists(key: String): Boolean
}

interface KeyValueClearPort {
    fun clear()
}

interface KeyValueSetExpirationPort {
    fun expire(key: String, at: Instant?): Boolean
}

interface KeyValueGetTtlPort {
    fun ttl(key: String): Pair<Instant?, Boolean>
}

interface KeyExpiryCleanupPort {
    fun proactiveExpireCleanup(numberOfKeysToClean: Int): Int
}

interface KeyValueStore : KeyValueGetPort, KeyValueSetPort, KeyValueDeletePort, KeyExistsPort, KeyValueClearPort,
    KeyValueSetExpirationPort, KeyValueGetTtlPort, KeyExpiryCleanupPort



