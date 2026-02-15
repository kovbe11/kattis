package com.softpaw.systems.command

import arrow.core.Either
import com.softpaw.systems.resp.*
import com.softpaw.systems.store.KeyExistsPort

data class ExistsCommand(val keys: List<RespBulkString>) : KattisCommand

object ExistsCommandFactory : KattisCommandFactory {
    override fun fromArgs(args: RespArray): Either<RespSimpleError, KattisCommand> {
        val tail = args.tailOrNull()
            ?: return Either.Left(RespSimpleError("ERR wrong number of arguments for 'EXISTS' command"))

        if (tail.any { it !is RespBulkString }) {
            return Either.Left(RespSimpleError("ERR wrong type of arguments for 'EXISTS' command"))
        }
        @Suppress("UNCHECKED_CAST")
        return Either.Right(ExistsCommand(tail as List<RespBulkString>))
    }
}

class ExistsCommandHandler(val store: KeyExistsPort) : KattisCommandHandler<ExistsCommand> {
    override suspend fun handle(command: ExistsCommand): Either<RespSimpleError, RespValue<*>> {
        val existsCount = command.keys.count { key -> store.exists(key.value) }
        return Either.Right(RespInteger(existsCount.toLong()))
    }
}
