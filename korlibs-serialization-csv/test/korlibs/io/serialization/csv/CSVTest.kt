package korlibs.io.serialization.csv

import kotlin.test.Test
import kotlin.test.assertEquals

class CSVTest {
    val csvStr = """
        a,b,c
        hello,world,this
        is,a,test
    """.trimIndent()

    @Test
    fun testParseLine() {
        assertEquals(listOf(""), CSV.parseLine(""))
        assertEquals(listOf("a"), CSV.parseLine("a"))
        assertEquals(listOf("a", ""), CSV.parseLine("a,"))
        assertEquals(listOf("a", "b"), CSV.parseLine("a,b"))
        assertEquals(listOf("a", "b"), CSV.parseLine("a,\"b\""))
        assertEquals(listOf("a", "b"), CSV.parseLine("\"a\",\"b\""))
        assertEquals(listOf("a", "\""), CSV.parseLine("\"a\",\"\"\"\""))
    }

    @Test
    fun testParse() {
        assertEquals(csvStr, CSV.parse(csvStr).toString())
    }

    @Test
    fun testParseGet() {
        val csv = CSV.parse(csvStr)

        assertEquals(listOf("hello", "is"), csv.map { it["a"] })
        assertEquals(listOf("world", "a"), csv.map { it["b"] })
        assertEquals(listOf("this", "test"), csv.map { it["c"] })

        assertEquals(listOf("hello", "is"), csv.map { it[0] })
        assertEquals(listOf("world", "a"), csv.map { it[1] })
        assertEquals(listOf("this", "test"), csv.map { it[2] })
    }
}
