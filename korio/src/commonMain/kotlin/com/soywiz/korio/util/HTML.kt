package com.soywiz.korio.util

import com.soywiz.korio.serialization.xml.*

fun String.htmlspecialchars() = Xml.Entities.encode(this)
