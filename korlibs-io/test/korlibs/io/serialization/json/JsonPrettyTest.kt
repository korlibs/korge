package korlibs.io.serialization.json

import kotlin.test.*

class JsonPrettyTest {

	fun encode1() {
		assertEquals("1", Json.stringify(1, pretty = true))
		//assertEquals("null", Json.encodePretty(null, mapper))
		assertEquals("true", Json.stringify(true, pretty = true))
		assertEquals("false", Json.stringify(false, pretty = true))
		assertEquals("{\n}", Json.stringify(mapOf<String, Any?>(), pretty = true))
		assertEquals("[\n]", Json.stringify(listOf<Any?>(), pretty = true))
		assertEquals("\"a\"", Json.stringify("a", pretty = true))
	}

	@Test
	fun encode2() {
		assertEquals(
			"""
			|[
			|	1,
			|	2,
			|	3
			|]
		""".trimMargin(), Json.stringify(listOf(1, 2, 3), pretty = true)
		)

		assertEquals(
			"""
			|{
			|	"a": 1,
			|	"b": 2
			|}
		""".trimMargin(), Json.stringify(linkedMapOf("a" to 1, "b" to 2), pretty = true)
		)
	}

	@Test
	fun encodeTyped() {
		assertEquals(
			"""
			|{
			|	"a": 1,
			|	"b": "test"
			|}
			""".trimMargin(), Json.stringify(mapOf("a" to 1, "b" to "test"), pretty = true)
		)
	}

	@Test
	fun encodeMix() {
		assertEquals(
			"""
				|{
				|	"a": [
				|		1,
				|		2,
				|		3,
				|		4
				|	],
				|	"b": [
				|		5,
				|		6
				|	],
				|	"c": {
				|		"a": true,
				|		"b": null,
				|		"c": "hello"
				|	}
				|}
			""".trimMargin(),
			Json.stringify(
				linkedMapOf(
					"a" to listOf(1, 2, 3, 4),
					"b" to listOf(5, 6),
					"c" to linkedMapOf(
						"a" to true,
						"b" to null,
						"c" to "hello"
					)
				), pretty = true
			)
		)
	}
}
