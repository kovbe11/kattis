package com.softpaw.systems.command

import arrow.core.Either
import com.softpaw.systems.resp.RespArray
import com.softpaw.systems.resp.RespSimpleError
import com.softpaw.systems.resp.RespSimpleString
import com.softpaw.systems.resp.RespValue
import com.softpaw.systems.store.KeyValueClearPort

object FlushDbCommand : KattisCommand

object FlushDbCommandFactory : KattisCommandFactory {
    override fun fromArgs(args: RespArray): Either<RespSimpleError, KattisCommand> {
        if (args.size != 1) {
            return Either.Left(RespSimpleError("ERR wrong number of arguments for 'FLUSHDB' command"))
        }
        return Either.Right(FlushDbCommand)
    }
}

class FlushDbCommandHandler(val store: KeyValueClearPort) : KattisCommandHandler<FlushDbCommand> {
    override suspend fun handle(command: FlushDbCommand): Either<RespSimpleError, RespValue<*>> {
        store.clear()
        return Either.Right(RespSimpleString("OK"))
    }
}