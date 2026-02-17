package com.softpaw.systems.store

import com.softpaw.systems.resp.RespValue
import kotlinx.io.bytestring.ByteString
import java.time.Instant


interface KeyValueGetPort {
    fun get(key: ByteString): RespValue<*>?
}

interface KeyValueSetPort {
    fun set(key: ByteString, value: RespValue<*>)
}

interface KeyValueDeletePort {
    fun delete(key: ByteString): Boolean
}

interface KeyExistsPort {
    fun exists(key: ByteString): Boolean
}

interface KeyValueClearPort {
    fun clear()
}

interface KeyValueSetExpirationPort {
    fun expire(key: ByteString, at: Instant?): Boolean
}

interface KeyValueGetTtlPort {
    fun ttl(key: ByteString): Pair<Instant?, Boolean>
}

interface KeyExpiryCleanupPort {
    fun proactiveExpireCleanup(numberOfKeysToClean: Int): Int
}

interface KeyValueStore : KeyValueGetPort, KeyValueSetPort, KeyValueDeletePort, KeyExistsPort, KeyValueClearPort,
    KeyValueSetExpirationPort, KeyValueGetTtlPort, KeyExpiryCleanupPort



