package com.softpaw.systems.resp

import arrow.core.Either
import arrow.core.Either.Left

abstract class RespGrammar<out T>: Parser<T>{

    protected abstract val root: Parser<T>

    override fun tryParse(args: List<RespBulkString>, pos: Int): Either<ParseError, Parsed<T>> {
        return root.tryParseToEnd(args)
    }

    fun tryParseToEnd(input: RespArray): Either<RespSimpleError, T>{
        if(!input.isAllBulkStrings()){
            return Left(RespSimpleError("Input must be uppercase"))
        }
        @Suppress("UNCHECKED_CAST")
        return root.tryParseToEnd(input.value as List<RespBulkString>).fold(
            ifLeft = { Left(RespSimpleError("Parsing failed")) }, // todo: errors!
            ifRight = { Either.Right(it.value) }
        )
    }
}

