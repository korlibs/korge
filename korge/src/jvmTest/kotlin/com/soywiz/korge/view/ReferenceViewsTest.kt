package com.soywiz.korge.view

import com.soywiz.korge.test.*
import com.soywiz.korge.tests.*
import com.soywiz.korma.geom.*
import org.junit.*

class ReferenceViewsTest : ViewsForTesting(
    windowSize = SizeInt(200, 200),
    virtualSize = SizeInt(100, 100),
    log = true,
) {
    @Test
    fun testClippedContainerInFlippedContainerInTexture() = viewsTest {
        val container = Container().apply {
            y = views.virtualHeightDouble; scaleY = -1.0
            clipContainer(150, 100) {
                xy(75, 50)
                solidRect(300, 400)
            }
        }
        addChild(container)
        delayFrame()
        logAg.log.add("---------")
        container.unsafeRenderToBitmapSync(views.renderContext)
        assertEqualsFileReference(
            "korge/render/ClippedContainerInFlippedContainerInTexture.log",
            listOf(
                logAg.getLogAsString(),
            ).joinToString("\n")
        )
    }
}
