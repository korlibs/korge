package com.dragonbones.parser

import com.soywiz.korio.file.std.ResourcesVfs
import com.soywiz.korio.file.std.localCurrentDirVfs
import com.soywiz.korio.util.OS

val MyResourcesVfs = when {
    OS.isJs -> localCurrentDirVfs["src/commonTest/resources"]
    OS.isNative -> localCurrentDirVfs["../../../../../../src/commonTest/resources"]
    else -> ResourcesVfs
}
