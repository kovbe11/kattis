package com.softpaw.systems.resp

import arrow.core.Either
import arrow.core.Either.Left
import kotlinx.io.bytestring.ByteString

abstract class RespGrammar<out T>: Parser<T>{

    protected abstract val root: Parser<T>

    override fun tryParse(args: List<ByteString>, pos: Int): Either<ParseError, Parsed<T>> {
        return root.tryParseToEnd(args)
    }

    fun tryParseToEnd(input: RespArray): Either<RespSimpleError, T>{
        if(!input.isAllBulkStrings()){
            return Left(RespSimpleError("Input must be uppercase"))
        }
        @Suppress("UNCHECKED_CAST")
        val bulkStrings = input.value as List<RespBulkString>
        // todo: we probably shouldn't create a new list, and we should probably have better error messages
        return root.tryParseToEnd(bulkStrings.map{it.value}).fold(
            ifLeft = { Left(RespSimpleError("Parsing failed")) },
            ifRight = { Either.Right(it.value) }
        )
    }
}

