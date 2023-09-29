package com.soywiz.kproject

import com.soywiz.kproject.internal.*
import kotlin.test.*

// http://nodeca.github.io/js-yaml/
class YamlTest {
    @kotlin.test.Test
    fun basic() {
        assertEquals("str", Yaml.read("str"))
        assertEquals(10, Yaml.read("10"))
    }

    @kotlin.test.Test
    fun array() {
        assertEquals(listOf(1, 2, 3), Yaml.read("[1,2,3]"))
    }

    @kotlin.test.Test
    fun name() {
        assertEquals(
            listOf(1, 2, 3),
            Yaml.read(
                """
			- 1
			- 2
			- 3
			""".trimIndent()
            )
        )
    }

    @kotlin.test.Test
    fun name2() {
        assertEquals(
            linkedMapOf("hr" to 65, "avg" to 0.278, "rbi" to 147),
            Yaml.read(
                """
				hr:  65    # Home runs
				avg: 0.278 # Batting average
				rbi: 147   # Runs Batted In
			""".trimIndent()
            )
        )
    }

    @kotlin.test.Test
    fun name3() {
        assertEquals(
            listOf(listOf(listOf(1))),
            Yaml.read("- - - 1")
        )
    }

    @kotlin.test.Test
    fun name4() {
        assertEquals(
            listOf(linkedMapOf("a" to 1), linkedMapOf("a" to 2)),
            Yaml.read(
                """
				|-
				|	a: 1
				|-
				|	a: 2
			""".trimMargin()
            )
        )
    }

    @kotlin.test.Test
    fun name5() {
        assertEquals(
            listOf(
                linkedMapOf(
                    "name" to "Mark McGwire",
                    "hr" to 65,
                    "avg" to 0.278
                ),
                linkedMapOf(
                    "name" to "Sammy Sosa",
                    "hr" to 63,
                    "avg" to 0.288
                )
            ),
            Yaml.read(
                """
				|-
				|  name: Mark McGwire
				|  hr:   65
				|  avg:  0.278
				|-
				| name: Sammy Sosa
				| hr:   63
				| avg:  0.288
			""".trimMargin()
            )
        )
    }

    @kotlin.test.Test
    fun name6() {
        assertEquals(
            linkedMapOf(
                "hr" to listOf("Mark McGwire", "Sammy Sosa"),
                "rbi" to listOf("Sammy Sosa", "Ken Griffey")
            ),
            Yaml.read(
                """
				|hr: # 1998 hr ranking
				|  - Mark McGwire
				|  - Sammy Sosa
				|rbi:
				|  # 1998 rbi ranking
				|  - Sammy Sosa
				|  - Ken Griffey
			""".trimMargin()
            )
        )
    }

    @kotlin.test.Test
    fun name7() {
        assertEquals(
            linkedMapOf(
                "null" to null,
                "booleans" to listOf(true, false),
                "string" to "012345"
            ),
            Yaml.read(
                """
				|null:
				|booleans: [ true, false ]
				|string: '012345'

			""".trimMargin()
            )
        )
    }

    enum class MyEnum { DEMO, HELLO, WORLD }
    data class ClassWithEnum(val size: Int = 70, val a: MyEnum = MyEnum.HELLO)

    @Test
    fun testChunk() {
        val yamlStr = """
        layout: post
        layout2: null
        demo: false
        permalink: /lorem-ipsum/
        title: "Lorem Ipsum"
        feature_image: "/images/2019/lorem_ipsum.jpg"
        tags: [lorem,lorem-ipsum]
        date: 2019-10-07 00:00:00 
        """.trimIndent()
        //println(Yaml.tokenize(yamlStr))
        assertEquals(
            mapOf(
                "layout" to "post",
                "layout2" to null,
                "demo" to false,
                "permalink" to "/lorem-ipsum/",
                "title" to "Lorem Ipsum",
                "feature_image" to "/images/2019/lorem_ipsum.jpg",
                "tags" to listOf("lorem", "lorem-ipsum"),
                "date" to "2019-10-07 00:00:00"
            ),
            Yaml.decode(yamlStr)
        )
        assertEquals(
            "layout:null",
            Yaml.decode("layout:null")
        )
        assertEquals(
            mapOf(
                "layout" to null,
            ),
            Yaml.decode("layout: null")
        )
    }

    @Test
    fun testChunk2() {
        assertEquals(
            mapOf(
                "tags" to listOf("lorem", "ipsum"),
                "layout" to "post",
                "hello" to mapOf("world" to listOf("a", "b")),
                "title" to "demo: 2D test demo lorem ipsum",
                "title_es" to "lorem: ipsum sim de 2D te test",
                "date" to "2009-05-05T10:45:00.000+02:00",
                "author" to "abc def hij",
                "feature_image" to "/images/2009/ipsum.png",
                "modified_time" to "2011-05-14T15:39:49.185+02:00",
                "thumbnail" to "http://1.bp.example.com/-lorem/ipsum/sit/AMEN-demo/s72-c/test.png",
                "blogger_id" to "tag:blogger.com,1999:blog-1212121212121212121212122.post-12121212121212121212121",
                "blogger_orig_url" to "http://blog.example.es/2009/05/demo-loreim-ip-sit-demo.html"
            ),
            Yaml.decode(
                """
                tags:
                - lorem
                - ipsum
                layout: post
                hello:
                    world:
                    - a
                    - b
                title: 'demo: 2D test demo lorem ipsum'
                title_es: 'lorem: ipsum sim de 2D te test'
                date: '2009-05-05T10:45:00.000+02:00'
                author: abc def hij
                feature_image: /images/2009/ipsum.png
                modified_time: '2011-05-14T15:39:49.185+02:00'
                thumbnail: http://1.bp.example.com/-lorem/ipsum/sit/AMEN-demo/s72-c/test.png
                blogger_id: tag:blogger.com,1999:blog-1212121212121212121212122.post-12121212121212121212121
                blogger_orig_url: http://blog.example.es/2009/05/demo-loreim-ip-sit-demo.html
            """.trimIndent()
            )
        )
    }
    //@Test
    //fun name8() {
    //	assertEquals(
    //		null,
    //		Yaml.read("[a:1,b:2]")
    //	)
    //}

    @Test
    fun testMapListIssue() {
        val testYmlString = """
            hello:
            - a
            
            - b
              
            lineWithSpaces:
              
              
            - aa
            - bb
              
            world:
            - c
            - d
            test:
              - e
              - f
        """.trimIndent()
        //println("\n\n[[[$testYmlString]]]\n\n")
        assertEquals(
            mapOf(
                "hello" to listOf("a", "b"),
                "lineWithSpaces" to listOf("aa", "bb"),
                "world" to listOf("c", "d"),
                "test" to listOf("e", "f")
            ),
            Yaml.decode(testYmlString)
        )
    }

    @Test
    fun testWindowsLineEndings() {
        assertEquals(
            mapOf(
                "key1" to mapOf("read" to true),
                "key2" to mapOf("read" to false),
            ),
            Yaml.decode("key1:\r\n  read: true\r\nkey2:\r\n  read: false\r\n")
        )
    }

    @Test
    fun testWindowsLineEndings2() {
        assertEquals(
            mapOf(
                "key1" to mapOf("read" to true),
                "key2" to mapOf("read" to false),
            ),
            Yaml.decode("key1:\r\n  read: true\r\n\r\nkey2:\r\n  read: false\r\n")
        )
    }

    @Test
    fun testHyphenInKeys() {
        assertEquals(
            mapOf(
                "this-is-an-example" to mapOf("fail" to true),
            ),
            Yaml.decode("""
                this-is-an-example:
                  fail: true
            """.trimIndent())
        )
    }
}
