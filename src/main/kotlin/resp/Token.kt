package com.softpaw.systems.resp

import arrow.core.Either
import arrow.core.Either.Left
import arrow.core.Either.Right
import com.softpaw.systems.command.caseInsensitiveMatchLeftAlwaysUppercase
import com.softpaw.systems.command.isAllUppercase
import kotlinx.io.bytestring.ByteString
import kotlinx.io.bytestring.decodeToString
import kotlinx.io.bytestring.encodeToByteString

interface Token<out T> : Parser<T> {
    fun match(byteString: ByteString): Boolean
}

fun literal(name: String): LiteralToken<Unit> =
    LiteralToken(name.encodeToByteString(), Unit)
fun <T> literal(name: String, result: T): LiteralToken<T> =
    LiteralToken(name.encodeToByteString(), result)


data class LiteralToken<T>(val name: ByteString, val result: T) : Token<T> {

    init {
        if (!name.isAllUppercase()) {
            throw IllegalArgumentException("Token name must be uppercase")
        }
    }

    override fun match(byteString: ByteString): Boolean {
        return name.caseInsensitiveMatchLeftAlwaysUppercase(byteString)
    }

    override fun tryParse(
        args: List<ByteString>,
        pos: Int
    ): Either<ParseError, Parsed<T>> {
        val tokenMatch = args.getOrNull(pos) ?: return Left(UnexpectedEof(args.last()))
        return when {
            name.caseInsensitiveMatchLeftAlwaysUppercase(tokenMatch) -> Right(ParsedValue(result, pos + 1))
            else -> Left(MismatchedToken(name, tokenMatch))
        }
    }
}

object ByteStringToken : Token<ByteString> {
    override fun match(byteString: ByteString): Boolean = true

    override fun tryParse(
        args: List<ByteString>,
        pos: Int
    ): Either<ParseError, Parsed<ByteString>> {
        val tokenMatch = args.getOrNull(pos) ?: return Left(UnexpectedEof(args.last()))
        return Right(ParsedByteString(tokenMatch, pos + 1))
    }
}

object IntegerToken : Token<Long> {
    override fun match(byteString: ByteString): Boolean = true

    override fun tryParse(
        args: List<ByteString>,
        pos: Int
    ): Either<ParseError, Parsed<Long>> {
        val tokenMatch = args.getOrNull(pos) ?: return Left(UnexpectedEof(args.last()))
        val long = tokenMatch.decodeToString().toLongOrNull() ?: return Left(WrongType(tokenMatch, Long::class))
        return Right(ParsedInteger(long, pos + 1))
    }
}