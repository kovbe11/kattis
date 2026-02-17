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
        private val emptyTtl = Pair(null, false)
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
        val result = map[key] ?: return null
        if (result.isExpired(clock)) {
            return getValueWithExpirationComputed(key)?.value
        }
        return result.value
    }

    override fun set(key: String, value: RespValue<*>) {
        map[key] = StoreValue(value)
    }

    override fun delete(key: String): Boolean {
        return map.remove(key) != null
    }

    override fun exists(key: String): Boolean {
        return get(key) != null
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
        var result = map[key] ?: return emptyTtl
        if (result.isExpired(clock)) {
            // result will be updated only if concurrent update happens
            result = getValueWithExpirationComputed(key) ?: return emptyTtl
        }
        return Pair(result.expires, true)
    }

    override fun proactiveExpireCleanup(numberOfKeysToClean: Int): Int {
        var removed = 0
        repeat(numberOfKeysToClean) {
            val next = expiringKeysSet.poll() ?: return removed
            val value = map[next] ?: return@repeat
            when {
                value.expires == null -> return@repeat
                value.isExpired(clock) -> {
                    val concurrentUpdateResultOrNull = getValueWithExpirationComputed(next)
                    when {
                        concurrentUpdateResultOrNull == null -> removed++
                        concurrentUpdateResultOrNull.expires != null -> expiringKeysSet.offer(next)
                    }
                }

                else -> expiringKeysSet.offer(next)
            }
        }
        return removed
    }

    // if present, the map will lock the key either way
    private fun getValueWithExpirationComputed(key: String): StoreValue? {
        return map.computeIfPresent(key) { _, v -> if (v.isExpired(clock)) null else v }
    }
}
