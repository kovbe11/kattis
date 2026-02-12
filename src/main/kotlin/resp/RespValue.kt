package com.softpaw.systems.resp

import kotlin.collections.Map
import kotlin.collections.Set

sealed class RespValue<T> {
    abstract val value: T
    abstract val firstByte: Byte
}

data class RespSimpleString(override val value: String) : RespValue<String>() {
    companion object {
        const val FIRST_BYTE: Byte = '+'.code.toByte()
    }

    override val firstByte: Byte get() = FIRST_BYTE
}

data class RespSimpleError(override val value: String) : RespValue<String>() {
    companion object {
        const val FIRST_BYTE: Byte = '-'.code.toByte()
    }

    override val firstByte: Byte get() = FIRST_BYTE
}

data class RespInteger(override val value: Long) : RespValue<Long>() {
    companion object {
        const val FIRST_BYTE: Byte = ':'.code.toByte()
    }

    override val firstByte: Byte get() = FIRST_BYTE
}

data class RespBulkString(override val value: String) : RespValue<String>() {
    companion object {
        const val FIRST_BYTE: Byte = '$'.code.toByte()
    }

    override val firstByte: Byte get() = FIRST_BYTE
}

data class RespArray(override val value: List<RespValue<*>>) : RespValue<List<RespValue<*>>>() {
    companion object {
        const val FIRST_BYTE: Byte = '*'.code.toByte()
    }

    override val firstByte: Byte get() = FIRST_BYTE
}

data class RespNull(override val value: Unit = Unit) : RespValue<Unit>() {
    companion object {
        const val FIRST_BYTE: Byte = '_'.code.toByte()
    }

    override val firstByte: Byte get() = FIRST_BYTE
}

data class RespBoolean(override val value: kotlin.Boolean) : RespValue<kotlin.Boolean>() {
    companion object {
        const val FIRST_BYTE: Byte = '#'.code.toByte()
    }

    override val firstByte: Byte get() = FIRST_BYTE
}

data class RespDouble(override val value: kotlin.Double) : RespValue<kotlin.Double>() {
    companion object {
        const val FIRST_BYTE: Byte = ','.code.toByte()
    }

    override val firstByte: Byte get() = FIRST_BYTE
}

data class RespBigNumber(override val value: String) : RespValue<String>() {
    companion object {
        const val FIRST_BYTE: Byte = '('.code.toByte()
    }

    override val firstByte: Byte get() = FIRST_BYTE
}

data class RespBulkError(override val value: String) : RespValue<String>() {
    companion object {
        const val FIRST_BYTE: Byte = '!'.code.toByte()
    }

    override val firstByte: Byte get() = FIRST_BYTE
}

data class RespVerbatimString(override val value: Pair<String, String>) : RespValue<Pair<String, String>>() {
    companion object {
        const val FIRST_BYTE: Byte = '='.code.toByte()
    }

    override val firstByte: Byte get() = FIRST_BYTE
}

data class RespMap(override val value: Map<RespValue<*>, RespValue<*>>) :
    RespValue<Map<RespValue<*>, RespValue<*>>>() {
    companion object {
        const val FIRST_BYTE: Byte = '%'.code.toByte()
    }

    override val firstByte: Byte get() = FIRST_BYTE
}

data class RespSet(override val value: Set<RespValue<*>>) :
    RespValue<Set<RespValue<*>>>() {
    companion object {
        const val FIRST_BYTE: Byte = '~'.code.toByte()
    }

    override val firstByte: Byte get() = FIRST_BYTE
}

data class RespPush(override val value: List<RespValue<*>>) : RespValue<List<RespValue<*>>>() {
    companion object {
        const val FIRST_BYTE: Byte = '>'.code.toByte()
    }

    override val firstByte: Byte get() = FIRST_BYTE
}

