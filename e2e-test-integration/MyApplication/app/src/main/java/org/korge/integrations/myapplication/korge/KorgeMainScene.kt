package org.korge.integrations.myapplication.korge

import korlibs.image.color.*
import korlibs.korge.scene.*
import korlibs.korge.tween.*
import korlibs.korge.view.*
import korlibs.math.geom.*
import korlibs.math.interpolation.*
import korlibs.time.*

class KorgeMainScene : Scene() {
    override suspend fun SContainer.sceneMain() {
        val minDegrees = (-16).degrees
        val maxDegrees = (+16).degrees

        //val image = image(resourcesVfs["korge.png"].readBitmap()) {
        val image = solidRect(100, 100, Colors.RED).also {
            it.rotation = maxDegrees
            it.anchor(.5, .5)
            it.scale(.8)
            it.position(400, 300)
        }

        while (true) {
            image.tween(image::rotation[minDegrees], time = 1.seconds, easing = Easing.EASE_IN_OUT)
            image.tween(image::rotation[maxDegrees], time = 1.seconds, easing = Easing.EASE_IN_OUT)
        }
    }
}
