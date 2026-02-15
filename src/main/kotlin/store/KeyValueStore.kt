package com.softpaw.systems.store

import com.softpaw.systems.resp.RespValue
import java.util.concurrent.ConcurrentMap


interface KeyValueGetPort {
    suspend fun get(key: String): RespValue<*>?
}

interface KeyValueSetPort {
    suspend fun set(key: String, value: RespValue<*>)
}

interface KeyValueStore : KeyValueGetPort, KeyValueSetPort

class BasicKeyValueStore(val map: ConcurrentMap<String, RespValue<*>>) : KeyValueStore {
    override suspend fun get(key: String): RespValue<*>? {
        return map[key]
    }

    override suspend fun set(key: String, value: RespValue<*>) {
        map[key] = value
    }
}