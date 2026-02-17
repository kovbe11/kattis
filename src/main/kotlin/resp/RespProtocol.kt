package com.softpaw.systems.resp

import io.ktor.utils.io.*
import kotlinx.coroutines.withTimeout
import kotlinx.io.bytestring.ByteString


object RespProtocol {

    private val registry = mapOf<Byte, suspend (ByteReadChannel) -> RespValue<*>>(
        RespSimpleString.FIRST_BYTE to ::handleSimpleString,
        RespSimpleError.FIRST_BYTE to ::handleSimpleError,
        RespInteger.FIRST_BYTE to ::handleInteger,
        RespBulkString.FIRST_BYTE to ::handleBulkString,
        RespArray.FIRST_BYTE to ::handleArray,
        RespNull.FIRST_BYTE to ::handleNull,
        RespBoolean.FIRST_BYTE to ::handleBoolean,
        RespDouble.FIRST_BYTE to ::handleDouble,
        RespBigNumber.FIRST_BYTE to ::handleBigNumber,
        RespBulkError.FIRST_BYTE to ::handleBulkError,
        RespVerbatimString.FIRST_BYTE to ::handleVerbatimString,
        RespMap.FIRST_BYTE to ::handleMap,
        RespSet.FIRST_BYTE to ::handleSet,
        RespPush.FIRST_BYTE to ::handlePush
    )

    private const val MAX_BULK_STRING_SIZE = 128L * 1024L * 1024L // 128 MiB
    private const val TIMEOUT_FOR_READ = 5000L // 5 seconds

    // this does not support inline commands. I do not comprehend why anyone came up with that idea.
    suspend fun deserialize(byteReadChannel: ByteReadChannel): RespValue<*> {
        val firstByte = byteReadChannel.readByte()
        val handler = registry[firstByte] ?: handleUnimplemented()
        return handler(byteReadChannel)
    }

    suspend fun handleSimpleString(byteReadChannel: ByteReadChannel): RespSimpleString {
        val line = byteReadChannel.readUTF8Line()
            ?: throw RuntimeException("Unexpected end of input while reading SimpleString")
        return RespSimpleString(line)
    }

    suspend fun handleSimpleError(byteReadChannel: ByteReadChannel): RespSimpleError {
        val line = byteReadChannel.readUTF8Line()
            ?: throw RuntimeException("Unexpected end of input while reading SimpleError")
        return RespSimpleError(line)
    }

    suspend fun handleInteger(byteReadChannel: ByteReadChannel): RespInteger {
        return RespInteger(readLong(byteReadChannel, "Integer"))
    }

    private suspend fun readLong(byteReadChannel: ByteReadChannel, type: String, max: Int = 21): Long {
        val line = byteReadChannel.readUTF8Line(max = max)
            ?: throw RuntimeException("Unexpected end of input while reading $type")
        return line.toLongOrNull() ?: throw RuntimeException("Invalid integer format: `$line`")
    }

    suspend fun handleBulkString(byteReadChannel: ByteReadChannel): RespValue<*> {
        val length = readLong(byteReadChannel, "BulkString length", max = 11)
        if (length < 0) return RespNull

        if (length > MAX_BULK_STRING_SIZE) {
            throw RuntimeException("Bulk string too large: $length bytes (Max: $MAX_BULK_STRING_SIZE)")
        }
        val bytes = ByteArray(length.toInt())

        // we close connection if they take too long.
        withTimeout(TIMEOUT_FOR_READ) {
            byteReadChannel.readFully(bytes, 0, length.toInt())
        }

        val cr = byteReadChannel.readByte()
        val lf = byteReadChannel.readByte()

        if (cr != '\r'.code.toByte() || lf != '\n'.code.toByte()) {
            throw RuntimeException("Bulk string expected CRLF terminator")
        }

        return RespBulkString(ByteString(bytes))
    }

    suspend fun handleArray(byteReadChannel: ByteReadChannel): RespArray {
        val length = readLong(byteReadChannel, "Array length", max = 11)
        if (length < 0) return RespArray(emptyList())

        val elements = Array(length.toInt()) { _ ->
            deserialize(byteReadChannel)
        }
        return RespArray(elements.asList())
    }

    suspend fun handleNull(byteReadChannel: ByteReadChannel): RespNull {
        val line = byteReadChannel.readUTF8Line(max = 2)
        if (line != "") throw RuntimeException("Unexpected input while reading null: $line")
        return RespNull
    }

    suspend fun handleBoolean(byteReadChannel: ByteReadChannel): RespBoolean {
        return when (val line = byteReadChannel.readUTF8Line(max = 3)) {
            "t" -> RespBoolean(true)
            "f" -> RespBoolean(false)
            else -> throw RuntimeException("Invalid boolean format: `$line`")
        }
    }

    suspend fun handleDouble(byteReadChannel: ByteReadChannel): RespDouble {
        val line = byteReadChannel.readUTF8Line(max = 128)
            ?: throw RuntimeException("Unexpected end of input while reading Double")

        val value = when (line) {
            "inf" -> Double.POSITIVE_INFINITY
            "-inf" -> Double.NEGATIVE_INFINITY
            "nan" -> Double.NaN
            else -> line.toDoubleOrNull() ?: throw RuntimeException("Invalid double format: `$line`")
        }

        return RespDouble(value)
    }

    suspend fun handleBigNumber(byteReadChannel: ByteReadChannel): RespBigNumber = handleUnimplemented()
    suspend fun handleBulkError(byteReadChannel: ByteReadChannel): RespBulkError = handleUnimplemented()
    suspend fun handleVerbatimString(byteReadChannel: ByteReadChannel): RespVerbatimString = handleUnimplemented()
    suspend fun handleMap(byteReadChannel: ByteReadChannel): RespMap = handleUnimplemented()
    suspend fun handleSet(byteReadChannel: ByteReadChannel): RespSet = handleUnimplemented()
    suspend fun handlePush(byteReadChannel: ByteReadChannel): RespPush = handleUnimplemented()

    val RespValue<*>.firstChar: Char get() = this.firstByte.toInt().toChar()

    suspend fun writeSerialized(byteWriteChannel: ByteWriteChannel, value: RespValue<*>) {
        when (value) {
            is RespSimpleString, is RespSimpleError, is RespInteger ->
                byteWriteChannel.writeStringUtf8("${value.firstChar}${value.value}\r\n")

            is RespBulkString -> {
                byteWriteChannel.writeStringUtf8("${value.firstChar}${value.size}\r\n")
                byteWriteChannel.writeFully(value.value.toByteArray())
                byteWriteChannel.writeStringUtf8("\r\n")
            }

            is RespArray -> {
                byteWriteChannel.writeStringUtf8("${value.firstChar}${value.size}\r\n")
                for (element in value.value) {
                    writeSerialized(byteWriteChannel, element)
                }

            }

            is RespNull -> byteWriteChannel.writeStringUtf8("${value.firstChar}\r\n")
            is RespBoolean ->
                byteWriteChannel.writeStringUtf8("${value.firstChar}${if (value.value) "t" else "f"}\r\n")

            else -> throw IllegalArgumentException("Unsupported RespValue type: ${value::class}")
        }
    }

    fun handleUnimplemented(): Nothing {
        throw RuntimeException("No such RESP type implemented")
    }
}


