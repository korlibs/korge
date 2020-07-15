package com.soywiz.korio.async

import kotlinx.coroutines.channels.*
import kotlin.test.*

class ChannelExtTest {
	@Test
	fun test() = suspendTest {
		assertEquals(
			listOf(listOf("a", "b"), listOf("c", "d"), listOf("e")),
			listOf("a", "b", "c", "d", "e").toChannel().chunks(2).toList()
		)
	}
}