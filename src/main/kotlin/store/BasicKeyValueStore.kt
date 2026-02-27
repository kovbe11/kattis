package com.softpaw.systems.store

import com.softpaw.systems.command.*
import com.softpaw.systems.resp.RespValue
import kotlinx.io.bytestring.ByteString
import java.time.Clock
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.ConcurrentMap

class BasicKeyValueStore(
    val map: ConcurrentMap<ByteString, StoreValue>,
    private val clock: Clock = Clock.systemUTC()
) : KeyValueStore {

    // best effort to track expiring keys for proactive cleanup, may contain keys that are not actually expiring or have already been removed
    private val expiringKeysSet = ConcurrentLinkedQueue<ByteString>()

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

    override fun get(key: ByteString): RespValue<*>? {
        val result = map[key] ?: return null
        if (result.isExpired(clock)) {
            return getValueWithExpirationComputed(key)?.value
        }
        return result.value
    }

    private fun setIf(onlySetIf: OnlySetIf?, oldValue: RespValue<*>? = null): Boolean {
        return when (onlySetIf) {
            null -> true
            OnlySetIfItDoesNotExist -> oldValue == null
            OnlySetIfItExists -> oldValue != null
            is OnlySetIfEqualToValue -> onlySetIf.value == oldValue // todo: this logic actually falls apart if we start saving types :/
            is OnlySetIfNotEqualToValue -> onlySetIf.value != oldValue
        }
    }

    private fun expiringAt(expiry: Expiry?, oldValue: StoreValue?): Instant? {
        return when (expiry) {
            null -> null
            KeepTtl -> oldValue?.expires
            is ExpireAfter -> clock.instant().plusSeconds(expiry.seconds)
        }
    }

    override fun set(key: ByteString, value: RespValue<*>, onlySetIf: OnlySetIf?, expiry: Expiry?) {
        setThenGet(key, value, onlySetIf, expiry)
    }

    override fun setThenGet(
        key: ByteString,
        value: RespValue<*>,
        onlySetIf: OnlySetIf?,
        expiry: Expiry?
    ): RespValue<*>? {
        var expiringNeeded = false
        map.compute(key) { _, v ->
            if (!setIf(onlySetIf, v?.value)) return@compute v
            val expiresAt = expiringAt(expiry, v)
            expiresAt?.run { expiringNeeded = true }
            (StoreValue(value, expiringAt(expiry, v)))
        }
        if (expiringNeeded) {
            expiringKeysSet.offer(key)
        }
        return map[key]?.value
    }

    override fun delete(key: ByteString): Boolean {
        return map.remove(key) != null
    }

    override fun exists(key: ByteString): Boolean {
        return get(key) != null
    }

    override fun clear() {
        map.clear()
        expiringKeysSet.clear()
    }

    override fun expire(key: ByteString, at: Instant?): Boolean {
        val result = map.compute(key) { _, v -> v?.copy(expires = at)?.let { if (it.isExpired(clock)) null else it } }
        if (result != null && at != null) {
            expiringKeysSet.offer(key)
        }
        return result != null
    }

    override fun ttl(key: ByteString): Pair<Instant?, Boolean> {
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
    private fun getValueWithExpirationComputed(key: ByteString): StoreValue? {
        return map.computeIfPresent(key) { _, v -> if (v.isExpired(clock)) null else v }
    }
}
