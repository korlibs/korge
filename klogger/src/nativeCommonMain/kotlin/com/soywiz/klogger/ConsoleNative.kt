package com.soywiz.klogger

actual inline fun Console.error(vararg msg: Any?) {
	println(msg.joinToString(", "))
}

actual inline fun Console.log(vararg msg: Any?) {
	println(msg.joinToString(", "))
}
