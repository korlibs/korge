package korlibs.korge.view.fast

import korlibs.korge.test.*
import korlibs.korge.testing.*
import korlibs.korge.tests.*
import korlibs.korge.view.*
import korlibs.image.bitmap.*
import korlibs.image.color.*
import korlibs.math.geom.*
import org.junit.*
import org.junit.Test
import kotlin.test.*

class MultiTextureFastRender : ViewsForTesting(log = true) {
    @Test
    @Suppress("UNUSED_CHANGED_VALUE")
    fun test() = korgeScreenshotTest(SizeInt(180, 180)) {
        val tex0 = Bitmap32(16, 16) { x, y -> Colors.RED }.premultipliedIfRequired()
        val tex1 = Bitmap32(16, 16) { x, y -> Colors.GREEN }.premultipliedIfRequired()
        val tex2 = Bitmap32(16, 16) { x, y -> Colors.BLUE }.premultipliedIfRequired()
        val tex3 = Bitmap32(16, 16) { x, y -> Colors.YELLOW }.premultipliedIfRequired()
        val tex4 = Bitmap32(16, 16) { x, y -> Colors.PINK }.premultipliedIfRequired()
        var n = 0
        image(tex0).xy(16 * n, 16 * n); n++
        image(tex1).xy(16 * n, 16 * n); n++
        image(tex2).xy(16 * n, 16 * n); n++
        image(tex3).xy(16 * n, 16 * n); n++
        image(tex0).xy(16 * n, 16 * n); n++
        image(tex2).xy(16 * n, 16 * n); n++
        image(tex1).xy(16 * n, 16 * n); n++
        image(tex3).xy(16 * n, 16 * n); n++
        image(tex4).xy(16 * n, 16 * n); n++
        image(tex4).xy(16 * n, 16 * n); n++
        val batch = views.renderContext.batch
        assertScreenshot()
        assertEquals("2/1", "${batch.batchCount}/${batch.fullBatchCount}")
    }
}
