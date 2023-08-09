package korlibs.template

import korlibs.template.dynamic.DynamicContext
import korlibs.template.dynamic.DynamicType
import korlibs.template.dynamic.Mapper2
import korlibs.template.util.KorteDeferred
import kotlin.js.JsName
import kotlin.test.Test
import kotlin.test.assertEquals

class TemplateTest : BaseTest() {
    //@Reflect
    data class Person(@JsName("name") val name: String, @JsName("surname") val surname: String) :
        DynamicType<Person> by DynamicType({ register(Person::name, Person::surname) })

    @Test
    fun testDummy() = suspendTest {
        assertEquals("hello", (Template("hello"))(null))
    }

    @Test
    fun testDoubleLiteral() = suspendTest {
        assertEquals("1.1", (Template("{{ 1.1 }}"))(null))
    }

    @Test
    fun testChunked() = suspendTest {
        assertEquals("[[1, 2], [3, 4], [5]]", (Template("{{ [1, 2, 3, 4, 5]|chunked(2) }}"))(null))
    }

    @Test
    fun testSwitch() = suspendTest {
        val template = Template(
            """
            {% switch value %}
            {% case "a" %}1
            {% case "b" %}2
            {% default %}3
            {% endswitch %}
        """.trimIndent()
        )
        assertEquals("1", template(mapOf("value" to "a")).trim())
        assertEquals("2", template(mapOf("value" to "b")).trim())
        assertEquals("3", template(mapOf("value" to "c")).trim())
        assertEquals("3", template(mapOf("value" to "d")).trim())
    }

    @Test
    fun testSimple() = suspendTest {
        assertEquals("hello soywiz", Template("hello {{ name }}")("name" to "soywiz"))
        assertEquals("soywizsoywiz", Template("{{name}}{{ name }}")("name" to "soywiz"))
    }

    @Test
    fun testAnd() = suspendTest {
        assertEquals("true", Template("{{ 1 and 2 }}")())
        assertEquals("false", Template("{{ 0 and 0 }}")())
        assertEquals("false", Template("{{ 0 and 1 }}")())
        assertEquals("false", Template("{{ 1 and 0 }}")())
    }

    @Test
    fun testOr() = suspendTest {
        assertEquals("true", Template("{{ 1 or 2 }}")())
        assertEquals("false", Template("{{ 0 or 0 }}")())
        assertEquals("true", Template("{{ 0 or 1 }}")())
        assertEquals("true", Template("{{ 1 or 0 }}")())
    }

    @Test
    fun testIn() = suspendTest {
        assertEquals("true", Template("{{ 'soy' in name }}")("name" to "soywiz"))
        assertEquals("false", Template("{{ 'demo' in name }}")("name" to "soywiz"))
    }

    @Test
    fun testContains() = suspendTest {
        assertEquals("true", Template("{{ name contains 'soy' }}")("name" to "soywiz"))
        assertEquals("false", Template("{{ name contains 'demo' }}")("name" to "soywiz"))
    }

    @Test
    fun testFor() = suspendTest {
        val tpl = Template("{% for n in numbers %}{{ n }}{% end %}")
        assertEquals("", tpl("numbers" to listOf<Int>()))
        assertEquals("123", tpl("numbers" to listOf(1, 2, 3)))
    }

    @Test
    fun testForAdv() = suspendTest {
        val tpl =
            Template("{% for n in numbers %}{{ n }}:{{ loop.index0 }}:{{ loop.index }}:{{ loop.revindex }}:{{ loop.revindex0 }}:{{ loop.first }}:{{ loop.last }}:{{ loop.length }}{{ '\\n' }}{% end %}")
        assertEquals(
            """
				a:0:1:2:3:true:false:3
				b:1:2:1:2:false:false:3
				c:2:3:0:1:false:true:3
			""".trimIndent().trim(),
            tpl("numbers" to listOf("a", "b", "c")).trim()
        )
    }

    @Test
    fun testForMap() = suspendTest {
        val tpl = Template("{% for k, v in map %}{{ k }}:{{v}}{% end %}")
        assertEquals("a:10b:c", tpl("map" to mapOf("a" to 10, "b" to "c")))
    }

    @Test
    fun testForElse() = suspendTest {
        val tpl = Template("{% for n in numbers %}{{ n }}{% else %}none{% end %}")
        assertEquals("123", tpl("numbers" to listOf(1, 2, 3)))
        assertEquals("none", tpl("numbers" to listOf<Int>()))
    }

    @Test
    fun testForElse2() = suspendTest {
        val tpl = Template("{% for n in numbers %}[{{ n }}]{% else %}none{% end %}")
        assertEquals("[1][2][3]", tpl("numbers" to listOf(1, 2, 3)))
        assertEquals("none", tpl("numbers" to listOf<Int>()))
    }

    @Test
    fun testDebug() = suspendTest {
        var result: String? = null
        var stdout = ""
        val tpl = Template("a {% debug 'hello ' + name %} b", TemplateConfig().apply {
            this.debugPrintln = { stdout += "$it" }
        })
        result = tpl("name" to "world")
        assertEquals("hello world", stdout.trim())
        assertEquals("a  b", result)
    }

    @Test
    fun testCapture() = suspendTest {
        assertEquals("REPEAT and REPEAT", Template("{% capture variable %}REPEAT{% endcapture %}{{ variable }} and {{ variable }}")())
    }

    @Test
    fun testSimpleIf() = suspendTest {
        assertEquals("true", Template("{% if cond %}true{% else %}false{% end %}")("cond" to 1))
        assertEquals("false", Template("{% if cond %}true{% else %}false{% end %}")("cond" to 0))
        assertEquals("true", Template("{% if cond %}true{% end %}")("cond" to 1))
        assertEquals("", Template("{% if cond %}true{% end %}")("cond" to 0))
    }

    @Test
    fun testSimpleUnless() = suspendTest {
        assertEquals("1false", Template("{% unless cond %}1true{% else %}1false{% end %}")("cond" to 1))
        assertEquals("2true", Template("{% unless cond %}2true{% else %}2false{% end %}")("cond" to 0))
        assertEquals("3true", Template("{% unless !cond %}3true{% end %}")("cond" to 1))
        assertEquals("4true", Template("{% unless cond %}4true{% end %}")("cond" to 0))
        assertEquals("", Template("{% unless !cond %}5true{% end %}")("cond" to 0))
    }

    @Test
    fun testNot() = suspendTest {
        assertEquals("true", Template("{% if not cond %}true{% end %}")("cond" to 0))
    }

    @Test
    fun testSimpleElseIf() = suspendTest {
        val tpl =
            Template("{% if v == 1 %}one{% elseif v == 2 %}two{% elseif v < 5 %}less than five{% elseif v > 8 %}greater than eight{% else %}other{% end %}")
        assertEquals("one", tpl("v" to 1))
        assertEquals("two", tpl("v" to 2))
        assertEquals("less than five", tpl("v" to 3))
        assertEquals("less than five", tpl("v" to 4))
        assertEquals("other", tpl("v" to 5))
        assertEquals("other", tpl("v" to 6))
        assertEquals("greater than eight", tpl("v" to 9))
    }

    @Test
    fun testEval() = suspendTest {
        assertEquals("-5", Template("{{ -(1 + 4) }}")(null))
        assertEquals("false", Template("{{ 1 == 2 }}")(null))
        assertEquals("true", Template("{{ 1 < 2 }}")(null))
        assertEquals("true", Template("{{ 1 <= 1 }}")(null))
    }

    @Test
    fun testExists() = suspendTest {
        assertEquals("false", Template("{% if prop %}true{% else %}false{% end %}")(null))
        assertEquals("true", Template("{% if prop %}true{% else %}false{% end %}")("prop" to "any"))
        assertEquals("false", Template("{% if prop %}true{% else %}false{% end %}")("prop" to ""))
    }

    @Test
    fun testIfBooleanLiterals() = suspendTest {
        assertEquals("true", Template("{% if true %}true{% end %}")(null))
        assertEquals("false", Template("{% if !false %}false{% end %}")(null))
    }

    @Test
    fun testOverwriteFilter() = suspendTest {
        assertEquals("HELLO", Template("{{ 'hello' | upper }}")(null))
        assertEquals("[hello]", Template("{{ 'hello' | upper }}", TemplateConfig(extraFilters = listOf(Filter("upper") { "[" + subject.toDynamicString() + "]" })))(null))
    }

    @Test
    fun testCustomUnknownFilter() = suspendTest {
        assertEquals("-ERROR-", Template("{{ 'hello' | asdasdasdasdas }}", TemplateConfig(extraFilters = listOf(Filter("unknown") { "-ERROR-" })))(null))
    }

    @Test
    fun testForAccess() = suspendTest {
        assertEquals(
            ":Zard:Ballesteros",
            Template("{% for n in persons %}:{{ n.surname }}{% end %}")(
                "persons" to listOf(
                    Person("Soywiz", "Zard"),
                    Person("Carlos", "Ballesteros")
                )
            )
        )
        assertEquals(
            "ZardBallesteros",
            Template("{% for n in persons %}{{ n['sur'+'name'] }}{% end %}")(
                "persons" to listOf(
                    Person(
                        "Soywiz",
                        "Zard"
                    ), Person("Carlos", "Ballesteros")
                )
            )
        )
        assertEquals(
            "ZardBallesteros",
            Template("{% for nin in persons %}{{ nin['sur'+'name'] }}{% end %}")(
                "persons" to listOf(
                    Person(
                        "Soywiz",
                        "Zard"
                    ), Person("Carlos", "Ballesteros")
                )
            )
        )
    }

    @Test
    fun testStrictEquality() = suspendTest {
        assertEquals("false", Template("{{ '1' === 1 }}")())
        assertEquals("true", Template("{{ '1' !== 1 }}")())
    }

    @Test
    fun testEquality() = suspendTest {
        assertEquals("true", Template("{{ '1' == 1 }}")())
        assertEquals("false", Template("{{ '1' == 0 }}")())
    }

    @Test
    fun testFilters() = suspendTest {
        assertEquals("CARLOS", Template("{{ name|upper }}")("name" to "caRLos"))
        assertEquals("carlos", Template("{{ name|lower }}")("name" to "caRLos"))
        assertEquals("Carlos", Template("{{ name|capitalize }}")("name" to "caRLos"))
        assertEquals("Carlos", Template("{{ (name)|capitalize }}")("name" to "caRLos"))
        assertEquals("Carlos", Template("{{ 'caRLos'|capitalize }}")(null))
        assertEquals("hello KorTE", Template("{{'hello world' | replace('world', 'KorTE')}}")(null))
    }

    @Test
    fun testFilterArgument() = suspendTest {
        assertEquals("[car, los]", Template("{{ name | split: '|' }}")("name" to "car|los"))
    }

    @Test
    fun testArrayLiterals() = suspendTest {
        assertEquals("1234", Template("{% for n in [1, 2, 3, 4] %}{{ n }}{% end %}")(null))
        assertEquals("", Template("{% for n in [] %}{{ n }}{% end %}")(null))
        assertEquals("1, 2, 3, 4", Template("{{ [1, 2, 3, 4]|join(', ') }}")(null))
    }

    @Test
    fun testElvis() = suspendTest {
        assertEquals("1", Template("{{ 1 ?: 2 }}")(null))
        assertEquals("2", Template("{{ 0 ?: 2 }}")(null))
    }

    @Test
    fun testMerge() = suspendTest {
        assertEquals("[1, 2, 3, 4]", Template("{{ [1, 2]|merge([3, 4]) }}")(null))
    }

    @Test
    fun testJsonEncode() = suspendTest {
        assertEquals("{\"a\":2}", Template("{{ {'a': 2}|json_encode()|raw }}")(null))
    }

    @Test
    fun testComment() = suspendTest {
        assertEquals("a", Template("{# {{ 1 }} #}a{# #}")(null))
    }

    @Test
    fun testFormat() = suspendTest {
        assertEquals("hello test of 3", Template("{{ 'hello %s of %d'|format('test', 3) }}")(null))
    }

    @Test
    fun testTernary() = suspendTest {
        assertEquals("2", Template("{{ 1 ? 2 : 3 }}")(null))
        assertEquals("3", Template("{{ 0 ? 2 : 3 }}")(null))
    }

    @Test
    fun testSet() = suspendTest {
        assertEquals("1,2,3", Template("{% set a = [1,2,3] %}{{ a|join(',') }}")(null))
    }

    @Test
    fun testAccessGetter() = suspendTest {
        val success = "success!"

        class Test1 : DynamicType<Test1> by DynamicType({ register(Test1::a) }) {
            @JsName("a")
            val a: String get() = success
        }

        assertEquals(success, Template("{{ test.a }}")("test" to Test1()))
    }

    @Test
    fun testCustomTag() = suspendTest {
        class CustomNode(val text: String) : Block {
            override suspend fun eval(context: Template.EvalContext) = context.write("CUSTOM($text)")
        }

        val CustomTag = Tag("custom", setOf(), null) {
            CustomNode(chunks.first().tag.content)
        }

        assertEquals(
            "CUSTOM(test)CUSTOM(demo)",
            Template("{% custom test %}{% custom demo %}", TemplateConfig(extraTags = listOf(CustomTag))).invoke(null)
        )
    }

    @Test
    fun testSlice() = suspendTest {
        val map = linkedMapOf("v" to listOf(1, 2, 3, 4))
        assertEquals("[1, 2, 3, 4]", Template("{{ v }}")(map))
        assertEquals("[2, 3, 4]", Template("{{ v|slice(1) }}")(map))
        assertEquals("[2, 3]", Template("{{ v|slice(1, 2) }}")(map))
        assertEquals("ello", Template("{{ v|slice(1) }}")(mapOf("v" to "hello")))
        assertEquals("el", Template("{{ v|slice(1, 2) }}")(mapOf("v" to "hello")))
    }

    @Test
    fun testReverse() = suspendTest {
        val map = linkedMapOf("v" to listOf(1, 2, 3, 4))
        assertEquals("[4, 3, 2, 1]", Template("{{ v|reverse }}")(map))
        assertEquals("olleh", Template("{{ v|reverse }}")(mapOf("v" to "hello")))
        assertEquals("le", Template("{{ v|slice(1, 2)|reverse }}")(mapOf("v" to "hello")))
    }

    @Test
    fun testObject() = suspendTest {
        assertEquals("""{&quot;foo&quot;: 1, &quot;bar&quot;: 2}""", Template("{{ { 'foo': 1, 'bar': 2 } }}")())
    }

    @Test
    fun testFuncCycle() = suspendTest {
        assertEquals("a", Template("{{ cycle(['a', 'b'], 2) }}")())
        assertEquals("b", Template("{{ cycle(['a', 'b'], -1) }}")())
    }

    @Test
    fun testRange() = suspendTest {
        assertEquals("[0, 1, 2, 3]", Template("{{ 0..3 }}")())
        assertEquals("[0, 1, 2, 3]", Template("{{ range(0,3) }}")())
        assertEquals("[0, 2]", Template("{{ range(0,3,2) }}")())
    }

    @Test
    fun testEscape() = suspendTest {
        assertEquals("<b>&lt;a&gt;</b>", Template("<b>{{ a }}</b>")("a" to "<a>"))
        assertEquals("<b><a></b>", Template("<b>{{ a|raw }}</b>")("a" to "<a>"))
        assertEquals("<b>&lt;A&gt;</b>", Template("<b>{{ a|raw|upper }}</b>")("a" to "<a>"))
        assertEquals("<b><A></b>", Template("<b>{{ a|upper|raw }}</b>")("a" to "<a>"))
    }

    @Test
    fun testTrim() = suspendTest {
        assertEquals("""a  1  b""", Template("a  {{ 1 }}  b")())
        assertEquals("""a1  b""", Template("a  {{- 1 }}  b")())
        assertEquals("""a  1b""", Template("a  {{ 1 -}}  b")())
        assertEquals("""a1b""", Template("a  {{- 1 -}}  b")())

        assertEquals("""a     b""", Template("a  {% set a=1 %}   b")())
        assertEquals("""a   b""", Template("a  {%- set a=1 %}   b")())
        assertEquals("""a  b""", Template("a  {% set a=1 -%}   b")())
        assertEquals("""ab""", Template("a  {%- set a=1 -%}   b")())
    }

    @Test
    fun testOperatorPrecedence() = suspendTest {
        assertEquals("${4 + 5 * 7}", Template("{{ 4+5*7 }}")())
        assertEquals("${4 * 5 + 7}", Template("{{ 4*5+7 }}")())
    }

    @Test
    fun testOperatorPrecedence2() = suspendTest {
        assertEquals("${(4 + 5) * 7}", Template("{{ (4+5)*7 }}")())
        assertEquals("${(4 * 5) + 7}", Template("{{ (4*5)+7 }}")())
        assertEquals("${4 + (5 * 7)}", Template("{{ 4+(5*7) }}")())
        assertEquals("${4 * (5 + 7)}", Template("{{ 4*(5+7) }}")())
    }

    @Test
    fun testOperatorPrecedence3() = suspendTest {
        assertEquals("${-(4 + 5)}", Template("{{ -(4+5) }}")())
        assertEquals("${+(4 + 5)}", Template("{{ +(4+5) }}")())
    }

    @Test
    fun testFrontMatter() = suspendTest {
        assertEquals(
            """hello""",
            Template(
                """
					---
					title: hello
					---
					{{ title }}
				""".trimIndent()
            )()
        )
    }

    class TestMethods : DynamicType<TestMethods> by DynamicType({
        register("mytest123") { mytest123() }
        register("sum") { sum(it[0].toDynamicInt(), it[1].toDynamicInt()) }
    }), DynamicContext {
        var field = 1

        suspend fun mytest123(): Int {
            val deferred = KorteDeferred<Int>()
            deferred.complete(field)
            val r = deferred.await()
            return r + 7
        }

        @JsName("sum")
        suspend fun sum(a: Int, b: Int): Int {
            return a + b
        }
    }

    @Test
    fun testSuspendClass1() = suspendTest {
        assertEquals("""8""", Template("{{ v.mytest123 }}")("v" to TestMethods(), mapper = Mapper2))
    }

    @Test
    fun testSuspendClass2() = suspendTest {
        assertEquals("""8""", Template("{{ v.mytest123() }}")("v" to TestMethods(), mapper = Mapper2))
    }

    @Test
    fun testSuspendClass3() = suspendTest {
        assertEquals("""8""", Template("{{ v.sum(3, 5) }}")("v" to TestMethods(), mapper = Mapper2))
    }

    //@Test fun testStringInterpolation() = sync {
    //	assertEquals("a2b", Template("{{ \"a#{7 - 5}b\" }}")())
    //}

    @Test
    fun testConcatOperator() = suspendTest {
        assertEquals("12", Template("{{ 1 ~ 2 }}")())
    }

    @Test
    fun testUnknownFilter() = suspendTest {
        expectException<KorteException>("Unknown filter 'unknownFilter' at template:1:6") { Template("{{ 'a'|unknownFilter }}")() }
    }

    @Test
    fun testMissingFilterName() = suspendTest {
        expectException<KorteException>("Missing filter name at template:1:6") { Template("{{ 'a'| }}")() }
    }

    @Test
    fun testCustomBlockWriter() = suspendTest {
        val config = TemplateConfig().also {
            it.replaceWriteBlockExpressionResult { value, previous ->
                if (value == null) throw NullPointerException("null")
                previous(value)
            }
        }
        assertEquals("a", Template("{{ 'a' }}", config)())
        expectException<NullPointerException>("null") { Template("{{ null }}", config)() }
    }

    @Test
    fun testCustomVariablePreprocessor() = suspendTest {
        val config = TemplateConfig().also {
            it.replaceVariablePocessor { name, previous ->
                previous(name) ?: throw NullPointerException("Variable: $name cannot be null.")
            }
        }
        assertEquals("a", Template("{{ var1 }}", config)(mapOf("var1" to "a")))
        expectException<NullPointerException>("Variable: var2 cannot be null.") { Template("{{ var2 }}", config)() }
    }

    @Test fun testInvalid1() = suspendTest { expectException<KorteException>("String literal not closed at template:1:3") { Template("{{ ' }}")() } }
    @Test fun testInvalid2() = suspendTest { expectException<KorteException>("No expression at template:1:3") { Template("{{ }}")() } }
    @Test fun testInvalid3() = suspendTest { expectException<KorteException>("Expected expression at template:1:5") { Template("{{ 1 + }}")() } }
    @Test fun testInvalid4() = suspendTest { expectException<KorteException>("Unexpected token 'world' at template:1:13") { Template("{% set a = hello world %}")() } }
    @Test fun testInvalid5() = suspendTest { expectException<KorteException>("Expected id at template:1:3") { Template("{% set %}")() } }
    @Test fun testInvalid6() = suspendTest { expectException<KorteException>("Expected = but found end at template:1:3") { Template("{% set a %}")() } }
    @Test fun testInvalid7() = suspendTest { expectException<KorteException>("Expected expression at template:1:5") { Template("{% set a = %}")() } }

    @Test
    fun testImportMacros() = suspendTest {
        val templates = Templates(
            TemplateProvider(
                "root.html" to "{% import '_macros.html' as macros %}{{ macros.putUserLink('hello') }}",
                "_macros.html" to "{% macro putUserLink(user) %}<a>{{ user }}</a>{% endmacro %}"
            )
        )
        assertEquals("<a>hello</a>", templates.get("root.html").invoke(hashMapOf<Any, Any?>()))
    }

    @Test
    fun testCustomEscapeMode() = suspendTest {
        val template = "<hello>{{ test }}</hello>"
        assertEquals(
            """
                <hello>&lt;WORLD&gt;</hello>
                <hello><WORLD></hello>
            """.trimIndent(),
            listOf(AutoEscapeMode.HTML, AutoEscapeMode.RAW)
                .map { Template(template, TemplateConfig(autoEscapeMode = it))("test" to "<WORLD>") }
                .joinToString("\n")
        )
    }
}
