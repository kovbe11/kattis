package com.softpaw.systems.command

import arrow.core.Either
import com.softpaw.systems.resp.*
import com.softpaw.systems.store.KeyValueGetTtlPort
import java.time.Clock

data class TtlCommand(val key: String) : KattisCommand

object TtlCommandFactory : KattisCommandFactory {
    override fun fromArgs(args: RespArray): Either<RespSimpleError, KattisCommand> {
        return when {
            args.size != 2 -> Either.Left(RespSimpleError("ERR wrong number of arguments for 'TTL' command"))
            args[1] !is RespBulkString -> Either.Left(RespSimpleError("ERR wrong type of arguments for 'TTL' command"))
            else -> Either.Right(TtlCommand((args[1] as RespBulkString).decodeToString()))
        }
    }
}

class TtlCommandHandler(
    val store: KeyValueGetTtlPort,
    private val clock: Clock = Clock.systemUTC()
) : KattisCommandHandler<TtlCommand> {
    override suspend fun handle(command: TtlCommand): Either<RespSimpleError, RespValue<*>> {
        val (expires, exists) = store.ttl(command.key)
        val result = when {
            !exists -> -2
            expires == null -> -1
            else -> {
                val ttlSeconds = (expires.epochSecond - clock.instant().epochSecond)
                if (ttlSeconds < 0) -2 else ttlSeconds
            }
        }
        return Either.Right(RespInteger(result))
    }
}