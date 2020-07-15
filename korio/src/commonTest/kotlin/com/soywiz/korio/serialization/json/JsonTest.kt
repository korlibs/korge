package com.soywiz.korio.serialization.json

import com.soywiz.korio.dynamic.mapper.*
import com.soywiz.korio.dynamic.serialization.*
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

	val mapper = ObjectMapper()

	init {
		// GENERATED. THIS CODE SHOULD BE GENERATED.

		// Problems with Kotlin.JS

		//mapper.registerEnum(MyEnum.values())
		//mapper.registerType { Demo(it["a"].gen(), it["b"].gen()) }
		//mapper.registerType { Demo2().apply { a = it["a"].gen() } }
		//mapper.registerType { DemoList(it["demos"].genList<Demo>()) }
		//mapper.registerType { DemoSet(it["demos"].genSet<Demo>()) }
		//mapper.registerType { ClassWithEnum(it["a"]?.gen() ?: MyEnum.HELLO) }
		//mapper.registerUntypeEnum<MyEnum>()
		//mapper.registerUntype<Demo> { linkedMapOf("a" to it.a.gen(), "b" to it.b.gen()) }
		//mapper.registerUntype<Demo2> { linkedMapOf("a" to it.a.gen()) }
		//mapper.registerUntype<DemoList> { linkedMapOf("demos" to it.demos.gen()) }
		//mapper.registerUntype<DemoSet> { linkedMapOf("demos" to it.demos.gen()) }
		//mapper.registerUntype<ClassWithEnum> { linkedMapOf("a" to it.a.gen()) }

		mapper.registerEnum(MyEnum::class, MyEnum.values())
		mapper.registerType(Demo::class) { Demo(it["a"].gen(Int::class), it["b"].gen(String::class)) }
		mapper.registerType(Demo2::class) { Demo2().apply { a = it["a"].gen(Int::class) } }
		mapper.registerType(DemoList::class) { DemoList(it["demos"].genList(Demo::class)) }
		mapper.registerType(DemoSet::class) { DemoSet(it["demos"].genSet(Demo::class)) }
		mapper.registerType(ClassWithEnum::class) { ClassWithEnum(it["a"]?.gen(MyEnum::class) ?: MyEnum.HELLO) }
		mapper.registerUntypeEnum(MyEnum::class)
		mapper.registerUntype(Demo::class) { linkedMapOf("a" to it.a.gen(), "b" to it.b.gen()) }
		mapper.registerUntype(Demo2::class) { linkedMapOf("a" to it.a.gen()) }
		mapper.registerUntype(DemoList::class) { linkedMapOf("demos" to it.demos.gen()) }
		mapper.registerUntype(DemoSet::class) { linkedMapOf("demos" to it.demos.gen()) }
		mapper.registerUntype(ClassWithEnum::class) { linkedMapOf("a" to it.a.gen()) }
	}

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
	fun encode1() {
		assertEquals("1", Json.stringifyTyped(1, mapper))
		assertEquals("null", Json.stringifyTyped<Any>(null, mapper))
		assertEquals("true", Json.stringifyTyped(true, mapper))
		assertEquals("false", Json.stringifyTyped(false, mapper))
		assertEquals("{}", Json.stringifyTyped(mapOf<String, Any?>(), mapper))
		assertEquals("[]", Json.stringifyTyped(listOf<Any?>(), mapper))
		assertEquals("\"a\"", Json.stringifyTyped("a", mapper))
	}

	@kotlin.test.Test
	fun encode2() {
		assertEquals("[1,2,3]", Json.stringifyTyped(listOf(1, 2, 3), mapper))
		assertEquals("""{"a":1,"b":2}""", Json.stringifyTyped(linkedMapOf("a" to 1, "b" to 2), mapper))
	}

	@kotlin.test.Test
	fun encodeTyped() {
		assertEquals("""{"a":1,"b":"test"}""", Json.stringifyTyped(Demo(1, "test"), mapper))
	}

	@kotlin.test.Test
	fun decodeToType1() {
		//assertEquals(Demo(1, "hello"), Json.decodeToType<Demo>("""{"a": 1, "b": "hello"}""", mapper))
		assertEquals(Demo(1, "hello"), Json.parseTyped(Demo::class, """{"a": 1, "b": "hello"}""", mapper))
	}

	@kotlin.test.Test
	fun decodeUnicode() {
		assertEquals("aeb", Json.parse(""" "a\u0065b" """))
	}

	@kotlin.test.Test
	fun decodeTypedList() {
		//val result = Json.decodeToType<DemoList>("""{ "demos" : [{"a":1,"b":"A"}, {"a":2,"b":"B"}] }""", mapper)
		val result = Json.parseTyped(DemoList::class, """{ "demos" : [{"a":1,"b":"A"}, {"a":2,"b":"B"}] }""", mapper)
		val demo = result.demos.first()
		assertEquals(1, demo.a)
	}

	@kotlin.test.Test
	fun decodeTypedSet() {
		//val result = Json.decodeToType<DemoSet>("""{ "demos" : [{"a":1,"b":"A"}, {"a":2,"b":"B"}] }""", mapper)
		val result = Json.parseTyped(DemoSet::class, """{ "demos" : [{"a":1,"b":"A"}, {"a":2,"b":"B"}] }""", mapper)
		val demo = result.demos.first()
		assertEquals(1, demo.a)
	}

	@kotlin.test.Test
	fun decodeToPrim() {
		// Kotlin.JS BUG

		//assertEquals(listOf(1, 2, 3, 4, 5), Json.decodeToType<List<Int>>("""[1, 2, 3, 4, 5]""", mapper))
		//assertEquals(1, Json.decodeToType<Int>("1", mapper))
		//assertEquals(true, Json.decodeToType<Boolean>("true", mapper))
		//assertEquals("a", Json.decodeToType<String>("\"a\"", mapper))
		//assertEquals('a', Json.decodeToType<Char>("\"a\"", mapper))

		assertEquals(
			listOf(1, 2, 3, 4, 5).toString(),
			Json.parseTyped(List::class, """[1, 2, 3, 4, 5]""", mapper).toString()
		)
		assertEquals(1, Json.parseTyped(Int::class, "1", mapper))
		assertEquals(true, Json.parseTyped(Boolean::class, "true", mapper))
		assertEquals("a", Json.parseTyped(String::class, "\"a\"", mapper))
		assertEquals('a', Json.parseTyped(Char::class, "\"a\"", mapper))
	}

	@kotlin.test.Test
	fun decodeToPrimChar() {
		//assertEquals('a', Json.decodeToType<Char>("\"a\"", mapper))
		assertEquals('a', Json.parseTyped(Char::class, "\"a\"", mapper))
	}

	@kotlin.test.Test
	fun encodeWithStaticMembers() {
		assertEquals("""{"a":10}""", Json.stringifyTyped(Demo2(), mapper))
	}

	@kotlin.test.Test
	fun testEncodeEnum() {
		assertEquals("""{"a":"HELLO"}""", Json.stringifyTyped(ClassWithEnum(), mapper))
	}

	@kotlin.test.Test
	fun testDecodeEnum() {
		//assertEquals(ClassWithEnum(MyEnum.WORLD), Json.decodeToType<ClassWithEnum>("""{"a":"WORLD"}""", mapper))
		assertEquals(ClassWithEnum(MyEnum.WORLD), Json.parseTyped(ClassWithEnum::class, """{"a":"WORLD"}""", mapper))
	}

	@kotlin.test.Test
	fun testDecodeMap() {
		data class V(val a: Int, val b: Int)
		data class Demo(val v: Map<String, V>)

		//mapper.registerType { V(it["a"].gen(), it["b"].gen()) }
		//mapper.registerType { Demo(it["v"].toDynamicMap().map { it.key.toString() to it.value.gen<V>() }.toMap()) }

		mapper.registerType { V(it["a"].gen(Int::class), it["b"].gen(Int::class)) }
		mapper.registerType { Demo(it["v"].map.map { it.key.toString() to it.value.gen<V>() }.toMap()) }

		//assertEquals(Demo(linkedMapOf("z" to V(1, 2))), Json.decodeToType<Demo>("""{"v":{"z":{"a":1,"b":2}}}""", mapper))
		assertEquals(
			Demo(linkedMapOf("z" to V(1, 2))),
			Json.parseTyped(Demo::class, """{"v":{"z":{"a":1,"b":2}}}""", mapper)
		)
	}

	@kotlin.test.Test
	fun testEncodeMap() {
		data class V(val a: Int, val b: Int)
		data class Demo(val v: Map<String, V>)

		mapper.registerUntype<V> { linkedMapOf("a" to it.a.gen(), "b" to it.b.gen()) }
		mapper.registerUntype<Demo> { linkedMapOf("v" to it.v.gen()) }

		assertEquals(
			"""{"v":{"z1":{"a":1,"b":2},"z2":{"a":1,"b":2}}}""",
			Json.stringifyTyped(Demo(linkedMapOf("z1" to V(1, 2), "z2" to V(1, 2))), mapper)
		)
	}
}