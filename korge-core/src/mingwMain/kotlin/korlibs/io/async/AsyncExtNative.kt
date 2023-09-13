package korlibs.io.async

import kotlinx.coroutines.*
import platform.posix.*

@Suppress("ACTUAL_WITHOUT_EXPECT", "ACTUAL_TYPE_ALIAS_TO_CLASS_WITH_DECLARATION_SITE_VARIANCE")
actual typealias AsyncEntryPointResult = Unit

actual fun asyncEntryPoint(callback: suspend () -> Unit) = runBlocking {
    setlocale(LC_ALL, ".UTF-8")
    callback()
}

actual fun asyncTestEntryPoint(callback: suspend () -> Unit) = asyncEntryPoint(callback)
