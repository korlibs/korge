package com.soywiz.klogger

import android.util.Log

actual inline fun Console.error(vararg msg: Any?) {
	Log.e("Klogger", msg.joinToString(", "))
}

actual inline fun Console.log(vararg msg: Any?) {
	Log.i("Klogger", msg.joinToString(", "))
}
