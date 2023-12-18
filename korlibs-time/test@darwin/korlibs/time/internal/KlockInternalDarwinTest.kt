@file:OptIn(ExperimentalForeignApi::class)

package korlibs.time.internal

import korlibs.time.*
import korlibs.time.darwin.*
import kotlinx.cinterop.*
import platform.CoreFoundation.*
import platform.Foundation.*
import kotlin.test.*

@OptIn(ExperimentalForeignApi::class)
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

    @Test
    fun testCFAbsoluteTime() {
        assertEquals(0.0, DateTime.fromCFAbsoluteTime(0.0).cfAbsoluteTime())
        assertEquals(1000.0, DateTime.fromCFAbsoluteTime(1000.0).cfAbsoluteTime())
        assertEquals(-1000000.0, DateTime.fromCFAbsoluteTime(-1000000.0).cfAbsoluteTime())
        assertEquals("Mon, 01 Jan 2001 00:00:00 UTC", DateTime.fromCFAbsoluteTime(0.0).toStringDefault())
    }

    @Test
    fun testNSDate() {
        assertEquals("Mon, 01 Jan 2001 00:00:00 UTC", NSDate(0.0).toDateTime().toStringDefault())
        assertEquals(0L, DateTime.APPLE_REFERENCE_DATE.toNSDate().timeIntervalSinceReferenceDate.toLong())
        assertEquals(0L, DateTime(2001, Month.January, 1).toNSDate().timeIntervalSinceReferenceDate.toLong())
        assertEquals(-31622400L, DateTime(2000, Month.January, 1).toNSDate().timeIntervalSinceReferenceDate.toLong())
        assertEquals(946684800L, DateTime(2000, Month.January, 1).toNSDate().timeIntervalSince1970.toLong())
        assertEquals(0L, DateTime.EPOCH.toNSDate().timeIntervalSince1970.toLong())
    }
}

fun CFArrayRef?.toStrArray(): Array<String?> {
    val array = this

    return Array(CFArrayGetCount(array).convert()) {
        val ptr = CFArrayGetValueAtIndex(array, it.convert())
        CFStringGetCStringPtr(ptr?.reinterpret(), kCFStringEncodingUTF8)?.toKStringFromUtf8()
    }
}
