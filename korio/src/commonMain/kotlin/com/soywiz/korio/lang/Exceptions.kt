package com.soywiz.korio.lang

import com.soywiz.klogger.Console

expect open class IOException(msg: String) : Exception
expect open class EOFException(msg: String) : IOException
expect open class FileNotFoundException(msg: String) : IOException
open class MalformedInputException(msg: String) : Exception(msg) {
    constructor(index: Int, msg: String = "") : this("At index=$index $msg".trim())
}

class FileAlreadyExistsException(msg: String) : IOException(msg)

class InvalidOperationException(str: String = "Invalid Operation") : Exception(str)
class OutOfBoundsException(index: Int = -1, str: String = "Out Of Bounds") : Exception(str)
class KeyNotFoundException(str: String = "Key Not Found") : Exception(str)
class NotImplementedException(str: String = "Not Implemented") : Exception(str)
class InvalidArgumentException(str: String = "Invalid Argument") : Exception(str)
class UnreachableException(str: String = "Unreachable") : Exception(str)
class MustValidateCodeException(str: String = "Must Validate Code") : Exception(str)
class MustOverrideException(str: String = "Must Override") : Exception(str)
class DeprecatedException(str: String = "Deprecated") : Exception(str)
class UnexpectedException(str: String = "Unexpected") : Exception(str)
class CancelException(str: String = "Cancel") : Exception(str)

val deprecated: Nothing get() = throw MustValidateCodeException()
val mustValidate: Nothing get() = throw NotImplementedException()
val noImpl: Nothing get() = throw NotImplementedException()
val invalidOp: Nothing get() = throw InvalidOperationException()
val invalidArg: Nothing get() = throw InvalidArgumentException()
val unreachable: Nothing get() = throw UnreachableException()

fun deprecated(msg: String): Nothing = throw DeprecatedException(msg)
fun mustValidate(msg: String): Nothing = throw MustValidateCodeException(msg)
fun noImpl(msg: String): Nothing = throw NotImplementedException(msg)
fun invalidOp(msg: String): Nothing = throw InvalidOperationException(msg)
fun invalidArg(msg: String): Nothing = throw InvalidArgumentException(msg)
fun unsupported(msg: String): Nothing = throw UnsupportedOperationException(msg)
fun unsupported(): Nothing = throw UnsupportedOperationException("unsupported")
fun invalidArgument(msg: String): Nothing = throw InvalidArgumentException(msg)
fun unexpected(msg: String): Nothing = throw UnexpectedException(msg)
fun malformedInput(msg: String): Nothing = throw MalformedInputException(msg)

inline fun <R> runIgnoringExceptions(show: Boolean = false, action: () -> R): R? = try {
	action()
} catch (e: Throwable) {
	if (show) e.printStackTrace()
	null
}

fun Throwable.printStackTraceWithExtraMessage(msg: String) {
    Console.error(msg)
    Console.error(stackTraceToString())
}

fun printStackTrace(msg: String = "printStackTrace") {
    Console.error(Exception(msg).stackTraceToString())
}

expect fun enterDebugger(): Unit
