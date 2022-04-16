package com.soywiz.korge.baseview

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails

internal class BaseViewTest {
    data class TestDataClass(
        val a: Int,
        val b: Double
    )

    @Test
    fun getPropWorks() {
        val view = BaseView()

        view.addProp("str", "string")
        view.addProp("int", 123)
        view.addProp("double", 0.123)
        view.addProp("data", TestDataClass(11, 22.0))

        assertEquals(view.getProp<String>("str"), "string")
        assertEquals(view.getProp<Int>("int"), 123)
        assertEquals(view.getProp<Double>("double"), 0.123)
        assertEquals(view.getProp<TestDataClass>("data"), TestDataClass(11, 22.0))
    }

    @Test
    fun getPropCrashesIfTypeCastFails() {
        val view = BaseView()

        view.addProp("str", "string")

        assertFails {
            view.getProp<Int>("str")
        }
    }

    @Test
    fun getPropCrashesIfKeyNotFound() {
        val view = BaseView()

        assertFails {
            view.getProp<Int>("str")
        }
    }

    @Test
    fun getPropOrNullWorks() {
        val view = BaseView()

        view.addProp("str", "string")
        view.addProp("int", 123)
        view.addProp("double", 0.123)
        view.addProp("data", TestDataClass(11, 22.0))

        assertEquals(view.getPropOrNull<String>("str"), "string")
        assertEquals(view.getPropOrNull<Int>("int"), 123)
        assertEquals(view.getPropOrNull<Double>("double"), 0.123)
        assertEquals(view.getPropOrNull<TestDataClass>("data"), TestDataClass(11, 22.0))
        assertEquals(view.getPropOrNull<TestDataClass>("no_key"), null)
    }

    @Test
    fun getPropOrNullCrashesIfTypeCastFails() {
        val view = BaseView()

        view.addProp("str", "string")

        assertFails {
            view.getPropOrNull<Int>("str")
        }
    }
}
