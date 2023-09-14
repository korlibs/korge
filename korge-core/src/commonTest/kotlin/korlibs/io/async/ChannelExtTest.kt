package korlibs.io.async

import kotlinx.coroutines.channels.toList
import kotlin.test.Test
import kotlin.test.assertEquals

class ChannelExtTest {
	@Test
	fun test() = suspendTest {
		assertEquals(
			listOf(listOf("a", "b"), listOf("c", "d"), listOf("e")),
			listOf("a", "b", "c", "d", "e").toChannel().chunks(2).toList()
		)
	}
}
