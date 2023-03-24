package korlibs.math.geom

import kotlin.test.Test
import kotlin.test.assertEquals

class SizeIntTest {
    @Test
    fun cover() {
        assertEquals(
            MSizeInt(100, 400),
            MSizeInt(50, 200).applyScaleMode(container = MSizeInt(100, 100), mode = ScaleMode.COVER)
        )
        assertEquals(
            MSizeInt(25, 100),
            MSizeInt(50, 200).applyScaleMode(container = MSizeInt(25, 25), mode = ScaleMode.COVER)
        )
    }
}
