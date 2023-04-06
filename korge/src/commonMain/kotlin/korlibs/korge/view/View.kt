@file:OptIn(KorgeInternal::class)

package korlibs.korge.view

import korlibs.crypto.encoding.*
import korlibs.datastructure.*
import korlibs.datastructure.iterators.*
import korlibs.event.*
import korlibs.image.color.*
import korlibs.image.vector.*
import korlibs.io.lang.*
import korlibs.io.util.*
import korlibs.io.util.encoding.*
import korlibs.korge.component.*
import korlibs.korge.internal.*
import korlibs.korge.render.*
import korlibs.korge.ui.*
import korlibs.korge.view.filter.*
import korlibs.korge.view.property.*
import korlibs.math.geom.*
import korlibs.math.geom.shape.*
import korlibs.math.geom.vector.*
import korlibs.math.interpolation.*
import korlibs.memory.*
import korlibs.time.*
import kotlin.jvm.*
import kotlin.math.*

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
 * Regarding to how the views are drawn there are: [alphaF], [colorMul] ([tint]).
 *
 * [View] implements the [Extra] interface, thus allows to add arbitrary typed properties.
 * [View] implements the [EventListener] interface, and allows to handle and dispatch events.
 *
 * For views with [Updatable] components, [View] include a [speed] property where 1 is 1x and 2 is 2x the speed.
 */
@OptIn(KorgeInternal::class)
abstract class View internal constructor(
    /** Indicates if this class is a container or not. This is only overridden by Container. This check is performed like this, to avoid type checks. That might be an expensive operation in some targets. */
    val isContainer: Boolean
) : BaseView(), Renderable
    , BView
    , HitTestable
    , WithHitShape2D
//, EventDispatcher by EventDispatcher.Mixin()
{
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
    open val anchorDispX: Float get() = 0f
    @KorgeInternal
    open val anchorDispY: Float get() = 0f

    val anchorDispXF: Float get() = anchorDispX.toFloat()
    val anchorDispYF: Float get() = anchorDispY.toFloat()

    /** Read-only internal children list, or null when not a [Container] */
    @KorgeInternal
    @PublishedApi
    internal open val _children: List<View>? get() = null

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

    private var _hitShape2d: Shape2D? = null

    @Deprecated("Use hitShape2d instead")
    open var hitShape: VectorPath? = null
    @Deprecated("Use hitShape2d instead")
    open var hitShapes: List<VectorPath>? = null

    override var hitShape2d: Shape2D
        get() {
            if (_hitShape2d == null) {
                if (_hitShape2d == null && hitShapes != null) _hitShape2d = hitShapes!!.toShape2d()
                if (_hitShape2d == null && hitShape != null) _hitShape2d = hitShape!!.toShape2d()
                //if (_hitShape2d == null) _hitShape2d = Shape2d.Rectangle(getLocalBounds())
            }
            return _hitShape2d ?: EmptyShape2d
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
    @ViewProperty(min = 0.0, max = 1.0, clampMin = false, clampMax = false)
    open var ratio: Float = 0f

    @PublishedApi internal var _index: Int = 0
    @PublishedApi internal var _parent: Container? = null

    /** The index the child has in its parent */
    var index: Int
        get() = _index
        internal set(value) { _index = value }

    /** Ratio speed of this node, affecting all the [View.addUpdater] */
    @ViewProperty(min = -1.0, max = 1.0, clampMin = false, clampMax = false)
    var speed: Float = 1f

    internal var _stage: Stage? = null
        set(value) {
            if (field === value) return
            field = value
            forEachChild { it._stage = value }
        }

    var _invalidateNotifier: InvalidateNotifier? = null
        internal set(value) {
            if (field === value) return
            field = value
            val parent = _invalidateNotifierForChildren
            forEachChild { it._invalidateNotifier = parent }
        }

    open val _invalidateNotifierForChildren: InvalidateNotifier? get() = _invalidateNotifier

    protected open fun setInvalidateNotifier() {
        _invalidateNotifier = _parent?._invalidateNotifierForChildren
    }

    /** Parent [Container] of [this] View if any, or null */
    var parent: Container?
        get() = _parent
        internal set(value) {
            if (_parent === value) return
            _parent = value
            _stage = _parent?._stage
            setInvalidateNotifier()
            onParentChanged()
            changeEventListenerParent(value)
        }

    /** Optional name of this view */
    @ViewProperty()
    var name: String? = null

    /** The [BlendMode] used for this view [BlendMode.INHERIT] will use the ancestors [blendMode]s */
    @ViewProperty
    @ViewPropertyProvider(provider = BlendMode.Provider::class)
    var blendMode: BlendMode = BlendMode.INHERIT
        set(value) {
            if (field != value) {
                field = value
                invalidate()
            }
        }

    /** Computed [speed] combining all the speeds from ancestors */
    val globalSpeed: Float get() = if (parent != null) parent!!.globalSpeed * speed else speed

    protected var _x: Float = 0f
    protected var _y: Float = 0f
    private var _scaleX: Float = 1f
    private var _scaleY: Float = 1f
    private var _skewX: Angle = Angle.ZERO
    private var _skewY: Angle = Angle.ZERO
    private var _rotation: Angle = Angle.ZERO

    private fun setXY(x: Float, y: Float) {
        ensureTransform()
        if (this._x != x || this._y != y) {
            this._x = x
            this._y = y
            invalidateMatrix()
        }
    }

    @Deprecated("Use pos instead")
    fun getPosition(out: MPoint = MPoint()): MPoint {
        out.copyFrom(out)
        return out
    }

    @ViewProperty(min = -1000.0, max = +1000.0, name = "position")
    var pos: Point
        get() = Point(x, y)
        set(value) = setXY(value.x, value.y)

    /** Local X position of this view */
    var x: Float
        get() {
            ensureTransform()
            return _x
        }
        set(v) { setXY(v, y) }

    /** Local Y position of this view */
    var y: Float
        get() {
            ensureTransform()
            return _y
        }
        set(v) { setXY(x, v) }

    /** Local X position of this view */
    var xD: Double get() = x.toDouble() ; set(v) { x = v.toFloat() }
    /** Local Y position of this view */
    var yD: Double get() = y.toDouble() ; set(v) { y = v.toFloat() }

    private var _zIndex: Float = 0f

    // @TODO: Instead of resort everytime that something changes, let's keep an index in the zIndex collection
    //@PublishedApi internal var _zIndexIndex: Int = 0

    @ViewProperty
    var zIndex: Float
        get() = _zIndex
        set(v) {
            parent?.updatedChildZIndex(this, _zIndex, v)
            _zIndex = v
        }

    var zIndexD: Double get() = zIndex.toDouble() ; set(v) { zIndex = v.toFloat() }

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

    var scaleAvg: Float
        get() = scale.scaleAvg
        set(value) {
            scale = Scale(value, value)
        }

    /** Local scaling in the X axis of this view */
    var scaleX: Float
        get() { ensureTransform(); return _scaleX }
        set(v) { ensureTransform(); if (_scaleX != v) { _scaleX = v; invalidateMatrix() } }

    /** Local scaling in the Y axis of this view */
    var scaleY: Float
        get() { ensureTransform(); return _scaleY }
        set(v) { ensureTransform(); if (_scaleY != v) { _scaleY = v; invalidateMatrix() } }

    /** Local scaling in the X axis of this view */
    var scaleXD: Double get() = scaleX.toDouble(); set(value) { scaleX = value.toFloat() }

    /** Local scaling in the Y axis of this view */
    var scaleYD: Double get() = scaleY.toDouble(); set(value) { scaleY = value.toFloat() }

    @ViewProperty(name = "type", order = -1000, editable = false)
    private val __type: String
        get() = this::class.simpleName ?: "Unknown"

    @ViewProperty(min = 0.0, max = 1.0)
    var scale: Scale
        get() = Scale(scaleX, scaleY)
        set(value) {
            scaleX = value.scaleX
            scaleY = value.scaleY
        }

    var scaleXY: Float
        get() = (scaleX + scaleY) / 2f
        set(v) { scaleX = v; scaleY = v }

    /** Allows to change [scaleXD] and [scaleYD] at once. Returns the mean value of x and y scales. */
    var scaleD: Double
        get() = (scaleXD + scaleYD) / 2f
        set(v) { scaleXD = v; scaleYD = v }

    /** Local skewing in the X axis of this view */
    var skewX: Angle
        get() { ensureTransform(); return _skewX }
        set(v) { ensureTransform(); if (_skewX != v) { _skewX = v; invalidateMatrix() } }

    /** Local skewing in the Y axis of this view */
    var skewY: Angle
        get() { ensureTransform(); return _skewY }
        set(v) { ensureTransform(); if (_skewY != v) { _skewY = v; invalidateMatrix() } }

    /** Local rotation of this view */
    @ViewProperty()
    var rotation: Angle
        get() { ensureTransform(); return _rotation }
        set(v) { ensureTransform(); if (_rotation != v) { _rotation = v; invalidateMatrix() } }

    @ViewProperty(name = "skew")
    private var skewXY: Pair<Angle, Angle>
        get() = skewX to skewY
        set(v) {
            skewX = v.first
            skewY = v.second
        }

    var globalPos: Point
        get() = parent?.localToGlobal(Point(x, y)) ?: Point(x, y)
        set(value) { pos = parent?.globalToLocal(value) ?: value }

    var size: Size get() = unscaledSize ; set(value) { unscaledSize = value }

    open var unscaledSize: Size
        get() = getLocalBounds().size
        set(value) {
            val size = this.size
            scale = Scale(
                (if (scaleX == 0f) 1f else scaleX) * (value.width / size.width),
                (if (scaleY == 0f) 1f else scaleY) * (value.height / size.height)
            )
        }
    var unscaledWidth: Float
        get() = unscaledSize.width
        set(value) { unscaledSize = unscaledSize.copy(width = value) }
    var unscaledHeight: Float
        get() = unscaledSize.height
        set(value) { unscaledSize = unscaledSize.copy(height = value) }
    var unscaledWidthD: Double get() = unscaledWidth.toDouble() ; set(value) { unscaledWidth = value.toFloat() }
    var unscaledHeightD: Double get() = unscaledHeight.toDouble() ; set(value) { unscaledHeight = value.toFloat() }


    @ViewProperty(min = -1000.0, max = +1000.0, name = "size")
    var scaledSize: Size
        get() = unscaledSize * scale
        set(value) {
            unscaledSize = Size(
                if (scaleX == 0f) value.width else value.width / scaleX,
                if (scaleY == 0f) value.height else value.height / scaleY,
            )
        }

    var scaledWidth: Float get() = scaledSize.width ;set(value) { scaledSize = scaledSize.copy(width = value) }
    var scaledHeight: Float get() = scaledSize.height ; set(value) { scaledSize = scaledSize.copy(height = value) }
    var scaledWidthD: Double get() = scaledWidth.toDouble() ; set(value) { scaledWidth = value.toFloat() }
    var scaledHeightD: Double get() = scaledHeight.toDouble() ; set(value) { scaledHeight = value.toFloat() }

    /**
     * Changes the [width] of this view. Generically, this means adjusting the [scaleX] of the view to match that size using the current bounds,
     * but some views might override this to adjust its internal width or height (like [SolidRect] or [UIView] for example).
     *
     * To mutate use [unscaledWidth] or [scaledWidth].
     */
    var width: Float get() = unscaledWidth ; set(value) { unscaledWidth = value }

    /**
     * Changes the [height] of this view. Generically, this means adjusting the [scaleY] of the view to match that size using the current bounds,
     * but some views might override this to adjust its internal width or height (like [SolidRect] or [UIView] for example).
     *
     * To mutate use [unscaledHeight] or [scaledHeight].
     */
    var height: Float get() = unscaledHeight ; set(value) { unscaledHeight = value }

    var widthD: Double get() = unscaledWidth.toDouble() ; set(value) { unscaledWidth = value.toFloat() }
    var heightD: Double get() = unscaledHeight.toDouble() ; set(value) { unscaledHeight = value.toFloat() }

    /**
     * The multiplicative [RGBA] color.
     *
     * That means:
     * * [Colors.WHITE] would display the view without modifications
     * * [Colors.BLACK] would display a black shape
     * * [Colors.TRANSPARENT] would be equivalent to setting [alphaF]=0
     * * [Colors.RED] would only show the red component of the view
     */
    @ViewProperty
    var colorMul: RGBA
        get() = _colorTransform.colorMul
        set(v) {
            if (v != _colorTransform.colorMul) {
                _colorTransform.colorMul = v
                invalidateColorTransform()
            }
        }

    /**
     * Shortcut for adjusting the multiplicative alpha value manually.
     * Equivalent to [ColorTransform.mA] + [View.invalidate]
     */
    @ViewProperty(min = 0.0, max = 1.0, clampMin = true, clampMax = true)
    var alpha: Float
        get() = _colorTransform.a
        set(v) {
            if (_colorTransform.a != v) {
                _colorTransform.a = v
                invalidateColorTransform()
            }
        }

    var alphaD: Double
        get() = alphaF.toDouble()
        set(value) {
            alphaF = value.toFloat()
        }

    var alphaF: Float by this::alpha


    /** Alias for [colorMul] to make this familiar to people coming from other engines. */
    var tint: RGBA
        get() = this.colorMul
        set(value) {
            this.colorMul = value
        }

    protected fun ensureTransform() {
        if (validLocalProps) return
        validLocalProps = true
        val t = this._localMatrix.toTransform()
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
    open val stage: Stage? get() = _stage

    /** Determines if mouse events will be handled for this view and its children */
    //open var mouseEnabled: Boolean = true
    open var mouseEnabled: Boolean = false
    open var mouseChildren: Boolean = true

    /** Determines if the view will be displayed or not. It is different to alpha=0, since the render method won't be executed. Usually giving better performance. But also not receiving events. */
    @ViewProperty()
    open var visible: Boolean = true
        set(value) {
            if (field != value) {
                field = value
                invalidate()
            }
        }

    /** Sets the local transform matrix that includes [xD], [yD], [scaleXD], [scaleYD], [rotation], [skewX] and [skewY] encoded into a [Matrix] */
    fun setMatrix(matrix: Matrix) {
        this._localMatrix = matrix
        this.validLocalProps = false
        invalidate()
        invalidateLocalBounds()
    }

    /** Like [setMatrix] but directly sets an interpolated version of the [l] and [r] matrices with the [ratio] */
    fun setMatrixInterpolated(ratio: Double, l: Matrix, r: Matrix) {
        this._localMatrix = ratio.toRatio().interpolate(l, r)
        this.validLocalProps = false
        invalidate()
        invalidateLocalBounds()
    }

    ///**
    // * Sets the computed transform [Matrix] and all the decomposed transform properties at once.
    // * Normally this is used by animation libraries to set Views in a way that are fast to update
    // * and to access.
    // */
    //fun setComputedTransform(transform: MatrixComputed) {
    //    _localMatrix.copyFrom(transform.matrix)
    //    _setTransform(transform.transform)
    //    invalidate()
    //    validLocalProps = true
    //    validLocalMatrix = true
    //}

    /**
     * Sets the [MatrixTransform] decomposed version of the transformation,
     * that directly includes [xD], [yD], [scaleXD], [scaleYD], [rotation], [skewX] and [skewY].
     */
    fun setTransform(transform: MatrixTransform) {
        _setTransform(transform)
        invalidate()
        invalidateLocalBounds()
        validLocalProps = true
        validLocalMatrix = false
    }

    /**
     * Like [setTransform] but without invalidation. If used at all, should be used with care and invalidate when required.
     */
    @KorgeInternal
    @Deprecated("")
    fun _setTransform(t: MatrixTransform) {
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

    private var _localMatrix = Matrix.IDENTITY

    /**
     * Local transform [MMatrix]. If you plan to change its components manually
     * instead of setting it directly, you should call the [View.invalidate] method.
     */
    var localMatrix: Matrix
        get() {
            if (!validLocalMatrix) {
                validLocalMatrix = true
                _requireInvalidate = true
                _localMatrix = Matrix.fromTransform(x, y, rotation, scaleX, scaleY, skewX, skewY)
            }
            return _localMatrix
        }
        set(value) {
            setMatrix(value)
            invalidate()
            invalidateLocalBounds()
        }

    private var _globalMatrix = Matrix.IDENTITY
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
                _globalMatrix = (if (parent != null) localMatrix * parent!!.globalMatrix else localMatrix)
            }
            return _globalMatrix
        }
        set(value) {
            _requireInvalidate = true
            this.localMatrix = (if (parent != null) value * parent!!.globalMatrixInv else value)
        }

    private var _globalMatrixInv = Matrix.IDENTITY
    private var _globalMatrixInvVersion = -1

    /**
     * The inverted version of the [globalMatrix]
     */
    val globalMatrixInv: Matrix
        get() {
            if (_globalMatrixInvVersion != this._version) {
                _globalMatrixInvVersion = this._version
                _requireInvalidate = true
                _globalMatrixInv = this.globalMatrix.inverted()
            }
            return _globalMatrixInv
        }

    private val _colorTransform: ColorTransformMul = ColorTransformMul()
    private val _renderColorTransform: ColorTransformMul = ColorTransformMul()
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
    protected val renderColorTransform: ColorTransformMul get() {
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

    /** The concatenated/global version of the local [alpha] */
    val renderAlpha: Float get() {
        updateRenderColorTransformIfRequired()
        return renderColorTransform.a
    }

    /** The concatenated/global version of the local [alpha] */
    val renderAlphaD: Double get() = renderAlpha.toDouble()

    /** Computes the local X and Y coordinates of the mouse using the coords from the [Views] object */
    fun localMousePos(views: Views): Point = globalToLocal(views.input.mousePos)

    /**
     * Invalidates the [localMatrix] [MMatrix], so it gets updated from the decomposed properties: [xD], [yD], [scaleXD], [scaleYD], [rotation], [skewX] and [skewY].
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
        invalidateRender()
    }

    private var cachedLocalBounds: Rectangle? = null
    fun invalidateLocalBounds() {
        if (cachedLocalBounds != null) {
            cachedLocalBounds = null
            this.parent?.invalidateLocalBounds()
        }
    }

    protected open fun onParentChanged() {
    }

    override fun invalidateRender() {
        _invalidateNotifier?.invalidatedView(this)
        invalidateLocalBounds()
        //stage?.views?.invalidatedView(this)
    }

    open fun invalidateColorTransform() {
        this._versionColor++
        _requireInvalidateColor = false
        dirtyVertices = true
        invalidateRender()
    }

    var debugAnnotate: Boolean = false

    @PublishedApi
    internal var _renderPhases: FastArrayList<ViewRenderPhase>? = null

    val renderPhases: List<ViewRenderPhase> get() = _renderPhases ?: emptyList<ViewRenderPhase>()

    inline fun <reified T : ViewRenderPhase> removeRenderPhaseOfType() {
        _renderPhases?.removeAll { it is T }
    }

    inline fun <reified T : ViewRenderPhase> getRenderPhaseOfTypeOrNull(): T? = _renderPhases?.firstOrNull { it is T } as? T?

    fun addRenderPhase(phase: ViewRenderPhase) {
        if (_renderPhases == null) _renderPhases = fastArrayListOf()
        _renderPhases?.add(phase)
        _renderPhases?.sortBy { it.priority }
    }

    inline fun <reified T : ViewRenderPhase> replaceRenderPhase(create: () -> T) {
        removeRenderPhaseOfType<T>()
        addRenderPhase(create())
    }

    inline fun <reified T : ViewRenderPhase> getOrCreateAndAddRenderPhase(create: () -> T): T {
        getRenderPhaseOfTypeOrNull<T>()?.let { return it }
        return create().also { addRenderPhase(it) }
    }

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
        _renderPhases?.fastForEach { it.beforeRender(this, ctx) }
        try {
            renderFirstPhase(ctx)
        } finally {
            _renderPhases?.fastForEachReverse { it.afterRender(this, ctx) }
        }
    }

    fun renderFirstPhase(ctx: RenderContext) {
        val oldPhase = currentStage
        try {
            currentStage = 0
            renderNextPhase(ctx)
        } finally {
            currentStage = oldPhase
        }
    }

    private var currentStage: Int = 0
    fun renderNextPhase(ctx: RenderContext) {
        val stages = _renderPhases
        when {
            stages != null && currentStage < stages.size -> {
                stages[currentStage++].render(this, ctx)
            }
            currentStage == (stages?.size ?: 0) -> {
                renderInternal(ctx)
                currentStage++
            }
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
        val local = getLocalBounds()
        ctx.useLineBatcher { lines ->
            lines.blending(BlendMode.INVERT) {
                lines.drawVector(Colors.RED) {
                    rect(globalBounds)
                }
            }
            lines.drawVector(Colors.RED) {
                moveTo(localToGlobal(Point(local.left, local.top)))
                lineTo(localToGlobal(Point(local.right, local.top)))
                lineTo(localToGlobal(Point(local.right, local.bottom)))
                lineTo(localToGlobal(Point(local.left, local.bottom)))
                close()
            }
            lines.drawVector(Colors.YELLOW) {
                val anchorSize = 6f * ctx.views!!.windowToGlobalScaleAvg.toFloat()
                circle(localToGlobal(local.topLeft), anchorSize)
                circle(localToGlobal(local.topRight), anchorSize)
                circle(localToGlobal(local.bottomRight), anchorSize)
                circle(localToGlobal(local.bottomLeft), anchorSize)
                circle(localToGlobal(Ratio.HALF.interpolate(local.topLeft, local.topRight)), anchorSize)
                circle(localToGlobal(Ratio.HALF.interpolate(local.topRight, local.bottomRight)), anchorSize)
                circle(localToGlobal(Ratio.HALF.interpolate(local.bottomRight, local.bottomLeft)), anchorSize)
                circle(localToGlobal(Ratio.HALF.interpolate(local.bottomLeft, local.topLeft)), anchorSize)
            }
            lines.drawVector(Colors.BLUE) {
                val center = globalPos
                line(Point(center.x, center.y - 5), Point(center.x, center.y + 5))
                line(Point(center.x - 5, center.y), Point(center.x + 5, center.y))
            }
        }

        //ctx.flush()
    }

    /** Method that all views must override in order to control how the view is going to be rendered */
    protected abstract fun renderInternal(ctx: RenderContext)

    @Suppress("RemoveCurlyBracesFromTemplate")
    override fun toString(): String {
        var out = this::class.portableSimpleName
        if (x != 0f || y != 0f) out += ":pos=(${x.str},${y.str})"
        if (scaleX != 1f || scaleY != 1f) out += ":scale=(${scaleXD.str},${scaleYD.str})"
        if (skewX != Angle.ZERO || skewY != Angle.ZERO) out += ":skew=(${skewX.degreesD.str},${skewY.degreesD.str})"
        if (rotation.absoluteValue != Angle.ZERO) out += ":rotation=(${rotation.degreesD.str}º)"
        if (name != null) out += ":name=($name)"
        if (blendMode != BlendMode.INHERIT) out += ":blendMode=($blendMode)"
        if (!visible) out += ":visible=$visible"
        if (alphaF != 1f) out += ":alpha=${alphaF.niceStr(2)}"
        if (this.colorMul.rgb != Colors.WHITE.rgb) out += ":colorMul=${this.colorMul.hexString}"
        return out
    }

    protected val Float.str get() = this.toStringDecimal(2, skipTrailingZeros = true)
    protected val Double.str get() = this.toStringDecimal(2, skipTrailingZeros = true)

    // Version with root-most object as reference
    /** Converts the global point [p] (using root/stage as reference) into the local coordinate system. Allows to define [out] to avoid allocation. */
    fun globalToLocal(p: Point): Point = this.globalMatrixInv.transform(p)

    fun globalToLocalDelta(p0: Point, p1: Point): Point = globalToLocal(p1) - globalToLocal(p0)

    /** Converts the local point [p] into a global point (using root/stage as reference). Allows to define [out] to avoid allocation. */
    fun localToGlobal(p: Point): Point = this.globalMatrix.transform(p)

    // Version with View.Reference as reference
    /** Converts a point [p] in the nearest ancestor marked as [View.Reference] into the local coordinate system. Allows to define [out] to avoid allocation. */
    @Deprecated("Use globalToLocal") fun renderToLocal(p: Point): Point = this.globalMatrixInv.transform(p)

    /** Converts the local point [p] into a point in window coordinates. */
    fun localToWindow(views: Views, p: Point): Point =
        views.globalToWindowMatrix.transform(this.globalMatrix.transform(p))

    var hitTestEnabled = true

    /**
     * Determines the view at the global point defined by [xD] and [yD] if any, or null
     *
     * When a container, recursively finds the [View] displayed the given global [xD], [yD] coordinates.
     *
     * @returns The (visible) [View] displayed at the given coordinates or `null` if none is found.
     */
    open fun hitTest(globalPos: Point, direction: HitTestDirection = HitTestDirection.ANY): View? {
        if (!hitTestEnabled) return null
        if (!visible) return null

        _children?.fastForEachReverse { child ->
            child.hitTest(globalPos, direction)?.let {
                return it
            }
        }
        val res = hitTestInternal(globalPos)
        if (res != null) return res
        return if (this is Stage) this else null
    }

    fun hitTestLocal(p: Point, direction: HitTestDirection = HitTestDirection.ANY): View? =
        hitTest(localToGlobal(p), direction)

    override fun hitTestAny(p: Point, direction: HitTestDirection): Boolean =
        hitTest(p, direction) != null

    fun hitTestView(views: List<View>, direction: HitTestDirection = HitTestDirection.ANY): View? {
        views.fastForEach { view -> hitTestView(view, direction)?.let { return it } }
        return null
    }

    fun hitTestView(view: View, direction: HitTestDirection = HitTestDirection.ANY): View? {
        if (!hitTestEnabled) return null
        if (!visible) return null
        if (_hitShape2d == null) {
            _children?.fastForEachReverse { child ->
                if (child !== view) {
                    child.hitTestView(view, direction)?.let {
                        return it
                    }
                }
            }
        }
        val res = hitTestShapeInternal(view.hitShape2d, view.getGlobalMatrixWithAnchor(), direction)
        if (res != null) return res
        return if (this is Stage) this else null
    }

    fun hitTestShape(shape: Shape2D, matrix: Matrix, direction: HitTestDirection = HitTestDirection.ANY): View? {
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

    open val customHitShape get() = false
    protected open fun hitTestShapeInternal(shape: Shape2D, matrix: Matrix, direction: HitTestDirection): View? {
        //println("View.hitTestShapeInternal: $this, $shape")
        if (Shape2D.intersects(this.hitShape2d, getGlobalMatrixWithAnchor(), shape, matrix)) {
            //println(" -> true")
            return this
        }
        return null
    }

    // @TODO: we should compute view bounds on demand
    /** [x] and [y] are in global coordinates */
    fun mouseHitTest(p: Point): View? {
        //return hitTest(p)
        if (!hitTestEnabled) return null
        if (!visible) return null
        if (mouseChildren) {
            _children?.fastForEachReverse { child ->
                child.mouseHitTest(p)?.let {
                    return it
                }
            }
        }
        if (!mouseEnabled) return null
        hitTestInternal(p)?.let { view ->
            // @TODO: This should not be required if we compute bounds
            val area = getClippingAreaInternal()
            if (!area.isNIL && !area.contains(p)) return null
            return view
        }
        return if (this is Stage) this else null
    }

    @KorgeInternal
    fun getClippingAreaInternal(): Rectangle {
        var out = Rectangle.INFINITE
        var count = 0
        forEachAscendant(true) {
            if (it !is Stage && it is FixedSizeContainer && it.clip) {
                val _localBounds = it.getGlobalBounds()
                if (count == 0) {
                    out = _localBounds
                } else {
                    out = out.intersection(_localBounds)
                }
                count++
            }
        }

        return if (count == 0) Rectangle.NIL else out
    }

    fun hitTestAny(p: Point): Boolean = hitTest(p) != null

    var hitTestUsingShapes: Boolean? = null

    /** [x] and [y] coordinates are global */
    protected open fun hitTestInternal(p: Point, direction: HitTestDirection = HitTestDirection.ANY): View? {
        if (!hitTestEnabled) return null

        //println("x,y: $x,$y")
        //println("bounds: ${getGlobalBounds(_localBounds)}")
        //if (!getGlobalBounds(_localBounds).contains(x, y)) return null

        // Adjusted coordinates to compensate anchoring
        val ll = globalToLocal(p)

        val bounds = getLocalBounds()
        if (!bounds.contains(ll)) {
            //println("bounds = null : $bounds")
            return null
        }

        val l = ll + Point(this.anchorDispX, this.anchorDispY)

        if (hitTestUsingShapes == false) return this

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

        //println("result=$anchorDispX,$anchorDispY")
        //println("result=$lx,$ly")
        //println("result=$llx,$lly")
        //println("result=${hitShape2d}")
        //println("result=${hitShape2d.containsPoint(lx, ly)}")

        //return if (hitShape2d.containsPoint(lx, ly)) this else null

        // @TODO: Use hitShape2d
        val hitShape = this.hitShape
        val hitShapes = this.hitShapes
        if (hitTestUsingShapes == null && (hitShape != null || hitShapes != null)) {
            hitShapes?.fastForEach { if (it.containsPoint(l)) return this }
            if (hitShape != null && hitShape.containsPoint(l)) return this
            return null
        } else {
            return this
        }

    }

    /** [x] and [y] are global, while [sLeft], [sTop], [sRight], [sBottom] are local */
    protected fun checkGlobalBounds(
        gp: Point,
        lrect: Rectangle,
    ): Boolean {
        val lp = globalToLocal(gp)
        return checkLocalBounds(lp, lrect)
    }

    protected fun checkLocalBounds(lp: Point, lrect: Rectangle): Boolean = lp in lrect

    /**
     * Resets the View properties to an identity state.
     */
    open fun reset() {
        _localMatrix = Matrix.IDENTITY
        _x = 0f; _y = 0f
        _scaleX = 1f; _scaleY = 1f
        _skewX = 0.0.radians; _skewY = 0.0.radians
        _rotation = 0.0.radians
        validLocalMatrix = false
        invalidate()
        invalidateLocalBounds()
    }

    /**
     * Removes this view from its parent.
     */
    fun removeFromParent() {
        parent?.removeChild(this)
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
     * Gets the concatenated [MMatrix] of this [View] up to the [target] view.
     * If [inclusive] is true, the concatenated matrix will include the [target] view too.
     * Allows to define an [out] matrix that will hold the result to prevent allocations.
     */
    fun getConcatMatrix(target: View, inclusive: Boolean = false): Matrix {
        var out: Matrix = when {
            target === parent -> this.localMatrix
            target === this -> Matrix.IDENTITY
            else -> {
                val commonAncestor = View.commonAncestor(this, target)
                when {
                    commonAncestor !== null -> {
                        if (target.parent == null && inclusive) {
                            return globalMatrix
                        }
                        globalMatrix * target.globalMatrixInv
                    }
                    else -> Matrix.IDENTITY
                }
            }
        }
        if (inclusive) {
            out *= target.localMatrix
        }
        return out
    }

    fun getConcatMatrixAccurateSlow(target: View, inclusive: Boolean = false): Matrix {
        var out = Matrix.IDENTITY
        if (target !== this) {
            var current: View? = this
            val stopAt = if (inclusive) target.parent else target
            while (current !== null && current !== stopAt) {
                out *= current.localMatrix // Verified
                current = current.parent
            }
        }
        return out
    }

    /** Returns the global bounds of this object. Note this incurs in allocations. Use [getGlobalBounds] (out) to avoid it */
    val windowBounds: Rectangle get() = getWindowBoundsOrNull() ?: getGlobalBounds()

    fun getWindowBoundsOrNull(): Rectangle? {
        val stage = root
        if (stage !is Stage) return null
        //return getBounds(stage, out, inclusive = true).applyTransform(stage.views.globalToWindowMatrix)
        return getWindowBounds(stage)
    }

    fun getWindowBounds(bp: BoundsProvider): Rectangle =
        getGlobalBounds().transformed(bp.globalToWindowMatrix)

    fun getRenderTargetBounds(ctx: RenderContext): Rectangle {
        //println("ctx.ag.isRenderingToWindow=${ctx.ag.isRenderingToWindow}")
        return if (ctx.isRenderingToWindow) getWindowBounds(ctx) else getGlobalBounds()
    }

    fun getClippingBounds(ctx: RenderContext): Rectangle =
        getRenderTargetBounds(ctx)

    /** Returns the global bounds of this object. Note this incurs in allocations. Use [getGlobalBounds] (out) to avoid it */
    val globalBounds: Rectangle get() = getGlobalBounds()

    /** Returns the global bounds of this object. Allows to specify an [out] [MRectangle] to prevent allocations. */
    //fun getGlobalBounds(out: Rectangle = Rectangle()): Rectangle = getBounds(root, out, inclusive = false)
    fun getGlobalBounds(includeFilters: Boolean = false): Rectangle = getBounds(root, inclusive = true, includeFilters = includeFilters)

    /** Tries to set the global bounds of the object. If there are rotations in the ancestors, this might not work as expected. */
    @KorgeUntested
    fun setGlobalBounds(bounds: Rectangle) {
        val transform = parent!!.globalMatrix.toTransform()
        globalPos = bounds.topLeft
        sizeScaled(Size(
            (bounds.width * transform.scaleX),
            (bounds.height * transform.scaleY),
        ))
    }

    // @TODO: Would not include strokes
    //fun getRect(target: View? = this, out: Rectangle = Rectangle()): Rectangle = TODO()

    /** Get the bounds of this view, using the [target] view as coordinate system. Not providing a [target] will return the local bounds. Allows to specify [out] [Rectangle] to prevent allocations. */
    fun getBoundsNoAnchoring(target: View? = this, inclusive: Boolean = false, includeFilters: Boolean = false): Rectangle {
        return getBounds(target, false, inclusive, includeFilters)
    }

    protected fun _getBounds(concat: Matrix, doAnchoring: Boolean = true, includeFilters: Boolean = false): Rectangle {
        var out = getLocalBounds(doAnchoring, includeFilters)

        if (concat.isNotNIL && !concat.isIdentity) {
            out = BoundsBuilder(
                concat.transform(out.topLeft),
                concat.transform(out.topRight),
                concat.transform(out.bottomRight),
                concat.transform(out.bottomLeft)
            ).bounds
        }
        return out
    }

    fun getBounds(target: View? = this, doAnchoring: Boolean = true, inclusive: Boolean = false, includeFilters: Boolean = false): Rectangle {
        return _getBounds(this.getConcatMatrix(target ?: this, inclusive), doAnchoring, includeFilters)
    }

    ///** Kind of bounds we are checking */
    //enum class BoundsKind {
    //    /** The bounds including pixels, so all the non-transparent pixels will be instead. For filters, etc. */
    //    PIXELS,
    //    /** For hit boxes and physics */
    //    SHAPE
    //}

    /**
     * Get local bounds of the view. Allows to specify [out] [MRectangle] if you want to reuse an object.
     */
    fun getLocalBounds(doAnchoring: Boolean = true, includeFilters: Boolean = false): Rectangle {
        var out = cachedLocalBounds ?: getLocalBoundsInternal().also { cachedLocalBounds = it }
        if (!doAnchoring) {
            out = out.translated(Point(anchorDispX, anchorDispY))
        }
        if (includeFilters) {
            filter?.let {
                out = it.expandedBorderRectangle(out)
            }
        }
        return out
    }

    /**
     * Get the bounds of the current [this] view in another view [viewSpace].
     */
    fun getBoundsInSpace(viewSpace: View?, doAnchoring: Boolean = true, includeFilters: Boolean = false): Rectangle {
        val bounds = getLocalBounds(doAnchoring, includeFilters)
        return BoundsBuilder(
            View.convertViewSpace(this, bounds.topLeft, viewSpace),
            View.convertViewSpace(this, bounds.topRight, viewSpace),
            View.convertViewSpace(this, bounds.bottomLeft, viewSpace),
            View.convertViewSpace(this, bounds.bottomRight, viewSpace),
        ).bounds
    }

    open fun getLocalBoundsInternal(): Rectangle = Rectangle.NIL

    protected open fun createInstance(): View =
        throw MustOverrideException("Must Override ${this::class}.createInstance()")

    /**
     * Allows to copy the basic properties (transform [localMatrix], [visible], [colorTransform], [ratio], [speed], [name]...)
     * from [source] into [this]
     */
    open fun copyPropsFrom(source: View) {
        this.name = source.name
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

    fun globalLocalBoundsPointRatio(anchor: Anchor, out: MPoint = MPoint()): MPoint = globalLocalBoundsPointRatio(anchor.doubleX, anchor.doubleY, out)

    fun globalLocalBoundsPointRatio(ratioX: Double, ratioY: Double, out: MPoint = MPoint()): MPoint {
        val bounds = getLocalBounds()
        val x = ratioX.toRatio().interpolate(bounds.left, bounds.right)
        val y = ratioY.toRatio().interpolate(bounds.top, bounds.bottom)
        return out.setTo(localToGlobal(Point(x, y)))
    }

    fun getGlobalMatrixWithAnchor(): Matrix {
        val view = this
        var out: Matrix = view.localMatrix.pretranslated(-view.anchorDispX, -view.anchorDispY)
        view.parent?.globalMatrix?.let { out *= it }
        return out
    }
}

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

inline fun <T2 : View, T> extraViewProp(
    name: String? = null,
    noinline default: T2.() -> T
): Extra.PropertyThis<T2, T> = extraPropertyThis(name, transform = { invalidateRender(); it}, default)

interface ViewRenderPhase {
    val priority: Int get() = 0
    fun render(view: View, ctx: RenderContext) = view.renderNextPhase(ctx)
    fun beforeRender(view: View, ctx: RenderContext) = Unit
    fun afterRender(view: View, ctx: RenderContext) = Unit
}

/**
 * Checks if this view has the specified [ancestor].
 */
fun View.hasAncestor(ancestor: View): Boolean =
    if (this == ancestor) true else this.parent?.hasAncestor(ancestor) ?: false

fun View?.commonAncestor(ancestor: View?): View? = View.commonAncestor(this, ancestor)

/**
 * Replaces this view in its parent with [view].
 * Returns true if the replacement was successful.
 * If this view doesn't have a parent or [view] is the same as [this], returns false.
 */
@OptIn(KorgeInternal::class)
fun View.replaceWith(view: View): Boolean = this.parent?.replaceChild(this, view) ?: false

/** Adds a block that will be executed per frame to this view. As parameter the block will receive a [TimeSpan] with the time elapsed since the previous frame. */
fun <T : View> T.addUpdater(first: Boolean = true, firstTime: TimeSpan = TimeSpan.ZERO, updatable: T.(dt: TimeSpan) -> Unit): CloseableCancellable {
    if (first) updatable(this, firstTime)
    return onEvent(UpdateEvent) { updatable(this, it.deltaTime * this.globalSpeed) }
}
fun <T : View> T.addUpdater(updatable: T.(dt: TimeSpan) -> Unit): CloseableCancellable = addUpdater(true, updatable = updatable)

fun <T : View> T.addUpdaterWithViews(updatable: T.(views: Views, dt: TimeSpan) -> Unit): CloseableCancellable = onEvent(ViewsUpdateEvent) {
    updatable(this@addUpdaterWithViews, it.views, it.delta * this.globalSpeed)
}

/** Registers a [block] that will be executed once in the next frame that this [View] is displayed with the [Views] singleton */
fun <T : View> T.deferWithViews(views: Views? = null, tryImmediate: Boolean = true, block: (views: Views) -> Unit): T {
    if (tryImmediate) {
        (views ?: this.stage?.views)?.let {
            block(it)
            return this
        }
    }
    onNextFrame {
        block(it)
    }
    return this
}

fun <T : View> T.addOptFixedUpdater(time: TimeSpan = TimeSpan.NIL, updatable: T.(dt: TimeSpan) -> Unit): CloseableCancellable = when (time) {
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
 * The [first] properly allows to adjust if the [updatable] will be called immediately after calling this function.
 * To avoid executing too many blocks, when there is a long pause, [limitCallsPerFrame] limits the number of times the block can be executed in a single frame.
 */
fun <T : View> T.addFixedUpdater(
    time: TimeSpan,
    first: Boolean = true,
    limitCallsPerFrame: Int = 16,
    updatable: T.() -> Unit
): CloseableCancellable {
    var accum = 0.0.milliseconds
    return addUpdater(first = first, firstTime = time) { dt ->
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
}

@Deprecated("Use addUpdater instead", ReplaceWith("addUpdater(updatable)"))
inline fun <T : View> T.onFrame(noinline updatable: T.(dt: TimeSpan) -> Unit): Cancellable = addUpdater(updatable)

fun <T : View> T.onNextFrame(block: T.(views: Views) -> Unit): CloseableCancellable {
    var closeable: Closeable? = null
    closeable = addUpdaterWithViews { views, _ ->
        block(views)
        closeable?.close()
    }
    return closeable
}


// @TODO: Replace width, height with SizeInt
fun <T : View> T.onStageResized(firstTrigger: Boolean = true, block: Views.(width: Int, height: Int) -> Unit): T = this.apply {
    if (firstTrigger) {
        deferWithViews { views -> block(views, views.actualVirtualWidth, views.actualVirtualHeight) }
    }

    onEvent(ViewsResizedEvent) {
        block(it.views, it.views.actualVirtualWidth, it.views.actualVirtualHeight)
    }
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

fun View?.firstAncestor(includeThis: Boolean = true, condition: (View) -> Boolean): View? {
    if (this == null) return null
    if (includeThis && condition(this)) return this
    return parent?.firstAncestor(true, condition)
}

/**
 * Scroll ancestors to make this view is visible
 */
fun View.scrollParentsToMakeVisible() {
    firstAncestorOfType<UIScrollable>(includeThis = false)?.ensureViewIsVisible(this)
}

inline fun <reified T : View> View?.firstAncestorOfType(includeThis: Boolean = true): T? = firstAncestor(includeThis) { it is T } as T?

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

/** Returns a list of descendants views that are of type [T]. */
inline fun <reified T : View> View.getDescendantsOfType() = this.descendantsWith { it is T }

/** Sets the position [point] of the view and returns this (chaineable). */
inline fun <T : View> T.visible(visible: Boolean): T = this.also { it.visible = visible }
inline fun <T : View> T.name(name: String?): T = this.also { it.name = name }

inline fun <T : View> T.hitShape(crossinline block: @ViewDslMarker VectorBuilder.() -> Unit): T {
    buildVectorPath { block() }.also {
        this.hitShape = it
    }
    return this
}

fun <T : View> T.sizeScaled(size: Size): T {
    this.size = (Size(
        if (scaleX == 0f) size.width else size.width / scaleX,
        if (scaleY == 0f) size.height else size.height / scaleY,
    ))
    return this
}

fun <T : View> T.size(size: Size): T {
    this.size = size
    return this
}
fun <T : View> T.size(width: Double, height: Double): T = size(Size(width, height))
fun <T : View> T.size(width: Float, height: Float): T = size(Size(width, height))
fun <T : View> T.size(width: Int, height: Int): T = size(Size(width, height))

fun <T : View> T.globalPos(p: Point): T {
    this.globalPos = p
    return this
}

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
fun <T : View> T.xy(p: Point): T {
    this.pos = p
    return this
}
fun <T : View> T.xy(x: Double, y: Double): T = xy(Point(x, y))
fun <T : View> T.xy(x: Float, y: Float): T = xy(Point(x, y))
fun <T : View> T.xy(x: Int, y: Int): T = xy(Point(x, y))
fun <T : View> T.xy(p: MPoint): T = xy(p.point)

/** Chainable method returning this that sets [View.x] and [View.y] */
fun <T : View> T.position(x: Double, y: Double): T = xy(Point(x, y))
fun <T : View> T.position(x: Float, y: Float): T = xy(Point(x, y))
fun <T : View> T.position(x: Int, y: Int): T = xy(Point(x, y))
fun <T : View> T.position(p: Point): T = xy(p)

fun <T : View> T.bounds(left: Double, top: Double, right: Double, bottom: Double): T = xy(left, top).size(Size(right - left, bottom - top))
fun <T : View> T.bounds(rect: Rectangle): T = bounds(rect.left.toDouble(), rect.top.toDouble(), rect.right.toDouble(), rect.bottom.toDouble())

fun <T : View> T.positionX(x: Double): T {
    this.xD = x
    return this
}
fun <T : View> T.positionX(x: Float): T = positionX(x.toDouble())
fun <T : View> T.positionX(x: Int): T = positionX(x.toDouble())

fun <T : View> T.positionY(y: Double): T {
    this.yD = y
    return this
}
fun <T : View> T.positionY(y: Float): T = positionY(y.toDouble())
fun <T : View> T.positionY(y: Int): T = positionY(y.toDouble())

fun View.getPositionRelativeTo(view: View): Point {
    val mat = this.parent!!.getConcatMatrix(view, inclusive = false)
    return mat.transform(pos)
}

fun View.setPositionRelativeTo(view: View, pos: Point) {
    val mat = this.parent!!.getConcatMatrix(view, inclusive = false)
    val matInv = mat.inverted()
    val out = matInv.transform(pos)
    this.x = out.x
    this.y = out.y
}

fun View.getPointRelativeTo(pos: Point, view: View): Point {
    val mat = this.getConcatMatrix(view, inclusive = false)
    return mat.transform(pos)
}

fun View.getPointRelativeToInv(pos: Point, view: View): Point {
    return this.getConcatMatrix(view, inclusive = false).inverted().transform(pos)
}

/** Chainable method returning this that sets [this] View in the middle between [x1] and [x2] */
fun <T : View> T.centerXBetween(x1: Double, x2: Double): T {
    this.xD = (x2 + x1 - this.widthD) / 2
    return this
}
fun <T : View> T.centerXBetween(x1: Float, x2: Float): T = centerXBetween(x1.toDouble(), x2.toDouble())
fun <T : View> T.centerXBetween(x1: Int, x2: Int): T = centerXBetween(x1.toDouble(), x2.toDouble())

/** Chainable method returning this that sets [this] View in the middle between [y1] and [y2] */
fun <T : View> T.centerYBetween(y1: Double, y2: Double): T {
    this.yD = (y2 + y1 - this.heightD) / 2
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
 * Chainable method returning this that sets [View.xD] so that
 * [this] View is centered on the [other] View horizontally
 */
fun <T : View> T.centerXOn(other: View): T = this.alignX(other, 0.5, true)

/**
 * Chainable method returning this that sets [View.yD] so that
 * [this] View is centered on the [other] View vertically
 */
fun <T : View> T.centerYOn(other: View): T = this.alignY(other, 0.5, true)

/**
 * Chainable method returning this that sets [View.xD] and [View.yD]
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
    val localBounds = this.getLocalBounds()

    //bounds.setTo(other.x, other.y, other.unscaledWidth, other.unscaledHeight)
    val ratioM1_1 = (ratio * 2 - 1)
    val rratioM1_1 = if (inside) ratioM1_1 else -ratioM1_1
    val iratio = if (inside) ratio else 1.0 - ratio
    //println("this: $this, other: $other, bounds=$bounds, scaledWidth=$scaledWidth, scaledHeight=$scaledHeight, width=$width, height=$height, scale=$scale, $scaleX, $scaleY")
    if (doX) {
        xD = (bounds.x + (bounds.width * ratio) - localBounds.left) - (this.scaledWidthD * iratio) - (padding * rratioM1_1)
    } else {
        yD = (bounds.y + (bounds.height * ratio) - localBounds.top) - (this.scaledHeightD * iratio) - (padding * rratioM1_1)
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
 * Chainable method returning this that sets [View.xD] so that
 * [this] View's left side is aligned with the [other] View's left side
 */
// @TODO: What about rotations? we might need to adjust y too?
fun <T : View> T.alignLeftToLeftOf(other: View, padding: Double = 0.0): T = alignX(other, 0.0, inside = true, padding = padding)
fun <T : View> T.alignLeftToLeftOf(other: View, padding: Float): T = alignLeftToLeftOf(other, padding.toDouble())
fun <T : View> T.alignLeftToLeftOf(other: View, padding: Int): T = alignLeftToLeftOf(other, padding.toDouble())

/**
 * Chainable method returning this that sets [View.xD] so that
 * [this] View's left side is aligned with the [other] View's right side
 */
fun <T : View> T.alignLeftToRightOf(other: View, padding: Double = 0.0): T = alignX(other, 1.0, inside = false, padding = padding)
fun <T : View> T.alignLeftToRightOf(other: View, padding: Float): T = alignLeftToRightOf(other, padding.toDouble())
fun <T : View> T.alignLeftToRightOf(other: View, padding: Int): T = alignLeftToRightOf(other, padding.toDouble())

/**
 * Chainable method returning this that sets [View.xD] so that
 * [this] View's right side is aligned with the [other] View's left side
 */
fun <T : View> T.alignRightToLeftOf(other: View, padding: Double = 0.0): T = alignX(other, 0.0, inside = false, padding = padding)
fun <T : View> T.alignRightToLeftOf(other: View, padding: Float): T = alignRightToLeftOf(other, padding.toDouble())
fun <T : View> T.alignRightToLeftOf(other: View, padding: Int): T = alignRightToLeftOf(other, padding.toDouble())

/**
 * Chainable method returning this that sets [View.xD] so that
 * [this] View's right side is aligned with the [other] View's right side
 */
fun <T : View> T.alignRightToRightOf(other: View, padding: Double = 0.0): T = alignX(other, 1.0, inside = true, padding = padding)
fun <T : View> T.alignRightToRightOf(other: View, padding: Float): T = alignRightToRightOf(other, padding.toDouble())
fun <T : View> T.alignRightToRightOf(other: View, padding: Int): T = alignRightToRightOf(other, padding.toDouble())

/**
 * Chainable method returning this that sets [View.yD] so that
 * [this] View's top side is aligned with the [other] View's top side
 */
fun <T : View> T.alignTopToTopOf(other: View, padding: Double = 0.0): T = alignY(other, 0.0, inside = true, padding = padding)
fun <T : View> T.alignTopToTopOf(other: View, padding: Float): T = alignTopToTopOf(other, padding.toDouble())
fun <T : View> T.alignTopToTopOf(other: View, padding: Int): T = alignTopToTopOf(other, padding.toDouble())

/**
 * Chainable method returning this that sets [View.yD] so that
 * [this] View's top side is aligned with the [other] View's bottom side
 */
fun <T : View> T.alignTopToBottomOf(other: View, padding: Double = 0.0): T = alignY(other, 1.0, inside = false, padding = padding)
fun <T : View> T.alignTopToBottomOf(other: View, padding: Float): T = alignTopToBottomOf(other, padding.toDouble())
fun <T : View> T.alignTopToBottomOf(other: View, padding: Int): T = alignTopToBottomOf(other, padding.toDouble())

/**
 * Chainable method returning this that sets [View.yD] so that
 * [this] View's bottom side is aligned with the [other] View's top side
 */
fun <T : View> T.alignBottomToTopOf(other: View, padding: Double = 0.0): T = alignY(other, 0.0, inside = false, padding = padding)
fun <T : View> T.alignBottomToTopOf(other: View, padding: Float): T = alignBottomToTopOf(other, padding.toDouble())
fun <T : View> T.alignBottomToTopOf(other: View, padding: Int): T = alignBottomToTopOf(other, padding.toDouble())

/**
 * Chainable method returning this that sets [View.yD] so that
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

/** Chainable method returning this that sets [View.scaleXD] and [View.scaleYD] */
fun <T : View> T.scale(sx: Double, sy: Double = sx): T {
    this.scaleXD = sx
    this.scaleYD = sy
    return this
}
fun <T : View> T.scale(sx: Float, sy: Float = sx): T = scale(sx.toDouble(), sy.toDouble())
fun <T : View> T.scale(sx: Int, sy: Int = sx): T = scale(sx.toDouble(), sy.toDouble())

/** Chainable method returning this that sets [View.colorMul] */
fun <T : View> T.colorMul(color: RGBA): T {
    this.colorMul = color
    return this
}

/** Chainable method returning this that sets [View.alphaF] */
fun <T : View> T.alpha(alpha: Float): T {
    this.alphaF = alpha
    return this
}
fun <T : View> T.alpha(alpha: Double): T = alpha(alpha.toFloat())
fun <T : View> T.alpha(alpha: Int): T = alpha(alpha.toFloat())

fun <T : View> T.zIndex(index: Float): T {
    this.zIndex = index
    return this
}
fun <T : View> T.zIndex(index: Double): T = zIndex(index.toFloat())
fun <T : View> T.zIndex(index: Int): T = zIndex(index.toFloat())

typealias ViewDslMarker = korlibs.math.annotations.ViewDslMarker
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
        return view ?: stage!!
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

sealed class ScalingOption {
    // Scales the view to fit within the provided `width` and `height`.
    data class ByWidthAndHeight(val width: Double, val height: Double) : ScalingOption()
    // Scale the view's width to match the provided `width`.
    data class ByWidth(val width: Double) : ScalingOption()
    // Scale the view's height to match the provided `height`.
    data class ByHeight(val height: Double) : ScalingOption()
}

// Scales `this` view by the provided `scalingOption` while maintaining the aspect ratio.
fun <T : View> T.scaleWhileMaintainingAspect(scalingOption: ScalingOption): T {
    val scaleValue = when (scalingOption) {
        is ScalingOption.ByHeight -> {
            scalingOption.height / this.scaledHeightD
        }
        is ScalingOption.ByWidth -> {
            scalingOption.width / this.scaledWidthD
        }
        is ScalingOption.ByWidthAndHeight -> {
            val scaledByWidth = scalingOption.width / this.scaledWidthD
            val scaledByHeight = scalingOption.height / this.scaledHeightD
            kotlin.math.min(scaledByHeight, scaledByWidth)
        }
    }
    this.scaledHeightD = this.scaledHeightD * scaleValue
    this.scaledWidthD = this.scaledWidthD * scaleValue
    return this
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
