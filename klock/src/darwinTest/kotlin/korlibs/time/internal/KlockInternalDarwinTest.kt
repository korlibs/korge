package korlibs.time.internal

import korlibs.time.*
import kotlinx.cinterop.*
import platform.CoreFoundation.*
import kotlin.test.*

class KlockInternalDarwinTest {
    @Test
    fun test() {
        val names = CFTimeZoneCopyKnownNames().toStrArray().filterNotNull().toList()
        assertEquals(true, "Europe/Madrid" in names)
        val EuropeMadrid = CFTimeZoneCreateWithName(null, CFStringCreateWithCString(null, "Europe/Madrid", kCFStringEncodingUTF8), true)
        //val CET = CFTimeZoneCreateWithName(null, CFStringCreateWithCString(null, "CET", kCFStringEncodingUTF8), true)
        //val CEST = CFTimeZoneCreateWithName(null, CFStringCreateWithCString(null, "CEST", kCFStringEncodingUTF8), true)

        // CEST (UTC +2)
        // CET (UTC +1)
        assertEquals(
            """
                CEST: 120
                CET: 60
            """.trimIndent(),
            """
                CEST: ${getLocalTimezoneOffsetDarwin(EuropeMadrid, DateTime(2023, Month.July, 10)).minutes.toInt()}
                CET: ${getLocalTimezoneOffsetDarwin(EuropeMadrid, DateTime(2023, Month.January, 10)).minutes.toInt()}
            """.trimIndent()
        )
    }
}

fun CFArrayRef?.toStrArray(): Array<String?> {
    val array = this

    return Array(CFArrayGetCount(array).convert()) {
        val ptr = CFArrayGetValueAtIndex(array, it.convert())
        CFStringGetCStringPtr(ptr?.reinterpret(), kCFStringEncodingUTF8)?.toKStringFromUtf8()
    }
}
