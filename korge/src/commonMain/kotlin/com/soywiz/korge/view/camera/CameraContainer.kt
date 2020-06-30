package com.soywiz.korge.view.camera

import com.soywiz.klock.*
import com.soywiz.korge.tween.*
import com.soywiz.korge.view.*
import com.soywiz.korma.geom.*
import com.soywiz.korma.interpolation.*

inline fun Container.cameraContainer(
    width: Double,
    height: Double,
    camera: Camera = Camera(0.0, 0.0, width, height),
    noinline decoration: @ViewsDslMarker CameraContainer.() -> Unit = {},
    content: @ViewsDslMarker Container.() -> Unit = {}
) = CameraContainer(width, height, camera, decoration).addTo(this).also { content(it.content) }

class CameraContainer(
    override var width: Double = 100.0,
    override var height: Double = 100.0,
    camera: Camera = Camera(0.0, 0.0, width, height),
    decoration: @ViewsDslMarker CameraContainer.() -> Unit = {}
) : FixedSizeContainer(width, height, true), View.Reference {

    private val contentContainer = Container()

    val content: Container = object : Container(), Reference {
        override fun getLocalBoundsInternal(out: Rectangle) {
            out.setTo(0, 0, this@CameraContainer.width, this@CameraContainer.height)
        }
    }

    var camera: Camera = camera
        set(newCamera) {
            if (newCamera.container != null) throw IllegalStateException("You can't use Camera that is already attached")
            camera.detach()
            newCamera.attach(this)
            setTo(newCamera)
            field = newCamera
        }

    init {
        decoration(this)
        contentContainer.addTo(this)
        content.addTo(contentContainer)
    }

    inline fun updateContent(action: Container.() -> Unit) {
        content.action()
    }

    /*private fun getLocalMatrixFor(camera: Camera): Matrix {
        val mat = globalMatrix.clone()
        val zoom = camera.zoom
        val x = camera.x + camera.width * (zoom - 1) / zoom * camera.anchorX
        val y = camera.y + camera.height * (zoom - 1) / zoom * camera.anchorY
        val centerX = camera.width * camera.anchorX / camera.zoom
        val centerY = camera.height * camera.anchorY / camera.zoom
        mat.translate(-x, -y)
        mat.scale(
            this.width / camera.width * zoom,
            this.height / camera.height * zoom
        )
        mat.translate(centerX, centerY)
        mat.rotate(camera.angle)
        return mat
    }*/

    internal fun setTo(camera: Camera) {
        val centerX = camera.width * camera.anchorX / camera.zoom
        val centerY = camera.height * camera.anchorY / camera.zoom
        content.x = -camera.x - centerX
        content.y = -camera.y - centerY
        content.scaleX = content.width / camera.width * camera.zoom
        content.scaleY = content.height / camera.height * camera.zoom
        contentContainer.x = centerX
        contentContainer.y = centerY
        contentContainer.rotation = camera.angle
    }

    internal suspend fun tweenTo(camera: Camera, time: TimeSpan, easing: Easing) {
        val centerX = camera.width * camera.anchorX / camera.zoom
        val centerY = camera.height * camera.anchorY / camera.zoom
        tween(
            this.content::x[-camera.x - centerX],
            this.content::y[-camera.y - centerY],
            this.content::scaleX[this.content.width / camera.width * camera.zoom],
            this.content::scaleY[this.content.height / camera.height * camera.zoom],
            this.contentContainer::x[centerX],
            this.contentContainer::y[centerY],
            this.contentContainer::rotation[camera.angle],
            *this.camera.createTweenPropertiesTo(camera),
            time = time,
            easing = easing
        )
    }
}
