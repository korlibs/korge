package com.soywiz.klock

import kotlin.test.*

class IssuesTest {
    @Test
    fun testIssue73() {
        val dateFormat = DateFormat("yyyy-mm-dd HH:mm z")

        fun tparse(str: String) = dateFormat.parse(str).local.toString("hh:mm a")

        assertEquals("11:10 am", tparse("2019-10-17 11:10 +12")) // Gives 11:10 am
        assertEquals("12:10 pm", tparse("2019-10-17 12:10 +12")) // Gives 00:10 pm, I believe it should be 12:10 pm
        assertEquals("01:10 pm", tparse("2019-10-17 13:10 +12")) // Gives 01:10 pm
    }

    @Test
    fun testIssue81() {
        val format = DateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSSS")
        val dateStr = "2019-04-26T19:00:00.0000000"
        assertEquals(dateStr, format.format(format.parse(dateStr)))
    }
}
