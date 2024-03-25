package korlibs.io.serialization.json

import kotlin.test.*

class JsonTest {
	enum class MyEnum { DEMO, HELLO, WORLD }
	data class ClassWithEnum(val a: MyEnum = MyEnum.HELLO)

	class Demo2 {
		var a: Int = 10

		companion object {
			//@JvmField
			var b: String = "test"
		}
	}

	data class Demo(val a: Int, val b: String)

	data class DemoList(val demos: ArrayList<Demo>)

	data class DemoSet(val demos: Set<Demo>)

	@kotlin.test.Test
	fun decode1() {
		assertEquals(linkedMapOf("a" to 1).toString(), Json.parse("""{"a":1}""").toString())
		//assertEquals(-1e7, Json.decode("""-1e7"""))
		assertEquals(-10000000, Json.parse("""-1e7"""))
	}

	@kotlin.test.Test
	fun decode2() {
		assertEquals(
			listOf("a", 1, -1, 0.125, 0, 11, true, false, null, listOf<Any?>(), mapOf<String, Any?>()).toString(),
			Json.parse("""["a", 1, -1, 0.125, 0, 11, true, false, null, [], {}]""").toString()
		)
	}

	@kotlin.test.Test
	fun decode3() {
		assertEquals("\"", Json.parse(""" "\"" """))
		assertEquals(listOf(1, 2).toString(), Json.parse(""" [ 1 , 2 ]""").toString())
	}

	@kotlin.test.Test
	fun decodeUnicode() {
		assertEquals("aeb", Json.parse(""" "a\u0065b" """))
	}
}
