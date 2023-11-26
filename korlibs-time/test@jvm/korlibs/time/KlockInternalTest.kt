package korlibs.time

import korlibs.time.internal.KlockInternalJvm
import korlibs.time.internal.TemporalKlockInternalJvm
import kotlin.test.Test
import kotlin.test.assertEquals

class KlockInternalTest {
    @Test
    fun testThatNowLocalHasTimezoneIntoAccount() {
        TemporalKlockInternalJvm(object : KlockInternalJvm {
            override val currentTime: Double get() = 1561403754469.0
            override val microClock: Double get() = TODO()
            override fun localTimezoneOffsetMinutes(time: DateTime): TimeSpan = 2.hours
        }) {
            assertEquals("Mon, 24 Jun 2019 19:15:54 UTC", DateTime.now().toStringDefault())
            assertEquals("Mon, 24 Jun 2019 21:15:54 GMT+0200", DateTime.nowLocal().toStringDefault())
        }
    }

	@Test
	fun testBug38() {
		val fixedUtcTime = 1555326870000L
		TemporalKlockInternalJvm(object : KlockInternalJvm {
			override val currentTime: Double get() = fixedUtcTime.toDouble()
			override val microClock: Double get() = TODO()
			override fun localTimezoneOffsetMinutes(time: DateTime): TimeSpan = (+9).hours
		}) {

			assertEquals("Mon, 15 Apr 2019 11:14:30 UTC", DateTime.now().toStringDefault())
			assertEquals("Mon, 15 Apr 2019 20:14:30 GMT+0900", DateTime.nowLocal().toStringDefault())
			assertEquals("1555326870000", DateTime.nowUnixMillisLong().toString())
			assertEquals("Mon, 15 Apr 2019 20:14:30 GMT+0900", DateTimeTz.nowLocal().toStringDefault())
			//println(SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").format(Date(fixedUtcTime)))
		}

		//println(DateTime.now())
		//println(DateTime.nowLocal())
		//println(DateTime.nowUnixLong())
		//println(DateTimeTz.nowLocal())
		//println(SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").format(Calendar.getInstance().time))
	}
}
