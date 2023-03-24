package korlibs.korge.view

import korlibs.time.TimeSpan
import korlibs.korge.tween.V2
import korlibs.korge.tween.get
import korlibs.korge.tween.tween
import korlibs.math.geom.*
import korlibs.math.interpolation.Easing

/**
 * Creates a new [Camera] and attaches to [this] [Container]. The [callback] argument is called with the [Camera] injected as this to be able to configure the camera.
 */
inline fun Container.camera(callback: @ViewDslMarker Camera.() -> Unit = {}): Camera = Camera().addTo(this, callback)

/**
 * A [Camera] is a [Container] that allows to center its position using [setTo] and [tweenTo] methods.
 *
 * Should be used along with a wrapping [ClipContainer] defining its bounds.
 * You shouldn't change [Camera]'s [x], [y], [width], [height] or any other transform property.
 *
 * You can consider that this view [x] and [y] starts at 0 and changes its coordinates and scaling to move everything inside.
 *
 * This is a [View.Reference] to prevent re-computing things inside here when you are just moving the camera.
 *
 * The [ClipContainer] or the nearest [View.Reference] ancestor will determine the size of the [Camera]
 */
// @TODO: Do not require a [ClipContainer] by handling the [renderInternal] to use a transformed Camera. To support legacy we should do this in a separate class NewCamera? CameraContainer?.
class Camera : Container(), View.Reference {
    override var width: Double
        set(_) = Unit
        get() = referenceParent?.width ?: 100.0
    override var height: Double
        set(_) = Unit
        get() = referenceParent?.height ?: 100.0

    override fun getLocalBoundsInternal() = Rectangle(0.0, 0.0, width, height)

	fun getLocalMatrixFittingGlobalRect(rect: Rectangle): Matrix {
		val destinationBounds = rect
		return (this.parent?.globalMatrix ?: Matrix())
		    .translated(-destinationBounds.x, -destinationBounds.y)
            .scaled(
                width / destinationBounds.width,
                height / destinationBounds.height
            )
	}

	fun getLocalMatrixFittingView(view: View?): Matrix =
		getLocalMatrixFittingGlobalRect((view ?: stage)?.globalBounds ?: Rectangle(0, 0, 100, 100))

	fun setTo(view: View?) { this.localMatrix = getLocalMatrixFittingView(view).immutable }
	fun setTo(rect: Rectangle) { this.localMatrix = getLocalMatrixFittingGlobalRect(rect).immutable }

	suspend fun tweenTo(view: View?, vararg vs: V2<*>, time: TimeSpan, easing: Easing = Easing.LINEAR) = this.tween(
		this::localMatrix[this.localMatrix.clone(), getLocalMatrixFittingView(view).immutable],
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