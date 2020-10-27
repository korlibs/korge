package com.soywiz.klogger

object Console

fun Console.assert(cond: Boolean, msg: String): Unit {
    if (cond) throw AssertionError(msg)
}

/** Registers an [error] in the console */
expect fun Console.error(vararg msg: Any?): Unit

/** Registers a [log] in the console */
expect fun Console.log(vararg msg: Any?): Unit

/** Registers a [warn] in the console */
expect fun Console.warn(vararg msg: Any?): Unit
