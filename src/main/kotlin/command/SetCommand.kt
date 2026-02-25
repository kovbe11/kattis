package com.softpaw.systems.command

import arrow.core.*
import arrow.core.Either.Left
import arrow.core.Either.Right
import com.github.h0tk3y.betterParse.lexer.Token
import com.softpaw.systems.command.SetCommandOptionsType.*
import com.softpaw.systems.resp.RespArray
import com.softpaw.systems.resp.RespBulkString
import com.softpaw.systems.resp.RespSimpleError
import com.softpaw.systems.resp.RespSimpleString.Companion.OK
import com.softpaw.systems.resp.RespValue
import com.softpaw.systems.store.KeyValueSetPort
import kotlinx.io.bytestring.ByteString
import kotlinx.io.bytestring.encodeToByteString

data class SetCommand(val key: ByteString, val value: RespBulkString, val options: SetCommandOptions? = null) :
    KattisCommand

private fun <T> Option<T>.or(other: Option<T>): Option<T> {
    return fold({ other }, { it.some() })
}

data class SetCommandOptions(
    val onlySetIf: Option<OnlySetIf> = none(),
    val getOldValue: Boolean = false,
    val expiry: Option<Expiry> = none()
) {
    internal val usedOptionTypes: Set<SetCommandOptionsType>
        get() {
            val result = mutableSetOf<SetCommandOptionsType>()
            onlySetIf.onSome { result.addAll(listOf(NX, XX, IFEQ, IFNE)) }
            if (getOldValue) result.add(GET)
            expiry.onSome { result.addAll(listOf(EX, KEEPTTL)) }
            return result
        }

    operator fun plus(options: SetCommandOptions): SetCommandOptions {
        return SetCommandOptions(
            onlySetIf.or(options.onlySetIf),
            getOldValue || options.getOldValue,
            expiry.or(options.expiry)
        )
    }
}

enum class SetCommandOptionsType {
    NX, XX, GET, KEEPTTL, IFEQ, IFNE, EX;

    val byteString: ByteString = name.encodeToByteString()
    val commandToken = object : Token(null, false) {
        override fun match(input: CharSequence, fromIndex: Int): Int {
            return 0
        }
    }

    fun matches(other: ByteString): Boolean {
        return this.byteString.caseInsensitiveMatchLeftAlwaysUppercase(other)
    }
}

sealed interface OnlySetIf
object OnlySetIfItDoesNotExist : OnlySetIf
object OnlySetIfItExists : OnlySetIf
data class OnlySetIfEqualToValue(val value: RespBulkString) : OnlySetIf
data class OnlySetIfNotEqualToValue(val value: RespBulkString) : OnlySetIf

sealed interface Expiry
data class ExpireAfter(val seconds: Long) : Expiry
object KeepTtl : Expiry

fun List<RespBulkString>.tail(): List<RespBulkString> {
    if (this.size <= 1) return emptyList()
    return subList(1, this.size - 1)
}

object SetCommandFactory : KattisCommandFactory {
    override fun fromArgs(args: RespArray): Either<RespSimpleError, KattisCommand> {
        if (!args.isAllBulkStrings()) return Left(RespSimpleError("ERR wrong type of arguments for 'SET' command"))
        @Suppress("UNCHECKED_CAST")
        val argsList = args.value as List<RespBulkString>
        return when (argsList.size) {
            3 -> Right(SetCommand(argsList[1].value, argsList[2]))
            in 4..8 -> optionFromArgs(argsList.subList(3, argsList.size)).map {
                SetCommand(
                    argsList[1].value,
                    argsList[2],
                    it
                )
            }

            else -> Left(RespSimpleError("ERR wrong number of arguments for 'SET' command"))
        }
    }

    private fun optionFromArgs(
        argsList: List<RespBulkString>,
    ): Either<RespSimpleError, SetCommandOptions> {
        var result = SetCommandOptions()

        if (argsList.isEmpty()) return Right(result)

        var cursor = 0
        var value = argsList[cursor].value


        result = when {
            NX.matches(value) -> SetCommandOptions(onlySetIf = OnlySetIfItDoesNotExist.some())
            XX.matches(value) -> SetCommandOptions(onlySetIf = OnlySetIfItExists.some())
            GET.matches(value) -> SetCommandOptions(getOldValue = true)
            KEEPTTL.matches(value) -> return Right(SetCommandOptions(expiry = KeepTtl.some()))
            IFEQ.matches(value) -> {
                if (argsList.size == 1) return Left(RespSimpleError("ERR wrong number of arguments for 'SET' command"))
                SetCommandOptions(onlySetIf = OnlySetIfEqualToValue(argsList[cursor + 1]).some())
                    .also {
                        cursor++
                    }
            }

            IFNE.matches(value) -> {
                if (argsList.size == 1) return Left(RespSimpleError("ERR wrong number of arguments for 'SET' command"))
                SetCommandOptions(onlySetIf = OnlySetIfNotEqualToValue(argsList[cursor + 1]).some())
                    .also {
                        cursor++
                    }
            }

            EX.matches(value) -> {
                if (argsList.size == 1) return Left(RespSimpleError("ERR wrong number of arguments for 'SET' command"))
                return argsList[1].asInteger().flatMap { Right(SetCommandOptions(expiry = ExpireAfter(it).some())) }
            }

            else -> return Left(RespSimpleError("ERR wrong arguments for 'SET' command"))
        }
        if(argsList.size <= ++cursor) return Right(result)
        value = argsList[cursor].value
        result = when {
            GET.matches(value) && !result.getOldValue -> result.copy(getOldValue = true)
            KEEPTTL.matches(value) -> return Right(result.copy(expiry = KeepTtl.some()))
            EX.matches(value) -> {
                if (argsList.size != cursor + 2) return Left(RespSimpleError("ERR wrong number of arguments for 'SET' command"))
                return argsList[cursor + 1].asInteger()
                    .flatMap { Right(result.copy(expiry = ExpireAfter(it).some())) }
            }

            else -> return Left(RespSimpleError("ERR wrong arguments for 'SET' command"))
        }
        if(argsList.size <= ++cursor) return Right(result)
        value = argsList[cursor].value

        when {
            KEEPTTL.matches(value) -> return Right(result.copy(expiry = KeepTtl.some()))
            EX.matches(value) -> {
                if (argsList.size != cursor + 2) return Left(RespSimpleError("ERR wrong number of arguments for 'SET' command"))
                return argsList[cursor + 1].asInteger()
                    .flatMap { Right(result.copy(expiry = ExpireAfter(it).some())) }
            }

            else -> return Left(RespSimpleError("ERR wrong arguments for 'SET' command"))
        }
    }

//    private fun optionFromSingleArg(
//        opt: RespBulkString,
//        usedOptionTypes: Set<SetCommandOptionsType> = setOf()
//    ): Either<RespSimpleError, SetCommandOptions> {
//        val value = opt.value
//        return when {
//            NX.matches(value) && !usedOptionTypes.contains(NX) -> Right(SetCommandOptions(onlySetIf = OnlySetIfItDoesNotExist.some()))
//            XX.matches(value) && !usedOptionTypes.contains(XX) -> Right(SetCommandOptions(onlySetIf = OnlySetIfItExists.some()))
//            GET.matches(value) && !usedOptionTypes.contains(GET) -> Right(SetCommandOptions(getOldValue = true))
//            KEEPTTL.matches(value) && !usedOptionTypes.contains(KEEPTTL) -> Right(SetCommandOptions(expiry = KeepTtl.some()))
//
//            else -> Left(RespSimpleError("ERR invalid option combination"))
//        }
//    }

//    private fun optionFromDoubleArg(
//        opt1: RespBulkString,
//        opt2: RespBulkString
//    ): Either<RespSimpleError, SetCommandOptions> {
//        val value = opt1.value
//        return when {
//            NX.matches(value) -> optionFromSingleArg(opt2, setOf(NX, XX))
//                .flatMap { Right(it.copy(onlySetIf = OnlySetIfItDoesNotExist.some())) }
//
//            XX.matches(value) -> optionFromSingleArg(opt2, setOf(NX, XX))
//                .flatMap { Right(it.copy(onlySetIf = OnlySetIfItExists.some())) }
//
//            GET.matches(value) -> optionFromSingleArg(opt2, setOf(GET))
//                .flatMap { Right(it.copy(getOldValue = true)) }
//
//            KEEPTTL.matches(value) -> optionFromSingleArg(opt2, setOf(KEEPTTL))
//                .flatMap { Right(it.copy(expiry = KeepTtl.some())) }
//
//            IFEQ.matches(value) -> Right(SetCommandOptions(onlySetIf = OnlySetIfEqualToValue(opt2).some()))
//            IFNE.matches(value) -> Right(SetCommandOptions(onlySetIf = OnlySetIfNotEqualToValue(opt2).some()))
//            EX.matches(value) -> opt2.asInteger()
//                .flatMap { Right(SetCommandOptions(expiry = ExpireAfter(it).some())) }
//
//            else -> Left(RespSimpleError("ERR invalid option combination"))
//        }
//    }

}

class SetCommandHandler(val store: KeyValueSetPort) : KattisCommandHandler<SetCommand> {
    override suspend fun handle(command: SetCommand): Either<RespSimpleError, RespValue<*>> {
        store.set(command.key, command.value)
        return Right(OK)
    }
}