package com.softpaw.systems.resp

import arrow.core.Either
import arrow.core.Either.Left
import arrow.core.Either.Right
import arrow.core.flatMap
import arrow.core.right
import kotlinx.io.bytestring.ByteString
import kotlinx.io.bytestring.decodeToString
import java.util.*
import kotlin.reflect.KClass

interface Parser<out T> {
    fun tryParse(args: List<RespBulkString>, pos: Int): Either<ParseError, Parsed<T>>
}

fun <T> Parser<T>.tryParseToEnd(args: List<RespBulkString>): Either<ParseError, Parsed<T>> {
    return this.tryParse(args, 0).flatMap {
        if (it.nextPosition != args.size)
            Left(UnparsedRemainder(args.subList(it.nextPosition, args.size)))
        else it.right()
    }
}

class SkipParser(val parser: Parser<*>) : Parser<Unit> {
    override fun tryParse(
        args: List<RespBulkString>,
        pos: Int
    ): Either<ParseError, Parsed<Unit>> {
        return parser.tryParse(args, pos).flatMap { Right(UnitValue(it.nextPosition)) }
    }
}

fun <T> skip(parser: Parser<T>): SkipParser = SkipParser(parser)

@JvmName("skipAnd")
inline infix fun <reified T> SkipParser.and(other: Parser<T>): AndParser<T> =
    AndParser(listOf(this, other)) { (_, t) -> t as T }

@JvmName("and0")
inline infix fun <reified A, reified B> Parser<A>.and(other: Parser<B>): AndParser<Tuple2<A, B>> =
    AndParser(listOf(this, other)) { (a1, a2) -> Tuple2(a1 as A, a2 as B) }

@JvmName("and1")
inline infix fun <reified A, reified B, reified C> AndParser<Tuple2<A, B>>.and(other: Parser<C>): AndParser<Tuple3<A, B, C>> =
    AndParser(parsers + other) { (a1, a2, a3) -> Tuple3(a1 as A, a2 as B, a3 as C) }

@JvmName("and2")
inline infix fun <reified A, reified B, reified C, reified D> AndParser<Tuple3<A, B, C>>.and(other: Parser<D>): AndParser<Tuple4<A, B, C, D>> =
    AndParser(parsers + other) { (a1, a2, a3, a4) -> Tuple4(a1 as A, a2 as B, a3 as C, a4 as D) }

class AndParser<out R>(val parsers: List<Parser<*>>, val transform: (List<Any?>) -> R) : Parser<R> {
    override fun tryParse(
        args: List<RespBulkString>,
        pos: Int
    ): Either<ParseError, Parsed<R>> {
        val results = LinkedList<Any?>()
        var nextPos = pos
        parsers.forEach { parser ->
            when (parser) {
                is SkipParser -> parser.tryParse(args, nextPos)
                    .onRight { results.add(Unit); nextPos = it.nextPosition }
                    .onLeft { return Left(it) }

                is Parser<*> -> parser.tryParse(args, nextPos)
                    .onRight { results.add(it.value); nextPos = it.nextPosition }
                    .onLeft { return Left(it) }
            }
        }
        return Right(ParsedValue(transform(results), nextPos))
    }

}

infix fun <T> Parser<T>.or(other: Parser<T>): Parser<T> {
    return when {
        this is OrParser<T> -> OrParser(parsers + other)
        else -> OrParser(listOf(this, other))
    }
}

infix fun <T> OrParser<T>.or(other: Parser<T>): Parser<T> = OrParser(parsers + other)

class OrParser<T>(val parsers: List<Parser<T>>) : Parser<T> {
    override fun tryParse(args: List<RespBulkString>, pos: Int): Either<ParseError, Parsed<T>> {
        val failures = LinkedList<ParseError>()
        parsers.forEach {
            it.tryParse(args, pos)
                .onRight { result -> return Right(result) }
                .onLeft { failure -> failures.add(failure) }
        }
        return Left(AlternativesFailure(failures))
    }
}

infix fun <T, R> Parser<T>.map(transform: (T) -> R): Parser<R> = MapParser(this, transform)

class MapParser<T, R>(
    val parser: Parser<T>,
    val transform: (T) -> R
) : Parser<R> {
    override fun tryParse(
        args: List<RespBulkString>,
        pos: Int
    ): Either<ParseError, Parsed<R>> =
        parser.tryParse(args, pos)
            .flatMap { result -> Right(ParsedValue(transform(result.value), result.nextPosition)) }
}

fun <T> optional(parser: Parser<T>): Parser<T?> = OptionalParser(parser)

data class OptionalParser<T>(private val parser: Parser<T>) : Parser<T?> {
    override fun tryParse(args: List<RespBulkString>, pos: Int): Either<ParseError, Parsed<T?>> =
        parser.tryParse(args, pos)
            .fold({ NullValue(pos) }, { it })
            .right()
}

sealed interface ParseError

data class UnparsedRemainder(val remainder: List<RespBulkString>) : ParseError {
    override fun toString() = "UnparsedRemainder: ${remainder.size} arguments"
}

data class MismatchedToken(val expected: ByteString, val found: ByteString) : ParseError {
    override fun toString() =
        "MismatchedToken: expected '${expected.decodeToString()}', found '${found.decodeToString()}'"
}

data class AlternativesFailure(val failures: List<ParseError>) : ParseError

data class UnexpectedEof(val lastToken: RespBulkString) : ParseError
data class WrongType(val found: ByteString, val expectedType: KClass<*>) : ParseError


sealed class Parsed<out T> {
    abstract val value: T
    abstract val nextPosition: Int

    override fun toString(): String = "Parsed($value)"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as Parsed<*>

        if (value != other.value) return false
        if (nextPosition != other.nextPosition) return false

        return true
    }

    override fun hashCode(): Int {
        var result = value?.hashCode() ?: 0
        result = 31 * result + nextPosition
        return result
    }
}

data class NullValue(override val nextPosition: Int) : Parsed<Nothing?>() {
    override val value = null
}

data class UnitValue(override val nextPosition: Int) : Parsed<Unit>() {
    override val value: Unit = Unit
}

data class ParsedValue<T>(override val value: T, override val nextPosition: Int) : Parsed<T>()

data class ParsedByteString(override val value: ByteString, override val nextPosition: Int) : Parsed<ByteString>() {
    override fun toString(): String = "Parsed(${value.decodeToString()})"
}

data class ParsedInteger(override val value: Long, override val nextPosition: Int) : Parsed<Long>()
