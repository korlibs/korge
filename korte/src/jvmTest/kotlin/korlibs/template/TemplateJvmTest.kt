package korlibs.template

import korlibs.template.dynamic.*
import java.time.LocalDate
import java.time.Month
import kotlin.test.Test
import kotlin.test.assertEquals

class TemplateJvmTest {
    class GeoLocation {
        @JvmField
        var latitude: Double = 0.0
        @JvmField
        var longitude: Double = 0.0
    }

    @Test
    // https://github.com/korlibs/korte/issues/18
    fun testJvmFieldNotBeingAccepted() = suspendTest {
        val location = GeoLocation()
        location.latitude = 1.0
        location.longitude = 2.0
        assertEquals(
            "1,2",
            Template("{{ location.latitude }},{{ location.longitude }}")("location" to location)
        )
    }

    @Test
    // https://github.com/korlibs/korte/issues/20
    fun testJvmLocalDate() = suspendTest {
        val startDate = LocalDate.of(2021, Month.NOVEMBER, 7)
        val days = 3

        data class Entry(val title: String, val date: LocalDate)
        val rows = (1..10).map { Entry(title = "test$it", date = LocalDate.of(2021, Month.NOVEMBER, 5 + it)) }

        assertEquals(
            "test2,test3,test4,",
            Template("{% for row in rows %}{% if row.date >= startDate && row.date < startDate.plusDays(days) %}{{ row.title }},{% endif %}{% endfor %}")("startDate" to startDate, "days" to days, "rows" to rows)
        )
    }

    @Suppress("unused")
    class Getter {
        @JvmField var a: Int? = 10
        @JvmField var b: Int? = 10
        var c: Int? = 10
        fun getA(): Int? = null
        fun getB(): Int? = 20
    }

    @Suppress("unused")
    class GetterWithMap : LinkedHashMap<String, Any?>() {
        @JvmField var a: Int? = 10
        @JvmField var b: Int? = 10
        var c: Int? = 10
        fun getA(): Int? = null
        fun getB(): Int? = 20
    }

    @Test
    fun testGetter() = suspendTest {
        assertEquals(
            ",20,10",
            Template("{{ data.a }},{{ data.b }},{{ data.c }}")("data" to Getter())
        )
    }

    @Test
    fun testGetterCustomMapper() = suspendTest {
        val getter = GetterWithMap().also { it["a"] = "A"; it["b"] = "B" }
        assertEquals(
            "A,B,",
            Template("{{ data.a }},{{ data.b }},{{ data.c }}")("data" to getter)
        )
        assertEquals(
            "A,20,10",
            Template("{{ data.a }},{{ data.b }},{{ data.c }}")("data" to getter, mapper = object : ObjectMapper2 by Mapper2 {
                override suspend fun accessAny(instance: Any?, key: Any?): Any? {
                    return super.accessAnyObject(instance, key) ?: super.accessAny(instance, key)
                }
            })
        )
    }
}
