package com.soywiz.korio.serialization.xml

import kotlin.test.*

class XmlTest {
	@kotlin.test.Test
	fun name() {
		val xml = Xml("<hello a=\"10\" Zz='20'><demo c='7' /></hello>")
		assertEquals(10, xml.int("a"))
		assertEquals(10, xml.int("A"))
		assertEquals(20, xml.int("zZ"))
		assertEquals("hello", xml.name)
		assertEquals(7, xml["demo"].first().int("c"))
		assertEquals(7, xml["Demo"].first().int("c"))
		assertEquals("""<hello a="10" Zz="20"><demo c="7"/></hello>""", xml.toString())
	}

	@kotlin.test.Test
	fun name2() {
		val xml = Xml("<a_b />")
	}

	@kotlin.test.Test
	fun name3() {
		assertEquals("""<test z="1" b="2"/>""", Xml.Tag("test", linkedMapOf("z" to 1, "b" to 2), listOf()).outerXml)
	}

	@kotlin.test.Test
	fun name4() {
		Xml(
			"""
			<?xml version="1.0" encoding="UTF-8"?>
			<!DOCTYPE svg PUBLIC "-//W3C//DTD SVG 1.1//EN" "http://www.w3.org/Graphics/SVG/1.1/DTD/svg11.dtd">
			<svg version="1.1" xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink" x="0" y="0" width="612" height="254" viewBox="0, 0, 612, 254">
			  <defs>
				<linearGradient id="Gradient_1" gradientUnits="userSpaceOnUse" x1="0" y1="60.104" x2="0" y2="299.106" gradientTransform="matrix(0.707, -0.707, 0.707, 0.707, 0, 0)">
				  <stop offset="0" stop-color="#00B0FF"/>
				  <stop offset="0.165" stop-color="#00AEFF"/>
				  <stop offset="0.499" stop-color="#955FF9"/>
				  <stop offset="0.762" stop-color="#E87353"/>
				  <stop offset="1" stop-color="#FF8900"/>
				</linearGradient>
			  </defs>
			  <g id="Image">
				<path d="M0,85 L85,0 L254,169 L169,254 L0,85 z" fill="url(#Gradient_1)"/>
				<path d="M84,85 L170,85 L170,169 L84,169 L84,85 z" fill="#000000"/>
			  </g>
			  <g id="Text">
				<path d="M560,83 L560,166 L583,166 L583,136 C583,136 582.062,106 610,106 L610,80.99 C610,80.99 590.07,80.159 583,100 L583,83 L560,83 z" fill="#FFFFFF"/>
				<path d="M501,80.99 C525.853,80.99 546,100.468 546,124.495 C546,148.522 525.853,168 501,168 C476.147,168 456,148.522 456,124.495 C456,100.468 476.147,80.99 501,80.99 z M501,101 C488.85,101 479,111.521 479,124.5 C479,137.479 488.85,148 501,148 C513.15,148 523,137.479 523,124.5 C523,111.521 513.15,101 501,101 z" fill="#FFFFFF"/>
				<path d="M395,83 L395,103 L405,103 L405,144 C405,144 404.133,169.763 429,167 C447,165 448,162 448,162 L448,143 C448,143 429,154 429,138 C429,119 429,103 429,103 L448,103 L448,83 L429,83 L429,62 L405,62 L405,83 L395,83 z" fill="#FFFFFF"/>
				<path d="M298,57 L321,57 L321,105 L365,57 L394,57 L351,103 L396,166 L368,166 L334,120 L322,133 L322,166 L298,166 L298,57 z" fill="#FFFFFF"/>
			  </g>
			</svg>
		""".trimIndent()
		)
	}

    @Test
    fun testIndent() {
        val xml1 = buildXml("name") {
            text("SimpleName")
        }
        assertEquals("<name>SimpleName</name>\n", xml1.toOuterXmlIndented().toString())

        val xml2 = buildXml("name") {
            text("SimpleName")
            text("ComplicatedName")
        }
        assertEquals("<name>\n\tSimpleName\n\tComplicatedName\n</name>\n", xml2.toOuterXmlIndented().toString())

        val xml3 = buildXml("name", "number" to 1) {
            comment("Some comment")
            node("value", "number" to 2) {
                text("SimpleName")
            }
        }
        val result2 = "<name number=\"1\">\n\t<!--Some comment-->\n\t<value number=\"2\">SimpleName</value>\n</name>\n"
        assertEquals(result2, xml3.toOuterXmlIndented().toString())
    }

    @Test
    fun testOutputQuote() {
        assertEquals("&lt;&amp;&gt;", buildXml("t") { text("<&>") }.innerXml)
        assertEquals("<&>", buildXml("t") { raw("<&>") }.innerXml)
        assertEquals("<![CDATA[<&>]]>", buildXml("t") { cdata("<&>") }.innerXml)
        assertFails {
            buildXml("t") { cdata("]]>") }.innerXml
        }
    }

    @Test
    fun testParseCData() {
        assertEquals("<xml><![CDATA[<&>]]></xml>", Xml("<xml><![CDATA[<&>]]></xml>").outerXml)
        assertEquals("<xml>&lt;&amp;&gt;</xml>", Xml("<xml>&lt;&amp;&gt;</xml>").outerXml)
    }
}
