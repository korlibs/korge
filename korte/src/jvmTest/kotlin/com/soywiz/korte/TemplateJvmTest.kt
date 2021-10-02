package com.soywiz.korte

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
}
