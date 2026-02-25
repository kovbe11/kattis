package com.softpaw.systems.command

import kotlinx.io.bytestring.ByteString
import kotlinx.io.bytestring.indices


private val az = 'a'.code..'z'.code
private val AZ = 'A'.code..'Z'.code

fun ByteString.caseInsensitiveMatchLeftAlwaysUppercase(other: ByteString): Boolean {
    if (this == other) return true
    if (this.size != other.size) return false

    for (i in indices) {
        val thisByte = this[i]

        when (val otherByte = other[i]) {
            thisByte -> continue
            in az if otherByte - 32 == thisByte.toInt() -> continue
            else -> return false
        }
    }
    return true
}

fun ByteString.isAllUppercase(): Boolean {
    for(i in indices){
        if (this[i] !in AZ) return false
    }
    return true
}


