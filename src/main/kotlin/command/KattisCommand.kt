package com.softpaw.systems.command

import arrow.core.Either
import com.softpaw.systems.resp.RespArray
import com.softpaw.systems.resp.RespBulkString
import com.softpaw.systems.resp.RespSimpleError


enum class KattisCommandType {
    PING,
    ECHO,
    SET,
    GET,
    DEL,
    EXISTS;

    companion object {
        private val nameToTypeMap = entries.associateBy { it.name }

        fun fromName(name: String): KattisCommandType? = nameToTypeMap[name.uppercase()]
    }

}

interface KattisCommandFactory {
    fun fromArgs(args: RespArray): Either<RespSimpleError, KattisCommand>

    companion object {

        private val registry: Map<KattisCommandType, KattisCommandFactory> = mapOf(
            KattisCommandType.PING to PingCommandFactory,
            KattisCommandType.ECHO to EchoCommandFactory,
            KattisCommandType.SET to SetCommandFactory,
            KattisCommandType.GET to GetCommandFactory,
            KattisCommandType.DEL to DelCommandFactory,
            KattisCommandType.EXISTS to ExistsCommandFactory,
        )

        fun createCommand(commandType: KattisCommandType, args: RespArray): Either<RespSimpleError, KattisCommand> {
            val factory = registry[commandType] ?: return Either.Left(KattisCommand.unknownCommand)
            return factory.fromArgs(args)
        }
    }
}

interface KattisCommand {

    companion object {
        val unknownCommand: RespSimpleError = RespSimpleError("ERR unknown command")

        fun resolve(args: RespArray): Either<RespSimpleError, KattisCommand> {
            val commandType: KattisCommandType = when (val name = args.firstOrNull()) {
                is RespBulkString -> KattisCommandType.fromName(name.value) ?: return Either.Left(unknownCommand)
                else -> return Either.Left(unknownCommand)
            }
            return KattisCommandFactory.createCommand(commandType, args)
        }
    }
}


