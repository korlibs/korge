package korlibs.bignumber

open class BigIntException(message: String) : Throwable(message)
open class BigIntInvalidFormatException(message: String) : BigIntException(message)
open class BigIntDivisionByZeroException() : BigIntException("Division by zero")
open class BigIntOverflowException(message: String) : BigIntException(message)
open class BigIntNegativeExponentException() : BigIntOverflowException("Negative exponent")
