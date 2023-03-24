package korlibs.logger.internal

import kotlinx.cinterop.*

internal actual val miniEnvironmentVariables: Map<String, String> by lazy {
    autoreleasepool { platform.Foundation.NSProcessInfo.processInfo.environment.map { it.key.toString() to it.value.toString() }.toMap() }
}