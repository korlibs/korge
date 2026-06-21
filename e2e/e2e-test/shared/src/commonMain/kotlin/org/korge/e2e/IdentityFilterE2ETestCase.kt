package org.korge.e2e

import korlibs.image.bitmap.Bitmap32
import korlibs.image.color.Colors
import korlibs.korge.view.Container
import korlibs.korge.view.filter.IdentityFilter
import korlibs.korge.view.filter.filters
import korlibs.korge.view.image
import korlibs.korge.view.solidRect
import korlibs.korge.view.xy

object IdentityFilterE2ETestCase : E2ETestCase() {
    override val pixelPerfect: Boolean = true

    override suspend fun Container.run() {
        solidRect(768, 512, Colors.CYAN)
        image(Bitmap32.Companion(766, 510, Colors.MAGENTA.premultiplied)).xy(1, 1).filters(IdentityFilter.Companion)
    }
}
