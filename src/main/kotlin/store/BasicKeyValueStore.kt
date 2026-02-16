package com.softpaw.systems.store

import com.softpaw.systems.resp.RespValue
import java.time.Clock
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.ConcurrentMap

class BasicKeyValueStore(
    val map: ConcurrentMap<String, StoreValue>,
    private val clock: Clock = Clock.systemUTC()
) : KeyValueStore {

    // best effort to track expiring keys for proactive cleanup, may contain keys that are not actually expiring or have already been removed
    private val expiringKeysSet = ConcurrentLinkedQueue<String>()

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

    override fun get(key: String): RespValue<*>? {
        return map.computeIfPresent(key) { _, v -> if (v.isExpired(clock)) null else v }?.value
    }

    override fun set(key: String, value: RespValue<*>) {
        map[key] = StoreValue(value)
    }

    override fun delete(key: String): Boolean {
        return map.remove(key) != null
    }

    override fun exists(key: String): Boolean {
        return map.containsKey(key)
    }

    override fun clear() {
        map.clear()
        expiringKeysSet.clear()
    }

    override fun expire(key: String, at: Instant?): Boolean {
        val result = map.compute(key) { _, v -> v?.copy(expires = at)?.let { if (it.isExpired(clock)) null else it } }
        if (result != null && at != null) {
            expiringKeysSet.offer(key)
        }
        return result != null
    }

    override fun ttl(key: String): Pair<Instant?, Boolean> {
        val storeValue = map.computeIfPresent(key) { _, v -> if (v.isExpired(clock)) null else v }
            ?: return Pair(null, false)
        return Pair(storeValue.expires, true)
    }

    override fun proactiveExpireCleanup(numberOfKeysToClean: Int): Int {
        var removed = 0
        repeat(numberOfKeysToClean) {
            val next = expiringKeysSet.poll() ?: return removed
            map.computeIfPresent(next) { _, v ->
                if (v.isExpired(clock)) {
                    removed++
                    null
                } else v
            }?.let {
                expiringKeysSet.offer(next)
            }
        }
        return removed
    }
}
