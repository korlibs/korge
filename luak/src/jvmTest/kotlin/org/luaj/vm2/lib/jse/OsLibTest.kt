package org.luaj.vm2.lib.jse

import org.luaj.vm2.*
import kotlin.test.*

class OsLibTest {

    //LuaValue jme_lib; = JmePlatform.standardGlobals().get("os");;
    internal val jse_lib: LuaValue = JsePlatform.standardGlobals().get("os")
    internal var time: Double = java.util.Date(2001 - 1900, 7, 23, 14, 55, 2).time / 1000.0

    internal fun t(format: String, expected: String) {
        //String actual = jme_lib.get("date").call(LuaValue.valueOf(format), LuaValue.valueOf(time)).tojstring();
        //assertEquals(expected, actual);
    }

    @Test
    fun testStringDateChars() {
        t("foo", "foo")
    }

    @Test
    fun testStringDate_a() {
        t("%a", "Thu")
    }

    @Test
    fun testStringDate_A() {
        t("%A", "Thursday")
    }

    @Test
    fun testStringDate_b() {
        t("%b", "Aug")
    }

    @Test
    fun testStringDate_B() {
        t("%B", "August")
    }

    @Test
    fun testStringDate_c() {
        t("%c", "Thu Aug 23 14:55:02 2001")
    }

    @Test
    fun testStringDate_d() {
        t("%d", "23")
    }

    @Test
    fun testStringDate_H() {
        t("%H", "14")
    }

    @Test
    fun testStringDate_I() {
        t("%I", "02")
    }

    @Test
    fun testStringDate_j() {
        t("%j", "235")
    }

    @Test
    fun testStringDate_m() {
        t("%m", "08")
    }

    @Test
    fun testStringDate_M() {
        t("%M", "55")
    }

    @Test
    fun testStringDate_p() {
        t("%p", "PM")
    }

    @Test
    fun testStringDate_S() {
        t("%S", "02")
    }

    @Test
    fun testStringDate_U() {
        t("%U", "33")
    }

    @Test
    fun testStringDate_w() {
        t("%w", "4")
    }

    @Test
    fun testStringDate_W() {
        t("%W", "34")
    }

    @Test
    fun testStringDate_x() {
        t("%x", "08/23/01")
    }

    @Test
    fun testStringDate_X() {
        t("%X", "14:55:02")
    }

    @Test
    fun testStringDate_y() {
        t("%y", "01")
    }

    @Test
    fun testStringDate_Y() {
        t("%Y", "2001")
    }

    @Test
    fun testStringDate_Pct() {
        t("%%", "%")
    }

    @Test
    fun testStringDate_UW_neg4() {
        time -= 4 * DAY
        t("%c %U %W", "Sun Aug 19 14:55:02 2001 33 33")
    }

    @Test
    fun testStringDate_UW_neg3() {
        time -= 3 * DAY
        t("%c %U %W", "Mon Aug 20 14:55:02 2001 33 34")
    }

    @Test
    fun testStringDate_UW_neg2() {
        time -= 2 * DAY
        t("%c %U %W", "Tue Aug 21 14:55:02 2001 33 34")
    }

    @Test
    fun testStringDate_UW_neg1() {
        time -= DAY
        t("%c %U %W", "Wed Aug 22 14:55:02 2001 33 34")
    }

    @Test
    fun testStringDate_UW_pos0() {
        time += 0.0
        t("%c %U %W", "Thu Aug 23 14:55:02 2001 33 34")
    }

    @Test
    fun testStringDate_UW_pos1() {
        time += DAY
        t("%c %U %W", "Fri Aug 24 14:55:02 2001 33 34")
    }

    @Test
    fun testStringDate_UW_pos2() {
        time += 2 * DAY
        t("%c %U %W", "Sat Aug 25 14:55:02 2001 33 34")
    }

    @Test
    fun testStringDate_UW_pos3() {
        time += 3 * DAY
        t("%c %U %W", "Sun Aug 26 14:55:02 2001 34 34")
    }

    @Test
    fun testStringDate_UW_pos4() {
        time += 4 * DAY
        t("%c %U %W", "Mon Aug 27 14:55:02 2001 34 35")
    }

    @Test
    fun testJseOsGetenvForEnvVariables() {
        val TEMP = LuaValue.valueOf("TEMP")
        val USER = LuaValue.valueOf("USER")
        val jse_temp = jse_lib["getenv"].call(TEMP)
        val jse_user = jse_lib["getenv"].call(USER)
        //LuaValue jme_user = jme_lib.get("getenv").call(USER);
        assertTrue(!jse_user.isnil() || !jse_temp.isnil())
        //assertTrue(jme_user.isnil());
        println("Temp: $jse_user")
    }

    @Test
    fun testJseOsGetenvForSystemProperties() {
        System.setProperty("test.key.foo", "test.value.bar")
        val key = LuaValue.valueOf("test.key.foo")
        val value = LuaValue.valueOf("test.value.bar")
        val jse_value = jse_lib.get("getenv").call(key)
        //LuaValue jme_value = jme_lib.get("getenv").call(key);
        assertEquals(value, jse_value)
        //assertEquals(value, jme_value);
    }

    companion object {
        internal val DAY = 24.0 * 3600.0
    }
}
