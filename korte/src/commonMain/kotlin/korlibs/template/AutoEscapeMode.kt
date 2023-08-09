package korlibs.template

import korlibs.template.internal.htmlspecialchars

class AutoEscapeMode(val transform: (String) -> String) {
    companion object {
        val HTML = AutoEscapeMode { it.htmlspecialchars() }
        val RAW = AutoEscapeMode { it }
    }
}
