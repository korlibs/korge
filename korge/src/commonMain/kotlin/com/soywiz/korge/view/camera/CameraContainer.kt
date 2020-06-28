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

    val content: Container = object : RectBase(), Reference {
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
        content.addTo(this)
    }

    private fun getLocalMatrixFor(camera: Camera): Matrix {
        val mat = globalMatrix.clone()
        val zoom = camera.zoom
        val x = camera.x + camera.width * (zoom - 1) / zoom * camera.anchorX
        val y = camera.y + camera.height  * (zoom - 1) / zoom * camera.anchorY
        mat.translate(-x, -y)
        mat.scale(
            this.width / camera.width * zoom,
            this.height / camera.height * zoom
        )
        //TODO: soywiz, implement correct rotation with anchoring
        //mat.rotate(camera.angle)
        return mat
    }

    internal fun setTo(camera: Camera) {
        content.localMatrix = getLocalMatrixFor(camera)
    }

    internal suspend fun tweenTo(camera: Camera, time: TimeSpan, easing: Easing) {
        val curMatrix = content.localMatrix.clone()
        tween(content::localMatrix[curMatrix, getLocalMatrixFor(camera)], time = time, easing = easing)
    }
}
