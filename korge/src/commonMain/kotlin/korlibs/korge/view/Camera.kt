package korlibs.korge.view

import korlibs.korge.annotations.*
import korlibs.korge.tween.*
import korlibs.math.geom.*
import korlibs.math.interpolation.*
import korlibs.time.*

/**
 * Creates a new [Camera] and attaches to [this] [Container]. The [callback] argument is called with the [Camera] injected as this to be able to configure the camera.
 */
@KorgeExperimental
inline fun Container.camera(callback: @ViewDslMarker Camera.() -> Unit = {}): Camera = Camera().addTo(this, callback)

/**
 * A [Camera] is a [Container] that allows to center its position using [setTo] and [tweenTo] methods.
 *
 * Should be used along with a wrapping [ClipContainer] defining its bounds.
 * You shouldn't change [Camera]'s [xD], [yD], [widthD], [heightD] or any other transform property.
 *
 * You can consider that this view [xD] and [yD] starts at 0 and changes its coordinates and scaling to move everything inside.
 *
 * This is a [View.Reference] to prevent re-computing things inside here when you are just moving the camera.
 *
 * The [ClipContainer] or the nearest [View.Reference] ancestor will determine the size of the [Camera]
 */
// @TODO: Do not require a [ClipContainer] by handling the [renderInternal] to use a transformed Camera. To support legacy we should do this in a separate class NewCamera? CameraContainer?.
@KorgeExperimental
open class Camera : Container(), View.Reference {
    //private val globalTransform = parent?.globalMatrix?.toTransform() ?: MatrixTransform()

    override var unscaledSize: Size
        //get() = Size((referenceParent?.width ?: 100f) * globalTransform.scaleAvg, (referenceParent?.height ?: 100f) * globalTransform.scaleAvg)
        get() = Size((referenceParent?.width ?: 100f), (referenceParent?.height ?: 100f))
        set(_) = Unit

    override fun getLocalBoundsInternal() = Rectangle(0f, 0f, width, height)

	fun getLocalMatrixFittingGlobalRect(rect: Rectangle): Matrix {
		val destinationBounds = rect
		return (this.parent?.globalMatrix ?: Matrix())
        //return Matrix()
		    .translated(-destinationBounds.x, -destinationBounds.y)
            .scaled(
                width / destinationBounds.width,
                height / destinationBounds.height
            )
	}

	fun getLocalMatrixFittingView(view: View?): Matrix =
		getLocalMatrixFittingGlobalRect((view ?: stage)?.globalBounds ?: Rectangle(0, 0, 100, 100))

	fun setTo(view: View?) { this.localMatrix = getLocalMatrixFittingView(view) }
	fun setTo(rect: Rectangle) { this.localMatrix = getLocalMatrixFittingGlobalRect(rect) }

	suspend fun tweenTo(view: View?, vararg vs: V2<*>, time: TimeSpan, easing: Easing = Easing.LINEAR) = this.tween(
		this::localMatrix[this.localMatrix, getLocalMatrixFittingView(view)],
		*vs,
		time = time,
		easing = easing
	)

	suspend fun tweenTo(rect: Rectangle, vararg vs: V2<*>, time: TimeSpan, easing: Easing = Easing.LINEAR) = this.tween(
		this::localMatrix[this.localMatrix, getLocalMatrixFittingGlobalRect(rect)],
		*vs,
		time = time,
		easing = easing
	)
}
