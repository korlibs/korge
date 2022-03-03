package com.soywiz.korio

import com.soywiz.korio.posix.*
import kotlinx.cinterop.*
import platform.posix.*

fun getCurrentExe(): String? = posixReadlink("/proc/self/exe")
	?: posixReadlink("/proc/curproc/file")
	?: posixReadlink("/proc/self/path/a.out")

fun getCurrentExeFolder() = getCurrentExe()?.substringBeforeLast('/')

actual fun nativeCwd(): String = getCurrentExeFolder() ?: posixRealpath(".") ?: "."
