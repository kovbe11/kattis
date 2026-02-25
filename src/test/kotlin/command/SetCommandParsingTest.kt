package com.softpaw.systems.command

import com.softpaw.systems.resp.RespArray
import com.softpaw.systems.resp.RespBulkString
import com.softpaw.systems.resp.RespSimpleError
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.io.bytestring.encodeToByteString

class SetCommandParsingTest : FunSpec({

    fun setArgs(vararg args: String): RespArray =
        RespArray(listOf(RespBulkString("SET")) + args.map { RespBulkString(it) })

    fun parseSet(vararg args: String) = KattisCommand.resolve(setArgs(*args))

    fun parseSetOk(vararg args: String): SetCommand {
        val result = parseSet(*args)
        result.isRight() shouldBe true
        return result.getOrNull() as SetCommand
    }

    fun parseSetErr(vararg args: String): RespSimpleError {
        val result = parseSet(*args)
        result.isLeft() shouldBe true
        return result.leftOrNull()!!
    }

    // =============================================
    // HAPPY PATH: basic SET key value
    // =============================================

    test("SET key value - no options") {
        val cmd = parseSetOk("mykey", "myvalue")
        cmd.key shouldBe "mykey".encodeToByteString()
        cmd.value shouldBe RespBulkString("myvalue")
    }

    // =============================================
    // HAPPY PATH: single options
    // =============================================

    test("SET key value NX") {
        val cmd = parseSetOk("mykey", "myvalue", "NX")
        cmd.options!!.onlySetIf shouldBe OnlySetIfItDoesNotExist
    }

    test("SET key value XX") {
        val cmd = parseSetOk("mykey", "myvalue", "XX")
        cmd.options!!.onlySetIf shouldBe OnlySetIfItExists
    }

    test("SET key value GET") {
        val cmd = parseSetOk("mykey", "myvalue", "GET")
        cmd.options!!.getOldValue shouldBe true
    }

    test("SET key value KEEPTTL") {
        val cmd = parseSetOk("mykey", "myvalue", "KEEPTTL")
        cmd.options!!.expiry shouldBe KeepTtl
    }

    test("SET key value EX 10") {
        val cmd = parseSetOk("mykey", "myvalue", "EX", "10")
        cmd.options!!.expiry shouldBe ExpireAfter(10)
    }

    test("SET key value IFEQ oldval") {
        val cmd = parseSetOk("mykey", "myvalue", "IFEQ", "oldval")
        cmd.options!!.onlySetIf.shouldBeInstanceOf<OnlySetIfEqualToValue>()
        (cmd.options!!.onlySetIf as OnlySetIfEqualToValue).value shouldBe RespBulkString("oldval")
    }

    test("SET key value IFNE oldval") {
        val cmd = parseSetOk("mykey", "myvalue", "IFNE", "oldval")
        cmd.options!!.onlySetIf.shouldBeInstanceOf<OnlySetIfNotEqualToValue>()
        (cmd.options!!.onlySetIf as OnlySetIfNotEqualToValue).value shouldBe RespBulkString("oldval")
    }

    // =============================================
    // HAPPY PATH: case insensitivity
    // =============================================

    test("SET key value nx (lowercase)") {
        val cmd = parseSetOk("mykey", "myvalue", "nx")
        cmd.options!!.onlySetIf shouldBe OnlySetIfItDoesNotExist
    }

    test("SET key value Nx (mixed case)") {
        val cmd = parseSetOk("mykey", "myvalue", "Nx")
        cmd.options!!.onlySetIf shouldBe OnlySetIfItDoesNotExist
    }

    test("SET key value ex 10 (lowercase)") {
        val cmd = parseSetOk("mykey", "myvalue", "ex", "10")
        cmd.options!!.expiry shouldBe ExpireAfter(10)
    }

    test("SET key value keepttl (lowercase)") {
        val cmd = parseSetOk("mykey", "myvalue", "keepttl")
        cmd.options!!.expiry shouldBe KeepTtl
    }

    // =============================================
    // HAPPY PATH: two option combinations
    // =============================================

    test("SET key value NX GET") {
        val cmd = parseSetOk("mykey", "myvalue", "NX", "GET")
        cmd.options!!.onlySetIf shouldBe OnlySetIfItDoesNotExist
        cmd.options!!.getOldValue shouldBe true
    }

    test("SET key value XX GET") {
        val cmd = parseSetOk("mykey", "myvalue", "XX", "GET")
        cmd.options!!.onlySetIf shouldBe OnlySetIfItExists
        cmd.options!!.getOldValue shouldBe true
    }

    test("SET key value NX EX 10") {
        val cmd = parseSetOk("mykey", "myvalue", "NX", "EX", "10")
        cmd.options!!.onlySetIf shouldBe OnlySetIfItDoesNotExist
        cmd.options!!.expiry shouldBe ExpireAfter(10)
    }

    test("SET key value XX EX 10") {
        val cmd = parseSetOk("mykey", "myvalue", "XX", "EX", "10")
        cmd.options!!.onlySetIf shouldBe OnlySetIfItExists
        cmd.options!!.expiry shouldBe ExpireAfter(10)
    }

    test("SET key value NX KEEPTTL") {
        val cmd = parseSetOk("mykey", "myvalue", "NX", "KEEPTTL")
        cmd.options!!.onlySetIf shouldBe OnlySetIfItDoesNotExist
        cmd.options!!.expiry shouldBe KeepTtl
    }

    test("SET key value XX KEEPTTL") {
        val cmd = parseSetOk("mykey", "myvalue", "XX", "KEEPTTL")
        cmd.options!!.onlySetIf shouldBe OnlySetIfItExists
        cmd.options!!.expiry shouldBe KeepTtl
    }

    test("SET key value GET EX 10") {
        val cmd = parseSetOk("mykey", "myvalue", "GET", "EX", "10")
        cmd.options!!.getOldValue shouldBe true
        cmd.options!!.expiry shouldBe ExpireAfter(10)
    }

    test("SET key value GET KEEPTTL") {
        val cmd = parseSetOk("mykey", "myvalue", "GET", "KEEPTTL")
        cmd.options!!.getOldValue shouldBe true
        cmd.options!!.expiry shouldBe KeepTtl
    }

    test("SET key value IFEQ oldval GET") {
        val cmd = parseSetOk("mykey", "myvalue", "IFEQ", "oldval", "GET")
        cmd.options!!.onlySetIf.shouldBeInstanceOf<OnlySetIfEqualToValue>()
        cmd.options!!.getOldValue shouldBe true
    }

    test("SET key value IFNE oldval GET") {
        val cmd = parseSetOk("mykey", "myvalue", "IFNE", "oldval", "GET")
        cmd.options!!.onlySetIf.shouldBeInstanceOf<OnlySetIfNotEqualToValue>()
        cmd.options!!.getOldValue shouldBe true
    }

    test("SET key value IFEQ oldval EX 10") {
        val cmd = parseSetOk("mykey", "myvalue", "IFEQ", "oldval", "EX", "10")
        cmd.options!!.onlySetIf.shouldBeInstanceOf<OnlySetIfEqualToValue>()
        cmd.options!!.expiry shouldBe ExpireAfter(10)
    }

    test("SET key value IFEQ oldval KEEPTTL") {
        val cmd = parseSetOk("mykey", "myvalue", "IFEQ", "oldval", "KEEPTTL")
        cmd.options!!.onlySetIf.shouldBeInstanceOf<OnlySetIfEqualToValue>()
        cmd.options!!.expiry shouldBe KeepTtl
    }

    // =============================================
    // HAPPY PATH: three option combinations
    // =============================================

    test("SET key value NX GET EX 10") {
        val cmd = parseSetOk("mykey", "myvalue", "NX", "GET", "EX", "10")
        cmd.options!!.onlySetIf shouldBe OnlySetIfItDoesNotExist
        cmd.options!!.getOldValue shouldBe true
        cmd.options!!.expiry shouldBe ExpireAfter(10)
    }

    test("SET key value XX GET EX 10") {
        val cmd = parseSetOk("mykey", "myvalue", "XX", "GET", "EX", "10")
        cmd.options!!.onlySetIf shouldBe OnlySetIfItExists
        cmd.options!!.getOldValue shouldBe true
        cmd.options!!.expiry shouldBe ExpireAfter(10)
    }

    test("SET key value NX GET KEEPTTL") {
        val cmd = parseSetOk("mykey", "myvalue", "NX", "GET", "KEEPTTL")
        cmd.options!!.onlySetIf shouldBe OnlySetIfItDoesNotExist
        cmd.options!!.getOldValue shouldBe true
        cmd.options!!.expiry shouldBe KeepTtl
    }

    test("SET key value XX GET KEEPTTL") {
        val cmd = parseSetOk("mykey", "myvalue", "XX", "GET", "KEEPTTL")
        cmd.options!!.onlySetIf shouldBe OnlySetIfItExists
        cmd.options!!.getOldValue shouldBe true
        cmd.options!!.expiry shouldBe KeepTtl
    }

    test("SET key value IFEQ oldval GET EX 10") {
        val cmd = parseSetOk("mykey", "myvalue", "IFEQ", "oldval", "GET", "EX", "10")
        cmd.options!!.onlySetIf.shouldBeInstanceOf<OnlySetIfEqualToValue>()
        cmd.options!!.getOldValue shouldBe true
        cmd.options!!.expiry shouldBe ExpireAfter(10)
    }

    test("SET key value IFEQ oldval GET KEEPTTL") {
        val cmd = parseSetOk("mykey", "myvalue", "IFEQ", "oldval", "GET", "KEEPTTL")
        cmd.options!!.onlySetIf.shouldBeInstanceOf<OnlySetIfEqualToValue>()
        cmd.options!!.getOldValue shouldBe true
        cmd.options!!.expiry shouldBe KeepTtl
    }

    test("SET key value IFNE oldval GET EX 10") {
        val cmd = parseSetOk("mykey", "myvalue", "IFNE", "oldval", "GET", "EX", "10")
        cmd.options!!.onlySetIf.shouldBeInstanceOf<OnlySetIfNotEqualToValue>()
        cmd.options!!.getOldValue shouldBe true
        cmd.options!!.expiry shouldBe ExpireAfter(10)
    }

    test("SET key value IFNE oldval GET KEEPTTL") {
        val cmd = parseSetOk("mykey", "myvalue", "IFNE", "oldval", "GET", "KEEPTTL")
        cmd.options!!.onlySetIf.shouldBeInstanceOf<OnlySetIfNotEqualToValue>()
        cmd.options.getOldValue shouldBe true
        cmd.options.expiry shouldBe KeepTtl
    }

    // =============================================
    // EDGE CASES: error conditions
    // =============================================

    test("SET with no key or value") {
        parseSetErr()
    }

    test("SET with only key") {
        parseSetErr("mykey")
    }

    test("SET key value EX without seconds") {
        parseSetErr("mykey", "myvalue", "EX")
    }

    test("SET key value EX with non-integer") {
        parseSetErr("mykey", "myvalue", "EX", "notanumber")
    }

    test("SET key value IFEQ without comparison value") {
        parseSetErr("mykey", "myvalue", "IFEQ")
    }

    test("SET key value IFNE without comparison value") {
        parseSetErr("mykey", "myvalue", "IFNE")
    }

    test("SET key value UNKNOWN_OPTION") {
        val result = parseSet("mykey", "myvalue", "UNKNOWN")
        result.isLeft() shouldBe true
    }

    test("SET key value NX UNKNOWN") {
        val result = parseSet("mykey", "myvalue", "NX", "UNKNOWN")
        result.isLeft() shouldBe true
    }

    test("SET key value NX GET UNKNOWN") {
        val result = parseSet("mykey", "myvalue", "NX", "GET", "UNKNOWN")
        result.isLeft() shouldBe true
    }

    test("SET key value NX EX without seconds") {
        val result = parseSet("mykey", "myvalue", "NX", "EX")
        result.isLeft() shouldBe true
    }

    test("SET key value NX EX notanumber") {
        parseSetErr("mykey", "myvalue", "NX", "EX", "notanumber")
    }

    test("SET key value GET EX without seconds") {
        val result = parseSet("mykey", "myvalue", "GET", "EX")
        result.isLeft() shouldBe true
    }

    test("SET key value NX GET EX without seconds") {
        val result = parseSet("mykey", "myvalue", "NX", "GET", "EX")
        result.isLeft() shouldBe true
    }

    // EX 0 and negative - should be accepted by parser, business logic validates later
    test("SET key value EX 0") {
        val cmd = parseSetOk("mykey", "myvalue", "EX", "0")
        cmd.options!!.expiry shouldBe ExpireAfter(0)
    }

    test("SET key value EX -1") {
        val cmd = parseSetOk("mykey", "myvalue", "EX", "-1")
        cmd.options!!.expiry shouldBe ExpireAfter(-1)
    }
})