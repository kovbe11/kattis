package com.softpaw.systems.store

import com.softpaw.systems.resp.RespValue
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

interface KeyValueStore : KeyValueGetPort, KeyValueSetPort, KeyValueDeletePort, KeyExistsPort, KeyValueClearPort

class BasicKeyValueStore(val map: ConcurrentMap<String, RespValue<*>>) : KeyValueStore {
    override suspend fun get(key: String): RespValue<*>? {
        return map[key]
    }

    override suspend fun set(key: String, value: RespValue<*>) {
        map[key] = value
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
}
