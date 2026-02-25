package com.softpaw.systems.command

import com.softpaw.systems.resp.RespArray
import com.softpaw.systems.resp.RespBulkString
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.io.bytestring.encodeToByteString

class SetCommandGrammarTest : FunSpec({

    fun setArgs(vararg args: String): RespArray =
        RespArray(listOf(RespBulkString("SET")) + args.map { RespBulkString(it) })

    fun parseWithGrammar(vararg args: String) =
        SetCommandGrammar.tryParseToEnd(setArgs(*args))

    fun parseOk(vararg args: String): SetCommand {
        val result = parseWithGrammar(*args)
        result.isRight() shouldBe true
        return result.getOrNull()!!
    }

    fun parseErr(vararg args: String) {
        val result = parseWithGrammar(*args)
        result.isLeft() shouldBe true
    }

    // =============================================
    // HAPPY PATH
    // =============================================

    test("SET key value - no options") {
        val cmd = parseOk("mykey", "myvalue")
        cmd shouldBe SetCommand(
            key = "mykey".encodeToByteString(),
            value = RespBulkString("myvalue"),
            options = SetCommandOptions(
                onlySetIf = null,
                getOldValue = false,
                expiry = null
            )
        )
    }

    test("SET key value NX") {
        val cmd = parseOk("mykey", "myvalue", "NX")
        cmd.options!!.onlySetIf shouldBe OnlySetIfItDoesNotExist
    }

    test("SET key value GET") {
        val cmd = parseOk("mykey", "myvalue", "GET")
        cmd.options!!.getOldValue shouldBe true
    }

    test("SET key value EX 10") {
        val cmd = parseOk("mykey", "myvalue", "EX", "10")
        cmd.options!!.expiry shouldBe ExpireAfter(10)
    }

    test("SET key value KEEPTTL") {
        val cmd = parseOk("mykey", "myvalue", "KEEPTTL")
        cmd.options!!.expiry shouldBe KeepTtl
    }

    test("SET key value IFEQ oldval") {
        val cmd = parseOk("mykey", "myvalue", "IFEQ", "oldval")
        cmd.options!!.onlySetIf shouldBe OnlySetIfEqualToValue(RespBulkString("oldval"))
    }

    test("SET key value IFNE oldval") {
        val cmd = parseOk("mykey", "myvalue", "IFNE", "oldval")
        cmd.options!!.onlySetIf shouldBe OnlySetIfNotEqualToValue(RespBulkString("oldval"))
    }

    test("SET key value NX GET EX 10 (supported order)") {
        val cmd = parseOk("mykey", "myvalue", "NX", "GET", "EX", "10")
        cmd.options!!.onlySetIf shouldBe OnlySetIfItDoesNotExist
        cmd.options.getOldValue shouldBe true
        cmd.options.expiry shouldBe ExpireAfter(10)
    }

    // =============================================
    // FAILURE CASES (don’t check exact error)
    // =============================================

    test("fails: SET with no args") {
        parseErr()
    }

    test("fails: SET with only key") {
        parseErr("mykey")
    }

    test("fails: EX without seconds") {
        parseErr("mykey", "myvalue", "EX")
    }

    test("fails: EX with non-integer") {
        parseErr("mykey", "myvalue", "EX", "notanumber")
    }

    test("fails: IFEQ without comparison value") {
        parseErr("mykey", "myvalue", "IFEQ")
    }

    test("fails: unknown option") {
        parseErr("mykey", "myvalue", "UNKNOWN")
    }

    test("fails: unsupported option order (GET before NX)") {
        parseErr("mykey", "myvalue", "GET", "NX")
    }

    test("fails: trailing garbage after valid parse") {
        parseErr("mykey", "myvalue", "NX", "GET", "EX", "10", "EXTRA")
    }
})