package com.soywiz.korim.text

import com.soywiz.korim.bitmap.*
import com.soywiz.korim.color.*
import com.soywiz.korim.font.*
import com.soywiz.korim.format.*
import com.soywiz.korio.async.*
import com.soywiz.korma.geom.*
import com.soywiz.korma.geom.vector.*
import kotlin.test.*

class RichTextDataRendererText {
    @Test
    fun test() = suspendTest {
        val nativeImage = NativeImage(512, 512)
        nativeImage.context2d {
            val textBounds = Rectangle(50, 50, 150, 100)
            stroke(Colors.BLUE, lineWidth = 2.0) {
                rect(textBounds)
            }
            drawRichText(
                RichTextData("hello world this is a long test", textSize = 32.0, font = DefaultTtfFont),
                bounds = textBounds,
                ellipsis = "...",
                fill = Colors.RED,
                //align = TextAlignment.RIGHT,
                //align = TextAlignment.CENTER,
                align = TextAlignment.JUSTIFIED,
            )
        }
        //nativeImage.showImageAndWait()
    }
}
