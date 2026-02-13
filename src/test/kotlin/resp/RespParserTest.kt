package com.softpaw.systems.resp

import com.softpaw.systems.resp.RespProtocol.deserialize
import com.softpaw.systems.resp.RespProtocol.serialize
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.ktor.utils.io.*

class RespParserTest : FunSpec({

    test("parse simple string - OK") {
        val input = "+OK\r\n"
        val channel = ByteReadChannel(input.encodeToByteArray())

        val result = deserialize(channel)

        result shouldBe RespSimpleString("OK")
    }

    test("parse simple error - ERR unknown command") {
        val input = "-ERR unknown command 'asdf'\r\n"
        val channel = ByteReadChannel(input.encodeToByteArray())

        val result = deserialize(channel)

        result shouldBe RespSimpleError("ERR unknown command 'asdf'")
    }

    test("parse integer - positive") {
        val input = ":1000\r\n"
        val channel = ByteReadChannel(input.encodeToByteArray())

        val result = deserialize(channel)

        result shouldBe RespInteger(1000)
    }

    test("parse integer - negative") {
        val input = ":-1000\r\n"
        val channel = ByteReadChannel(input.encodeToByteArray())

        val result = deserialize(channel)

        result shouldBe RespInteger(-1000)
    }

    test("parse bulk string - hello") {
        val input = "$5\r\nhello\r\n"
        val channel = ByteReadChannel(input.encodeToByteArray())

        val result = deserialize(channel)

        result shouldBe RespBulkString("hello")
    }

    test("parse bulk string - empty") {
        val input = "$0\r\n\r\n"
        val channel = ByteReadChannel(input.encodeToByteArray())

        val result = deserialize(channel)

        result shouldBe RespBulkString("")
    }

    test("parse bulk string - with newline") {
        val input = "$11\r\nhello\nworld\r\n"
        val channel = ByteReadChannel(input.encodeToByteArray())

        val result = deserialize(channel)

        result shouldBe RespBulkString("hello\nworld")
    }

    test("parse array - two bulk strings") {
        val input = "*2\r\n$5\r\nhello\r\n$5\r\nworld\r\n"
        val channel = ByteReadChannel(input.encodeToByteArray())

        val result = deserialize(channel)

        result shouldBe RespArray(
            listOf(
                RespBulkString("hello"),
                RespBulkString("world")
            )
        )
    }

    test("parse array - LLEN command") {
        val input = "*2\r\n$4\r\nLLEN\r\n$6\r\nmylist\r\n"
        val channel = ByteReadChannel(input.encodeToByteArray())

        val result = deserialize(channel)

        result shouldBe RespArray(
            listOf(
                RespBulkString("LLEN"),
                RespBulkString("mylist")
            )
        )
    }

    test("parse array - mixed types") {
        val input = "*5\r\n:1\r\n$5\r\nhello\r\n+OK\r\n-ERR\r\n#t\r\n"
        val channel = ByteReadChannel(input.encodeToByteArray())

        val result = deserialize(channel)

        result shouldBe RespArray(
            listOf(
                RespInteger(1),
                RespBulkString("hello"),
                RespSimpleString("OK"),
                RespSimpleError("ERR"),
                RespBoolean(true)
            )
        )
    }

    test("parse array - empty") {
        val input = "*0\r\n"
        val channel = ByteReadChannel(input.encodeToByteArray())

        val result = deserialize(channel)

        result shouldBe RespArray(emptyList())
    }

    test("parse bulk string - null via -1 length") {
        val input = "$-1\r\n"
        val channel = ByteReadChannel(input.encodeToByteArray())

        val result = deserialize(channel)

        result shouldBe RespNull
    }

    test("throw on bulk string length mismatch - data too short") {
        val input = "$5\r\nabc\r\n"
        val channel = ByteReadChannel(input.encodeToByteArray())

        shouldThrow<Exception> {
            deserialize(channel)
        }
    }

    test("throw on bulk string length mismatch - CRLF not at expected position") {
        val input = "$3\r\nabcdefghij\r\n"
        val channel = ByteReadChannel(input.encodeToByteArray())

        shouldThrow<RuntimeException> {
            deserialize(channel)
        }
    }

    test("parse null - RESP3") {
        val input = "_\r\n"
        val channel = ByteReadChannel(input.encodeToByteArray())

        val result = deserialize(channel)

        result shouldBe RespNull
    }

    test("parse boolean - true") {
        val input = "#t\r\n"
        val channel = ByteReadChannel(input.encodeToByteArray())

        val result = deserialize(channel)

        result shouldBe RespBoolean(true)
    }

    test("parse boolean - false") {
        val input = "#f\r\n"
        val channel = ByteReadChannel(input.encodeToByteArray())

        val result = deserialize(channel)

        result shouldBe RespBoolean(false)
    }

    test("parse double - positive") {
        val input = ",1.23\r\n"
        val channel = ByteReadChannel(input.encodeToByteArray())

        val result = deserialize(channel)

        result shouldBe RespDouble(1.23)
    }

    test("parse double - negative") {
        val input = ",-1.23\r\n"
        val channel = ByteReadChannel(input.encodeToByteArray())

        val result = deserialize(channel)

        result shouldBe RespDouble(-1.23)
    }

    test("parse double - infinity") {
        val input = ",inf\r\n"
        val channel = ByteReadChannel(input.encodeToByteArray())

        val result = deserialize(channel)

        result shouldBe RespDouble(Double.POSITIVE_INFINITY)
    }

    test("parse double - negative infinity") {
        val input = ",-inf\r\n"
        val channel = ByteReadChannel(input.encodeToByteArray())

        val result = deserialize(channel)

        result shouldBe RespDouble(Double.NEGATIVE_INFINITY)
    }

    test("parse double - NaN") {
        val input = ",nan\r\n"
        val channel = ByteReadChannel(input.encodeToByteArray())

        val result = deserialize(channel)

        result shouldBe RespDouble(Double.NaN)
    }

    test("throw on unknown type") {
        val input = "?\r\n"
        val channel = ByteReadChannel(input.encodeToByteArray())

        shouldThrow<RuntimeException> {
            deserialize(channel)
        }
    }

    test("throw on invalid integer format") {
        val input = ":abc\r\n"
        val channel = ByteReadChannel(input.encodeToByteArray())

        shouldThrow<RuntimeException> {
            deserialize(channel)
        }
    }

    test("serialize and deserialize all implemented types in array") {
        val original = RespArray(
            listOf(
                RespSimpleString("OK"),
                RespSimpleError("ERR something went wrong"),
                RespInteger(42),
                RespInteger(-100),
                RespBulkString("hello world"),
                RespBulkString(""),
                RespNull,
                RespBoolean(true),
                RespBoolean(false),
                RespArray(
                    listOf(
                        RespBulkString("nested"),
                        RespInteger(123)
                    )
                ),
                RespArray(emptyList())
            )
        )

        val serialized = serialize(original)
        val channel = ByteReadChannel(serialized.encodeToByteArray())
        val deserialized = deserialize(channel)

        deserialized shouldBe original
    }

})