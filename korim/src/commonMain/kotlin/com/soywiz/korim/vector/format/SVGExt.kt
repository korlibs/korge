package com.soywiz.korim.vector.format

import com.soywiz.korio.file.VfsFile
import com.soywiz.korio.serialization.xml.readXml

suspend fun VfsFile.readSVG() = SVG(this.readXml())
