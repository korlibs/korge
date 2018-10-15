package com.soywiz.korge

import com.soywiz.korio.file.std.*
import com.soywiz.korio.util.*

val TestAssertVfs = when {
	OS.isNative -> ResourcesVfs
	OS.isJs -> ResourcesVfs
	else -> localCurrentDirVfs["testresources"]
}

