package com.soywiz.korio.util

val Process.isAliveJre7: Boolean
	get() = try {
		exitValue()
		false
	} catch (e: IllegalThreadStateException) {
		true
	}
