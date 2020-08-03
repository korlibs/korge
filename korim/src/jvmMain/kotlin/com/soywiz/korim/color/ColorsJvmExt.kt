package com.soywiz.korim.color

import java.awt.*

fun RGBA.toAwt(): Color = Color(r, g, b, a)
