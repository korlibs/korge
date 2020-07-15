package com.soywiz.korio.util

import com.soywiz.korio.file.*
import java.net.*

val URL.basename: String get() = PathInfo(this.file).baseName