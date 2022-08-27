package com.soywiz.korge.view

import com.soywiz.klock.milliseconds
import com.soywiz.korge.view.animation.ImageAnimationView
import com.soywiz.korim.bitmap.Bitmaps
import com.soywiz.korim.format.ImageAnimation
import com.soywiz.korim.format.ImageFrame
import com.soywiz.korim.format.ImageFrameLayer
import com.soywiz.korim.format.ImageLayer
import kotlin.test.Test
import kotlin.test.assertEquals

class ImageAnimationViewTest {
    private val animImages4Layers1 = ImageAnimation(
        frames = listOf(
            ImageFrame(0, 60.milliseconds, listOf(ImageFrameLayer(
                ImageLayer(0, "layer0"),
                Bitmaps.transparent,
                1
            ))),
            ImageFrame(1, 60.milliseconds, listOf(ImageFrameLayer(
                ImageLayer(0, "layer0"),
                Bitmaps.transparent,
                2
            ))),
            ImageFrame(2, 60.milliseconds, listOf(ImageFrameLayer(
                ImageLayer(0, "layer0"),
                Bitmaps.transparent,
                3
            ))),
            ImageFrame(3, 60.milliseconds, listOf(ImageFrameLayer(
                ImageLayer(0, "layer0"),
                Bitmaps.transparent,
                4
            )))
        ),
        direction = ImageAnimation.Direction.FORWARD,
        name = "anim1",
        layers = listOf(ImageLayer(0, "layer0"))
    )
    private val animImages6Layers2 = ImageAnimation(
        frames = listOf(
            ImageFrame(10, 60.milliseconds, listOf(ImageFrameLayer(
                ImageLayer(0, "layer0"),
                Bitmaps.transparent,
                1
            ), ImageFrameLayer(ImageLayer(1, "layer1"), Bitmaps.transparent, 10))),
            ImageFrame(12, 60.milliseconds, listOf(ImageFrameLayer(
                ImageLayer(0, "layer0"),
                Bitmaps.transparent,
                2
            ), ImageFrameLayer(ImageLayer(1, "layer1"), Bitmaps.transparent, 20))),
            ImageFrame(11, 60.milliseconds, listOf(ImageFrameLayer(
                ImageLayer(0, "layer0"),
                Bitmaps.transparent,
                3
            ), ImageFrameLayer(ImageLayer(1, "layer1"), Bitmaps.transparent, 30))),
            ImageFrame(13, 60.milliseconds, listOf(ImageFrameLayer(
                ImageLayer(0, "layer0"),
                Bitmaps.transparent,
                4
            ), ImageFrameLayer(ImageLayer(1, "layer1"), Bitmaps.transparent, 40))),
            ImageFrame(14, 60.milliseconds, listOf(ImageFrameLayer(
                ImageLayer(0, "layer0"),
                Bitmaps.transparent,
                5
            ), ImageFrameLayer(ImageLayer(1, "layer1"), Bitmaps.transparent, 50))),
            ImageFrame(15, 60.milliseconds, listOf(ImageFrameLayer(
                ImageLayer(0, "layer0"),
                Bitmaps.transparent,
                6
            ), ImageFrameLayer(ImageLayer(1, "layer1"), Bitmaps.transparent, 60)))
        ),
        direction = ImageAnimation.Direction.REVERSE,
        name = "anim2",
        layers = listOf(ImageLayer(0, "layer0"), ImageLayer(1, "layer1"))
    )
    private val anim = ImageAnimationView { Image(Bitmaps.transparent) }

    @Test
    fun testNumberOfChildren() {
        // Test if correct number of layers are added as children to the Container
        anim.animation = animImages4Layers1
        assertEquals(1, anim.children.size)
        anim.animation = animImages6Layers2
        assertEquals(2, anim.children.size)
    }

    @Test
    fun testSetFirstFrame() {
        // Test if the correct frame is set as first frame
        // Here we check if the corresponding targetX value was set as X position of the layer.
        anim.animation = animImages4Layers1
        assertEquals(1, anim.children[0].x.toInt())
        anim.direction = ImageAnimation.Direction.REVERSE
        anim.rewind()
        assertEquals(4, anim.children[0].x.toInt())
        anim.animation = animImages6Layers2
        assertEquals(6, anim.children[0].x.toInt())
        assertEquals(60, anim.children[1].x.toInt())
    }

//        println("children: ${imageanimView.children.size}")

}
