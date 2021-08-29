package com.soywiz.klock

import com.soywiz.klock.js.*
import kotlin.js.Date
import kotlin.test.Test
import kotlin.test.assertEquals

class DateExtTest {
    @Test
    fun test() {
        assertEquals("Thu, 20 Dec 2018 12:06:35 GMT", DateTime(1545307595729L).toDate().toUTCString())
        assertEquals("Thu, 20 Dec 2018 12:06:35 UTC", Date(1545307595729.0).toDateTime().toStringDefault())
    }
}
