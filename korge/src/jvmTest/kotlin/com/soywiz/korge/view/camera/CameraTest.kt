package com.soywiz.korge.view.camera

import com.soywiz.klock.*
import com.soywiz.korge.*
import com.soywiz.korge.time.*
import com.soywiz.korge.tween.*
import com.soywiz.korge.view.*
import com.soywiz.korgw.*
import com.soywiz.korim.color.*
import com.soywiz.korma.geom.*
import kotlinx.coroutines.*

fun main(): Unit = runBlocking { korge() }

//TODO: Move completed sample to korge-samples
suspend fun korge() = Korge(quality = GameWindow.Quality.PERFORMANCE, title = "Camera test", bgcolor = Colors.WHITE) {

    fun elements() = Container().apply {
        solidRect(400.0, 400.0, Colors.YELLOW)
        solidRect(350.0, 350.0, Colors.YELLOWGREEN)
        solidRect(300.0, 300.0, Colors.GREENYELLOW)
        solidRect(250.0, 250.0, Colors.GREEN)
        polygon(50.0, 7, Colors.DARKGREY).position(200.0, 100.0)
    }

    val cam = Camera(0.0, 0.0, 400.0, 400.0)
    val cam2 = cam.copy()
    val container = cameraContainer(400.0, 400.0, decoration = { solidRect(width, height, Colors.PINK) }) {
        addChild(elements())
    }
    container.camera = cam

    val container2 = cameraContainer(800.0, 800.0, decoration = { position(500.0, 0.0); solidRect(width, height, Colors.PINK) }) {
        addChild(elements())
    }
    container2.camera.zoom = 0.5

    delay(1.seconds)
    cam.x = -50.0
    delay(1.seconds)
    cam.x = 0.0

    delay(1.seconds)
    cam.size(350.0, 350.0)
    delay(1.seconds)
    cam.size(300.0, 300.0)
    delay(1.seconds)
    cam.size(250.0, 250.0)
    delay(1.seconds)
    cam.setTo(cam2)
    delay(1.seconds)

    cam.moveTo(10.0, 10.0, time = 1.seconds)
    cam.moveTo(50.0, 50.0, time = 1.seconds)
    cam.moveTo(0.0, 0.0, time = 1.seconds)

    cam.resizeTo(350.0, 300.0, 1.seconds)
    cam.resizeTo(300.0, 200.0, 1.seconds)
    cam.resizeTo(250.0, 100.0, 1.seconds)

    cam.anchor(0.0, 0.0)
    cam.zoom(0.25, time = 2.seconds)

    cam.tweenTo(cam2, time = 2.seconds)

    cam.rotate(90.degrees, time = 1.seconds)
    cam.rotate((-90).degrees, time = 1.seconds)

    val actor = solidRect(50.0, 50.0, Colors.RED)
    container.content.addChild(actor)
    actor.centerOn(container.content)
    delay(1.seconds)
    cam.zoom(2.0, 1.seconds)
    delay(1.seconds)

    cam.follow(actor, 20.0)
    actor.moveBy(150.0, 150.0, 3.seconds)
    actor.moveBy(-100.0, -100.0, 2.seconds)
    cam.unfollow()
    actor.moveBy(100.0, 100.0, 1.seconds)
}
