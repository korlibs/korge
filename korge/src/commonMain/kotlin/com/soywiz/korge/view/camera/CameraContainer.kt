package com.soywiz.korge.view.camera

import com.soywiz.korge.annotations.*
import com.soywiz.korge.view.*
import com.soywiz.korma.geom.*

@KorgeExperimental
inline fun Container.cameraContainer(
    width: Double,
    height: Double,
    camera: Camera = Camera(0.0, 0.0, width, height),
    noinline decoration: @ViewDslMarker CameraContainer.() -> Unit = {},
    content: @ViewDslMarker Container.() -> Unit = {}
) = CameraContainer(width, height, camera, decoration).addTo(this).also { content(it.content) }

@KorgeExperimental
class CameraContainer(
    override var width: Double = 100.0,
    override var height: Double = 100.0,
    camera: Camera = Camera(0.0, 0.0, width, height),
    decoration: @ViewDslMarker CameraContainer.() -> Unit = {}
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

    internal fun setTo(
        x: Double,
        y: Double,
        width: Double,
        height: Double,
        zoom: Double,
        angle: Angle,
        anchorX: Double,
        anchorY: Double
    ) {
        val realScaleX = (content.unscaledWidth / width) * zoom
        val realScaleY = (content.unscaledHeight / height) * zoom

        val contentContainerX = width * anchorX
        val contentContainerY = height * anchorY

        content.x = -x
        content.y = -y
        contentContainer.x = contentContainerX
        contentContainer.y = contentContainerY
        contentContainer.rotation = angle
        contentContainer.scaleX = realScaleX
        contentContainer.scaleY = realScaleY
    }

    internal fun setTo(camera: Camera) = setTo(
        camera.x,
        camera.y,
        camera.width,
        camera.height,
        camera.zoom,
        camera.angle,
        camera.anchorX,
        camera.anchorY
    )
}
