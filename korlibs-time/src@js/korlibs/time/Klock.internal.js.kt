@file:Suppress("PackageDirectoryMismatch")

package korlibs.time.internal

import korlibs.time.*

@JsName("globalThis")
private external val globalThis: dynamic

internal actual object KlockInternal {
    actual val currentTime: Double get() = (js("Date.now()").unsafeCast<Double>())

    actual val now: TimeSpan get() = TimeSpan.fromMilliseconds(globalThis.performance.now())

    actual fun localTimezoneOffsetMinutes(time: DateTime): TimeSpan {
        @Suppress("UNUSED_VARIABLE")
        val rtime = time.unixMillisDouble
        return js("-(new Date(rtime)).getTimezoneOffset()").unsafeCast<Int>().minutes
    }

    actual fun sleep(time: TimeSpan) {
        spinlock(time)
    }
}

actual interface Serializable
