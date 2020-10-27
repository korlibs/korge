package com.soywiz.klogger

actual inline fun Console.error(vararg msg: Any?) {
	System.err.println("#" + Thread.currentThread().id + ": " + msg.joinToString(", "))
}

actual inline fun Console.log(vararg msg: Any?) {
	System.out.println("#" + Thread.currentThread().id + ": " + msg.joinToString(", "))
}

actual inline fun Console.warn(vararg msg: Any?) {
    System.out.println("#" + Thread.currentThread().id + ": " + msg.joinToString(", "))
}
