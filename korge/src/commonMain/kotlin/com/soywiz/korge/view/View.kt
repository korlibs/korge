@file:OptIn(KorgeInternal::class)

package com.soywiz.korge.view

import com.soywiz.kds.*
import com.soywiz.kds.iterators.*
import com.soywiz.klock.*
import com.soywiz.korev.*
import com.soywiz.korge.baseview.*
import com.soywiz.korge.component.*
import com.soywiz.korge.debug.*
import com.soywiz.korge.internal.*
import com.soywiz.korge.render.*
import com.soywiz.korge.view.filter.*
import com.soywiz.korim.color.*
import com.soywiz.korim.vector.*
import com.soywiz.korio.lang.*
import com.soywiz.korio.util.*
import com.soywiz.korio.util.encoding.*
import com.soywiz.korma.geom.*
import com.soywiz.korma.geom.shape.*
import com.soywiz.korma.geom.vector.*
import com.soywiz.korma.interpolation.*
import com.soywiz.korui.*
import com.soywiz.krypto.encoding.*

/**
 * KorGE includes a DOM-based tree of views that makes a chain of affine transforms starting with the [Stage], that is the root node.
 *
 * ## Basic description
 *
 * The [View] class is the base class for all the nodes in the display tree. It is abstract with the [renderInternal] method missing.
 * [View] itself can't contain children, but the [Container] class and subclasses allow to have children.
 * Typical non-container views are: [Image], [SolidRect] or [Text].
 *
 * Most views don't have the concept of size. They act just as points (x,y) or rather affine transforms (since they also include scale, rotation and skew)
 *
 * ## Properties
 *
 * Basic transform properties of the [View] are [x], [y], [scaleX], [scaleY], [rotation], [skewX] and [skewY].
 * Regarding to how the views are drawn there are: [alpha], [colorMul] ([tint]), [colorAdd].
 *
 * [View] implements the [Extra] interface, thus allows to add arbitrary typed properties.
 * [View] implements the [EventDispatcher] interface, and allows to handle and dispatch events.
 *
 * ## Components
 *
 * Views can have zero or more [Component]s attached. [Component] handle the behaviour of the [View] under several events.
 * For example, the [UpdateComponent] will trigger its [UpdateComponent.update] method each frame.
 *
 * For views with [Updatable] components, [View] include a [speed] property where 1 is 1x and 2 is 2x the speed.
 */
@OptIn(KorgeInternal::class)
abstract class View internal constructor(
    /** Indicates if this class is a container or not. This is only overridden by Container. This check is performed like this, to avoid type checks. That might be an expensive operation in some targets. */
    val isContainer: Boolean
) : BaseView(), Renderable
    , Extra
    , KorgeDebugNode
    , BView
    , XY
    , HitTestable
    , WithHitShape2d
//, EventDispatcher by EventDispatcher.Mixin()
{
    override var extra: ExtraType = null

    override val bview: View get() = this
    override val bviewAll: List<View> by lazy { listOf(this) }

    constructor() : this(false)
    //internal val _transform = ViewTransform(this)


    ///**
    // * Propagates an [Event] to all child [View]s.
    // *
    // * The [Event] is propagated to all the child [View]s of the container, iterated in reverse orted.
    // */
    //override fun <T : Event> dispatch(clazz: KClass<T>, event: T) {
    //    if (propagateEvents) {
    //        forEachChildrenReversed { child ->
    //            child.dispatch(clazz, event)
    //        }
    //    }
    //}

    @KorgeInternal
    open val anchorDispX get() = 0.0
    @KorgeInternal
    open val anchorDispY get() = 0.0

    @KorgeInternal
    @PublishedApi
    internal open val _children: FastArrayList<View>? get() = null

    /** Iterates all the children of this container in normal order of rendering. */
    inline fun forEachChild(callback: (child: View) -> Unit) = _children?.fastForEach(callback)

    /** Iterates all the children of this container in normal order of rendering. */
    @Deprecated(
        message = "An older name of `forEachChild`",
        replaceWith = ReplaceWith("forEachChild(callback)"),
        level = DeprecationLevel.WARNING
    )
    inline fun forEachChildren(callback: (child: View) -> Unit) = _children?.fastForEach(callback)

    /** Iterates all the children of this container in normal order of rendering. Providing an index in addition to the child to the callback. */
    inline fun forEachChildWithIndex(callback: (index: Int, child: View) -> Unit) =
        _children?.fastForEachWithIndex(callback)

    /** Iterates all the children of this container in normal order of rendering. Providing an index in addition to the child to the callback. */
    @Deprecated(
        message = "An older name of `forEachChildWithIndex`",
        replaceWith = ReplaceWith("forEachChildWithIndex(callback)"),
        level = DeprecationLevel.WARNING
    )
    inline fun forEachChildrenWithIndex(callback: (index: Int, child: View) -> Unit) =
        _children?.fastForEachWithIndex(callback)

    /** Iterates all the children of this container in reverse order of rendering. */
    inline fun forEachChildReversed(callback: (child: View) -> Unit) = _children?.fastForEachReverse(callback)

    /** Iterates all the children of this container in reverse order of rendering. */
    @Deprecated(
        message = "An older name of `forEachChildReversed`",
        replaceWith = ReplaceWith("forEachChildReversed(callback)"),
        level = DeprecationLevel.WARNING
    )
    inline fun forEachChildrenReversed(callback: (child: View) -> Unit) = _children?.fastForEachReverse(callback)

    /** Indicates if this view is going to propagate the events that reach this node to its children */
    var propagateEvents = true

    /**
     * Views marked with this, break batching by acting as a reference point to compute vertices.
     * Specially useful for containers most children of which are less likely to change while the containers
     * themselves are going to change (like cameras, viewports and the [Stage]).
     */
    interface Reference // View that breaks batching Viewport
    interface ColorReference // View that breaks batching Viewport

    private var _hitShape2d: Shape2d? = null

    @Deprecated("Use hitShape2d instead")
    open var hitShape: VectorPath? = null
    @Deprecated("Use hitShape2d instead")
    open var hitShapes: List<VectorPath>? = null

    override var hitShape2d: Shape2d
        get() {
            if (_hitShape2d == null) {
                if (_hitShape2d == null && hitShapes != null) _hitShape2d = hitShapes!!.toShape2d()
                if (_hitShape2d == null && hitShape != null) _hitShape2d = hitShape!!.toShape2d()
                //if (_hitShape2d == null) _hitShape2d = Shape2d.Rectangle(getLocalBounds())
            }
            return _hitShape2d ?: Shape2d.Empty
        }
        set(value) {
            _hitShape2d = value
        }

    companion object {
        //private val identity = Matrix()

        /**
         * Determines the common [View] ancestor of [left] and [right] (if any) or null.
         * If [left] and [right] are the same, the common ancestor is that value.
         * If [left] or [right] are null, there is no common ancestor, and it returns null.
         */
        fun commonAncestor(left: View?, right: View?): View? {
            var l: View? = left
            var r: View? = right
            var lCount = l.ancestorCount
            var rCount = r.ancestorCount
            //println("commonAncestor: $lCount, $rCount")
            while (lCount >= 0 || rCount >= 0) {
                if (l == r) return l
                val ldec = lCount >= rCount
                val rdec = rCount >= lCount

                if (ldec) {
                    lCount--
                    l = l?.parent
                }
                if (rdec) {
                    rCount--
                    r = r?.parent
                }
            }
            return null
        }
    }

    /** Property used for interpolable views like morph shapes, progress bars etc. */
    open var ratio: Double = 0.0

    @PublishedApi internal var _index: Int = 0
    @PublishedApi internal var _parent: Container? = null

    /** The index the child has in its parent */
    var index: Int
        get() = _index
        internal set(value) { _index = value }

    /** Ratio speed of this node, affecting all the [UpdateComponent] */
    var speed: Double = 1.0

    /** Parent [Container] of [this] View if any, or null */
    var parent: Container?
        get() = _parent
        internal set(value) { _parent = value }

    /** Optional name of this view */
    var name: String? = null

    /** The [BlendMode] used for this view [BlendMode.INHERIT] will use the ancestors [blendMode]s */
    var blendMode: BlendMode = BlendMode.INHERIT
        set(value) {
            if (field != value) {
                field = value
                invalidate()
            }
        }

    /** Computed [speed] combining all the speeds from ancestors */
    val globalSpeed: Double get() = if (parent != null) parent!!.globalSpeed * speed else speed

    private var _x: Double = 0.0
    private var _y: Double = 0.0
    private var _scaleX: Double = 1.0
    private var _scaleY: Double = 1.0
    private var _skewX: Angle = 0.0.radians
    private var _skewY: Angle = 0.0.radians
    private var _rotation: Angle = 0.0.radians

    private val _pos = Point()

    /** Position of the view. **@NOTE**: If [pos] coordinates are manually changed, you should call [View.invalidateMatrix] later to keep the matrix in sync */
    var pos: Point
        get() {
            _pos.setTo(x, y)
            return _pos
        }
        set(value) {
            ensureTransform()
            if (_x != value.x || _y != value.y) {
                this._x = value.x
                this._y = value.y
                invalidateMatrix()
            }
        }

    /** Local X position of this view */
    override var x: Double
        get() {
            ensureTransform()
            return _x
        }
        set(v) {
            ensureTransform()
            if (_x != v) {
                _x = v
                invalidateMatrix()
            }
        }

    /** Local Y position of this view */
    override var y: Double
        get() {
            ensureTransform()
            return _y
        }
        set(v) {
            ensureTransform()
            if (_y != v) {
                _y = v
                invalidateMatrix()
            }
        }

    /*
    var xf: Float get() = x.toFloat() ; set(v) { x = v.toDouble() }

    var yf: Float get() = y.toFloat() ; set(v) { y = v.toDouble() }

    var scaleXf: Float
        get() = ensureTransform()._scaleXf
        set(v) { ensureTransform(); if (_scaleXf != v) { _scaleXf = v; invalidateMatrix() } }

    var scaleYf: Float
        get() = ensureTransform()._scaleYf
        set(v) { ensureTransform(); if (_scaleYf != v) { _scaleYf = v; invalidateMatrix() } }

    var scalef: Float get() = scale.toFloat() ; set(v) { scale = v.toDouble() }
    */

    /** Local scaling in the X axis of this view */
    var scaleX: Double
        get() { ensureTransform(); return _scaleX }
        set(v) { ensureTransform(); if (_scaleX != v) { _scaleX = v; invalidateMatrix() } }

    /** Local scaling in the Y axis of this view */
    var scaleY: Double
        get() { ensureTransform(); return _scaleY }
        set(v) { ensureTransform(); if (_scaleY != v) { _scaleY = v; invalidateMatrix() } }

    /** Allows to change [scaleX] and [scaleY] at once. Returns the mean value of x and y scales. */
    var scale: Double
        get() = (scaleX + scaleY) / 2f
        set(v) { scaleX = v; scaleY = v }

    /** Local skewing in the X axis of this view */
    var skewX: Angle
        get() { ensureTransform(); return _skewX }
        set(v) { ensureTransform(); if (_skewX != v) { _skewX = v; invalidateMatrix() } }

    /** Local skewing in the Y axis of this view */
    var skewY: Angle
        get() { ensureTransform(); return _skewY }
        set(v) { ensureTransform(); if (_skewY != v) { _skewY = v; invalidateMatrix() } }

    /** Local rotation of this view */
    var rotation: Angle
        get() { ensureTransform(); return _rotation }
        set(v) { ensureTransform(); if (_rotation != v) { _rotation = v; invalidateMatrix() } }

    /** The global x position of this view */
    var globalX: Double
        get() = parent?.localToGlobalX(x, y) ?: x;
        set(value) { x = parent?.globalToLocalX(value, globalY) ?: value }

    /** The global y position of this view */
    var globalY: Double
        get() = parent?.localToGlobalY(x, y) ?: y;
        set(value) { y = parent?.globalToLocalY(globalX, value) ?: value }

    fun globalXY(out: Point = Point()): Point = out.setTo(globalX, globalY)
    fun localXY(out: Point = Point()): Point = out.setTo(x, y)

    /**
     * Changes the [width] and [height] to match the parameters.
     */
    open fun setSize(width: Double, height: Double) {
        this.width = width
        this.height = height
    }

    open fun setSizeScaled(width: Double, height: Double) {
        this.scaledWidth = width
        this.scaledHeight = height
    }

    /**
     * Changes the [width] of this view. Generically, this means adjusting the [scaleX] of the view to match that size using the current bounds,
     * but some views might override this to adjust its internal width or height (like [SolidRect] or [UIView] for example).
     *
     * @TODO: In KorGE 2.0, View.width/View.height will be immutable and available from an extension method for Views that doesn't have a width/height properties
     */
    open var width: Double
        get() = getLocalBoundsOptimizedAnchored().width
        @Deprecated("Shouldn't set width but scaleWidth instead")
        set(value) {
            scaleX = (if (scaleX == 0.0) 1.0 else scaleX) * (value / width)
        }

    /**
     * Changes the [height] of this view. Generically, this means adjusting the [scaleY] of the view to match that size using the current bounds,
     * but some views might override this to adjust its internal width or height (like [SolidRect] or [UIView] for example).
     *
     * @TODO: In KorGE 2.0, View.width/View.height will be immutable and available from an extension method for Views that doesn't have a width/height properties
     */
    open var height: Double
        get() = getLocalBoundsOptimizedAnchored().height
        @Deprecated("Shouldn't set height but scaleHeight instead")
        set(value) {
            scaleY = (if (scaleY == 0.0) 1.0 else scaleY) * (value / getLocalBoundsOptimizedAnchored().height)
        }

    val unscaledWidth: Double get() = width
    val unscaledHeight: Double get() = height

    var scaledWidth: Double
        get() = unscaledWidth * scaleX
        set(value) {
            width = if (scaleX == 0.0) value else value / scaleX
        }

    /**
     * Changes the [height] of this view. Generically, this means adjusting the [scaleY] of the view to match that size using the current bounds,
     * but some views might override this to adjust its internal width or height (like [SolidRect] or [UIView] for example).
     */
    var scaledHeight: Double
        get() = unscaledHeight * scaleY
        set(value) {
            height = if (scaleY == 0.0) value else value / scaleY
        }

    /**
     * The multiplicative [RGBA] color.
     *
     * That means:
     * * [Colors.WHITE] would display the view without modifications
     * * [Colors.BLACK] would display a black shape
     * * [Colors.TRANSPARENT_BLACK] would be equivalent to setting [alpha]=0
     * * [Colors.RED] would only show the red component of the view
     */
    var colorMul: RGBA
        get() = _colorTransform.colorMul
        set(v) {
            if (v != _colorTransform.colorMul) {
                _colorTransform.colorMul = v
                invalidateColorTransform()
            }
        }

    /**
     * Additive part of the color transform.
     * This Int is a packed version of R,G,B,A one-component byte values determining additive color transform.
     * @NOTE: If you don't have this value computed, you can use [ColorTransform.aR] aB, aG and aA to control the
     * per component values. You should call the [View.invalidate] method after that.
     */
    var colorAdd: ColorAdd
        get() = _colorTransform.colorAdd;
        set(v) {
            if (v != _colorTransform.colorAdd) {
                _colorTransform.colorAdd = v
                invalidateColorTransform()
            }
        }

    /**
     * Shortcut for adjusting the multiplicative alpha value manually.
     * Equivalent to [ColorTransform.mA] + [View.invalidate]
     */
    var alpha: Double
        get() = _colorTransform.mA;
        set(v) {
            if (v != _colorTransform.mA) {
                _colorTransform.mA = v
                invalidateColorTransform()
            }
        }

    /** Alias for [colorMul] to make this familiar to people coming from other engines. */
    var tint: RGBA
        get() = this.colorMul
        set(value) {
            this.colorMul = value
        }

    private val tempTransform = Matrix.Transform()
    //private val tempMatrix = Matrix2d()

    private fun ensureTransform() {
        if (validLocalProps) return
        validLocalProps = true
        val t = tempTransform
        t.setMatrixNoReturn(this._localMatrix)
        this._x = t.x
        this._y = t.y
        this._scaleX = t.scaleX
        this._scaleY = t.scaleY
        this._skewX = t.skewX
        this._skewY = t.skewY
        this._rotation = t.rotation
    }

    /** The ancestor view without parents. When attached (visible or invisible), this is the [Stage]. When no parents, it is [this] */
    val root: View get() = parent?.root ?: this

    /** When included in the tree, this returns the stage. When not attached yet, this will return null. */
    open val stage: Stage? get() = root as? Stage?

    /** Determines if mouse events will be handled for this view and its children */
    //open var mouseEnabled: Boolean = true
    open var mouseEnabled: Boolean = false
    open var mouseChildren: Boolean = true

    /** Determines if the view will be displayed or not. It is different to alpha=0, since the render method won't be executed. Usually giving better performance. But also not receiving events. */
    open var visible: Boolean = true

    /** Sets the local transform matrix that includes [x], [y], [scaleX], [scaleY], [rotation], [skewX] and [skewY] encoded into a [Matrix] */
    fun setMatrix(matrix: Matrix) {
        this._localMatrix.copyFrom(matrix)
        this.validLocalProps = false
        invalidate()
    }

    /** Like [setMatrix] but directly sets an interpolated version of the [l] and [r] matrices with the [ratio] */
    fun setMatrixInterpolated(ratio: Double, l: Matrix, r: Matrix) {
        this._localMatrix.setToInterpolated(ratio, l, r)
        this.validLocalProps = false
        invalidate()
    }

    /**
     * Sets the computed transform [Matrix] and all the decomposed transform properties at once.
     * Normally this is used by animation libraries to set Views in a way that are fast to update
     * and to access.
     */
    fun setComputedTransform(transform: Matrix.Computed) {
        _localMatrix.copyFrom(transform.matrix)
        _setTransform(transform.transform)
        invalidate()
        validLocalProps = true
        validLocalMatrix = true
    }

    /**
     * Sets the [Matrix.Transform] decomposed version of the transformation,
     * that directly includes [x], [y], [scaleX], [scaleY], [rotation], [skewX] and [skewY].
     */
    fun setTransform(transform: Matrix.Transform) {
        _setTransform(transform)
        invalidate()
        validLocalProps = true
        validLocalMatrix = false
    }

    /**
     * Like [setTransform] but without invalidation. If used at all, should be used with care and invalidate when required.
     */
    @KorgeInternal
    fun _setTransform(t: Matrix.Transform) {
        //transform.toMatrix(_localMatrix)
        _x = t.x; _y = t.y
        _scaleX = t.scaleX; _scaleY = t.scaleY
        _skewX = t.skewY; _skewY = t.skewY
        _rotation = t.rotation
    }

    //fun setTransform(x: Double, y: Double, sx: Double, sy: Double, angle: Double, skewX: Double, skewY: Double, pivotX: Double = 0.0, pivotY: Double = 0.0) =
    //	setTransform(tempTransform.setTo(x, y, sx, sy, skewX, skewY, angle))


    internal var validLocalProps = true
    internal var validLocalMatrix = true

    private val _localMatrix = Matrix()

    /**
     * Local transform [Matrix]. If you plan to change its components manually
     * instead of setting it directly, you should call the [View.invalidate] method.
     */
    var localMatrix: Matrix
        get() {
            if (!validLocalMatrix) {
                validLocalMatrix = true
                _requireInvalidate = true
                _localMatrix.setTransform(x, y, scaleX, scaleY, rotation, skewX, skewY)
            }
            return _localMatrix
        }
        set(value) {
            setMatrix(value)
            invalidate()
        }

    private var _globalMatrix = Matrix()
    private var _globalMatrixVersion = -1

    /**
     * Global transform [Matrix].
     * Matrix that concatenates all the affine transforms of this view and its ancestors.
     */
    var globalMatrix: Matrix
        get() {
            if (_globalMatrixVersion != this._version) {
                _globalMatrixVersion = this._version
                _requireInvalidate = true
                if (parent != null) {
                    _globalMatrix.multiply(localMatrix, parent!!.globalMatrix)
                } else {
                    _globalMatrix.copyFrom(localMatrix)
                }
            }
            return _globalMatrix
        }
        set(value) {
            _requireInvalidate = true
            if (parent != null) {
                this.localMatrix.multiply(value, parent!!.globalMatrixInv)
            } else {
                this.localMatrix.copyFrom(value)
            }
        }

    private val _globalMatrixInv = Matrix()
    private var _globalMatrixInvVersion = -1

    /**
     * The inverted version of the [globalMatrix]
     */
    val globalMatrixInv: Matrix
        get() {
            if (_globalMatrixInvVersion != this._version) {
                _globalMatrixInvVersion = this._version
                _requireInvalidate = true
                _globalMatrixInv.invert(this.globalMatrix)
            }
            return _globalMatrixInv
        }

    private val _colorTransform = ColorTransform()

    /**
     * The [ColorTransform] of this view.
     * If you plan to change its components manually, you should call the [View.invalidate] method.
     * You can also use: [alpha], [colorMul] and [colorAdd] that won't require the [invalidate].
     */
    var colorTransform: ColorTransform
        get() = _colorTransform
        set(v) {
            if (v != _colorTransform) {
                _colorTransform.copyFrom(v)
                invalidate()
            }
        }

    private val _renderColorTransform = ColorTransform(1.0, 1.0, 1.0, 1.0, 0, 0, 0, 0)
    private var _renderColorTransformVersion = -1

    private fun updateRenderColorTransform() {
        _renderColorTransformVersion = this._versionColor
        _requireInvalidateColor = true
        val parent = this.parent
        when {
            parent?.filter != null -> _renderColorTransform.copyFrom(_colorTransform)
            parent != null && this !is ColorReference -> _renderColorTransform.setToConcat(
                _colorTransform,
                parent.renderColorTransform
            )
            else -> _renderColorTransform.copyFrom(_colorTransform)
        }
    }

    private fun updateRenderColorTransformIfRequired() {
        if (_renderColorTransformVersion == this._versionColor) return
        updateRenderColorTransform()
    }

    /**
     * The concatenated version of [colorTransform] having into account all the color transformations of the ancestors
     */
    val renderColorTransform: ColorTransform get() {
        updateRenderColorTransformIfRequired()
        return _renderColorTransform
    }

    private var _renderBlendMode: BlendMode = BlendMode.INHERIT
    private var _renderBlendModeVersion: Int = -1

    private fun updateRenderBlendMode() {
        _renderBlendModeVersion = this._version
        _requireInvalidate = true
        _renderBlendMode = when (blendMode) {
            BlendMode.INHERIT -> parent?.renderBlendMode ?: BlendMode.NORMAL
            else -> blendMode
        }
    }

    private fun updateRenderBlendModeIfRequired() {
        if (_renderBlendModeVersion == this._version) return
        updateRenderBlendMode()
    }

    /**
     * The actual [blendMode] of the view after computing the ancestors and reaching a view with a non [BlendMode.INHERIT].
     */
    val renderBlendMode: BlendMode get() {
        updateRenderBlendModeIfRequired()
        return _renderBlendMode
    }

    /** The concatenated/global version of the local [colorMul] */
    val renderColorMul: RGBA get() {
        updateRenderColorTransformIfRequired()
        return _renderColorTransform.colorMul
    }

    /** The concatenated/global version of the local [colorAdd] */
    val renderColorAdd: ColorAdd get() {
        updateRenderColorTransformIfRequired()
        return _renderColorTransform.colorAdd
    }

    /** The concatenated/global version of the local [alpha] */
    val renderAlpha: Double get() {
        updateRenderColorTransformIfRequired()
        return renderColorTransform.mA
    }

    /** Computes the local X coordinate of the mouse using the coords from the [Views] object */
    fun localMouseX(views: Views): Double = this.globalMatrixInv.transformX(views.input.mouse)

    /** Computes the local Y coordinate of the mouse using the coords from the [Views] object */
    fun localMouseY(views: Views): Double = this.globalMatrixInv.transformY(views.input.mouse)

    /** Computes the local X and Y coordinate of the mouse using the coords from the [Views] object. You can use the [target] parameter to specify a target [Point] to avoid allocation. */
    fun localMouseXY(views: Views, target: Point = Point()): Point =
        target.setTo(localMouseX(views), localMouseY(views))

    /**
     * Invalidates the [localMatrix] [Matrix], so it gets updated from the decomposed properties: [x], [y], [scaleX], [scaleY], [rotation], [skewX] and [skewY].
     */
    fun invalidateMatrix() {
        validLocalMatrix = false
        invalidate()
    }

    protected var dirtyVertices = true

    private var _version = 0
    private var _versionColor = 0
    internal var _requireInvalidate = false
    internal var _requireInvalidateColor = false

    /**
     * Invalidates the [View] after changing some of its properties so the geometry can be computed again.
     * If you change the [localMatrix] directly, you should call [invalidateMatrix] instead.
     */
    open fun invalidate() {
        this._version++
        _requireInvalidate = false
        dirtyVertices = true
    }

    open fun invalidateColorTransform() {
        this._versionColor++
        _requireInvalidateColor = false
        dirtyVertices = true
    }

    /**
     * An optional [Filter] attached to this view.
     * Filters allow to render this view to a texture, and to control how to render that texture (using shaders, repeating the texture, etc.).
     * You add multiple filters by creating a composite filter [ComposedFilter].
     */
    private var _isFilterSet = false
    var filter: Filter? = null
        set(value) {
            field = value
            _isFilterSet = (value != null)
        }

    fun addFilter(filter: Filter) {
        when (this.filter) {
            null -> this.filter = filter
            is ComposedFilter -> this.filter = ComposedFilter((this.filter as ComposedFilter).filters + filter)
            else -> this.filter = ComposedFilter(listOf(this.filter!!, filter))
        }
    }

    fun removeFilter(filter: Filter) {
        when (this.filter) {
            filter -> this.filter = null
            is ComposedFilter -> this.filter = ComposedFilter((this.filter as ComposedFilter).filters.filter { it != filter })
        }
    }

    var debugAnnotate: Boolean = false

    /**
     * The [render] method that is in charge of rendering.
     * This method receives the [ctx] [RenderContext] that allows to buffer
     * geometry to be drawn in batches.
     *
     * This method is final, and to control rendering you have to override [renderInternal].
     * When a [filter] is set, the render is performed into a texture, and the [Filter]
     * decides how to render that texture containing the View representation.
     */
    final override fun render(ctx: RenderContext) {
        if (!visible) return
        if (_isFilterSet) {
            renderFiltered(ctx)
        } else {
            renderInternal(ctx)
        }
    }

    open fun renderDebug(ctx: RenderContext) {
        if (debugAnnotate || this === ctx.debugAnnotateView) {
            renderDebugAnnotationsInternal(ctx)
        }
    }

    protected open fun renderDebugAnnotationsInternal(ctx: RenderContext) {
        //println("DEBUG ANNOTATE VIEW!")
        //ctx.flush()
        val local = getLocalBoundsOptimizedAnchored()
        ctx.useLineBatcher { lines ->
            lines.drawVector(Colors.RED) {
                rect(windowBounds)
            }
            lines.drawVector(Colors.WHITE) {
                moveTo(localToGlobal(Point(local.left, local.top)))
                lineTo(localToGlobal(Point(local.right, local.top)))
                lineTo(localToGlobal(Point(local.right, local.bottom)))
                lineTo(localToGlobal(Point(local.left, local.bottom)))
                close()
            }
            lines.drawVector(Colors.YELLOW) {
                val anchorSize = 5.0
                circle(localToGlobal(local.topLeft), anchorSize)
                circle(localToGlobal(local.topRight), anchorSize)
                circle(localToGlobal(local.bottomRight), anchorSize)
                circle(localToGlobal(local.bottomLeft), anchorSize)
                circle(localToGlobal(local.topLeft.interpolateWith(0.5, local.topRight)), anchorSize)
                circle(localToGlobal(local.topRight.interpolateWith(0.5, local.bottomRight)), anchorSize)
                circle(localToGlobal(local.bottomRight.interpolateWith(0.5, local.bottomLeft)), anchorSize)
                circle(localToGlobal(local.bottomLeft.interpolateWith(0.5, local.topLeft)), anchorSize)
            }
            lines.drawVector(Colors.BLUE) {
                val centerX = globalX
                val centerY = globalY
                line(centerX, centerY - 5, centerX, centerY + 5)
                line(centerX - 5, centerY, centerX + 5, centerY)
            }
        }

        //ctx.flush()
    }

    private fun renderFiltered(ctx: RenderContext) {
        renderFiltered(ctx, filter!!)
    }

    fun renderFiltered(ctx: RenderContext, filter: Filter) {
        val bounds = getLocalBoundsOptimizedAnchored()

        val borderEffect = filter.border
        ctx.matrixPool.alloc { tempMat2d ->
            val texWidth = bounds.width.toInt() + borderEffect * 2
            val texHeight = bounds.height.toInt() + borderEffect * 2

            val addx = -bounds.x + borderEffect
            val addy = -bounds.y + borderEffect

            //println("FILTER: $texWidth, $texHeight : $globalMatrixInv, $globalMatrix, addx=$addx, addy=$addy, renderColorAdd=$renderColorAdd, renderColorMulInt=$renderColorMulInt, blendMode=$blendMode")

            /*
            run {
                val bmp = ctx.renderToBitmap(texWidth, texHeight) {
                    tempMat2d.copyFrom(globalMatrixInv)
                    tempMat2d.translate(addx, addy)
                    //println("globalMatrixInv:$globalMatrixInv, tempMat2d=$tempMat2d")
                    //println("texWidth=$texWidth, texHeight=$texHeight, $bounds, addx=$addx, addy=$addy, globalMatrix=$globalMatrix, globalMatrixInv:$globalMatrixInv, tempMat2d=$tempMat2d")
                    ctx.batch.setViewMatrixTemp(tempMat2d) {
                        renderInternal(ctx)
                    }
                }
                com.soywiz.korio.async.launchImmediately(ctx.coroutineContext) {
                    bmp.writeTo("/tmp/bitmap.png".uniVfs, PNG)
                }
            }
            */

            ctx.renderToTexture(texWidth, texHeight, render = {
                tempMat2d.copyFrom(globalMatrixInv)
                //tempMat2d.copyFrom(globalMatrix)
                tempMat2d.translate(addx, addy)
                //println("globalMatrixInv:$globalMatrixInv, tempMat2d=$tempMat2d")
                //println("texWidth=$texWidth, texHeight=$texHeight, $bounds, addx=$addx, addy=$addy, globalMatrix=$globalMatrix, globalMatrixInv:$globalMatrixInv, tempMat2d=$tempMat2d")
                @Suppress("DEPRECATION")
                ctx.batch.setViewMatrixTemp(tempMat2d) {
                    renderInternal(ctx)
                }
            }) { texture ->
                tempMat2d.copyFrom(globalMatrix)
                tempMat2d.pretranslate(-addx, -addy)
                filter.render(
                    ctx,
                    tempMat2d,
                    texture,
                    texWidth,
                    texHeight,
                    renderColorAdd,
                    renderColorMul,
                    blendMode
                )
            }
        }
    }

    /** Method that all views must override in order to control how the view is going to be rendered */
    protected abstract fun renderInternal(ctx: RenderContext)

    @Suppress("RemoveCurlyBracesFromTemplate")
    override fun toString(): String {
        var out = this::class.portableSimpleName
        if (x != 0.0 || y != 0.0) out += ":pos=(${x.str},${y.str})"
        if (scaleX != 1.0 || scaleY != 1.0) out += ":scale=(${scaleX.str},${scaleY.str})"
        if (skewX.radians != 0.0 || skewY.radians != 0.0) out += ":skew=(${skewX.degrees.str},${skewY.degrees.str})"
        if (rotation.absoluteValue != 0.radians) out += ":rotation=(${rotation.degrees.str}º)"
        if (name != null) out += ":name=($name)"
        if (blendMode != BlendMode.INHERIT) out += ":blendMode=($blendMode)"
        if (!visible) out += ":visible=$visible"
        if (alpha != 1.0) out += ":alpha=$alpha"
        if (this.colorMul.rgb != Colors.WHITE.rgb) out += ":colorMul=${this.colorMul.hexString}"
        if (colorAdd != ColorAdd.NEUTRAL) out += ":colorAdd=${colorAdd.shex}"
        return out
    }

    protected val Double.str get() = this.toStringDecimal(2, skipTrailingZeros = true)

    // Version with root-most object as reference
    /** Converts the global point [p] (using root/stage as reference) into the local coordinate system. Allows to define [out] to avoid allocation. */
    fun globalToLocal(p: IPoint, out: Point = Point()): Point = globalToLocalXY(p.x, p.y, out)

    /** Converts the global point [x] [y] (using root/stage as reference) into the local coordinate system. Allows to define [out] to avoid allocation. */
    fun globalToLocalXY(x: Double, y: Double, out: Point = Point()): Point = this.globalMatrixInv.transform(x, y, out)

    /** Converts the global point [x], [y] (using root/stage as reference) into the X in the local coordinate system. */
    fun globalToLocalX(x: Double, y: Double): Double = this.globalMatrixInv.transformX(x, y)

    /** Converts the global point [x], [y] (using root/stage as reference) into the Y in the local coordinate system. */
    fun globalToLocalY(x: Double, y: Double): Double = this.globalMatrixInv.transformY(x, y)

    fun globalToLocalDX(x0: Double, y0: Double, x1: Double, y1: Double): Double = globalToLocalX(x1, y1) - globalToLocalX(x0, y0)
    fun globalToLocalDY(x0: Double, y0: Double, x1: Double, y1: Double): Double = globalToLocalY(x1, y1) - globalToLocalY(x0, y0)
    fun globalToLocalDXY(x0: Double, y0: Double, x1: Double, y1: Double, out: Point = Point()): Point = out.setTo(
        globalToLocalDX(x0, y0, x1, y1),
        globalToLocalDY(x0, y0, x1, y1),
    )
    fun globalToLocalDXY(p0: IPoint, p1: IPoint, out: Point = Point()): Point = globalToLocalDXY(p0.x, p0.y, p1.x, p1.y, out)

    /** Converts the local point [p] into a global point (using root/stage as reference). Allows to define [out] to avoid allocation. */
    fun localToGlobal(p: IPoint, out: Point = Point()): Point = localToGlobalXY(p.x, p.y, out)

    /** Converts the local point [x], [y] into a global point (using root/stage as reference). Allows to define [out] to avoid allocation. */
    fun localToGlobalXY(x: Double, y: Double, out: Point = Point()): Point = this.globalMatrix.transform(x, y, out)

    /** Converts the local point [x], [y] into a global X coordinate (using root/stage as reference). */
    fun localToGlobalX(x: Double, y: Double): Double = this.globalMatrix.transformX(x, y)

    /** Converts the local point [x], [y] into a global Y coordinate (using root/stage as reference). */
    fun localToGlobalY(x: Double, y: Double): Double = this.globalMatrix.transformY(x, y)

    // Version with View.Reference as reference
    /** Converts a point [p] in the nearest ancestor marked as [View.Reference] into the local coordinate system. Allows to define [out] to avoid allocation. */
    fun renderToLocal(p: IPoint, out: Point = Point()): Point = renderToLocalXY(p.x, p.y, out)

    /** Converts a point [x], [y] in the nearest ancestor marked as [View.Reference] into the local coordinate system. Allows to define [out] to avoid allocation. */
    fun renderToLocalXY(x: Double, y: Double, out: Point = Point()): Point = this.globalMatrixInv.transform(x, y, out)

    /** Converts a point [x], [y] in the nearest ancestor marked as [View.Reference] into the local X coordinate. */
    fun renderToLocalX(x: Double, y: Double): Double = this.globalMatrixInv.transformX(x, y)

    /** Converts a point [x], [y] in the nearest ancestor marked as [View.Reference] into the local Y coordinate. */
    fun renderToLocalY(x: Double, y: Double): Double = this.globalMatrixInv.transformY(x, y)

    /** Converts the local point [p] into a point in the nearest ancestor masked as [View.Reference]. Allows to define [out] to avoid allocation. */
    fun localToRender(p: IPoint, out: Point = Point()): Point = localToRenderXY(p.x, p.y, out)

    /** Converts the local point [x],[y] into a point in the nearest ancestor masked as [View.Reference]. Allows to define [out] to avoid allocation. */
    fun localToRenderXY(x: Double, y: Double, out: Point = Point()): Point = this.globalMatrix.transform(x, y, out)

    /** Converts the local point [x],[y] into a X coordinate in the nearest ancestor masked as [View.Reference]. */
    fun localToRenderX(x: Double, y: Double): Double = this.globalMatrix.transformX(x, y)

    /** Converts the local point [x],[y] into a Y coordinate in the nearest ancestor masked as [View.Reference]. */
    fun localToRenderY(x: Double, y: Double): Double = this.globalMatrix.transformY(x, y)

    var hitTestEnabled = true

    /**
     * Determines the view at the global point defined by [x] and [y] if any, or null
     *
     * When a container, recursively finds the [View] displayed the given global [x], [y] coordinates.
     *
     * @returns The (visible) [View] displayed at the given coordinates or `null` if none is found.
     */
    open fun hitTest(x: Double, y: Double, direction: HitTestDirection = HitTestDirection.ANY): View? {
        if (!hitTestEnabled) return null
        if (!visible) return null

        _children?.fastForEachReverse { child ->
            child.hitTest(x, y, direction)?.let {
                return it
            }
        }
        val res = hitTestInternal(x, y)
        if (res != null) return res
        return if (this is Stage) this else null
    }
    fun hitTest(x: Float, y: Float): View? = hitTest(x.toDouble(), y.toDouble())
    fun hitTest(x: Int, y: Int): View? = hitTest(x.toDouble(), y.toDouble())

    override fun hitTestAny(x: Double, y: Double, direction: HitTestDirection): Boolean =
        hitTest(x, y, direction) != null

    fun hitTestView(views: List<View>, direction: HitTestDirection = HitTestDirection.ANY): View? {
        views.fastForEach { view -> hitTestView(view, direction)?.let { return it } }
        return null
    }

    fun hitTestView(view: View, direction: HitTestDirection = HitTestDirection.ANY): View? {
        if (!hitTestEnabled) return null
        if (!visible) return null
        if (_hitShape2d == null) {
            _children?.fastForEachReverse { child ->
                if (child != view) {
                    child.hitTestView(view, direction)?.let {
                        return it
                    }
                }
            }
        }
        val res = hitTestShapeInternal(view.hitShape2d, view.getGlobalMatrixWithAnchor(tempMatrix1), direction)
        if (res != null) return res
        return if (this is Stage) this else null
    }

    fun hitTestShape(shape: Shape2d, matrix: Matrix, direction: HitTestDirection = HitTestDirection.ANY): View? {
        if (!hitTestEnabled) return null
        if (!visible) return null
        if (_hitShape2d == null) {
            _children?.fastForEachReverse { child ->
                child.hitTestShape(shape, matrix)?.let {
                    return it
                }
            }
        }
        val res = hitTestShapeInternal(shape, matrix, direction)
        if (res != null) return res
        return if (this is Stage) this else null
    }

    private val tempMatrix1 = Matrix()
    private val tempMatrix2 = Matrix()
    private val tempMatrix = Matrix()

    open val customHitShape get() = false
    open protected fun hitTestShapeInternal(shape: Shape2d, matrix: Matrix, direction: HitTestDirection): View? {
        //println("View.hitTestShapeInternal: $this, $shape")
        if (Shape2d.intersects(this.hitShape2d, getGlobalMatrixWithAnchor(tempMatrix2), shape, matrix, tempMatrix)) {
            //println(" -> true")
            return this
        }
        return null
    }

    // @TODO: we should compute view bounds on demand
    fun mouseHitTest(x: Double, y: Double): View? {
        if (!hitTestEnabled) return null
        if (!visible) return null
        if (mouseChildren) {
            _children?.fastForEachReverse { child ->
                child.mouseHitTest(x, y)?.let {
                    return it
                }
            }
        }
        if (!mouseEnabled) return null
        hitTestInternal(x, y)?.let {

            // @TODO: This should not be required if we compute bounds
            val area = getClippingAreaInternal()
            if (area != null && !area.contains(x, y)) return null

            return it
        }
        return if (this is Stage) this else null
    }

    private val _localBounds2: Rectangle = Rectangle()

    @KorgeInternal
    fun getClippingAreaInternal(): Rectangle? {
        this._localBounds2.setTo(Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY)
        var count = 0
        forEachAscendant(true) {
            if (it !is Stage && it is FixedSizeContainer && it.clip) {
                it.getWindowBounds(this._localBounds)
                if (count == 0) {
                    this._localBounds2.copyFrom(this._localBounds)
                } else {
                    this._localBounds2.setToIntersection(this._localBounds2, this._localBounds)
                }
                count++
            }
        }
        return if (count == 0) null else this._localBounds2
    }

    fun mouseHitTest(x: Float, y: Float): View? = hitTest(x.toDouble(), y.toDouble())
    fun mouseHitTest(x: Int, y: Int): View? = hitTest(x.toDouble(), y.toDouble())

    fun hitTestAny(x: Double, y: Double): Boolean = hitTest(x, y) != null

    var hitTestUsingShapes: Boolean? = null

    /** [x] and [y] coordinates are global */
    open protected fun hitTestInternal(x: Double, y: Double, direction: HitTestDirection = HitTestDirection.ANY): View? {
        if (!hitTestEnabled) return null

        //println("x,y: $x,$y")
        //println("bounds: ${getGlobalBounds(_localBounds)}")
        //if (!getGlobalBounds(_localBounds).contains(x, y)) return null

        // Adjusted coordinates to compensate anchoring
        val llx = globalToLocalX(x, y)
        val lly = globalToLocalY(x, y)

        val bounds = getLocalBoundsOptimizedAnchored()
        if (!bounds.contains(llx, lly)) return null
        val anchorDispX = this.anchorDispX
        val anchorDispY = this.anchorDispY

        val lx = llx + anchorDispX
        val ly = lly + anchorDispY

        /*
        val sLeft = bounds.left
        val sTop = bounds.top
        val bwidth = bounds.width
        val bheight = bounds.height

        val centerX = sLeft + bwidth * 0.5 + anchorDispX
        val centerY = sTop + bheight * 0.5 + anchorDispY

        val manhattanDist = (lx - centerX).absoluteValue + (ly - centerY).absoluteValue
        val manhattanDist2 = bwidth * 0.5 + bheight * 0.5
        //println("($centerX, $centerY)-($lx, $ly): $manhattanDist > $manhattanDist2")
        if (manhattanDist > manhattanDist2) return null
        val outerCircleRadius = hypot(bwidth * 0.5, bheight * 0.5)
        val dist = Point.distance(lx, ly, centerX, centerY)
        //println("($centerX, $centerY)-($lx, $ly): $dist > $outerCircleRadius")
        if (dist > outerCircleRadius) return null
        */

        //println("lx=$lx,ly=$ly")
        //println("localBounds:$bounds")

        val hitShape = this.hitShape
        val hitShapes = this.hitShapes
        if (hitTestUsingShapes == true || (hitTestUsingShapes == null && (hitShape != null || hitShapes != null))) {
            hitShapes?.fastForEach { if (it.containsPoint(lx, ly)) return this }
            if (hitShape != null && hitShape.containsPoint(lx, ly)) return this
            return null
        } else {
            return this
        }
    }

    //fun hitTest(x: Double, y: Double): View? {
    //	if (!mouseEnabled) return null
    //	return hitTestInternal(x, y)
    //}

    /*
    /** @TODO: Check this */
    @KorgeInternal
    open fun hitTestInternal(x: Double, y: Double): View? {
        val bounds = getLocalBounds()
        return if (checkGlobalBounds(x, y, bounds.left, bounds.top, bounds.right, bounds.bottom)) this else null
    }
    */

    /*
    /** @TODO: Check this */
    @KorgeInternal
    open fun hitTestBoundingInternal(x: Double, y: Double): View? {
        val bounds = getGlobalBounds()
        return if (bounds.contains(x, y)) this else null
    }
     */

    /** [x] and [y] are global, while [sLeft], [sTop], [sRight], [sBottom] are local */
    protected fun checkGlobalBounds(
        x: Double,
        y: Double,
        sLeft: Double,
        sTop: Double,
        sRight: Double,
        sBottom: Double
    ): Boolean = checkLocalBounds(globalToLocalX(x, y), globalToLocalY(x, y), sLeft, sTop, sRight, sBottom)

    //protected fun checkGlobalBounds(
    //    x: Double,
    //    y: Double,
    //    grect: Rectangle
    //): Boolean = grect.contains(x, y)

    protected fun checkLocalBounds(
        lx: Double,
        ly: Double,
        sLeft: Double,
        sTop: Double,
        sRight: Double,
        sBottom: Double
    ): Boolean = lx >= sLeft && ly >= sTop && lx < sRight && ly < sBottom

    //protected fun checkLocalBounds(
    //    lx: Double,
    //    ly: Double,
    //    lrect: Rectangle
    //): Boolean = lrect.contains(lx, ly)

    /**
     * Resets the View properties to an identity state.
     */
    open fun reset() {
        _localMatrix.identity()
        _x = 0.0; _y = 0.0
        _scaleX = 1.0; _scaleY = 1.0
        _skewX = 0.0.radians; _skewY = 0.0.radians
        _rotation = 0.0.radians
        validLocalMatrix = false
        invalidate()
    }

    /**
     * Removes this view from its parent.
     */
    fun removeFromParent() {
        if (parent == null) return
        val p = parent!!
        for (i in index + 1 until p.numChildren) p[i].index--
        p._children?.removeAt(index)
        parent = null
        index = -1
    }

    //fun getConcatMatrix(target: View, out: Matrix = Matrix2d()): Matrix {
    //	var current: View? = this
    //	out.setToIdentity()
    //
    //	val views = arrayListOf<View>()
    //	while (current != null) {
    //		views += current
    //		if (current == target) break
    //		current = current.parent
    //	}
    //	for (view in views.reversed()) out.premultiply(view.localMatrix)
    //
    //	return out
    //}

    //fun getConcatMatrix(target: View, out: Matrix = Matrix()): Matrix = getConcatMatrix(target, false, out)

    /**
     * Gets the concatenated [Matrix] of this [View] up to the [target] view.
     * If [inclusive] is true, the concatenated matrix will include the [target] view too.
     * Allows to define an [out] matrix that will hold the result to prevent allocations.
     */
    fun getConcatMatrix(target: View, out: Matrix = Matrix(), inclusive: Boolean = false): Matrix {
        when {
            target === parent -> out.copyFrom(this.localMatrix)
            target === this -> out.identity()
            else -> {
                val commonAncestor = View.commonAncestor(this, target)
                when {
                    commonAncestor !== null -> {
                        if (target.parent == null && inclusive) {
                            return out.copyFrom(globalMatrix)
                        }
                        out.multiply(globalMatrix, target.globalMatrixInv)
                    }
                    else -> {
                        out.identity()
                    }
                }
            }
        }
        if (inclusive) {
            out.multiply(out, target.localMatrix)
        }
        return out
    }

    fun getConcatMatrixAccurateSlow(target: View, out: Matrix = Matrix(), inclusive: Boolean = false): Matrix {
        out.identity()
        if (target !== this) {
            var current: View? = this
            val stopAt = if (inclusive) target.parent else target
            while (current !== null && current !== stopAt) {
                out.multiply(out, current.localMatrix) // Verified
                current = current.parent
            }
        }
        return out
    }

    /** Returns the global bounds of this object. Note this incurs in allocations. Use [getGlobalBounds] (out) to avoid it */
    val windowBounds: Rectangle get() = getWindowBounds()

    /** Returns the global bounds of this object. Allows to specify an [out] [Rectangle] to prevent allocations. */
    fun getWindowBounds(out: Rectangle = Rectangle()): Rectangle = getBounds(root, out, inclusive = true)

    /** Returns the global bounds of this object. Note this incurs in allocations. Use [getGlobalBounds] (out) to avoid it */
    val globalBounds: Rectangle get() = getGlobalBounds()

    /** Returns the global bounds of this object. Allows to specify an [out] [Rectangle] to prevent allocations. */
    fun getGlobalBounds(out: Rectangle = Rectangle()): Rectangle = getBounds(root, out, inclusive = false)

    // @TODO: Would not include strokes
    //fun getRect(target: View? = this, out: Rectangle = Rectangle()): Rectangle = TODO()

    /** Get the bounds of this view, using the [target] view as coordinate system. Not providing a [target] will return the local bounds. Allows to specify [out] [Rectangle] to prevent allocations. */
    private val boundsTemp = Matrix()
    private val bb = BoundsBuilder()

    fun getBoundsNoAnchoring(target: View? = this, out: Rectangle = Rectangle(), inclusive: Boolean = false): Rectangle {
        return getBounds(target, out, false, inclusive)
    }

    protected fun _getBounds(concat: Matrix?, out: Rectangle = Rectangle(), doAnchoring: Boolean = true): Rectangle {
        getLocalBounds(out, doAnchoring)

        if (concat != null && !concat.isIdentity()) {
            val p1x = out.left
            val p1y = out.top

            val p2x = out.right
            val p2y = out.top

            val p3x = out.right
            val p3y = out.bottom

            val p4x = out.left
            val p4y = out.bottom

            bb.reset()
            bb.add(concat.transformX(p1x, p1y), concat.transformY(p1x, p1y))
            bb.add(concat.transformX(p2x, p2y), concat.transformY(p2x, p2y))
            bb.add(concat.transformX(p3x, p3y), concat.transformY(p3x, p3y))
            bb.add(concat.transformX(p4x, p4y), concat.transformY(p4x, p4y))

            bb.getBounds(out)
        }
        return out
    }

    fun getBounds(target: View? = this, out: Rectangle = Rectangle(), doAnchoring: Boolean = true, inclusive: Boolean = false): Rectangle {
        return _getBounds(this.getConcatMatrix(target ?: this, boundsTemp, inclusive), out, doAnchoring)
    }

    /**
     * **NOTE:** that if [out] is not provided, the [Rectangle] returned shouldn't stored and modified since it is owned by this class.
     */
    fun getLocalBoundsOptimized(): Rectangle = getLocalBounds(_localBounds)

    fun getLocalBoundsOptimizedAnchored(): Rectangle = getLocalBounds(_localBounds, doAnchoring = true)

    @Deprecated("Allocates")
    fun getLocalBounds(doAnchoring: Boolean = true) = getLocalBounds(Rectangle(), doAnchoring)

    /**
     * Get local bounds of the view. Allows to specify [out] [Rectangle] if you want to reuse an object.
     */
    fun getLocalBounds(out: Rectangle, doAnchoring: Boolean = true): Rectangle {
        getLocalBoundsInternal(out)
        val it = out
        if (!doAnchoring) {
            it.x += anchorDispX
            it.y += anchorDispY
        }
        return it
    }

    private val _localBounds: Rectangle = Rectangle()
    open fun getLocalBoundsInternal(out: Rectangle = _localBounds): Unit {
        out.clear()
    }

    protected open fun createInstance(): View =
        throw MustOverrideException("Must Override ${this::class}.createInstance()")

    /**
     * Allows to copy the basic properties (transform [localMatrix], [visible], [colorTransform], [ratio], [speed], [name]...)
     * from [source] into [this]
     */
    open fun copyPropsFrom(source: View) {
        this.name = source.name
        this.colorAdd = source.colorAdd
        this.colorMul = source.colorMul
        this.setMatrix(source.localMatrix)
        this.visible = source.visible
        this.ratio = source.ratio
        this.speed = source.speed
        this.blendMode = source.blendMode
    }

    /**
     * Allows to find a descendant view whose [View.name] property is [name].
     * Returns null if can't find any.
     */
    open fun findViewByName(name: String): View? {
        if (this.name == name) return this
        return null
    }

    /**
     * Allows to clone this view.
     * This method is inadvisable in normal circumstances.
     * This might not work properly if the [View] doesn't override the [createInstance] method.
     */
    open fun clone(): View = createInstance().apply {
        this@apply.copyPropsFrom(this@View)
    }

    fun globalLocalBoundsPointRatio(anchor: Anchor, out: Point = Point()): Point = globalLocalBoundsPointRatio(anchor.sx, anchor.sy, out)

    fun globalLocalBoundsPointRatio(ratioX: Double, ratioY: Double, out: Point = Point()): Point {
        val bounds = getLocalBoundsOptimizedAnchored()
        val x = ratioX.interpolate(bounds.left, bounds.right)
        val y = ratioY.interpolate(bounds.top, bounds.bottom)
        return out.setTo(localToGlobalX(x, y), localToGlobalY(x, y))
    }

    var extraBuildDebugComponent: ((views: Views, view: View, container: UiContainer) -> Unit)? = null

    override fun buildDebugComponent(views: Views, container: UiContainer) {
        val view = this

        extraBuildDebugComponent?.invoke(views, view, container)

        if (filter != null) {
            container.uiCollapsibleSection("Filter") {
                filter!!.buildDebugComponent(views, this)
            }
        }

        container.uiCollapsibleSection("View") {
            addChild(UiRowEditableValue(app, "type", UiLabel(app).also { it.text = view::class.simpleName ?: "Unknown" }))
            uiEditableValue(view::name)
            uiEditableValue(view::colorMul)
            uiEditableValue(view::blendMode, values = { BlendMode.values().toList() })
            uiEditableValue(view::alpha, min = 0.0, max = 1.0, clamp = true)
            uiEditableValue(view::speed, min = -1.0, max = 1.0, clamp = false)
            uiEditableValue(view::ratio, min = 0.0, max = 1.0, clamp = false)
            uiEditableValue(Pair(view::x, view::y), min = -1000.0, max = +1000.0, clamp = false, name = "position")
            uiEditableValue(Pair(view::scaledWidth, view::scaledHeight), min = -1000.0, max = 1000.0, clamp = false, name = "size")
            uiEditableValue(view::scale, min = 0.0, max = 1.0, clamp = false)
            uiEditableValue(Pair(view::scaleX, view::scaleY), min = 0.0, max = 1.0, clamp = false, name = "scaleXY")
            uiEditableValue(view::rotation, name = "rotation")
            uiEditableValue(Pair(view::skewX, view::skewY), name = "skew")
            uiEditableValue(view::visible)
        }

        views.viewExtraBuildDebugComponent.fastForEach {
            it(views, view, container)
        }
    }

    fun getGlobalMatrixWithAnchor(out: Matrix = Matrix()): Matrix {
        val view = this
        out.copyFrom(view.localMatrix)
        out.pretranslate(-view.anchorDispX, -view.anchorDispY)
        view.parent?.globalMatrix?.let { out.multiply(out, it) }
        return out
    }
}

val View.width: Double get() = unscaledWidth
val View.height: Double get() = unscaledHeight

// Doesn't seem to work
//operator fun <T : View, R> T.invoke(callback: T.() -> R): R = this.apply(callback)


/*
class CachedImmutable<T>(val initial: T, val compute: () -> T) {
	private var _valid = false
	private var _cached: T = initial

	val value: T get() {
		if (!_valid) {
			_valid = true
			_cached = compute()
		}
		return _cached
	}

	fun invalidate() {
		_valid = false
	}
}

class CachedMutable<T>(val instance: T, val compute: (T) -> Unit) {
	private var _valid = false
	private val _cached: T = instance

	val value: T get() {
		if (!_valid) {
			_valid = true
			compute(_cached)
		}
		return _cached
	}

	fun invalidate() {
		_valid = false
	}
}

class ViewTransform(var view: View) {
	val parent get() = view.parent?._transform

	var blendMode: BlendMode = BlendMode.INHERIT

	val localMatrix: Matrix get() = _localMatrix
	private val _localMatrix = Matrix2d()

	val globalMatrix: Matrix get() = _globalMatrix.value
	private val _globalMatrix = CachedMutable(Matrix2d()) {
		if (parent != null) {
			it.multiply(localMatrix, parent!!.globalMatrix)
		} else {
			it.copyFrom(localMatrix)
		}
	}

	val renderMatrix: Matrix get() = _renderMatrix.value
	private val _renderMatrix = CachedMutable(Matrix2d()) {
		if (parent != null && view !is View.Reference) {
			it.multiply(localMatrix, parent!!.renderMatrix)
		} else {
			it.copyFrom(localMatrix)
		}
	}

	val renderBlendMode: BlendMode get() = _renderBlendMode.value
	private val _renderBlendMode = CachedImmutable(BlendMode.INHERIT) {
		if (blendMode != BlendMode.INHERIT) blendMode else parent?.renderBlendMode ?: BlendMode.NORMAL
	}

	fun invalidate() {
		_globalMatrix.invalidate()
		_renderMatrix.invalidate()
		_renderBlendMode.invalidate()
	}
}
*/

/**
 * Determines if the given coords [x] and [y] hit this view or any of its descendants.
 * Returns the view that was hit or null
 */
fun View.hitTest(x: Int, y: Int): View? = hitTest(x.toDouble(), y.toDouble())

/**
 * Determines if the given coords [pos] hit this view or any of its descendants.
 * Returns the view that was hit or null
 */
fun View.hitTest(pos: IPoint): View? = hitTest(pos.x, pos.y)
//fun View.hitTest(pos: Point): View? = hitTest(pos.x, pos.y)

/**
 * Checks if this view has the specified [ancestor].
 */
fun View.hasAncestor(ancestor: View): Boolean {
    return if (this == ancestor) true else this.parent?.hasAncestor(ancestor) ?: false
}

fun View?.commonAncestor(ancestor: View?): View? {
    return View.commonAncestor(this, ancestor)
}

/**
 * Replaces this view in its parent with [view].
 * Returns true if the replacement was successful.
 * If this view doesn't have a parent or [view] is the same as [this], returns false.
 */
@OptIn(KorgeInternal::class)
fun View.replaceWith(view: View): Boolean {
    if (this == view) return false
    if (parent == null) return false
    view.parent?._children?.remove(view)
    parent!!.childrenInternal[this.index] = view
    view.index = this.index
    view.parent = parent
    parent = null
    view.invalidate()
    this.index = -1
    return true
}

/** Adds a block that will be executed per frame to this view. As parameter the block will receive a [TimeSpan] with the time elapsed since the previous frame. */
fun <T : View> T.addUpdater(updatable: T.(dt: TimeSpan) -> Unit): Cancellable {
    val component = object : UpdateComponent {
        override val view: View get() = this@addUpdater
        override fun update(dt: TimeSpan) {
            updatable(this@addUpdater, dt)
        }
    }.attach()
    component.update(TimeSpan.ZERO)
    return Cancellable { component.detach() }
}

fun <T : View> T.addUpdaterWithViews(updatable: T.(views: Views, dt: TimeSpan) -> Unit): Cancellable {
    val component = object : UpdateComponentWithViews {
        override val view: View get() = this@addUpdaterWithViews
        override fun update(views: Views, dt: TimeSpan) {
            updatable(this@addUpdaterWithViews, views, dt)
        }
    }.attach()
    return Cancellable { component.detach() }
}

fun <T : View> T.addOptFixedUpdater(time: TimeSpan = TimeSpan.NIL, updatable: T.(dt: TimeSpan) -> Unit): Cancellable = when (time) {
    TimeSpan.NIL -> addUpdater(updatable)
    else -> addFixedUpdater(time) { updatable(time) }
}

fun <T : View> T.addFixedUpdater(
    timesPerSecond: Frequency,
    initial: Boolean = true,
    limitCallsPerFrame: Int = 16,
    updatable: T.() -> Unit
): Cancellable = addFixedUpdater(timesPerSecond.timeSpan, initial, limitCallsPerFrame, updatable)

/**
 * Adds an [updatable] block that will be executed every [time] time, the calls will be discretized on each frame and will handle accumulations.
 * The [initial] properly allows to adjust if the [updatable] will be called immediately after calling this function.
 * To avoid executing too many blocks, when there is a long pause, [limitCallsPerFrame] limits the number of times the block can be executed in a single frame.
 */
fun <T : View> T.addFixedUpdater(
    time: TimeSpan,
    initial: Boolean = true,
    limitCallsPerFrame: Int = 16,
    updatable: T.() -> Unit
): Cancellable {
    var accum = 0.0.milliseconds
    val component = object : UpdateComponent {
        override val view: View get() = this@addFixedUpdater
        override fun update(dt: TimeSpan) {
            accum += dt
            //println("UPDATE: accum=$accum, tickTime=$tickTime")
            var calls = 0
            while (accum >= time * 0.75) {
                accum -= time
                updatable(this@addFixedUpdater)
                calls++
                if (calls >= limitCallsPerFrame) {
                    // We do not accumulate for the next frame in this case
                    accum = 0.0.milliseconds
                    break
                }
            }
            if (calls > 0) {
                // Do not accumulate for small fractions since this would cause hiccups!
                if (accum < time * 0.25) {
                    accum = 0.0.milliseconds
                }
            }
        }
    }.attach()
    if (initial) {
        updatable(this@addFixedUpdater)
    }
    return Cancellable { component.detach() }
}

@Deprecated("Use addUpdater instead", ReplaceWith("addUpdater(updatable)"))
inline fun <T : View> T.onFrame(noinline updatable: T.(dt: TimeSpan) -> Unit): Cancellable = addUpdater(updatable)

fun <T : View> T.onNextFrame(updatable: T.(views: Views) -> Unit) {
    object : UpdateComponentWithViews {
        override val view: View get() = this@onNextFrame
        override fun update(views: Views, dt: TimeSpan) {
            removeFromView()
            updatable(this@onNextFrame, views)
        }
    }.attach()
}

/**
 * Returns the number of ancestors of this view.
 * Views without parents return 0.
 */
// @TODO: This should be computed and invalidated when a view is attached to a container
val View?.ancestorCount: Int get() {
    var count = 0
    var parent = this?.parent
    while (parent != null) {
        count++
        parent = parent.parent
    }
    return count
    /*
    val parent = parent ?: return 0
    return parent.ancestorCount + 1
     */
}

/**
 * Returns a list of all the ancestors including this in order to reach from this view to the [target] view,
 * or a list of all the ancestors in the case [target] is not an ancestor.
 */
fun View?.ancestorsUpTo(target: View?): List<View> {
    var current = this
    val out = arrayListOf<View>()
    while (current != null && current != target) {
        out += current
        current = current.parent
    }
    return out
}

/**
 * Returns a list of all the ancestors (including this) to reach the root node (usually the stage).
 */
val View?.ancestors: List<View> get() = ancestorsUpTo(null)

/**
 * Dumps a view and its children for debugging purposes.
 * The [emit] block parameter allows to define how to print those results.
 */
fun View?.dump(indent: String = "", emit: (String) -> Unit = ::println) {
    emit("$indent$this")
    if (this != null && this.isContainer) {
        this.forEachChild { child: View ->
            child.dump("$indent ", emit)
        }
    }
}

/**
 * Dumps a view and its children for debugging purposes into a [String].
 */
fun View?.dumpToString(): String {
    if (this == null) return ""
    val out = arrayListOf<String>()
    dump { out += it }
    return out.joinToString("\n")
}

/**
 * Iterates all the descendant [View]s including this calling the [handler].
 * Iteration happens in [Pre-order (NLR)](https://en.wikipedia.org/wiki/Tree_traversal#Pre-order_(NLR)).
 */
fun View?.foreachDescendant(handler: (View) -> Unit) {
    if (this != null) {
        handler(this)
        if (this.isContainer) {
            this.forEachChild { child: View ->
                child.foreachDescendant(handler)
            }
        }
    }
}

inline fun View?.forEachAscendant(includeThis: Boolean = false, handler: (View) -> Unit) {
    var view = this
    if (!includeThis) view = view?.parent
    while (view != null) {
        handler(view)
        view = view.parent
    }
}

/** Returns a list of descendants having the property [prop] optionally matching the value [value]. */
fun View?.descendantsWithProp(prop: String, value: String? = null): List<View> {
    if (this == null) return listOf()
    return this.descendantsWith {
        if (value != null) {
            it.props[prop] == value
        } else {
            prop in it.props
        }
    }
}

/** Returns a list of descendants having the property [prop] optionally matching the value [value]. */
fun View?.descendantsWithPropString(prop: String, value: String? = null): List<Pair<View, String>> =
    this.descendantsWithProp(prop, value).map { it to it.getPropString(prop) }

/** Returns a list of descendants having the property [prop] optionally matching the value [value]. */
fun View?.descendantsWithPropInt(prop: String, value: Int? = null): List<Pair<View, Int>> =
    this.descendantsWithProp(prop, if (value != null) "$value" else null).map { it to it.getPropInt(prop) }

/** Returns a list of descendants having the property [prop] optionally matching the value [value]. */
fun View?.descendantsWithPropDouble(prop: String, value: Double? = null): List<Pair<View, Int>> =
    this.descendantsWithProp(prop, if (value != null) "$value" else null).map { it to it.getPropInt(prop) }

/** Returns a list of descendants views that are of type [T]. */
inline fun <reified T : View> View.getDescendantsOfType() = this.descendantsWith { it is T }

/** Sets the position [point] of the view and returns this (chaineable). */
inline fun <T : View> T.position(point: IPoint): T = position(point.x, point.y)
inline fun <T : View> T.visible(visible: Boolean): T = this.also { it.visible = visible }
inline fun <T : View> T.name(name: String?): T = this.also { it.name = name }

inline fun <T : View> T.hitShape(crossinline block: @ViewDslMarker VectorBuilder.() -> Unit): T {
    buildPath { block() }.also {
        this.hitShape = it
    }
    return this
}

fun <T : View> T.size(width: Double, height: Double): T {
    this.setSize(width, height)
    return this
}
fun <T : View> T.size(width: Float, height: Float): T = size(width.toDouble(), height.toDouble())
fun <T : View> T.size(width: Int, height: Int): T = size(width.toDouble(), height.toDouble())

/** Returns a list of all the non-null [View.name] values of this and the descendants */
val View?.allDescendantNames
    get(): List<String> {
        val out = arrayListOf<String>()
        foreachDescendant {
            if (it.name != null) out += it.name!!
        }
        return out
    }

/** Tries to find a view matching the [check] method or null if none is found */
fun View?.firstDescendantWith(check: (View) -> Boolean): View? {
    if (this == null) return null
    if (check(this)) return this
    if (this.isContainer) {
        this.forEachChild { child: View ->
            val res = child.firstDescendantWith(check)
            if (res != null) return res
        }
    }
    return null
}

/** Returns a list of descendants including this that matches the [check] method. Allows to provide an [out] array to reduce allocations. */
fun View?.descendantsWith(out: ArrayList<View> = arrayListOf(), check: (View) -> Boolean): List<View> {
    if (this != null) {
        if (check(this)) out += this
        if (this.isContainer) {
            this.forEachChild { child: View ->
                child.descendantsWith(out, check)
            }
        }
    }
    return out
}

inline fun <reified T> View?.descendantsOfType(): List<T> = descendantsWith { it is T } as List<T>

fun View?.allDescendants(out: ArrayList<View> = arrayListOf()): List<View> = descendantsWith { true }

/** Chainable method returning this that sets [View.x] and [View.y] */
fun <T : View> T.xy(x: Double, y: Double): T {
    this.x = x
    this.y = y
    return this
}
fun <T : View> T.xy(x: Float, y: Float): T = xy(x.toDouble(), y.toDouble())
fun <T : View> T.xy(x: Int, y: Int): T = xy(x.toDouble(), y.toDouble())
fun <T : View> T.xy(p: IPoint): T = xy(p.x, p.y)

/** Chainable method returning this that sets [View.x] and [View.y] */
fun <T : View> T.position(x: Double, y: Double): T = xy(x, y)
fun <T : View> T.position(x: Float, y: Float): T = xy(x.toDouble(), y.toDouble())
fun <T : View> T.position(x: Int, y: Int): T = xy(x.toDouble(), y.toDouble())

fun <T : View> T.bounds(left: Double, top: Double, right: Double, bottom: Double): T = xy(left, top).size(right - left, bottom - top)
fun <T : View> T.bounds(rect: Rectangle): T = bounds(rect.left, rect.top, rect.right, rect.bottom)

fun <T : View> T.positionX(x: Double): T {
    this.x = x
    return this
}
fun <T : View> T.positionX(x: Float): T = positionX(x.toDouble())
fun <T : View> T.positionX(x: Int): T = positionX(x.toDouble())

fun <T : View> T.positionY(y: Double): T {
    this.y = y
    return this
}
fun <T : View> T.positionY(y: Float): T = positionY(y.toDouble())
fun <T : View> T.positionY(y: Int): T = positionY(y.toDouble())

fun View.getPositionRelativeTo(view: View, out: Point = Point()): Point {
    val mat = this.parent!!.getConcatMatrix(view, inclusive = false)
    return mat.transform(x, y, out)
}

fun View.setPositionRelativeTo(view: View, pos: Point) {
    val mat = this.parent!!.getConcatMatrix(view, inclusive = false)
    val matInv = mat.inverted()
    val out = matInv.transform(pos)
    this.x = out.x
    this.y = out.y
}

fun View.getPointRelativeTo(pos: Point, view: View, out: Point = Point()): Point {
    val mat = this.getConcatMatrix(view, inclusive = false)
    return mat.transform(pos, out)
}

fun View.getPointRelativeToInv(pos: Point, view: View, out: Point = Point()): Point {
    val mat = this.getConcatMatrix(view, inclusive = false)
    val matInv = mat.inverted()
    matInv.transform(pos, out)
    return out
}

/** Chainable method returning this that sets [this] View in the middle between [x1] and [x2] */
fun <T : View> T.centerXBetween(x1: Double, x2: Double): T {
    this.x = (x2 + x1 - this.width) / 2
    return this
}
fun <T : View> T.centerXBetween(x1: Float, x2: Float): T = centerXBetween(x1.toDouble(), x2.toDouble())
fun <T : View> T.centerXBetween(x1: Int, x2: Int): T = centerXBetween(x1.toDouble(), x2.toDouble())

/** Chainable method returning this that sets [this] View in the middle between [y1] and [y2] */
fun <T : View> T.centerYBetween(y1: Double, y2: Double): T {
    this.y = (y2 + y1 - this.height) / 2
    return this
}
fun <T : View> T.centerYBetween(y1: Float, y2: Float): T = centerYBetween(y1.toDouble(), y2.toDouble())
fun <T : View> T.centerYBetween(y1: Int, y2: Int): T = centerYBetween(y1.toDouble(), y2.toDouble())

/**
 * Chainable method returning this that sets [this] View
 * in the middle between [x1] and [x2] and in the middle between [y1] and [y2]
 */
fun <T : View> T.centerBetween(x1: Double, y1: Double, x2: Double, y2: Double): T = this.centerXBetween(x1, x2).centerYBetween(y1, y2)
fun <T : View> T.centerBetween(x1: Float, y1: Float, x2: Float, y2: Float): T = centerBetween(x1.toDouble(), y1.toDouble(), x2.toDouble(), y2.toDouble())
fun <T : View> T.centerBetween(x1: Int, y1: Int, x2: Int, y2: Int): T = centerBetween(x1.toDouble(), y1.toDouble(), x2.toDouble(), y2.toDouble())

/**
 * Chainable method returning this that sets [View.x] so that
 * [this] View is centered on the [other] View horizontally
 */
fun <T : View> T.centerXOn(other: View): T = this.alignX(other, 0.5, true)

/**
 * Chainable method returning this that sets [View.y] so that
 * [this] View is centered on the [other] View vertically
 */
fun <T : View> T.centerYOn(other: View): T = this.alignY(other, 0.5, true)

/**
 * Chainable method returning this that sets [View.x] and [View.y]
 * so that [this] View is centered on the [other] View
 */
fun <T : View> T.centerOn(other: View): T = this.centerXOn(other).centerYOn(other)

fun <T : View> T.centerXOnStage(): T = this.centerXOn(root)
fun <T : View> T.centerYOnStage(): T = this.centerYOn(root)
fun <T : View> T.centerOnStage(): T = this.centerXOnStage().centerYOnStage()

fun <T : View> T.alignXY(other: View, ratio: Double, inside: Boolean, doX: Boolean, padding: Double = 0.0): T {
    //val parent = this.parent
    //val bounds = other.getBoundsNoAnchoring(this)
    val bounds = other.getBoundsNoAnchoring(parent)
    val localBounds = this.getLocalBoundsOptimized()

    //bounds.setTo(other.x, other.y, other.unscaledWidth, other.unscaledHeight)
    val ratioM1_1 = (ratio * 2 - 1)
    val rratioM1_1 = if (inside) ratioM1_1 else -ratioM1_1
    val iratio = if (inside) ratio else 1.0 - ratio
    //println("this: $this, other: $other, bounds=$bounds, scaledWidth=$scaledWidth, scaledHeight=$scaledHeight, width=$width, height=$height, scale=$scale, $scaleX, $scaleY")
    if (doX) {
        x = (bounds.x + (bounds.width * ratio) - localBounds.left) - (this.scaledWidth * iratio) - (padding * rratioM1_1)
    } else {
        y = (bounds.y + (bounds.height * ratio) - localBounds.top) - (this.scaledHeight * iratio) - (padding * rratioM1_1)
    }
    return this
}

fun <T : View> T.alignX(other: View, ratio: Double, inside: Boolean, padding: Double = 0.0): T {
    return alignXY(other, ratio, inside, doX = true, padding = padding)
}

fun <T : View> T.alignY(other: View, ratio: Double, inside: Boolean, padding: Double = 0.0): T {
    return alignXY(other, ratio, inside, doX = false, padding = padding)
}

/**
 * Chainable method returning this that sets [View.x] so that
 * [this] View's left side is aligned with the [other] View's left side
 */
// @TODO: What about rotations? we might need to adjust y too?
fun <T : View> T.alignLeftToLeftOf(other: View, padding: Double = 0.0): T = alignX(other, 0.0, inside = true, padding = padding)
fun <T : View> T.alignLeftToLeftOf(other: View, padding: Float): T = alignLeftToLeftOf(other, padding.toDouble())
fun <T : View> T.alignLeftToLeftOf(other: View, padding: Int): T = alignLeftToLeftOf(other, padding.toDouble())

/**
 * Chainable method returning this that sets [View.x] so that
 * [this] View's left side is aligned with the [other] View's right side
 */
fun <T : View> T.alignLeftToRightOf(other: View, padding: Double = 0.0): T = alignX(other, 1.0, inside = false, padding = padding)
fun <T : View> T.alignLeftToRightOf(other: View, padding: Float): T = alignLeftToRightOf(other, padding.toDouble())
fun <T : View> T.alignLeftToRightOf(other: View, padding: Int): T = alignLeftToRightOf(other, padding.toDouble())

/**
 * Chainable method returning this that sets [View.x] so that
 * [this] View's right side is aligned with the [other] View's left side
 */
fun <T : View> T.alignRightToLeftOf(other: View, padding: Double = 0.0): T = alignX(other, 0.0, inside = false, padding = padding)
fun <T : View> T.alignRightToLeftOf(other: View, padding: Float): T = alignRightToLeftOf(other, padding.toDouble())
fun <T : View> T.alignRightToLeftOf(other: View, padding: Int): T = alignRightToLeftOf(other, padding.toDouble())

/**
 * Chainable method returning this that sets [View.x] so that
 * [this] View's right side is aligned with the [other] View's right side
 */
fun <T : View> T.alignRightToRightOf(other: View, padding: Double = 0.0): T = alignX(other, 1.0, inside = true, padding = padding)
fun <T : View> T.alignRightToRightOf(other: View, padding: Float): T = alignRightToRightOf(other, padding.toDouble())
fun <T : View> T.alignRightToRightOf(other: View, padding: Int): T = alignRightToRightOf(other, padding.toDouble())

/**
 * Chainable method returning this that sets [View.y] so that
 * [this] View's top side is aligned with the [other] View's top side
 */
fun <T : View> T.alignTopToTopOf(other: View, padding: Double = 0.0): T = alignY(other, 0.0, inside = true, padding = padding)
fun <T : View> T.alignTopToTopOf(other: View, padding: Float): T = alignTopToTopOf(other, padding.toDouble())
fun <T : View> T.alignTopToTopOf(other: View, padding: Int): T = alignTopToTopOf(other, padding.toDouble())

/**
 * Chainable method returning this that sets [View.y] so that
 * [this] View's top side is aligned with the [other] View's bottom side
 */
fun <T : View> T.alignTopToBottomOf(other: View, padding: Double = 0.0): T = alignY(other, 1.0, inside = false, padding = padding)
fun <T : View> T.alignTopToBottomOf(other: View, padding: Float): T = alignTopToBottomOf(other, padding.toDouble())
fun <T : View> T.alignTopToBottomOf(other: View, padding: Int): T = alignTopToBottomOf(other, padding.toDouble())

/**
 * Chainable method returning this that sets [View.y] so that
 * [this] View's bottom side is aligned with the [other] View's top side
 */
fun <T : View> T.alignBottomToTopOf(other: View, padding: Double = 0.0): T = alignY(other, 0.0, inside = false, padding = padding)
fun <T : View> T.alignBottomToTopOf(other: View, padding: Float): T = alignBottomToTopOf(other, padding.toDouble())
fun <T : View> T.alignBottomToTopOf(other: View, padding: Int): T = alignBottomToTopOf(other, padding.toDouble())

/**
 * Chainable method returning this that sets [View.y] so that
 * [this] View's bottom side is aligned with the [other] View's bottom side
 */
fun <T : View> T.alignBottomToBottomOf(other: View, padding: Double = 0.0): T = alignY(other, 1.0, inside = true, padding = padding)
fun <T : View> T.alignBottomToBottomOf(other: View, padding: Float): T = alignBottomToBottomOf(other, padding.toDouble())
fun <T : View> T.alignBottomToBottomOf(other: View, padding: Int): T = alignBottomToBottomOf(other, padding.toDouble())

/** Chainable method returning this that sets [View.rotation] */
fun <T : View> T.rotation(rot: Angle): T {
    this.rotation = rot
    return this
}

/** Chainable method returning this that sets [View.skewX] and [View.skewY] */
fun <T : View> T.skew(sx: Angle, sy: Angle): T {
    this.skewX = sx
    this.skewY = sy
    return this
}

/** Chainable method returning this that sets [View.scaleX] and [View.scaleY] */
fun <T : View> T.scale(sx: Double, sy: Double = sx): T {
    this.scaleX = sx
    this.scaleY = sy
    return this
}
fun <T : View> T.scale(sx: Float, sy: Float = sx): T = scale(sx.toDouble(), sy.toDouble())
fun <T : View> T.scale(sx: Int, sy: Int = sx): T = scale(sx.toDouble(), sy.toDouble())

/** Chainable method returning this that sets [View.alpha] */
fun <T : View> T.alpha(alpha: Double): T {
    this.alpha = alpha
    return this
}
fun <T : View> T.alpha(alpha: Float): T = alpha(alpha.toDouble())
fun <T : View> T.alpha(alpha: Int): T = alpha(alpha.toDouble())

@Target(AnnotationTarget.TYPE, AnnotationTarget.CLASS) annotation class ViewDslMarker
// @TODO: This causes issues having to put some explicit this@ when it shouldn't be required
//typealias ViewDslMarker = KorDslMarker

interface ViewLeaf

fun View?.findFirstAscendant(cond: (view: View) -> Boolean): View? {
    var current: View? = this
    while (current != null) {
        if (cond(current)) return current
        current = current.parent
    }
    return null
}

fun View?.findLastAscendant(cond: (view: View) -> Boolean): View? {
    var current: View? = this
    var result: View? = null
    while (current != null) {
        if (cond(current)) result = current
        current = current.parent
    }
    return result
}

///////////////////


val View.prevSibling get() = parent?.children?.getOrNull(index - 1)
val View.nextSibling get() = parent?.children?.getOrNull(index + 1)

val Stage.lastTreeView: View
    get() {
        var view: View? = this
        while (view != null) {
            if (view is Container) {
                if (view.numChildren == 0) return view
                view = view.lastChild
            } else {
                return view
            }
        }
        return view ?: stage
    }

fun View.nextView(): View? {
    if (this is Container && this.numChildren > 0) return this.firstChild
    var cur: View? = this
    while (cur != null) {
        cur.nextSibling?.let { return it }
        cur = cur.parent
    }
    return null
}

fun View.prevView(): View? {
    prevSibling?.let { return it }
    return parent
}

inline fun View.nextView(filter: (View) -> Boolean): View? {
    var view = this
    while (true) {
        view = view.nextView() ?: return null
        if (filter(view)) return view
    }
}

inline fun View.prevView(filter: (View) -> Boolean): View? {
    var view = this
    while (true) {
        view = view.prevView() ?: return null
        if (filter(view)) return view
    }
}

inline fun <reified T> View.nextViewOfType(): T? = nextView { it is T } as? T?
inline fun <reified T> View.prevViewOfType(): T? = prevView { it is T } as? T?

fun View?.isDescendantOf(other: View, include: Boolean = true): Boolean {
    var current: View? = this
    if (!include) current = current?.parent
    while (current != null && current != other) {
        current = current.parent
    }
    return current == other
}

/*
fun <T : BaseView> T.addMouseComponent(block: (view: T, views: Views, event: MouseEvent) -> Unit): MouseComponent {
    return addComponent(object : MouseComponent {
        override val view: T = this@addMouseComponent
        override fun onMouseEvent(views: Views, event: MouseEvent) = block(view, views, event)
    })
}

fun <T : BaseView> T.addKeyComponent(block: (view: T, event: KeyEvent) -> Unit): KeyComponent {
    return addComponent(object : KeyComponent {
        override val view: T = this@addKeyComponent
        override fun Views.onKeyEvent(event: KeyEvent) = block(view, event)
    })
}
*/
