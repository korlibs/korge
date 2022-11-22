package com.soywiz.korio

import com.soywiz.korio.posix.posixReadlink
import com.soywiz.korio.posix.posixRealpath

fun getCurrentExe(): String? = posixReadlink("/proc/self/exe")
	?: posixReadlink("/proc/curproc/file")
	?: posixReadlink("/proc/self/path/a.out")

fun getCurrentExeFolder() = getCurrentExe()?.substringBeforeLast('/')

actual fun nativeCwd(): String = getCurrentExeFolder() ?: posixRealpath(".") ?: "."
