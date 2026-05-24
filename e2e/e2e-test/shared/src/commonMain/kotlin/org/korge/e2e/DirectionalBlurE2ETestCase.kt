package org.korge.e2e

import dockedTo
import korlibs.image.color.Colors
import korlibs.korge.view.Container
import korlibs.korge.view.centered
import korlibs.korge.view.circle
import korlibs.korge.view.filter.DirectionalBlurFilter
import korlibs.korge.view.filter.filters
import korlibs.korge.view.solidRect
import korlibs.math.geom.Anchor
import korlibs.math.geom.ScaleMode
import korlibs.math.geom.degrees

object DirectionalBlurE2ETestCase : E2ETestCase() {
    override suspend fun Container.run() {
        solidRect(size, Colors.WHITE)
        circle(32f, Colors.RED)
            .centered
            .dockedTo(Anchor.CENTER, ScaleMode.NO_SCALE)
            .filters(DirectionalBlurFilter.Companion(angle = 0.degrees, radius = 16f, expandBorder = true))
    }
}
