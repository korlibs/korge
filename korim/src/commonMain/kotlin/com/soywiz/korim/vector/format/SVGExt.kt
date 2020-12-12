package com.soywiz.korim.vector.format

import com.soywiz.korio.file.*
import com.soywiz.korio.serialization.xml.*

suspend fun VfsFile.readSVG() = SVG(this.readXml())
