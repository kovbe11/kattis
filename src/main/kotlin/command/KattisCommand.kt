package com.softpaw.systems.command

import arrow.core.Either
import com.softpaw.systems.resp.RespArray
import com.softpaw.systems.resp.RespBulkString
import com.softpaw.systems.resp.RespSimpleError
import kotlinx.io.bytestring.ByteString
import kotlinx.io.bytestring.encodeToByteString
import kotlinx.io.bytestring.indices


enum class KattisCommandType {
    PING,
    ECHO,
    SET,
    GET,
    DEL,
    EXISTS,
    FLUSHDB,
    EXPIRE,
    TTL,
    PERSIST;

    companion object {
        private val nameToTypeMap = entries.associateBy { it.name.encodeToByteString() }

        fun fromName(name: ByteString): KattisCommandType? {
            val exactMatch = nameToTypeMap[name]
            if (exactMatch != null) {
                return exactMatch
            }

            val result = nameToTypeMap.entries.firstOrNull { (k, _) ->
                if (k.size != name.size) {
                    return@firstOrNull false
                }
                for (i in k.indices) {
                    val nameByte = name[i]
                    val typeByte = k[i]

                    when (nameByte) {
                        typeByte -> continue
                        in 'a'.code.toByte()..'z'.code.toByte() if nameByte - 32 == typeByte.toInt() -> continue
                        else -> return@firstOrNull false
                    }
                }
                true
            }?.value
            return result
        }
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
            KattisCommandType.FLUSHDB to FlushDbCommandFactory,
            KattisCommandType.EXPIRE to ExpireCommandFactory,
            KattisCommandType.TTL to TtlCommandFactory,
            KattisCommandType.PERSIST to PersistCommandFactory,
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


