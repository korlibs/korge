package com.soywiz.korio.util

import com.soywiz.korio.serialization.xml.Xml

fun String.htmlspecialchars() = Xml.Entities.encode(this)
