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
    noinline decoration: @ViewDslMarker CameraContainer.() -> Unit = {},
    content: @ViewDslMarker Container.() -> Unit = {}
) = CameraContainer(width, height, camera, decoration).addTo(this).also { content(it.content) }

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

    private val tempMat = Matrix()

    internal fun setTo(
        x: Double,
        y: Double,
        width: Double,
        height: Double,
        anchorX: Double,
        anchorY: Double,
        zoom: Double,
        angle: Angle
    ) {
        this.camera.withoutUpdate {
            this.x = x
            this.y = y
            this.width = width
            this.height = height
            this.anchorX = anchorX
            this.anchorY = anchorY
            this.zoom = zoom
            this.angle = angle
        }

        val realScaleX = (content.unscaledWidth / width) * zoom
        val realScaleY = (content.unscaledHeight / height) * zoom

        println("realScale=$realScaleX,$realScaleY")

        val contentContainerX = width * anchorX
        val contentContainerY = height * anchorY

        /*
        contentContainer.setMatrix(tempMat.apply {
            identity()
            pretranslate(-contentContainerX, -contentContainerY)
            //pretranslate(-x, -y)
            prerotate(angle)
            pretranslate(+contentContainerX, +contentContainerY)
            prescale(realScaleX, realScaleY)
            translate(x, y)
        }
            */
        println("contentContainer: ${contentContainer.localMatrix}")

        content.x = -x
        content.y = -y
        contentContainer.x = contentContainerX
        contentContainer.y = contentContainerY
        contentContainer.rotation = angle
        contentContainer.scaleX = realScaleX
        contentContainer.scaleY = realScaleY

        /*
        val centerX = cameraWidth * anchorX / cameraZoom
        val centerY = cameraHeight * anchorY / cameraZoom
        content.x = -cameraX - centerX
        content.y = -cameraY - centerY
        content.scaleX = content.width / cameraWidth * cameraZoom
        content.scaleY = content.height / cameraHeight * cameraZoom
        contentContainer.x = centerX
        contentContainer.y = centerY
        */
    }

    internal fun setTo(camera: Camera) {
        setTo(camera.x, camera.y, camera.width, camera.height, camera.anchorX, camera.anchorY, camera.zoom, camera.angle)
    }

    internal suspend fun tweenTo(camera: Camera, time: TimeSpan, easing: Easing) {
        val cameraInitialX = this.camera.x
        val cameraInitialY = this.camera.y
        val cameraInitialWidth = this.camera.width
        val cameraInitialHeight = this.camera.height
        val cameraInitialAnchorX = this.camera.anchorX
        val cameraInitialAnchorY = this.camera.anchorY
        val cameraInitialZoom = this.camera.zoom
        val cameraInitialAngle = this.camera.angle
        tween(time = time, easing = easing) { ratio ->
            setTo(
                ratio.interpolate(cameraInitialX, camera.x),
                ratio.interpolate(cameraInitialY, camera.y),
                ratio.interpolate(cameraInitialWidth, camera.width),
                ratio.interpolate(cameraInitialHeight, camera.height),
                ratio.interpolate(cameraInitialAnchorX, camera.anchorX),
                ratio.interpolate(cameraInitialAnchorY, camera.anchorY),
                ratio.interpolate(cameraInitialZoom, camera.zoom),
                ratio.interpolate(cameraInitialAngle, camera.angle)
            )
        }
    }
}
