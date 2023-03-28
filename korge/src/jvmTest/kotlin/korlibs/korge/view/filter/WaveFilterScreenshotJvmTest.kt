package korlibs.korge.view.filter

import korlibs.image.color.*
import korlibs.korge.testing.*
import korlibs.korge.view.*
import korlibs.math.geom.*
import org.junit.*

class WaveFilterScreenshotJvmTest {
    @Test
    fun test() = korgeScreenshotTest(SizeInt(150, 150)) {
        solidRect(50, 50, Colors.GREEN).xy(50, 50)
            .filters(WaveFilter(amplitudeX = 15, amplitudeY = 10, crestDistanceX = 25.0, crestDistanceY = 10.0))

        assertScreenshot()
    }
}
