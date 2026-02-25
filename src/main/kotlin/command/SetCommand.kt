package com.softpaw.systems.command

import arrow.core.Either
import arrow.core.Either.Right
import com.softpaw.systems.command.SetCommandOptions.Companion.nullObj
import com.softpaw.systems.resp.*
import com.softpaw.systems.resp.RespSimpleString.Companion.OK
import com.softpaw.systems.store.KeyValueSetPort
import kotlinx.io.bytestring.ByteString

data class SetCommand(
    val key: ByteString,
    val value: RespBulkString,
    val options: SetCommandOptions = nullObj
) : KattisCommand

data class SetCommandOptions(
    val onlySetIf: OnlySetIf?,
    val getOldValue: Boolean = false,
    val expiry: Expiry?
) {
    companion object {
        val nullObj = SetCommandOptions(null, false, null)
    }
}

object SetCommandFactory : KattisCommandFactory {

    override fun fromArgs(args: RespArray): Either<RespSimpleError, KattisCommand> {
        return SetCommandGrammar.tryParseToEnd(args)
    }

}

class SetCommandHandler(val store: KeyValueSetPort) : KattisCommandHandler<SetCommand> {
    override suspend fun handle(command: SetCommand): Either<RespSimpleError, RespValue<*>> {
        store.set(command.key, command.value)
        return Right(OK)
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


object SetCommandGrammar : RespGrammar<SetCommand>() {
    private val VALUE = ByteStringToken
    private val SET = skip(literal("SET")) and VALUE and VALUE
    private val NX = literal<OnlySetIf>("NX", OnlySetIfItDoesNotExist)
    private val XX = literal<OnlySetIf>("XX", OnlySetIfItExists)
    private val GET = literal("GET", true)
    private val KEEPTTL = literal<Expiry>("KEEPTTL", KeepTtl)

    private val IFEQ = skip(literal("IFEQ")) and VALUE map { OnlySetIfEqualToValue(RespBulkString(it)) }
    private val IFNE = skip(literal("IFNE")) and VALUE map { OnlySetIfNotEqualToValue(RespBulkString(it)) }
    private val EX = skip(literal("EX")) and IntegerToken map { ExpireAfter(it) }

    private val OPTIONS =
        optional(NX or XX or IFEQ or IFNE) and
                optional(GET) and
                optional(KEEPTTL or EX) map { (onlySetIf, get, expiry) ->
            if (onlySetIf == null && get == null && expiry == null) return@map nullObj
            SetCommandOptions(onlySetIf, get ?: false, expiry)
        }

    override val root =
        SET and OPTIONS map { (key, value, options) ->
            SetCommand(key, RespBulkString(value), options)
        }
}