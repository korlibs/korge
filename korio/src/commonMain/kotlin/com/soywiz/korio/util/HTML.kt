package com.soywiz.korio.util

import com.soywiz.korio.serialization.xml.Xml

fun String.htmlspecialchars(): String = Xml.Entities.encode(this)
