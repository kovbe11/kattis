package com.softpaw.systems.command

import arrow.core.Either
import com.softpaw.systems.resp.*
import com.softpaw.systems.store.KeyValueSetExpirationPort

data class PersistCommand(val key: String) : KattisCommand

object PersistCommandFactory : KattisCommandFactory {
    override fun fromArgs(args: RespArray): Either<RespSimpleError, KattisCommand> {
        return when {
            args.size != 2 -> Either.Left(RespSimpleError("ERR wrong number of arguments for 'PERSIST' command"))
            args[1] !is RespBulkString -> Either.Left(RespSimpleError("ERR wrong type of arguments for 'PERSIST' command"))
            else -> {
                val key = (args[1] as RespBulkString).decodeToString()
                Either.Right(PersistCommand(key))
            }
        }
    }
}

class PersistCommandHandler(val store: KeyValueSetExpirationPort) : KattisCommandHandler<PersistCommand> {
    override suspend fun handle(command: PersistCommand): Either<RespSimpleError, RespValue<*>> {
        val success = store.expire(command.key, null)
        return Either.Right(RespInteger(if (success) 1 else 0))
    }
}