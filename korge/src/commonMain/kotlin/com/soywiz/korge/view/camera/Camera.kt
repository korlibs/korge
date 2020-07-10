package com.soywiz.korge.view.camera

import com.soywiz.kds.*
import com.soywiz.klock.*
import com.soywiz.korge.tween.*
import com.soywiz.korge.view.*
import com.soywiz.korio.lang.*
import com.soywiz.korma.geom.*
import com.soywiz.korma.interpolation.*
import kotlin.math.*

class Camera(
    x: Double = 0.0,
    y: Double = 0.0,
    width: Double = 100.0,
    height: Double = 100.0,
    zoom: Double = 1.0,
    angle: Angle = 0.degrees,
    override var anchorX: Double = 0.5,
    override var anchorY: Double = 0.5
) : MutableInterpolable<Camera>, Interpolable<Camera>, Anchorable {

    var x: Double by Observable(x, before = { if (withUpdate) setTo(x = it) })
    var y: Double by Observable(y, before = { if (withUpdate) setTo(y = it) })
    var width: Double by Observable(width, before = { if (withUpdate) setTo(width = it) })
    var height: Double by Observable(height, before = { if (withUpdate) setTo(height = it) })
    var zoom: Double by Observable(zoom, before = { if (withUpdate) setTo(zoom = it) })
    var angle: Angle by Observable(angle, before = { if (withUpdate) setTo(angle = it) })

    internal var container: CameraContainer? = null
    private var cancelFollowing: Cancellable? = null
    private var withUpdate: Boolean = true

    internal fun attach(container: CameraContainer) {
        this.container = container
    }

    internal fun detach() {
        container = null
    }

    // when we don't want to update CameraContainer
    internal inline fun withoutUpdate(callback: Camera.() -> Unit) {
        val prev = withUpdate
        withUpdate = false
        callback()
        withUpdate = prev
    }

    internal fun createTweenPropertiesTo(other: Camera): Array<V2<*>> = arrayOf(
        this::x[other.x],
        this::y[other.y],
        this::width[other.width],
        this::height[other.height],
        this::zoom[other.zoom],
        this::angle[other.angle],
        this::anchorX[other.anchorX],
        this::anchorY[other.anchorY]
    )

    fun setTo(
        x: Double = this.x,
        y: Double = this.y,
        width: Double = this.width,
        height: Double = this.height,
        zoom: Double = this.zoom,
        angle: Angle = this.angle,
        anchorX: Double = this.anchorX,
        anchorY: Double = this.anchorY
    ) = setTo(Camera(x, y, width, height, zoom, angle, anchorX, anchorY))

    fun setTo(other: Camera): Camera {
        if (withUpdate) {
            container?.setTo(other)
        }
        withoutUpdate {
            this.x = other.x
            this.y = other.y
            this.width = other.width
            this.height = other.height
            this.zoom = other.zoom
            this.angle = other.angle
            this.anchorX = other.anchorX
            this.anchorY = other.anchorY
        }
        return this
    }

    suspend fun tweenTo(other: Camera, time: TimeSpan, easing: Easing = Easing.LINEAR) {
        withoutUpdate {
            container?.tweenTo(other, time = time, easing = easing)
        }
    }

    fun follow(view: View, threshold: Double = 0.0) {
        val inside = container?.content?.let { view.hasAncestor(it) } ?: false
        if (!inside) throw IllegalStateException("Can't follow view that is not in the content of CameraContainer")
        cancelFollowing?.cancel()
        cancelFollowing = view.addUpdater {
            val camera = this@Camera

            camera.x = view.x + width / 2
            camera.y = view.y + height / 2


            /*
            val cxcenter = (camera.width * camera.anchorX)
            val cycenter = (camera.height * camera.anchorY)
            val vxcenter = (x + width / 2)
            val vycenter = (y + height / 2)
            camera.x = cxcenter - vxcenter
            camera.y = cycenter - vycenter
            println("camera=[${camera.width}, ${camera.height}], [${camera.anchorX}, ${camera.anchorY}]")
            println("view=[${view.x}, ${view.y}]")
             */

            /*

            println("dx=$dx, dy=$dy")

            val adx = abs(dx) - threshold
            val ady = abs(dy) - threshold
            if (adx > 0 || ady > 0) {
                camera.x -= dx.sign * adx
                camera.y -= dy.sign * ady
            }
            */
        }
    }

    fun unfollow() {
        cancelFollowing?.cancel()
    }

    fun copy(
        x: Double = this.x,
        y: Double = this.y,
        width: Double = this.width,
        height: Double = this.height,
        zoom: Double = this.zoom,
        angle: Angle = this.angle,
        anchorX: Double = this.anchorX,
        anchorY: Double = this.anchorY
    ) = Camera(
        x = x,
        y = y,
        width = width,
        height = height,
        zoom = zoom,
        angle = angle,
        anchorX = anchorX,
        anchorY = anchorY
    )

    override fun setToInterpolated(ratio: Double, l: Camera, r: Camera) = setTo(
        x = ratio.interpolate(l.x, r.x),
        y = ratio.interpolate(l.y, r.y),
        width = ratio.interpolate(l.width, r.width),
        height = ratio.interpolate(l.height, r.height),
        zoom = ratio.interpolate(l.zoom, r.zoom),
        angle = ratio.interpolate(l.angle, r.angle),
        anchorX = ratio.interpolate(l.anchorX, r.anchorX),
        anchorY = ratio.interpolate(l.anchorY, r.anchorY)
    )

    override fun interpolateWith(ratio: Double, other: Camera) = Camera().setToInterpolated(ratio, this, other)

    override fun toString() = "Camera(" +
        "x=$x, y=$y, width=$width, height=$height, " +
        "zoom=$zoom, angle=$angle, anchorX=$anchorX, anchorY=$anchorY)"
}

fun Camera.xy(x: Double, y: Double): Camera {
    return setTo(x = x, y = y)
}

fun Camera.size(width: Double, height: Double): Camera {
    return setTo(width = width, height = height)
}

fun Camera.anchor(anchorX: Double, anchorY: Double): Camera {
    return setTo(anchorX = anchorX, anchorY = anchorY)
}

suspend fun Camera.moveTo(x: Double, y: Double, time: TimeSpan, easing: Easing = Easing.LINEAR) {
    tweenTo(copy(x = x, y = y), time = time, easing = easing)
}

suspend fun Camera.moveBy(dx: Double, dy: Double, time: TimeSpan, easing: Easing = Easing.LINEAR) {
    tweenTo(copy(x = x + dx, y = y + dy), time = time, easing = easing)
}

suspend fun Camera.resizeTo(width: Double, height: Double, time: TimeSpan, easing: Easing = Easing.LINEAR) {
    tweenTo(copy(width = width, height = height), time = time, easing = easing)
}

suspend fun Camera.rotate(angle: Angle, time: TimeSpan, easing: Easing = Easing.LINEAR) {
    tweenTo(copy(angle = this.angle + angle), time = time, easing = easing)
}

suspend fun Camera.zoom(value: Double, time: TimeSpan, easing: Easing = Easing.LINEAR) {
    tweenTo(copy(zoom = value), time = time, easing = easing)
}
