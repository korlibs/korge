package korlibs.time

import kotlin.test.*

class TimezoneTest {
    @Test
    fun test() {
        assertEquals(
            "Fri, 24 Nov 2023 21:11:17 GMT+0530",
            DateTime.fromUnixMillis(1700840477693).toTimezone(Timezone.IST_INDIA).toStringDefault()
        )
        assertEquals(
            "Fri, 24 Nov 2023 16:41:17 GMT+0100",
            DateTime.fromUnixMillis(1700840477693).toTimezone(Timezone.CET).toStringDefault()
        )
    }
}
