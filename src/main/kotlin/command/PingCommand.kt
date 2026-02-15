package com.softpaw.systems.command

import arrow.core.Either
import com.softpaw.systems.resp.*


object PingCommand : KattisCommand

object PingCommandFactory : KattisCommandFactory {
    override fun fromArgs(args: RespArray): Either<RespSimpleError, KattisCommand> {
        return when (args.size) {
            1 -> Either.Right(PingCommand)
            2 if args[1] is RespBulkString -> Either.Right(EchoCommand(args[1] as RespBulkString))

            else -> Either.Left(RespSimpleError("ERR wrong number of arguments for 'PING' command"))
        }
    }
}

object PingCommandHandler : KattisCommandHandler<PingCommand> {
    override suspend fun handle(command: PingCommand): Either<RespSimpleError, RespValue<*>> {
        return Either.Right(RespSimpleString("PONG"))
    }
}
