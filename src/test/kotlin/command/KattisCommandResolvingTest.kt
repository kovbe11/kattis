package com.softpaw.systems.command

import com.softpaw.systems.resp.RespArray
import com.softpaw.systems.resp.RespBulkString
import com.softpaw.systems.resp.RespSimpleError
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.io.bytestring.encodeToByteString

class KattisCommandResolvingTest : FunSpec({

    test("resolve PING command from RESP array") {
        val respArray = RespArray(listOf(RespBulkString("PING")))
        val command = KattisCommand.resolve(respArray)

        command.isRight() shouldBe true
        command.getOrNull() shouldBe PingCommand
    }

    test("resolve PING with message from RESP array") {
        val respArray = RespArray(listOf(RespBulkString("PING"), RespBulkString("hello")))
        val command = KattisCommand.resolve(respArray)

        command.isRight() shouldBe true
        command.getOrNull() shouldBe EchoCommand(RespBulkString("hello"))
    }

    test("resolve ECHO command from RESP array") {
        val respArray = RespArray(listOf(RespBulkString("ECHO"), RespBulkString("hello world")))
        val command = KattisCommand.resolve(respArray)

        command.isRight() shouldBe true
        command.getOrNull() shouldBe EchoCommand(RespBulkString("hello world"))
    }

    test("resolve unknown command returns error") {
        val respArray = RespArray(listOf(RespBulkString("UNKNOWN")))
        val command = KattisCommand.resolve(respArray)

        command.isLeft() shouldBe true
        command.leftOrNull() shouldBe RespSimpleError("ERR unknown command")
    }

    test("case insensitive command resolution - lowercase") {
        val respArray = RespArray(listOf(RespBulkString("ping")))
        val command = KattisCommand.resolve(respArray)

        command.isRight() shouldBe true
        command.getOrNull() shouldBe PingCommand
    }

    test("case insensitive command resolution - mixed case") {
        val respArray = RespArray(listOf(RespBulkString("EcHo"), RespBulkString("test")))
        val command = KattisCommand.resolve(respArray)

        command.isRight() shouldBe true
        command.getOrNull() shouldBe EchoCommand(RespBulkString("test"))
    }

    test("resolve empty array returns error") {
        val respArray = RespArray(emptyList())
        val command = KattisCommand.resolve(respArray)

        command.isLeft() shouldBe true
        command.leftOrNull() shouldBe RespSimpleError("ERR unknown command")
    }

    test("PING with too many arguments returns error") {
        val respArray = RespArray(listOf(RespBulkString("PING"), RespBulkString("arg1"), RespBulkString("arg2")))
        val command = KattisCommand.resolve(respArray)

        command.isLeft() shouldBe true
        command.leftOrNull() shouldBe RespSimpleError("ERR wrong number of arguments for 'PING' command")
    }

    test("ECHO with no arguments returns error") {
        val respArray = RespArray(listOf(RespBulkString("ECHO")))
        val command = KattisCommand.resolve(respArray)

        command.isLeft() shouldBe true
        command.leftOrNull() shouldBe RespSimpleError("ERR wrong number of arguments for 'ECHO' command")
    }

    test("ECHO with too many arguments returns error") {
        val respArray = RespArray(listOf(RespBulkString("ECHO"), RespBulkString("arg1"), RespBulkString("arg2")))
        val command = KattisCommand.resolve(respArray)

        command.isLeft() shouldBe true
        command.leftOrNull() shouldBe RespSimpleError("ERR wrong number of arguments for 'ECHO' command")
    }

    test("resolve SET command from RESP array") {
        val respArray = RespArray(listOf(RespBulkString("SET"), RespBulkString("key"), RespBulkString("value")))
        val command = KattisCommand.resolve(respArray)

        command.isRight() shouldBe true
    }

    test("SET with wrong number of arguments - too few") {
        val respArray = RespArray(listOf(RespBulkString("SET"), RespBulkString("key")))
        val command = KattisCommand.resolve(respArray)

        command.isLeft() shouldBe true
        command.leftOrNull() shouldBe RespSimpleError("ERR wrong number of arguments for 'SET' command")
    }

    test("SET with wrong number of arguments - too many") {
        val respArray = RespArray(
            listOf(
                RespBulkString("SET"),
                RespBulkString("key"),
                RespBulkString("value"),
                RespBulkString("extra")
            )
        )
        val command = KattisCommand.resolve(respArray)

        command.isLeft() shouldBe true
        command.leftOrNull() shouldBe RespSimpleError("ERR wrong number of arguments for 'SET' command")
    }

    test("resolve GET command from RESP array") {
        val respArray = RespArray(listOf(RespBulkString("GET"), RespBulkString("key")))
        val command = KattisCommand.resolve(respArray)

        command.isRight() shouldBe true
    }

    test("GET with no arguments returns error") {
        val respArray = RespArray(listOf(RespBulkString("GET")))
        val command = KattisCommand.resolve(respArray)

        command.isLeft() shouldBe true
        command.leftOrNull() shouldBe RespSimpleError("ERR wrong number of arguments for 'GET' command")
    }

    test("GET with too many arguments returns error") {
        val respArray = RespArray(listOf(RespBulkString("GET"), RespBulkString("key1"), RespBulkString("key2")))
        val command = KattisCommand.resolve(respArray)

        command.isLeft() shouldBe true
        command.leftOrNull() shouldBe RespSimpleError("ERR wrong number of arguments for 'GET' command")
    }

    test("resolve DEL command with single key") {
        val respArray = RespArray(listOf(RespBulkString("DEL"), RespBulkString("key")))
        val command = KattisCommand.resolve(respArray)

        command.isRight() shouldBe true
        command.getOrNull() shouldBe DelCommand(listOf(RespBulkString("key")))
    }

    test("resolve DEL command with multiple keys") {
        val respArray = RespArray(
            listOf(
                RespBulkString("DEL"),
                RespBulkString("key1"),
                RespBulkString("key2"),
                RespBulkString("key3")
            )
        )
        val command = KattisCommand.resolve(respArray)

        command.isRight() shouldBe true
        command.getOrNull() shouldBe DelCommand(
            listOf(
                RespBulkString("key1"),
                RespBulkString("key2"),
                RespBulkString("key3")
            )
        )
    }

    test("DEL with no arguments returns error") {
        val respArray = RespArray(listOf(RespBulkString("DEL")))
        val command = KattisCommand.resolve(respArray)

        command.isLeft() shouldBe true
        command.leftOrNull() shouldBe RespSimpleError("ERR wrong number of arguments for 'DEL' command")
    }

    test("resolve EXISTS command with single key") {
        val respArray = RespArray(listOf(RespBulkString("EXISTS"), RespBulkString("key")))
        val command = KattisCommand.resolve(respArray)

        command.isRight() shouldBe true
        command.getOrNull() shouldBe ExistsCommand(listOf(RespBulkString("key")))
    }

    test("resolve EXISTS command with multiple keys") {
        val respArray = RespArray(
            listOf(
                RespBulkString("EXISTS"),
                RespBulkString("key1"),
                RespBulkString("key2"),
                RespBulkString("key3")
            )
        )
        val command = KattisCommand.resolve(respArray)

        command.isRight() shouldBe true
        command.getOrNull() shouldBe ExistsCommand(
            listOf(
                RespBulkString("key1"),
                RespBulkString("key2"),
                RespBulkString("key3")
            )
        )
    }

    test("EXISTS with no arguments returns error") {
        val respArray = RespArray(listOf(RespBulkString("EXISTS")))
        val command = KattisCommand.resolve(respArray)

        command.isLeft() shouldBe true
        command.leftOrNull() shouldBe RespSimpleError("ERR wrong number of arguments for 'EXISTS' command")
    }

    test("resolve FLUSHDB command from RESP array") {
        val respArray = RespArray(listOf(RespBulkString("FLUSHDB")))
        val command = KattisCommand.resolve(respArray)

        command.isRight() shouldBe true
        command.getOrNull() shouldBe FlushDbCommand
    }

    test("FLUSHDB with arguments returns error") {
        val respArray = RespArray(listOf(RespBulkString("FLUSHDB"), RespBulkString("extra")))
        val command = KattisCommand.resolve(respArray)

        command.isLeft() shouldBe true
        command.leftOrNull() shouldBe RespSimpleError("ERR wrong number of arguments for 'FLUSHDB' command")
    }

    test("case insensitive FLUSHDB command resolution") {
        val respArray = RespArray(listOf(RespBulkString("flushdb")))
        val command = KattisCommand.resolve(respArray)

        command.isRight() shouldBe true
        command.getOrNull() shouldBe FlushDbCommand
    }

    test("resolve EXPIRE command from RESP array") {
        val respArray = RespArray(listOf(RespBulkString("EXPIRE"), RespBulkString("key"), RespBulkString("10")))
        val command = KattisCommand.resolve(respArray)

        command.isRight() shouldBe true
        command.getOrNull() shouldBe ExpireCommand("key".encodeToByteString(), 10)
    }

    test("EXPIRE with wrong number of arguments returns error") {
        val respArray = RespArray(listOf(RespBulkString("EXPIRE"), RespBulkString("key")))
        val command = KattisCommand.resolve(respArray)

        command.isLeft() shouldBe true
        command.leftOrNull() shouldBe RespSimpleError("ERR wrong number of arguments for 'EXPIRE' command")
    }

    test("EXPIRE with invalid seconds returns error") {
        val respArray = RespArray(listOf(RespBulkString("EXPIRE"), RespBulkString("key"), RespBulkString("abc")))
        val command = KattisCommand.resolve(respArray)

        command.isLeft() shouldBe true
        command.leftOrNull() shouldBe RespSimpleError("ERR value is not an integer or out of range")
    }

    test("resolve TTL command from RESP array") {
        val respArray = RespArray(listOf(RespBulkString("TTL"), RespBulkString("key")))
        val command = KattisCommand.resolve(respArray)

        command.isRight() shouldBe true
        command.getOrNull() shouldBe TtlCommand("key".encodeToByteString())
    }

    test("TTL with wrong number of arguments returns error") {
        val respArray = RespArray(listOf(RespBulkString("TTL")))
        val command = KattisCommand.resolve(respArray)

        command.isLeft() shouldBe true
        command.leftOrNull() shouldBe RespSimpleError("ERR wrong number of arguments for 'TTL' command")
    }

    test("resolve PERSIST command from RESP array") {
        val respArray = RespArray(listOf(RespBulkString("PERSIST"), RespBulkString("key")))
        val command = KattisCommand.resolve(respArray)

        command.isRight() shouldBe true
        command.getOrNull() shouldBe PersistCommand("key".encodeToByteString())
    }

    test("PERSIST with wrong number of arguments returns error") {
        val respArray = RespArray(listOf(RespBulkString("PERSIST")))
        val command = KattisCommand.resolve(respArray)

        command.isLeft() shouldBe true
        command.leftOrNull() shouldBe RespSimpleError("ERR wrong number of arguments for 'PERSIST' command")
    }
})
