package com.softpaw.systems.command

import arrow.core.Either
import com.softpaw.systems.resp.RespSimpleError
import com.softpaw.systems.resp.RespValue
import com.softpaw.systems.store.KeyValueStore


interface KattisCommandHandler<Cmd : KattisCommand> {
    suspend fun handle(command: Cmd): Either<RespSimpleError, RespValue<*>>
}

class KattisCommandDispatcher(keyValueStore: KeyValueStore) {

    private fun handlerMissing(command: KattisCommand): Either.Left<RespSimpleError> =
        Either.Left(RespSimpleError("ERR handler missing for command: ${command::class.simpleName}"))

    private val handlers = mapOf(
        PingCommand::class to PingCommandHandler,
        EchoCommand::class to EchoCommandHandler,
        SetCommand::class to SetCommandHandler(keyValueStore),
        GetCommand::class to GetCommandHandler(keyValueStore),
        DelCommand::class to DelCommandHandler(keyValueStore),
        ExistsCommand::class to ExistsCommandHandler(keyValueStore)
    )

    suspend fun execute(command: KattisCommand): Either<RespSimpleError, RespValue<*>> {
        @Suppress("UNCHECKED_CAST")
        val handler = handlers[command::class] as? KattisCommandHandler<KattisCommand> ?: return handlerMissing(command)

        return handler.handle(command)
    }
}