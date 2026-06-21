package korlibs.korge.render

import korlibs.korge.annotations.*
import korlibs.korge.internal.*
import korlibs.math.*
import korlibs.math.geom.*
import kotlin.js.*

@JsName("NewBoundsProvider")
fun BoundsProvider(): BoundsProvider.Base = BoundsProvider.Base()

interface BoundsProvider {
    var windowToGlobalMatrix: Matrix
    var windowToGlobalTransform: MatrixTransform
    var globalToWindowMatrix: Matrix
    var globalToWindowTransform: MatrixTransform
    var actualVirtualBounds: Rectangle

    @KorgeExperimental val actualVirtualLeft: Int get() = actualVirtualBounds.left.toIntRound()
    @KorgeExperimental val actualVirtualTop: Int get() = actualVirtualBounds.top.toIntRound()
    @KorgeExperimental val actualVirtualWidth: Int get() = actualVirtualBounds.width.toIntRound()
    @KorgeExperimental val actualVirtualHeight: Int get() = actualVirtualBounds.height.toIntRound()
    //@KorgeExperimental var actualVirtualWidth = DefaultViewport.WIDTH; private set
    //@KorgeExperimental var actualVirtualHeight = DefaultViewport.HEIGHT; private set

    val virtualLeft: Double get() = actualVirtualBounds.left.toDouble()
    val virtualTop: Double get() = actualVirtualBounds.top.toDouble()
    val virtualRight: Double get() = actualVirtualBounds.right.toDouble()
    val virtualBottom: Double get() = actualVirtualBounds.bottom.toDouble()

    @KorgeExperimental
    val actualVirtualRight: Double get() = actualVirtualBounds.right.toDouble()
    @KorgeExperimental
    val actualVirtualBottom: Double get() = actualVirtualBounds.bottom.toDouble()

    fun globalToWindowBounds(bounds: Rectangle): Rectangle =
        bounds.transformed(globalToWindowMatrix)

    val windowToGlobalScale: Scale get() = windowToGlobalTransform.scale
    val windowToGlobalScaleX: Double get() = windowToGlobalTransform.scale.scaleX
    val windowToGlobalScaleY: Double get() = windowToGlobalTransform.scale.scaleY
    val windowToGlobalScaleAvg: Double get() = windowToGlobalTransform.scale.scaleAvg

    val globalToWindowScale: Scale get() = globalToWindowTransform.scale
    val globalToWindowScaleX: Double get() = globalToWindowTransform.scaleX
    val globalToWindowScaleY: Double get() = globalToWindowTransform.scaleY
    val globalToWindowScaleAvg: Double get() = globalToWindowTransform.scaleAvg

    fun windowToGlobalCoords(pos: Point): Point = windowToGlobalMatrix.transform(pos)
    fun globalToWindowCoords(pos: Point): Point = globalToWindowMatrix.transform(pos)

    open class Base : BoundsProvider {
        override var windowToGlobalMatrix: Matrix = Matrix()
        override var windowToGlobalTransform: MatrixTransform = MatrixTransform()
        override var globalToWindowMatrix: Matrix = Matrix()
        override var globalToWindowTransform: MatrixTransform = MatrixTransform()
        override var actualVirtualBounds: Rectangle = Rectangle(0, 0, DefaultViewport.WIDTH, DefaultViewport.HEIGHT)
    }
}
