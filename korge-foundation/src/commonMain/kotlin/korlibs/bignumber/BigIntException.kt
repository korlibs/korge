package korlibs.bignumber

/** A generic [BigInt] exception */
open class BigIntException(message: String) : Throwable(message)
/** A [BigInt] exception thrown when an invalid String value is provided while parsing */
open class BigIntInvalidFormatException(message: String) : BigIntException(message)
/** A [BigInt] exception thrown when trying to divide by zero */
open class BigIntDivisionByZeroException() : BigIntException("Division by zero")
/** A [BigInt] exception thrown when an overflow operation occurs, like for example when trying to convert a too big [BigInt] into an [Int] */
open class BigIntOverflowException(message: String) : BigIntException(message)
/** A [BigInt] exception thrown when doing a `pow` operation with a negative exponent */
open class BigIntNegativeExponentException() : BigIntOverflowException("Negative exponent")
