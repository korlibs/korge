package korlibs.math.geom.collider

import korlibs.math.geom.*
import kotlin.test.*

class HitTestDirectionTest {
    @Test
    fun test() {
        assertEquals(HitTestDirection.RIGHT, HitTestDirection.fromAngle(0.degrees))
        assertEquals(HitTestDirection.DOWN, HitTestDirection.fromAngle(90.degrees))
        assertEquals(HitTestDirection.LEFT, HitTestDirection.fromAngle(180.degrees))
        assertEquals(HitTestDirection.UP, HitTestDirection.fromAngle(270.degrees))

        //println("x=${0.degrees.cosine}, y=${0.degrees.sine}")
        //println("x=${90.degrees.cosine}, y=${90.degrees.sine}")
    }

    @Test
    fun testFlagsFromString() {
        assertEquals(HitTestDirectionFlags.ALL, HitTestDirectionFlags.fromString(null))
        assertEquals(HitTestDirectionFlags.ALL, HitTestDirectionFlags.fromString(""))
        assertEquals(HitTestDirectionFlags.ALL, HitTestDirectionFlags.fromString("collision"))
        assertEquals(HitTestDirectionFlags.NONE, HitTestDirectionFlags.fromString("other"))

        assertEquals(HitTestDirectionFlags.NONE, HitTestDirectionFlags.fromString(null, HitTestDirectionFlags.NONE))
        assertEquals(HitTestDirectionFlags.NONE, HitTestDirectionFlags.fromString("", HitTestDirectionFlags.NONE))
        assertEquals(HitTestDirectionFlags.ALL, HitTestDirectionFlags.fromString("collision", HitTestDirectionFlags.NONE))
        assertEquals(HitTestDirectionFlags.NONE, HitTestDirectionFlags.fromString("other", HitTestDirectionFlags.NONE))

        assertEquals(HitTestDirectionFlags(up = true, right = false, down = false, left = false), HitTestDirectionFlags.fromString("collision_up"))
        assertEquals(HitTestDirectionFlags(up = false, right = true, down = false, left = false), HitTestDirectionFlags.fromString("collision_right"))
        assertEquals(HitTestDirectionFlags(up = false, right = false, down = true, left = false), HitTestDirectionFlags.fromString("collision_down"))
        assertEquals(HitTestDirectionFlags(up = false, right = false, down = false, left = true), HitTestDirectionFlags.fromString("collision_left"))
        assertEquals(HitTestDirectionFlags(up = true, right = false, down = false, left = true), HitTestDirectionFlags.fromString("collision_up_left"))
        assertEquals(HitTestDirectionFlags(up = true, right = false, down = true, left = true), HitTestDirectionFlags.fromString("collision_up_left_down"))
        assertEquals(HitTestDirectionFlags.NONE, HitTestDirectionFlags.fromString("other_up"))

        //println("x=${0.degrees.cosine}, y=${0.degrees.sine}")
        //println("x=${90.degrees.cosine}, y=${90.degrees.sine}")
    }
}
