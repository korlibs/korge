package korlibs.time

import java.text.SimpleDateFormat
import java.time.*
import java.time.format.*
import java.util.*
import kotlin.test.*

class JvmReferenceTest {
    fun jvmParse(
        pattern: String,
        dateString: String
    ): Long {
        val formatter = DateTimeFormatter.ofPattern(pattern, Locale.ENGLISH)
        val dateTime = LocalDateTime.parse(dateString, formatter)
        val timestampMillis = dateTime.toInstant(ZoneOffset.UTC).toEpochMilli()
        return timestampMillis
    }

    fun klockParse(
        pattern: String,
        dateString: String
    ): Long = DateFormat(pattern).parseLocal(dateString).unixMillisLong

    @Test
    fun testJvmParse() {
        assertEquals(
            1676679000000L,
            jvmParse("EEE MMM dd HH:mm:ss Z yyyy", "Sat Feb 18 00:10:00 +0000 2023")
        )
        assertEquals(
            1540124184000L,
            jvmParse("EEE, dd MMM yyyy HH:mm:ss X", "Sun, 21 Oct 2018 12:16:24 +0300")
        )
    }

    @Test
    fun testKlockParseZ() {
        assertEquals(
            1676679000000L,
            klockParse("EEE MMM dd HH:mm:ss Z yyyy", "Sat Feb 18 00:10:00 +0000 2023")
        )
    }

    @Test
    fun testKlockParseX() {
        assertEquals(
            1540124184000L,
            klockParse("EEE, dd MMM yyyy HH:mm:ss X", "Sun, 21 Oct 2018 12:16:24 +0300")
        )
    }

    @Test
    fun testParsingDateTimesWithDeciSeconds() {
        var dtmilli = 1536379689009L
        assertEquals(dtmilli, DateTime(2018, 9, 8, 4, 8, 9, 9).unixMillisLong)
        assertEquals(
            message = "Sat, 08 Sep 2018 04:08:09.9 UTC",
            expected = dtmilli,
            actual = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss.S z", Locale.ENGLISH).parse("Sat, 08 Sep 2018 04:08:09.9 UTC").time
        )
    }
}
