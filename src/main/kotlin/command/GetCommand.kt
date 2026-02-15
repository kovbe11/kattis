package com.softpaw.systems.command

import arrow.core.Either
import com.softpaw.systems.resp.*
import com.softpaw.systems.store.KeyValueGetPort

data class GetCommand(val key: String) : KattisCommand

object GetCommandFactory : KattisCommandFactory {
    override fun fromArgs(args: RespArray): Either<RespSimpleError, KattisCommand> {
        return when {
            args.size == 2 && args[1] is RespBulkString ->
                Either.Right(GetCommand((args[1] as RespBulkString).value))

            else -> Either.Left(RespSimpleError("ERR wrong number of arguments for 'GET' command"))
        }
    }
}

class GetCommandHandler(val store: KeyValueGetPort) : KattisCommandHandler<GetCommand> {
    override suspend fun handle(command: GetCommand): Either<RespSimpleError, RespValue<*>> {
        val value = store.get(command.key) ?: return Either.Right(RespNull)
        return when (value) {
            is RespBulkString -> Either.Right(value)
            is RespInteger -> Either.Right(RespBulkString(value.value.toString()))
            else -> Either.Left(RespSimpleError("ERR unsupported operation"))
        }
    }
}