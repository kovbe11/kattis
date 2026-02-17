package com.softpaw.systems.command

import arrow.core.Either
import com.softpaw.systems.resp.RespArray
import com.softpaw.systems.resp.RespBulkString
import com.softpaw.systems.resp.RespSimpleError
import com.softpaw.systems.resp.RespSimpleString.Companion.OK
import com.softpaw.systems.resp.RespValue
import com.softpaw.systems.store.KeyValueSetPort

data class SetCommand(val key: String, val value: RespBulkString) : KattisCommand

object SetCommandFactory : KattisCommandFactory {
    override fun fromArgs(args: RespArray): Either<RespSimpleError, KattisCommand> {
        return when {
            args.size == 3 && args[1] is RespBulkString && args[2] is RespBulkString -> {
                Either.Right(SetCommand((args[1] as RespBulkString).decodeToString(), args[2] as RespBulkString))
            }

            else -> Either.Left(RespSimpleError("ERR wrong number of arguments for 'SET' command"))
        }
    }
}

class SetCommandHandler(val store: KeyValueSetPort) : KattisCommandHandler<SetCommand> {
    override suspend fun handle(command: SetCommand): Either<RespSimpleError, RespValue<*>> {
        store.set(command.key, command.value)
        return Either.Right(OK)
    }
}