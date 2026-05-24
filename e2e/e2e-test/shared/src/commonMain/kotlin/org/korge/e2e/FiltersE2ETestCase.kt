package org.korge.e2e

import korlibs.image.format.readBitmap
import korlibs.io.file.std.resourcesVfs
import korlibs.korge.view.Container
import korlibs.korge.view.filter.Convolute3Filter
import korlibs.korge.view.filter.PageFilter
import korlibs.korge.view.filter.SwizzleColorsFilter
import korlibs.korge.view.filter.addFilter
import korlibs.korge.view.image
import korlibs.korge.view.position
import korlibs.korge.view.scale
import korlibs.math.interpolation.Ratio

object FiltersE2ETestCase : E2ETestCase() {
    override val scale: Double = 0.25

    override suspend fun Container.run() {
        println("LOADING IMAGE...")
        val bitmap = resourcesVfs["gfx/korge/korge.png"].readBitmap()
        println("PREPARING VIEWS...")
        image(bitmap).scale(.5).position(0, 256).addFilter(PageFilter(hratio = Ratio.HALF, hamplitude1 = 20.0))
        image(bitmap).scale(.5).position(256, 256).addFilter(Convolute3Filter(Convolute3Filter.KERNEL_SHARPEN))
        image(bitmap).scale(.5).position(512, 256).addFilter(SwizzleColorsFilter("bgga"))
        println("VIEWS PREPARED")
    }
}
