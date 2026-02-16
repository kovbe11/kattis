package com.softpaw.systems.command

import com.softpaw.systems.resp.*
import com.softpaw.systems.store.KeyValueStore
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.time.Instant

class KattisCommandDispatcherTest : FunSpec({

    val mockStore = object : KeyValueStore {
        override suspend fun get(key: String): RespValue<*>? = null
        override suspend fun set(key: String, value: RespValue<*>) {}
        override suspend fun delete(key: String): Boolean = false
        override suspend fun exists(key: String): Boolean = false
        override suspend fun clear() {}
        override suspend fun expire(key: String, at: Instant?): Boolean = false
        override suspend fun ttl(key: String): Pair<Instant?, Boolean> = Pair(null, false)
    }
    val dispatcher = KattisCommandDispatcher(mockStore)

    test("dispatch PING command returns PONG") {
        val result = dispatcher.execute(PingCommand)

        result.isRight() shouldBe true
        result.getOrNull() shouldBe RespSimpleString("PONG")
    }

    test("dispatch PING with message returns echo of message") {
        val message = RespBulkString("hello")
        val command = EchoCommand(message)

        val result = dispatcher.execute(command)

        result.isRight() shouldBe true
        result.getOrNull() shouldBe RespBulkString("hello")
    }

    test("dispatch ECHO command returns the message") {
        val command = EchoCommand(RespBulkString("test message"))

        val result = dispatcher.execute(command)

        result.isRight() shouldBe true
        result.getOrNull() shouldBe RespBulkString("test message")
    }

    test("dispatch SET command returns OK") {
        val command = SetCommand("key", RespBulkString("value"))

        val result = dispatcher.execute(command)

        result.isRight() shouldBe true
        result.getOrNull() shouldBe RespSimpleString("OK")
    }

    test("dispatch GET command returns null") {
        val command = GetCommand("key")

        val result = dispatcher.execute(command)

        result.isRight() shouldBe true
        result.getOrNull() shouldBe RespNull
    }

    test("dispatch DEL command returns count of deleted keys") {
        val command = DelCommand(listOf(RespBulkString("key1"), RespBulkString("key2")))

        val result = dispatcher.execute(command)

        result.isRight() shouldBe true
        result.getOrNull() shouldBe RespInteger(0)
    }

    test("dispatch EXISTS command returns count of existing keys") {
        val command = ExistsCommand(listOf(RespBulkString("key1"), RespBulkString("key2")))

        val result = dispatcher.execute(command)

        result.isRight() shouldBe true
        result.getOrNull() shouldBe RespInteger(0)
    }

    test("dispatch FLUSHDB command returns OK") {
        val result = dispatcher.execute(FlushDbCommand)

        result.isRight() shouldBe true
        result.getOrNull() shouldBe RespSimpleString("OK")
    }

    test("dispatch EXPIRE command returns 0 for non-existent key") {
        val command = ExpireCommand("key", 10)

        val result = dispatcher.execute(command)

        result.isRight() shouldBe true
        result.getOrNull() shouldBe RespInteger(0)
    }

    test("dispatch TTL command returns -2 for non-existent key") {
        val command = TtlCommand("key")

        val result = dispatcher.execute(command)

        result.isRight() shouldBe true
        result.getOrNull() shouldBe RespInteger(-2)
    }

    test("dispatch PERSIST command returns 0 for non-existent key") {
        val command = PersistCommand("key")

        val result = dispatcher.execute(command)

        result.isRight() shouldBe true
        result.getOrNull() shouldBe RespInteger(0)
    }

})
