package com.soywiz.kbignum

open class BigIntException(message: String) : Throwable(message)
open class BigIntInvalidFormatException(message: String) : BigIntException(message)
open class BigIntDivisionByZeroException(message: String) : BigIntException(message)
open class BigIntOverflowException(message: String) : BigIntException(message)
open class BigIntInvalidOperationException(message: String) : BigIntException(message)
