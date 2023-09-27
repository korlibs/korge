package korlibs.math.segment

import korlibs.math.annotations.KormaExperimental
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(KormaExperimental::class)
class IntSegmentSetTest {
    val segment0 get() = IntSegmentSet().clone()
    val segment1 get() = IntSegmentSet().addUnsafe(0, 10).clone()
    val segment2 get() = IntSegmentSet().addUnsafe(0, 10).addUnsafe(12, 20).clone()
    val segment3 get() = IntSegmentSet().addUnsafe(0, 10).addUnsafe(20, 30).addUnsafe(50, 60).clone()
    val segment4 get() = IntSegmentSet().addUnsafe(0, 10).addUnsafe(20, 30).addUnsafe(50, 60).addUnsafe(70, 80).clone()
    val segment16a get() = IntSegmentSet().apply { for (n in 0 until 16) addUnsafe(n * 20, n * 20 + 10) }
    val segment16b get() = IntSegmentSet().apply { for (n in 0 until 16) addUnsafe(n * 20 + 5, n * 20 + 15) }
    val segment16c get() = IntSegmentSet().apply { for (n in 0 until 16) addUnsafe(n * 20 + 10, n * 20 + 20) }

    val IntSegmentSet.str get() = this.toString()

    @Test
    fun testAdd() {
        assertEquals("[]", segment0.str)
        assertEquals("[0-10]", segment1.str)
        assertEquals("[0-10, 11-20]", segment1.add(11, 20).str)
        assertEquals("[0-20]", segment1.add(10, 20).str)
        assertEquals("[0-20]", segment1.add(11, 20).add(10, 11).str)
        assertEquals("[0-10, 11-19, 20-30]", segment1.add(20, 30).add(11, 19).str)
    }

    @Test
    fun testHolesAdd3() {
        assertEquals("[-10--5, 0-10, 20-30, 50-60]", segment3.add(-10, -5).str)
        assertEquals("[0-10, 12-18, 20-30, 50-60]", segment3.add(12, 18).str)
        assertEquals("[0-10, 20-30, 32-48, 50-60]", segment3.add(32, 48).str)
        assertEquals("[0-10, 20-30, 50-60, 62-70]", segment3.add(62, 70).str)
    }

    @Test
    fun testHolesAdd4() {
        assertEquals("[-10--5, 0-10, 20-30, 50-60, 70-80]", segment4.add(-10, -5).str)
        assertEquals("[0-10, 12-18, 20-30, 50-60, 70-80]", segment4.add(12, 18).str)
        assertEquals("[0-10, 20-30, 32-48, 50-60, 70-80]", segment4.add(32, 48).str)
        assertEquals("[0-10, 20-30, 50-60, 62-68, 70-80]", segment4.add(62, 68).str)
        assertEquals("[0-10, 20-30, 50-60, 70-80, 82-90]", segment4.add(82, 90).str)
    }

    @Test
    fun testMiddleCombineAdd3() {
        assertEquals("[0-15, 20-30, 50-60]", segment3.add(5, 15).str)
        assertEquals("[0-10, 15-30, 50-60]", segment3.add(15, 25).str)
        assertEquals("[0-10, 20-30, 50-60]", segment3.add(20, 30).str)
        assertEquals("[0-10, 20-48, 50-60]", segment3.add(25, 48).str)
        assertEquals("[0-10, 20-30, 35-60]", segment3.add(35, 50).str)
        assertEquals("[0-10, 20-30, 35-60]", segment3.add(35, 55).str)
        assertEquals("[0-30, 50-60]", segment3.add(5, 20).str)
        assertEquals("[0-30, 50-60]", segment3.add(5, 25).str)
        assertEquals("[0-10, 15-60]", segment3.add(15, 50).str)
        assertEquals("[0-10, 20-60]", segment3.add(20, 50).str)
        assertEquals("[0-10, 20-60]", segment3.add(25, 50).str)
        assertEquals("[0-10, 20-60]", segment3.add(30, 50).str)
        assertEquals("[0-60]", segment3.add(0, 50).str)
        assertEquals("[0-60]", segment3.add(5, 50).str)
        assertEquals("[0-60]", segment3.add(10, 50).str)
        assertEquals("[0-60]", segment3.add(5, 55).str)
        assertEquals("[0-60]", segment3.add(10, 55).str)
        assertEquals("[0-60]", segment3.add(10, 60).str)
        assertEquals("[-10-70]", segment3.add(-10, 70).str)
    }

    @Test
    fun testMiddleCombineAdd4() {
        assertEquals("[0-10, 20-30, 50-60, 70-80]", segment4.add(0, 10).str)
        assertEquals("[0-15, 20-30, 50-60, 70-80]", segment4.add(5, 15).str)
        assertEquals("[0-10, 15-30, 50-60, 70-80]", segment4.add(15, 20).str)
        assertEquals("[0-10, 15-30, 50-60, 70-80]", segment4.add(15, 25).str)
        assertEquals("[0-10, 20-30, 45-60, 70-80]", segment4.add(45, 50).str)
        assertEquals("[0-10, 20-30, 45-60, 70-80]", segment4.add(45, 60).str)
        assertEquals("[0-10, 20-30, 50-60, 65-80]", segment4.add(65, 70).str)
        assertEquals("[0-30, 50-60, 70-80]", segment4.add(10, 20).str)
        assertEquals("[0-30, 50-60, 70-80]", segment4.add(5, 20).str)
        assertEquals("[0-30, 50-60, 70-80]", segment4.add(5, 25).str)
        assertEquals("[0-35, 50-60, 70-80]", segment4.add(5, 35).str)
        assertEquals("[0-60, 70-80]", segment4.add(5, 50).str)
        assertEquals("[0-60, 70-80]", segment4.add(5, 55).str)
        assertEquals("[0-60, 70-80]", segment4.add(5, 60).str)
        assertEquals("[0-65, 70-80]", segment4.add(5, 65).str)
        assertEquals("[0-80]", segment4.add(5, 70).str)
        assertEquals("[0-80]", segment4.add(5, 75).str)
        assertEquals("[0-80]", segment4.add(5, 80).str)
        assertEquals("[0-10, 20-35, 50-60, 70-80]", segment4.add(25, 35).str)
        assertEquals("[0-10, 20-60, 70-80]", segment4.add(25, 50).str)
        assertEquals("[0-10, 20-60, 70-80]", segment4.add(25, 55).str)
        assertEquals("[0-10, 20-60, 70-80]", segment4.add(25, 60).str)
        assertEquals("[0-10, 20-65, 70-80]", segment4.add(25, 65).str)
        assertEquals("[0-10, 20-80]", segment4.add(25, 70).str)
        assertEquals("[0-10, 20-80]", segment4.add(25, 75).str)
        assertEquals("[0-10, 20-80]", segment4.add(25, 80).str)
        assertEquals("[0-10, 20-85]", segment4.add(25, 85).str)
        assertEquals("[0-10, 20-30, 50-60, 70-80]", segment4.add(50, 60).str)
        assertEquals("[0-10, 20-30, 50-65, 70-80]", segment4.add(50, 65).str)
        assertEquals("[0-10, 20-30, 50-80]", segment4.add(50, 70).str)
        assertEquals("[0-10, 20-30, 50-80]", segment4.add(50, 75).str)
        assertEquals("[0-10, 20-30, 50-80]", segment4.add(50, 80).str)
        assertEquals("[0-10, 20-30, 50-85]", segment4.add(50, 85).str)
    }

    @Test
    fun testLeftAdd3() {
        assertEquals("[-10-10, 20-30, 50-60]", segment3.add(-10, +5).str)
        assertEquals("[-10-30, 50-60]", segment3.add(-10, +25).str)
        assertEquals("[-10-45, 50-60]", segment3.add(-10, +45).str)
        assertEquals("[-10-60]", segment3.add(-10, +50).str)
        assertEquals("[-10-60]", segment3.add(-10, +55).str)
        assertEquals("[-10-70]", segment3.add(-10, +70).str)
    }

    @Test
    fun testLeftAdd4() {
        assertEquals("[-20--10, 0-10, 20-30, 50-60, 70-80]", segment4.add(-20, -10).str)
        assertEquals("[-10-10, 20-30, 50-60, 70-80]", segment4.add(-10, +5).str)
        assertEquals("[-10-30, 50-60, 70-80]", segment4.add(-10, +25).str)
        assertEquals("[-10-45, 50-60, 70-80]", segment4.add(-10, +45).str)
        assertEquals("[-10-60, 70-80]", segment4.add(-10, +50).str)
        assertEquals("[-10-60, 70-80]", segment4.add(-10, +55).str)
        assertEquals("[-10-80]", segment4.add(-10, +70).str)
        assertEquals("[-10-80]", segment4.add(-10, +75).str)
        assertEquals("[-10-90]", segment4.add(-10, +90).str)
    }

    @Test
    fun testRightAdd3() {
        assertEquals("[0-10, 20-30, 50-60, 75-80]", segment3.add(75, 80).str)
        assertEquals("[0-10, 20-30, 50-80]", segment3.add(60, 80).str)
        assertEquals("[0-10, 20-30, 45-80]", segment3.add(45, 80).str)
        assertEquals("[0-10, 20-80]", segment3.add(30, 80).str)
        assertEquals("[0-10, 20-80]", segment3.add(25, 80).str)
        assertEquals("[0-10, 15-80]", segment3.add(15, 80).str)
        assertEquals("[0-80]", segment3.add(10, 80).str)
        assertEquals("[0-80]", segment3.add(5, 80).str)
        assertEquals("[0-80]", segment3.add(0, 80).str)
        assertEquals("[-5-80]", segment3.add(-5, 80).str)
    }

    @Test
    fun testRightAdd4() {
        assertEquals("[0-10, 20-30, 50-60, 70-80, 85-90]", segment4.add(85, 90).str)
        assertEquals("[0-10, 20-30, 50-60, 70-90]", segment4.add(80, 90).str)
        assertEquals("[0-10, 20-30, 50-60, 70-90]", segment4.add(75, 90).str)
        assertEquals("[0-10, 20-30, 50-60, 70-90]", segment4.add(70, 90).str)
        assertEquals("[0-10, 20-30, 50-60, 65-90]", segment4.add(65, 90).str)
        assertEquals("[0-10, 20-30, 50-90]", segment4.add(60, 90).str)
        assertEquals("[0-10, 20-30, 50-90]", segment4.add(55, 90).str)
        assertEquals("[0-10, 20-30, 50-90]", segment4.add(50, 90).str)
        assertEquals("[0-10, 20-30, 45-90]", segment4.add(45, 90).str)
        assertEquals("[0-10, 20-90]", segment4.add(30, 90).str)
        assertEquals("[0-10, 20-90]", segment4.add(25, 90).str)
        assertEquals("[0-10, 20-90]", segment4.add(20, 90).str)
        assertEquals("[0-10, 15-90]", segment4.add(15, 90).str)
        assertEquals("[0-90]", segment4.add(10, 90).str)
        assertEquals("[0-90]", segment4.add(5, 90).str)
        assertEquals("[0-90]", segment4.add(0, 90).str)
        assertEquals("[-10-90]", segment4.add(-10, 90).str)
    }

    @Test
    fun testAddBigSegments() {
        assertEquals("[0-10, 20-30, 40-50, 60-70, 80-90, 100-110, 120-130, 140-150, 160-170, 180-190, 200-210, 220-230, 240-250, 260-270, 280-290, 300-310]", segment16a.str)
        assertEquals("[5-15, 25-35, 45-55, 65-75, 85-95, 105-115, 125-135, 145-155, 165-175, 185-195, 205-215, 225-235, 245-255, 265-275, 285-295, 305-315]", segment16b.str)
        assertEquals("[10-20, 30-40, 50-60, 70-80, 90-100, 110-120, 130-140, 150-160, 170-180, 190-200, 210-220, 230-240, 250-260, 270-280, 290-300, 310-320]", segment16c.str)
        assertEquals(segment16a.str, segment16a.add(segment16a).str)
        assertEquals("[0-15, 20-35, 40-55, 60-75, 80-95, 100-115, 120-135, 140-155, 160-175, 180-195, 200-215, 220-235, 240-255, 260-275, 280-295, 300-315]", segment16a.add(segment16b).str)
        assertEquals("[0-320]", segment16a.add(segment16c).str)
        assertEquals("[0-320]", segment16a.add(0, 320).str)
        assertEquals("[-10-330]", segment16a.add(-10, 330).str)
        assertEquals("[0-10, 20-290, 300-310]", segment16a.add(25, 285).str)
    }

    fun intersect(a: IntSegmentSet, b: IntSegmentSet) = IntSegmentSet().setToIntersect(a, b)
    fun intersectSlow(a: IntSegmentSet, b: IntSegmentSet) = IntSegmentSet().setToIntersectSlow(a, b)

    @Test
    fun testIntersection() {
        assertEquals("[]", intersect(IntSegmentSet().add(1, 100), IntSegmentSet().add(101, 200)).str)
        assertEquals("[5-10]", intersect(segment1, IntSegmentSet().add(5, 15)).str)
        assertEquals("[5-10, 12-15]", intersect(segment2, IntSegmentSet().add(5, 15)).str)
        assertEquals("[5-10, 25-30, 45-50, 65-70, 85-90, 105-110, 125-130, 145-150, 165-170, 185-190, 205-210, 225-230, 245-250, 265-270, 285-290, 305-310]", intersect(segment16a, segment16b).str)
        assertEquals("[10-10, 20-20, 30-30, 40-40, 50-50, 60-60, 70-70, 80-80, 90-90, 100-100, 110-110, 120-120, 130-130, 140-140, 150-150, 160-160, 170-170, 180-180, 190-190, 200-200, 210-210, 220-220, 230-230, 240-240, 250-250, 260-260, 270-270, 280-280, 290-290, 300-300, 310-310]", intersect(segment16a, segment16c).str)
        assertEquals("[10-15, 30-35, 50-55, 70-75, 90-95, 110-115, 130-135, 150-155, 170-175, 190-195, 210-215, 230-235, 250-255, 270-275, 290-295, 310-315]", intersect(segment16b, segment16c).str)
    }
}
