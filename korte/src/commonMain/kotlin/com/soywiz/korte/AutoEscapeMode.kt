package com.soywiz.korte

import com.soywiz.korte.internal.*

class AutoEscapeMode(val transform: (String) -> String) {
    companion object {
        val HTML = AutoEscapeMode { it.htmlspecialchars() }
        val RAW = AutoEscapeMode { it }
    }
}
