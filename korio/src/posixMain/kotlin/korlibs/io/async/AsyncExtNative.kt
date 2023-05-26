package korlibs.io.async

import kotlin.coroutines.*
import kotlinx.coroutines.*

@Suppress("ACTUAL_WITHOUT_EXPECT", "ACTUAL_TYPE_ALIAS_TO_CLASS_WITH_DECLARATION_SITE_VARIANCE")
actual typealias AsyncEntryPointResult = Unit

actual fun asyncEntryPoint(callback: suspend () -> Unit) = runBlocking { callback() }
actual fun asyncTestEntryPoint(callback: suspend () -> Unit) = asyncEntryPoint(callback)
