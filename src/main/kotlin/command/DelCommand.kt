package com.softpaw.systems.command

import arrow.core.Either
import com.softpaw.systems.resp.*
import com.softpaw.systems.store.KeyValueDeletePort

data class DelCommand(val keys: List<RespBulkString>) : KattisCommand

object DelCommandFactory : KattisCommandFactory {
    override fun fromArgs(args: RespArray): Either<RespSimpleError, KattisCommand> {
        val tail = args.tailOrNull()
            ?: return Either.Left(RespSimpleError("ERR wrong number of arguments for 'DEL' command"))

        if (tail.any { it !is RespBulkString }) {
            return Either.Left(RespSimpleError("ERR wrong type of arguments for 'DEL' command"))
        }
        @Suppress("UNCHECKED_CAST")
        return Either.Right(DelCommand(tail as List<RespBulkString>))
    }
}

class DelCommandHandler(val store: KeyValueDeletePort) : KattisCommandHandler<DelCommand> {
    override suspend fun handle(command: DelCommand): Either<RespSimpleError, RespValue<*>> {
        val deletedCount = command.keys.count { key -> store.delete(key.decodeToString()) }
        return Either.Right(RespInteger(deletedCount.toLong()))
    }
}
