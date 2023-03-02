package com.soywiz.korge.view.fast

import com.soywiz.korge.test.*
import com.soywiz.korge.testing.*
import com.soywiz.korge.tests.*
import com.soywiz.korge.view.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korim.color.*
import com.soywiz.korma.geom.*
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
