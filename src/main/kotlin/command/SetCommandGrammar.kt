// src/main/kotlin/command/SetCommandWithBetterParse.kt
package com.softpaw.systems.command

import arrow.core.Option
import com.softpaw.systems.resp.ByteStringToken
import com.softpaw.systems.resp.IntegerToken
import com.softpaw.systems.resp.RespBulkString
import com.softpaw.systems.resp.RespGrammar
import com.softpaw.systems.resp.and
import com.softpaw.systems.resp.literal
import com.softpaw.systems.resp.map
import com.softpaw.systems.resp.optional
import com.softpaw.systems.resp.or
import com.softpaw.systems.resp.skip


object SetCommandGrammar : RespGrammar<SetCommand>() {
    private val VALUE = ByteStringToken
    private val SET = skip(literal("SET")) and VALUE and VALUE map { (key, value) ->
        SetCommand(key, RespBulkString(value))
    }
    private val NX = literal<OnlySetIf>("NX", OnlySetIfItDoesNotExist)
    private val XX = literal<OnlySetIf>("XX", OnlySetIfItExists)
    private val GET = literal("GET", true)
    private val KEEPTTL = literal<Expiry>("KEEPTTL", KeepTtl)

    private val IFEQ = skip(literal("IFEQ")) and VALUE map { OnlySetIfEqualToValue(RespBulkString(it)) }
    private val IFNE = skip(literal("IFNE")) and VALUE map { OnlySetIfNotEqualToValue(RespBulkString(it)) }
    private val EX = skip(literal("EX")) and IntegerToken map { ExpireAfter(it) }

    override val root =
        SET and optional(NX or XX or IFEQ or IFNE) and optional(GET) and optional(KEEPTTL or EX) map { (cmd, onlySetIf, get, expiry) ->
            cmd.copy(
                options = SetCommandOptions(
                    Option.fromNullable(onlySetIf),
                    get ?: false,
                    Option.fromNullable(expiry)
                )
            )
        }
}