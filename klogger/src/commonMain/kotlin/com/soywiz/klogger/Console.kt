package com.soywiz.klogger

object Console

/** Registers an [error] in the console */
expect fun Console.error(vararg msg: Any?): Unit

/** Registers a [log] in the console */
expect fun Console.log(vararg msg: Any?): Unit
