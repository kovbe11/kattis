package com.softpaw.systems.command

import arrow.core.Either
import com.softpaw.systems.resp.RespArray
import com.softpaw.systems.resp.RespBulkString
import com.softpaw.systems.resp.RespSimpleError
import com.softpaw.systems.resp.RespValue

data class EchoCommand(val msg: RespBulkString) : KattisCommand

object EchoCommandFactory : KattisCommandFactory {
    override fun fromArgs(args: RespArray): Either<RespSimpleError, KattisCommand> {
        return when (args.size) {
            2 if args[1] is RespBulkString -> Either.Right(EchoCommand(args[1] as RespBulkString))
            else -> Either.Left(RespSimpleError("ERR wrong number of arguments for 'ECHO' command"))
        }
    }
}

object EchoCommandHandler : KattisCommandHandler<EchoCommand> {
    override suspend fun handle(command: EchoCommand): Either<RespSimpleError, RespValue<*>> {
        return Either.Right(command.msg)
    }
}