package com.softpaw.systems.command

import arrow.core.Either
import com.softpaw.systems.resp.*
import com.softpaw.systems.store.KeyValueSetExpirationPort
import kotlinx.io.bytestring.ByteString
import kotlinx.io.bytestring.decodeToString
import java.time.Clock

data class ExpireCommand(val key: ByteString, val seconds: Long) : KattisCommand

object ExpireCommandFactory : KattisCommandFactory {
    override fun fromArgs(args: RespArray): Either<RespSimpleError, KattisCommand> {
        return when {
            args.size != 3 -> Either.Left(RespSimpleError("ERR wrong number of arguments for 'EXPIRE' command"))
            args[1] !is RespBulkString -> Either.Left(RespSimpleError("ERR wrong type of arguments for 'EXPIRE' command"))
            args[2] !is RespBulkString -> Either.Left(RespSimpleError("ERR wrong type of arguments for 'EXPIRE' command"))
            else -> {
                val key = (args[1] as RespBulkString).value
                val seconds = (args[2] as RespBulkString).value.decodeToString().toLongOrNull()
                    ?: return Either.Left(RespSimpleError("ERR value is not an integer or out of range"))
                Either.Right(ExpireCommand(key, seconds))
            }
        }
    }
}

class ExpireCommandHandler(
    val store: KeyValueSetExpirationPort,
    private val clock: Clock = Clock.systemUTC()
) : KattisCommandHandler<ExpireCommand> {
    override suspend fun handle(command: ExpireCommand): Either<RespSimpleError, RespValue<*>> {
        val expiresAt = clock.instant().plusSeconds(command.seconds)
        val success = store.expire(command.key, expiresAt)
        return Either.Right(RespInteger(if (success) 1 else 0))
    }
}