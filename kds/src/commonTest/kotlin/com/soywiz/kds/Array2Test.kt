package com.soywiz.kds

import kotlin.test.*

class Array2Test {
	@Test
	fun name() {
		val map = Array2.fromString(
			Tiles.MAPPING, -1, """
			:    #####
			:    #   #
			:    #$  #
			:  ###  $##
			:  #  $ $ #
			:### # ## #   ######
			:#   # ## #####  ..#
			:# $  $         *..#
			:##### ### #@##  ..#
			:    #     #########
			:    #######
		"""
		)


		val output = map.toString(Tiles.REV_MAPPING, margin = ":")

		val expected = listOf(
			":     #####          ",
			":     #   #          ",
			":     #$  #          ",
			":   ###  $##         ",
			":   #  $ $ #         ",
			": ### # ## #   ######",
			": #   # ## #####  ..#",
			": # $  $         *..#",
			": ##### ### #@##  ..#",
			":     #     #########",
			":     #######        "
		).joinToString("\n")

		assertEquals(expected, output)
	}

	object Tiles {
		const val GROUND = 0
		const val WALL = 1
		const val CONTAINER = 2
		const val BOX = 3
		const val BOX_OVER = 4
		const val CHARACTER = 10

		val AVAILABLE = setOf(GROUND, CONTAINER)
		val BOXLIKE = setOf(BOX, BOX_OVER)

		val MAPPING = mapOf(
			' ' to GROUND,
			'#' to WALL,
			'.' to CONTAINER,
			'$' to BOX,
			'*' to BOX_OVER,
			'@' to CHARACTER
		)

		val REV_MAPPING = MAPPING.map { it.value to it.key }.toMap()
	}

	@Test
	fun test2() {
		val map = FloatArray2(5, 5, 100f)
		map.each { x, y, v ->
			map[x, y] = v + (x * 10 + y).toFloat()
		}
		assertEquals("""
			100, 110, 120, 130, 140
			101, 111, 121, 131, 141
			102, 112, 122, 132, 142
			103, 113, 123, 133, 143
			104, 114, 124, 134, 144
		""".trimIndent(), map.toString().replace(".0", ""))
	}

	@Test
	fun test3() {
		assertEquals(FloatArray2(5, 5, 100f), FloatArray2(5, 5) { 100f })
		assertEquals(FloatArray2(5, 5, 100f), FloatArray2.withGen(5, 5) { _, _ -> 100f })
		assertEquals(FloatArray2(5, 5, 100f), FloatArray2((0 until 5).map { (0 until 5).map { 100f } }))
	}
}
