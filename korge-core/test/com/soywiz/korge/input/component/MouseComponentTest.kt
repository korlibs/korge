package com.soywiz.korge.input.component

import com.soywiz.korge.ViewsForTesting
import com.soywiz.korge.render.Texture
import com.soywiz.korge.view.image
import com.soywiz.korio.async.syncTest
import org.junit.Assert
import org.junit.Test

class MouseComponentTest : ViewsForTesting() {
    @Test
    fun name() = syncTest {
        val log = arrayListOf<String>()
        val tex = Texture(ag.createTexture(), 16, 16)
        val image = views.image(tex)
        views.root += image

        image.onOver { log += "onOver" }
        image.onOut { log += "onOut" }


        input.mouse.setTo(8.0, 8.0); views.update(0); this.step(0)
        Assert.assertEquals("onOver", log.joinToString(","))
        input.mouse.setTo(20.0, 20.0); views.update(0); this.step(0)
        Assert.assertEquals("onOver,onOut", log.joinToString(","))
    }
}