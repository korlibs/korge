package korlibs.time

import korlibs.time.locale.*
import kotlin.test.*

class TimezoneTest {
    @Test
    fun testToTimezone() {
        assertEquals(
            "Fri, 24 Nov 2023 21:11:17 GMT+0530",
            DateTime.fromUnixMillis(1700840477693).toTimezone(Timezone.IST_INDIA).toStringDefault()
        )
        assertEquals(
            "Fri, 24 Nov 2023 16:41:17 GMT+0100",
            DateTime.fromUnixMillis(1700840477693).toTimezone(Timezone.CET).toStringDefault()
        )
    }

    @Test
    fun testTimezoneAbbrCollision() {
        assertEquals(
            listOf(Timezone.IST_INDIA, Timezone.IST_IRISH, Timezone.IST_ISRAEL),
            ExtendedTimezoneNames.getAll("IST")
        )
    }
}
