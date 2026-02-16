package com.softpaw.systems.store

import com.softpaw.systems.resp.RespValue
import java.time.Clock
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap


interface KeyValueGetPort {
    suspend fun get(key: String): RespValue<*>?
}

interface KeyValueSetPort {
    suspend fun set(key: String, value: RespValue<*>)
}

interface KeyValueDeletePort {
    suspend fun delete(key: String): Boolean
}

interface KeyExistsPort {
    suspend fun exists(key: String): Boolean
}

interface KeyValueClearPort {
    suspend fun clear()
}

interface KeyValueSetExpirationPort {
    suspend fun expire(key: String, at: Instant?): Boolean
}

interface KeyValueGetTtlPort {
    suspend fun ttl(key: String): Pair<Instant?, Boolean>
}

interface KeyValueStore : KeyValueGetPort, KeyValueSetPort, KeyValueDeletePort, KeyExistsPort, KeyValueClearPort,
    KeyValueSetExpirationPort, KeyValueGetTtlPort


class BasicKeyValueStore(val map: ConcurrentMap<String, StoreValue>, private val clock: Clock = Clock.systemUTC()) :
    KeyValueStore {

    companion object {
        fun default(): BasicKeyValueStore {
            return BasicKeyValueStore(ConcurrentHashMap())
        }
    }

    data class StoreValue(
        val value: RespValue<*>,
        val expires: Instant? = null,
    ) {
        fun isExpired(clock: Clock = Clock.systemUTC()): Boolean {
            return expires != null && expires < clock.instant()
        }
    }

    override suspend fun get(key: String): RespValue<*>? {
        return map.computeIfPresent(key) { _, v -> if (v.isExpired(clock)) null else v }?.value
    }

    override suspend fun set(key: String, value: RespValue<*>) {
        map[key] = StoreValue(value)
    }

    override suspend fun delete(key: String): Boolean {
        return map.remove(key) != null
    }

    override suspend fun exists(key: String): Boolean {
        return map.containsKey(key)
    }

    override suspend fun clear() {
        return map.clear()
    }

    override suspend fun expire(key: String, at: Instant?): Boolean {
        val result = map.compute(key) { _, v -> v?.copy(expires = at)?.let { if (it.isExpired(clock)) null else it } }
        return result != null
    }

    override suspend fun ttl(key: String): Pair<Instant?, Boolean> {
        val storeValue = map.computeIfPresent(key) { _, v -> if (v.isExpired(clock)) null else v }
            ?: return Pair(null, false)
        return Pair(storeValue.expires, true)
    }
}
