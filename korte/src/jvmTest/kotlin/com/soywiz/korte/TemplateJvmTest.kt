package com.soywiz.korte

import java.time.*
import kotlin.test.*

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
}
