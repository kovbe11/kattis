package com.softpaw.systems.resp

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
    operator fun get(index: Int): RespValue<*> = value[index]
    operator fun iterator() = value.iterator()

    fun firstOrNull(): RespValue<*>? = value.firstOrNull()
    val size: Int get() = value.size
}

object RespNull : RespValue<Unit>() {
    const val FIRST_BYTE: Byte = '_'.code.toByte()

    override val value: Unit get() = Unit
    override val firstByte: Byte get() = FIRST_BYTE
}

data class RespBoolean(override val value: Boolean) : RespValue<Boolean>() {
    companion object {
        const val FIRST_BYTE: Byte = '#'.code.toByte()
    }

    override val firstByte: Byte get() = FIRST_BYTE
}

data class RespDouble(override val value: Double) : RespValue<Double>() {
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

