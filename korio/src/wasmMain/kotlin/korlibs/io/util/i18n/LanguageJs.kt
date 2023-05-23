package korlibs.io.util.i18n

import korlibs.io.*
import korlibs.io.lang.Environment
import kotlinx.browser.*

internal actual val systemLanguageStrings: List<String> by lazy { jsRuntime.langs() }
