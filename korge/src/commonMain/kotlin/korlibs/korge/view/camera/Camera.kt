package korlibs.korge.view.camera

import korlibs.io.async.*
import korlibs.korge.view.*
import korlibs.math.geom.*
import korlibs.math.interpolation.*
import korlibs.memory.*
import korlibs.time.*
import kotlin.math.*

inline fun Container.cameraContainer(
    size: Size,
    clip: Boolean = true,
    noinline contentBuilder: (camera: CameraContainer) -> Container = { FixedSizeContainer(it.size) },
    noinline block: @ViewDslMarker CameraContainer.() -> Unit = {},
    content: @ViewDslMarker Container.() -> Unit = {}
) = CameraContainer(size, clip, contentBuilder, block).addTo(this).also { content(it.content) }

class CameraContainer(
    size: Size,
    clip: Boolean = true,
    contentBuilder: (camera: CameraContainer) -> Container = { FixedSizeContainer(it.size) },
    block: @ViewDslMarker CameraContainer.() -> Unit = {}
) : FixedSizeContainer(size, clip), View.Reference {
    var clampToBounds: Boolean = false
    var cameraViewportBounds: Rectangle = Rectangle(0, 0, 4096, 4096)

    private val contentContainer = Container()

    class ContentContainer(val cameraContainer: CameraContainer) : FixedSizeContainer(cameraContainer.size), Reference {
        //out.setTo(0, 0, cameraContainer.width, cameraContainer.height)
        override fun getLocalBoundsInternal() = Rectangle(0.0, 0.0, widthD, heightD)
    }

    val content: Container by lazy { contentBuilder(this) }

    private val sourceCamera = Camera(x = size.width / 2.0, y = size.height / 2.0, anchorX = 0.5, anchorY = 0.5)
    private val currentCamera = sourceCamera.copy()
    private val targetCamera = sourceCamera.copy()

    override var unscaledSize: Size = size
        set(value) {
            if (field == value) return
            field = value
            sync()
        }

    var cameraX: Double
        set(value) {
            currentCamera.x = value
            manualSet()
        }
        get() = currentCamera.x
    var cameraY: Double
        set(value) {
            currentCamera.y = value
            manualSet()
        }
        get() = currentCamera.y
    var cameraZoom: Double
        set(value) {
            currentCamera.zoom = value
            manualSet()
        }
        get() = currentCamera.zoom
    var cameraAngle: Angle
        set(value) {
            currentCamera.angle = value
            manualSet()
        }
        get() = currentCamera.angle
    var cameraAnchorX: Double
        set(value) {
            currentCamera.anchorX = value
            manualSet()
        }
        get() = currentCamera.anchorX
    var cameraAnchorY: Double
        set(value) {
            currentCamera.anchorY = value
            manualSet()
        }
        get() = currentCamera.anchorY

    private fun manualSet() {
        elapsedTime = transitionTime
        sync()
    }

    val onCompletedTransition = Signal<Unit>()

    fun getCurrentCamera(out: Camera = Camera()): Camera = out.copyFrom(currentCamera)
    fun getDefaultCamera(out: Camera = Camera()): Camera = out.setTo(x = widthD / 2.0, y = heightD / 2.0, anchorX = 0.5, anchorY = 0.5)

    companion object {
        fun getCameraRect(rect: Rectangle, scaleMode: ScaleMode = ScaleMode.SHOW_ALL, cameraWidth: Double, cameraHeight: Double, cameraAnchorX: Double, cameraAnchorY: Double, out: Camera = Camera()): Camera {
            val size = Rectangle(0.0, 0.0, cameraWidth, cameraHeight).place(rect.size, Anchor.TOP_LEFT, scale = scaleMode)
            val scaleX = size.width / rect.width
            val scaleY = size.height / rect.height
            return out.setTo(
                rect.x + rect.width * cameraAnchorX,
                rect.y + rect.height * cameraAnchorY,
                zoom = min(scaleX, scaleY).toDouble(),
                angle = 0.degrees,
                anchorX = cameraAnchorX,
                anchorY = cameraAnchorY
            )
        }
    }

    fun getCameraRect(rect: Rectangle, scaleMode: ScaleMode = ScaleMode.SHOW_ALL, out: Camera = Camera()): Camera = getCameraRect(rect, scaleMode, widthD, heightD, cameraAnchorX, cameraAnchorY, out)
    fun getCameraToFit(rect: Rectangle, out: Camera = Camera()): Camera = getCameraRect(rect, ScaleMode.SHOW_ALL, out)
    fun getCameraToCover(rect: Rectangle, out: Camera = Camera()): Camera = getCameraRect(rect, ScaleMode.COVER, out)

    private var transitionTime = 1.0.seconds
    private var elapsedTime = 0.0.milliseconds
    //var easing = Easing.EASE_OUT
    private var easing = Easing.LINEAR

    private var following: View? = null

    fun follow(view: View?, setImmediately: Boolean = false) {
        following = view
        if (setImmediately) {
            val point = getFollowingXY()
            cameraX = point.xD
            cameraY = point.yD
            sourceCamera.x = cameraX
            sourceCamera.y = cameraY
        }
    }

    fun unfollow() {
        following = null
    }

    fun updateCamera(block: Camera.() -> Unit) {
        block(currentCamera)
    }

    fun setCurrentCamera(camera: Camera) {
        elapsedTime = transitionTime
        following = null
        sourceCamera.copyFrom(camera)
        currentCamera.copyFrom(camera)
        targetCamera.copyFrom(camera)
        sync()
    }

    fun setTargetCamera(camera: Camera, time: TimeSpan = 1.seconds, easing: Easing = Easing.LINEAR) {
        elapsedTime = 0.seconds
        this.transitionTime = time
        this.easing = easing
        following = null
        sourceCamera.copyFrom(currentCamera)
        targetCamera.copyFrom(camera)
    }

    suspend fun tweenCamera(camera: Camera, time: TimeSpan = 1.seconds, easing: Easing = Easing.LINEAR) {
        setTargetCamera(camera, time, easing)
        onCompletedTransition.waitOne()
    }

    fun getFollowingXY(): Point {
        val followGlobalX = following!!.globalPos.xD
        val followGlobalY = following!!.globalPos.yD
        return content.globalToLocal(Point(followGlobalX, followGlobalY))
    }

    init {
        block(this)
        contentContainer.addTo(this)
        content.addTo(contentContainer)
        addUpdater {
            when {
                following != null -> {
                    val point = getFollowingXY()
                    cameraX = 0.1.toRatio().interpolate(currentCamera.x, point.xD)
                    cameraY = 0.1.toRatio().interpolate(currentCamera.y, point.yD)
                    sourceCamera.x = cameraX
                    sourceCamera.y = cameraY
                    //cameraX = 0.0
                    //cameraY = 0.0
                    //println("$cameraX, $cameraY - ${following?.x}, ${following?.y}")
                    sync()
                }
                elapsedTime < transitionTime -> {
                    elapsedTime += it
                    val ratio = (elapsedTime / transitionTime).coerceIn(0f, 1f)
                    currentCamera.setToInterpolated(easing(ratio).toRatio(), sourceCamera, targetCamera)
                    /*
                    val ratioCamera = easing(ratio)
                    val ratioZoom = easing(ratio)
                    currentCamera.zoom = ratioZoom.interpolate(sourceCamera.zoom, targetCamera.zoom)
                    currentCamera.x = ratioCamera.interpolate(sourceCamera.x, targetCamera.x)
                    currentCamera.y = ratioCamera.interpolate(sourceCamera.y, targetCamera.y)
                    currentCamera.angle = ratioCamera.interpolate(sourceCamera.angle, targetCamera.angle)
                    currentCamera.anchorX = ratioCamera.interpolate(sourceCamera.anchorX, targetCamera.anchorX)
                    currentCamera.anchorY = ratioCamera.interpolate(sourceCamera.anchorY, targetCamera.anchorY)
                     */
                    sync()
                    if (ratio >= 1.0) {
                        onCompletedTransition()
                    }
                }
            }
        }
    }


    fun sync() {
        //val realScaleX = (content.unscaledWidth / width) * cameraZoom
        //val realScaleY = (content.unscaledHeight / height) * cameraZoom
        val realScaleX = cameraZoom
        val realScaleY = cameraZoom

        val contentContainerX = widthD * cameraAnchorX
        val contentContainerY = heightD * cameraAnchorY

        //println("content=${content.getLocalBoundsOptimized()}, contentContainer=${contentContainer.getLocalBoundsOptimized()}, cameraViewportBounds=$cameraViewportBounds")

        content.xD = if (clampToBounds) -cameraX.clamp(contentContainerX + cameraViewportBounds.left, contentContainerX + cameraViewportBounds.width - widthD) else -cameraX
        content.yD = if (clampToBounds) -cameraY.clamp(contentContainerY + cameraViewportBounds.top, contentContainerY + cameraViewportBounds.height - heightD) else -cameraY
        contentContainer.xD = contentContainerX
        contentContainer.yD = contentContainerY
        contentContainer.rotation = cameraAngle
        contentContainer.scaleXD = realScaleX
        contentContainer.scaleYD = realScaleY
    }

    fun setZoomAt(anchor: MPoint, zoom: Double) = setZoomAt(anchor.x, anchor.y, zoom)

    fun setZoomAt(anchorX: Double, anchorY: Double, zoom: Double) {
        setAnchorPosKeepingPos(anchorX, anchorY)
        cameraZoom = zoom
    }

    fun setAnchorPosKeepingPos(anchor: MPoint) = setAnchorPosKeepingPos(anchor.x, anchor.y)

    fun setAnchorPosKeepingPos(anchorX: Double, anchorY: Double) {
        setAnchorRatioKeepingPos(anchorX / widthD, anchorY / heightD)
    }
    fun setAnchorRatioKeepingPos(ratioX: Double, ratioY: Double) {
        currentCamera.setAnchorRatioKeepingPos(ratioX, ratioY, widthD, heightD)
        sync()
    }
}

data class Camera(
    var x: Double = 0.0,
    var y: Double = 0.0,
    var zoom: Double = 1.0,
    var angle: Angle = 0.degrees,
    var anchorX: Double = 0.5,
    var anchorY: Double = 0.5
) : MutableInterpolable<Camera> {
    fun setTo(
        x: Double = 0.0,
        y: Double = 0.0,
        zoom: Double = 1.0,
        angle: Angle = 0.degrees,
        anchorX: Double = 0.5,
        anchorY: Double = 0.5
    ): Camera = this.apply {
        this.x = x
        this.y = y
        this.zoom = zoom
        this.angle = angle
        this.anchorX = anchorX
        this.anchorY = anchorY
    }

    fun setAnchorRatioKeepingPos(anchorX: Double, anchorY: Double, width: Double, height: Double) {
        val sx = width / zoom
        val sy = height / zoom
        val oldPaX = this.anchorX * sx
        val oldPaY = this.anchorY * sy
        val newPaX = anchorX * sx
        val newPaY = anchorY * sy
        this.x += newPaX - oldPaX
        this.y += newPaY - oldPaY
        this.anchorX = anchorX
        this.anchorY = anchorY
        //println("ANCHOR: $anchorX, $anchorY")
    }

    fun copyFrom(source: Camera) = source.apply { this@Camera.setTo(x, y, zoom, angle, anchorX, anchorY) }

    // @TODO: Easing must be adjusted from the zoom change
    // @TODO: This is not exact. We have to preserve final pixel-level speed while changing the zoom
    fun posEasing(zoomLeft: Double, zoomRight: Double, lx: Double, rx: Double, it: Double): Double {
        val zoomChange = zoomRight - zoomLeft
        return if (zoomChange <= 0.0) {
            it.pow(sqrt(-zoomChange).toInt().toDouble())
        } else {
            val inv = it - 1.0
            inv.pow(sqrt(zoomChange).toInt().toDouble()) + 1
        }
    }

    override fun setToInterpolated(ratio: Ratio, l: Camera, r: Camera): Camera {
        // Adjust based on the zoom changes
        val posRatio = posEasing(l.zoom, r.zoom, l.x, r.x, ratio.valueD)

        return setTo(
            posRatio.toRatio().interpolate(l.x, r.x),
            posRatio.toRatio().interpolate(l.y, r.y),
            ratio.interpolate(l.zoom, r.zoom),
            ratio.interpolateAngleDenormalized(l.angle, r.angle), // @TODO: Fix KorMA angle interpolator
            ratio.interpolate(l.anchorX, r.anchorX),
            ratio.interpolate(l.anchorY, r.anchorY)
        )
    }
}
