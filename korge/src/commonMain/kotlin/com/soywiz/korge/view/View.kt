@file:UseExperimental(KorgeInternal::class)

package com.soywiz.korge.view

import com.soywiz.kds.*
import com.soywiz.kds.iterators.*
import com.soywiz.klock.*
import com.soywiz.klock.hr.*
import com.soywiz.korev.*
import com.soywiz.korge.component.*
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
import kotlin.collections.set
import kotlin.math.*
import kotlin.reflect.*

@DslMarker
@Target(AnnotationTarget.CLASS, AnnotationTarget.TYPE)
annotation class ViewsDslMarker

@Deprecated("", replaceWith = ReplaceWith("View"))
typealias DisplayObject = View

/**
 * KorGE includes a DOM-based tree of views that makes a chain of affine transforms starting with the [Stage], that is the root node.
 *
 * ## Basic description
 *
 * The [View] class is the base class for all the nodes in the display tree. It is abstract with the [renderInternal] method missing.
 * [View] itself can't contain children, but the [Container] class and subclasses allow to have children.
 * Typical non-container views are: [Image], [SolidRect] or [Text].
 *
 * Most views doesn't have the concept of size. They act just as points (x,y) or rather affine transforms (since they also include scale, rotation and skew)
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
    /** Indicates if this class is a container or not. This is only overrided by Container. This check is performed like this, to avoid type checks. That might be an expensive operation in some targets. */
    val isContainer: Boolean
) : Renderable
    , Extra by Extra.Mixin()
//, EventDispatcher by EventDispatcher.Mixin()
{
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

    internal open val anchorDispX get() = 0.0
    //@KorgeInternal
    internal open val anchorDispY get() = 0.0

    @KorgeInternal
    @PublishedApi
    internal var _children: ArrayList<View>? = null

    /** Iterates all the children of this container in normal order of rendering. */
    inline fun forEachChildren(callback: (child: View) -> Unit) = _children?.fastForEach(callback)

    /** Iterates all the children of this container in normal order of rendering. Providing an index in addition to the child to the callback. */
    inline fun forEachChildrenWithIndex(callback: (index: Int, child: View) -> Unit) =
        _children?.fastForEachWithIndex(callback)

    /** Iterates all the children of this container in reverse order of rendering. */
    inline fun forEachChildrenReversed(callback: (child: View) -> Unit) = _children?.fastForEachReverse(callback)

    /** Indicates if this view is going to propagate the events that reach this node to its children */
    var propagateEvents = true

    /**
     * Views marked with this, break batching by acting as reference point for computing vertices.
     * Specially useful for containers whose most of their child are less likely to change but the container
     * itself is going to change like cameras, viewports and the Stage.
     */
    interface Reference // View that breaks batching Viewport

    @KorgeInternal
    @Deprecated("Not used by now")
    enum class HitTestType {
        BOUNDING, SHAPE
    }

    open var hitShape: VectorPath? = null

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
            while (lCount != rCount) {
                if (lCount > rCount) {
                    lCount--
                    l = l?.parent
                } else {
                    rCount--
                    r = r?.parent
                }
                if (lCount < 0 && rCount < 0) break
            }
            return if (l == r) l else null
        }
    }

    /** Property used for interpolable views like morph shapes, progress bars etc. */
    open var ratio: Double = 0.0

    /** The index the child has in its parent */
    var index: Int = 0; internal set

    /** Ratio speed of this node, affecting all the [UpdateComponent] */
    var speed: Double = 1.0

    /** Parent [Container] of [this] View if any, or null */
    var parent: Container? = null; internal set

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

    private var _scaleX: Double = 1.0
    private var _scaleY: Double = 1.0
    private var _skewX: Double = 0.0
    private var _skewY: Double = 0.0
    private var _rotation: Angle = 0.radians

    /** Position of the view. **@NOTE**: If plan to change its values manually. You should call [View.invalidateMatrix] later to keep the matrix in sync */
    val pos = Point()

    /** Local X position of this view */
    var x: Double
        get() = ensureTransform().pos.x
        set(v) {
            ensureTransform(); if (pos.x != v) {
                pos.x = v; invalidateMatrix()
            }
        }

    /** Local Y position of this view */
    var y: Double
        get() = ensureTransform().pos.y
        set(v) {
            ensureTransform(); if (pos.y != v) {
                pos.y = v; invalidateMatrix()
            }
        }

    /** Local scaling in the X axis of this view */
    var scaleX: Double
        get() = ensureTransform()._scaleX
        set(v) {
            ensureTransform(); if (_scaleX != v) {
                _scaleX = v; invalidateMatrix()
            }
        }

    /** Local scaling in the Y axis of this view */
    var scaleY: Double
        get() = ensureTransform()._scaleY
        set(v) {
            ensureTransform(); if (_scaleY != v) {
                _scaleY = v; invalidateMatrix()
            }
        }

    /** Allows to change [scaleX] and [scaleY] at once. Returns the mean value of x and y scales. */
    var scale: Double
        get() = (scaleX + scaleY) / 2.0
        set(v) {
            scaleX = v; scaleY = v
        }

    /** Local skewing in the X axis of this view */
    var skewX: Double
        get() = ensureTransform()._skewX
        set(v) {
            ensureTransform(); if (_skewX != v) {
                _skewX = v; invalidateMatrix()
            }
        }

    /** Local skewing in the Y axis of this view */
    var skewY: Double
        get() = ensureTransform()._skewY
        set(v) {
            ensureTransform(); if (_skewY != v) {
                _skewY = v; invalidateMatrix()
            }
        }

    /** Local rotation of this view */
    var rotation: Angle
        get() = ensureTransform()._rotation
        set(v) {
            ensureTransform(); if (_rotation != v) {
                _rotation = v; invalidateMatrix()
            }
        }

    /** Local rotation in radians of this view */
    var rotationRadians: Double
        get() = rotation.radians
        set(v) {
            rotation = v.radians
        }

    /** Local rotation in degrees of this view */
    var rotationDegrees: Double
        get() = rotation.degrees
        set(v) {
            rotation = v.degrees
        }

    /** The global x position of this view */
    var globalX: Double
        get() = parent?.localToGlobalX(x, y) ?: x;
        set(value) {
            x = parent?.globalToLocalX(value, globalY) ?: value
        }

    /** The global y position of this view */
    var globalY: Double
        get() = parent?.localToGlobalY(x, y) ?: y;
        set(value) {
            y = parent?.globalToLocalY(globalX, value) ?: value
        }

    /**
     * Changes the [width] and [height] to match the parameters.
     */
    open fun setSize(width: Double, height: Double) {
        this.width = width
        this.height = height
    }

    /**
     * Changes the [width] of this view. Generically, this means adjusting the [scaleX] of the view to match that size using the current bounds,
     * but some views might override this to adjust its internal width or height (like [SolidRect] or [UIView] for example).
     */
    open var width: Double
        get() = getLocalBounds().width * scaleX
        set(value) {
            scaleX = value / this.getLocalBounds().width
        }

    /**
     * Changes the [height] of this view. Generically, this means adjusting the [scaleY] of the view to match that size using the current bounds,
     * but some views might override this to adjust its internal width or height (like [SolidRect] or [UIView] for example).
     */
    open var height: Double
        get() = getLocalBounds().height * scaleY
        set(value) {
            scaleY = value / this.getLocalBounds().height
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
            _colorTransform.colorMul = v
            invalidateColorTransform()
        }

    /**
     * Additive part of the color transform.
     * This Int is a packed version of R,G,B,A one-component byte values determining additive color transform.
     * @NOTE: If you don't have this value computed, you can use [ColorTransform.aR] aB, aG and aA to control the
     * per component values. You should call the [View.invalidate] method after that.
     */
    var colorAdd: Int
        get() = _colorTransform.colorAdd;
        set(v) {
            _colorTransform.colorAdd = v
            invalidateColorTransform()
        }

    /**
     * Shortcut for adjusting the multiplicative alpha value manually.
     * Equivalent to [ColorTransform.mA] + [View.invalidate]
     */
    var alpha: Double
        get() = _colorTransform.mA;
        set(v) {
            _colorTransform.mA = v
            invalidateColorTransform()
        }

    /** Alias for [colorMul] to make this familiar to people coming from other engines. */
    var tint: RGBA
        get() = this.colorMul
        set(value) {
            this.colorMul = value
        }

    // region Properties
    private val _props = linkedMapOf<String, Any?>()

    /** Immutable map of custom String properties attached to this view. Should use [hasProp], [getProp] and [addProp] methods to control this */
    val props: Map<String, Any?> get() = _props

    /** Checks if this view has the [key] property */
    fun hasProp(key: String) = key in _props

    /** Gets the [key] property of this view as a [String] or [default] when not found */
    fun getPropString(key: String, default: String = "") = _props[key]?.toString() ?: default

    /** Gets the [key] property of this view as an [Double] or [default] when not found */
    fun getPropDouble(key: String, default: Double = 0.0): Double {
        val value = _props[key]
        if (value is Number) return value.toDouble()
        if (value is String) return value.toDoubleOrNull() ?: default
        return default
    }

    /** Gets the [key] property of this view as an [Int] or [default] when not found */
    fun getPropInt(key: String, default: Int = 0) = getPropDouble(key, default.toDouble()).toInt()

    /** Adds or replaces the property [key] with the [value] */
    fun addProp(key: String, value: Any?) {
        _props[key] = value
        //val componentGen = views.propsTriggers[key]
        //if (componentGen != null) {
        //	componentGen(this, key, value)
        //}
    }

    /** Adds a list of [values] properties at once */
    fun addProps(values: Map<String, Any?>) {
        for (pair in values) addProp(pair.key, pair.value)
    }
    // endregion

    private val tempTransform = Matrix.Transform()
    //private val tempMatrix = Matrix2d()

    private fun ensureTransform(): View {
        if (!validLocalProps) {
            validLocalProps = true
            val t = tempTransform.setMatrix(this._localMatrix)
            this.pos.x = t.x
            this.pos.y = t.y
            this._scaleX = t.scaleX
            this._scaleY = t.scaleY
            this._skewX = t.skewX
            this._skewY = t.skewY
            this._rotation = t.rotation
        }
        return this
    }

    /** The ancestor view without parents. When attached (visible or invisible), this is the [Stage]. When no parents, it is [this] */
    val root: View get() = parent?.root ?: this

    /** When included in the three, this returns the stage. When not attached yet, this will return null. */
    open val stage: Stage? get() = root as? Stage?

    /** Determines if mouse events will be handled for this view and its children */
    open var mouseEnabled: Boolean = true
    //var mouseChildren: Boolean = false

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
        pos.x = t.x; pos.y = t.y
        _scaleX = t.scaleX; _scaleY = t.scaleY
        _skewX = t.skewY; _skewY = t.skewY
        _rotation = t.rotation
    }

    //fun setTransform(x: Double, y: Double, sx: Double, sy: Double, angle: Double, skewX: Double, skewY: Double, pivotX: Double = 0.0, pivotY: Double = 0.0) =
    //	setTransform(tempTransform.setTo(x, y, sx, sy, skewX, skewY, angle))


    internal var validLocalProps = true
    internal var validLocalMatrix = true

    @KorgeInternal
    @PublishedApi
    internal var _components: Components? = null

    @KorgeInternal
    @PublishedApi
    internal val componentsSure: Components
        get() {
            if (_components == null) _components = Components()
            return _components!!
        }

    // region Components
    @PublishedApi
    internal var components: ArrayList<Component>? = null

    /** Creates a typed [T] component (using the [gen] factory function) if the [View] doesn't have any of that kind, or returns a component of that type if already attached */
    @Deprecated("")
    inline fun <reified T : Component> getOrCreateComponent(gen: (View) -> T): T =
        componentsSure.getOrCreateComponent(this, T::class, gen)

    inline fun <reified T : Component> getOrCreateComponentOther(gen: (View) -> T): T =
        componentsSure.getOrCreateComponent(this, T::class, gen)

    inline fun <reified T : MouseComponent> getOrCreateComponentMouse(gen: (View) -> T): T =
        componentsSure.getOrCreateComponent(this, T::class, gen)

    inline fun <reified T : KeyComponent> getOrCreateComponentKey(gen: (View) -> T): T =
        componentsSure.getOrCreateComponent(this, T::class, gen)

    inline fun <reified T : GamepadComponent> getOrCreateComponentGamepad(gen: (View) -> T): T =
        componentsSure.getOrCreateComponent(this, T::class, gen)

    inline fun <reified T : TouchComponent> getOrCreateComponentTouch(gen: (View) -> T): T =
        componentsSure.getOrCreateComponent(this, T::class, gen)

    inline fun <reified T : EventComponent> getOrCreateComponentEvent(gen: (View) -> T): T =
        componentsSure.getOrCreateComponent(this, T::class, gen)

    inline fun <reified T : UpdateComponentWithViews> getOrCreateComponentUpdateWithViews(gen: (View) -> T): T =
        componentsSure.getOrCreateComponent(this, T::class, gen)

    inline fun <reified T : UpdateComponent> getOrCreateComponentUpdate(gen: (View) -> T): T =
        componentsSure.getOrCreateComponent(this, T::class, gen)

    inline fun <reified T : ResizeComponent> getOrCreateComponentResize(gen: (View) -> T): T =
        componentsSure.getOrCreateComponent(this, T::class, gen)

    /** Removes a specific [c] component from the view */
    fun removeComponent(c: Component) {
        _components?.remove(c)
    }

    fun removeComponent(c: MouseComponent) {
        _components?.remove(c)
    }

    fun removeComponent(c: KeyComponent) {
        _components?.remove(c)
    }

    fun removeComponent(c: GamepadComponent) {
        _components?.remove(c)
    }

    fun removeComponent(c: TouchComponent) {
        _components?.remove(c)
    }

    fun removeComponent(c: EventComponent) {
        _components?.remove(c)
    }

    fun removeComponent(c: UpdateComponentWithViews) {
        _components?.remove(c)
    }

    fun removeComponent(c: UpdateComponent) {
        _components?.remove(c)
    }

    fun removeComponent(c: ResizeComponent) {
        _components?.remove(c)
    }

    //fun removeComponents(c: KClass<out Component>) { components?.removeAll { it.javaClass.isSubtypeOf(c) } }
    /** Removes a set of components of the type [c] from the view */
    @Deprecated("")
    fun removeComponents(c: KClass<out Component>) {
        _components?.removeAll(c)
    }

    /** Removes all the components attached to this view */
    fun removeAllComponents(): Unit {
        _components?.removeAll()
    }

    /** Adds a component to this view */
    fun addComponent(c: Component): Component = componentsSure.add(c)
    fun addComponent(c: MouseComponent) = componentsSure.add(c)
    fun addComponent(c: KeyComponent) = componentsSure.add(c)
    fun addComponent(c: GamepadComponent) = componentsSure.add(c)
    fun addComponent(c: TouchComponent) = componentsSure.add(c)
    fun addComponent(c: EventComponent) = componentsSure.add(c)
    fun addComponent(c: UpdateComponentWithViews) = componentsSure.add(c)
    fun addComponent(c: UpdateComponent) = componentsSure.add(c)
    fun addComponent(c: ResizeComponent) = componentsSure.add(c)

    /** Adds a block that will be executed per frame to this view. This is deprecated, and you should use [addUpdater] instead that uses [TimeSpan] to provide the elapsed time */
    @Deprecated("Use addUpdater, since this method uses dtMs: Int instead of a TimeSpan due to bugs in initial Kotlin inline classes")
    fun addUpdatable(updatable: (dtMs: Int) -> Unit): Cancellable {
        val component = object : UpdateComponentV2 {
            override val view: View get() = this@View
            override fun update(dt: HRTimeSpan) = updatable(dt.millisecondsInt)
        }.attach()
        component.update(0.0)
        return Cancellable { component.detach() }
    }

    /** Registers a [block] that will be executed once in the next frame that this [View] is displayed with the [Views] singleton */
    fun deferWithViews(block: (views: Views) -> Unit) {
        addComponent(DeferWithViewsUpdateComponentWithViews(this@View, block))
    }

    internal class DeferWithViewsUpdateComponentWithViews(override val view: View, val block: (views: Views) -> Unit) :
        UpdateComponentWithViews {
        override fun update(views: Views, ms: Double) {
            block(views)
            detach()
        }
    }

// endregion

    private var _localMatrix = Matrix()

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
            _colorTransform.copyFrom(v); invalidate()
        }

    private var _renderColorTransform = ColorTransform(1.0, 1.0, 1.0, 1.0, 0, 0, 0, 0)
    private var _renderColorTransformVersion = -1

    /**
     * The concatenated version of [colorTransform] having into account all the color transformations of the ancestors
     */
    val renderColorTransform: ColorTransform
        get() {
            if (_renderColorTransformVersion != this._versionColor) {
                _renderColorTransformVersion = this._versionColor
                _requireInvalidateColor = true
                when {
                    parent != null && parent?.filter != null -> _renderColorTransform.copyFrom(_colorTransform)
                    parent != null && this !is View.Reference -> _renderColorTransform.setToConcat(
                        _colorTransform,
                        parent!!.renderColorTransform
                    )
                    else -> _renderColorTransform.copyFrom(_colorTransform)
                }
            }
            return _renderColorTransform
        }

    private var _renderBlendMode: BlendMode = BlendMode.INHERIT
    private var _renderBlendModeVersion: Int = -1

    /**
     * The actual [blendMode] of the view after computing the ancestors and reaching a view with a non [BlendMode.INHERIT].
     */
    val renderBlendMode: BlendMode
        get() {
            if (_renderBlendModeVersion != this._version) {
                _renderBlendModeVersion = this._version
                _requireInvalidate = true
                _renderBlendMode =
                    if (blendMode == BlendMode.INHERIT) parent?.renderBlendMode ?: BlendMode.NORMAL else blendMode
            }
            return _renderBlendMode
        }

    /** The concatenated/global version of the local [colorMul] */
    val renderColorMul: RGBA get() = renderColorTransform.colorMul

    /** The concatenated/global version of the local [colorAdd] */
    val renderColorAdd: Int get() = renderColorTransform.colorAdd

    /** The concatenated/global version of the local [alpha] */
    val renderAlpha: Double get() = renderColorTransform.mA

    /** Computes the local X coordinate of the mouse using the coords from the [Views] object */
    fun localMouseX(views: Views): Double = this.globalMatrixInv.fastTransformX(views.input.mouse)

    /** Computes the local Y coordinate of the mouse using the coords from the [Views] object */
    fun localMouseY(views: Views): Double = this.globalMatrixInv.fastTransformY(views.input.mouse)

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
    fun invalidate() {
        this._version++
        _requireInvalidate = false
        dirtyVertices = true
        _children?.fastForEach { child ->
            if (child._requireInvalidate) {
                child.invalidate()
            }
        }
    }

    fun invalidateColorTransform() {
        this._versionColor++
        _requireInvalidateColor = false
        dirtyVertices = true
        _children?.fastForEach { child ->
            if (child._requireInvalidateColor) {
                child.invalidateColorTransform()
            }
        }
    }

    /**
     * An optional [Filter] attached to this view.
     * Filters allow to render this view to a texture, and to controls how to render that texture (using shaders, repeating the texture, etc.).
     * You add multiple filters by creating a composite filter [ComposedFilter].
     */
    var filter: Filter? = null

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
        if (filter != null) {
            renderFiltered(ctx, filter!!)
        } else {
            renderInternal(ctx)
        }
    }

    private fun renderFiltered(ctx: RenderContext, filter: Filter) {
        val bounds = getLocalBounds()

        val borderEffect = filter.border
        ctx.matrixPool.alloc { tempMat2d ->
            ctx.matrix3DPool.alloc { oldViewMatrix ->
                val texWidth = bounds.width.toInt() + borderEffect * 2
                val texHeight = bounds.height.toInt() + borderEffect * 2

                val addx = -bounds.x + borderEffect
                val addy = -bounds.y + borderEffect

                //println("FILTER: $texWidth, $texHeight : $globalMatrixInv, $globalMatrix, addx=$addx, addy=$addy, renderColorAdd=$renderColorAdd, renderColorMulInt=$renderColorMulInt, blendMode=$blendMode")

                ctx.renderToTexture(texWidth, texHeight, render = {
                    tempMat2d.copyFrom(globalMatrixInv)
                    tempMat2d.translate(addx, addy)
                    //println("globalMatrixInv:$globalMatrixInv, tempMat2d=$tempMat2d")
                    ctx.batch.setViewMatrixTemp(tempMat2d, temp = oldViewMatrix) {
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
    }

    /** Method that all views must override in order to control how the view is going to be rendered */
    protected abstract fun renderInternal(ctx: RenderContext)

    @Suppress("RemoveCurlyBracesFromTemplate")
    override fun toString(): String {
        var out = this::class.portableSimpleName
        if (x != 0.0 || y != 0.0) out += ":pos=(${x.str},${y.str})"
        if (scaleX != 1.0 || scaleY != 1.0) out += ":scale=(${scaleX.str},${scaleY.str})"
        if (skewX != 0.0 || skewY != 0.0) out += ":skew=(${skewX.str},${skewY.str})"
        if (rotationRadians != 0.0) out += ":rotation=(${rotationDegrees.str}º)"
        if (name != null) out += ":name=($name)"
        if (blendMode != BlendMode.INHERIT) out += ":blendMode=($blendMode)"
        if (!visible) out += ":visible=$visible"
        if (alpha != 1.0) out += ":alpha=$alpha"
        if (this.colorMul.rgb != Colors.WHITE.rgb) out += ":colorMul=${this.colorMul.hexString}"
        if (colorAdd != 0x7f7f7f7f) out += ":colorAdd=${colorAdd.shex}"
        return out
    }

    protected val Double.str get() = this.toStringDecimal(2, skipTrailingZeros = true)

    // Version with root-most object as reference
    /** Converts the global point [p] (using root/stage as reference) into the local coordinate system. Allows to define [out] to avoid allocation. */
    fun globalToLocal(p: IPoint, out: Point = Point()): Point = globalToLocalXY(p.x, p.y, out)

    /** Converts the global point [x] [y] (using root/stage as reference) into the local coordinate system. Allows to define [out] to avoid allocation. */
    fun globalToLocalXY(x: Double, y: Double, out: Point = Point()): Point = this.globalMatrixInv.transform(x, y, out)

    /** Converts the global point [x], [y] (using root/stage as reference) into the X in the local coordinate system. */
    fun globalToLocalX(x: Double, y: Double): Double = this.globalMatrixInv.fastTransformX(x, y)

    /** Converts the global point [x], [y] (using root/stage as reference) into the Y in the local coordinate system. */
    fun globalToLocalY(x: Double, y: Double): Double = this.globalMatrixInv.fastTransformY(x, y)

    /** Converts the local point [p] into a global point (using root/stage as reference). Allows to define [out] to avoid allocation. */
    fun localToGlobal(p: IPoint, out: Point = Point()): Point = localToGlobalXY(p.x, p.y, out)

    /** Converts the local point [x], [y] into a global point (using root/stage as reference). Allows to define [out] to avoid allocation. */
    fun localToGlobalXY(x: Double, y: Double, out: Point = Point()): Point = this.globalMatrix.transform(x, y, out)

    /** Converts the local point [x], [y] into a global X coordinate (using root/stage as reference). */
    fun localToGlobalX(x: Double, y: Double): Double = this.globalMatrix.fastTransformX(x, y)

    /** Converts the local point [x], [y] into a global Y coordinate (using root/stage as reference). */
    fun localToGlobalY(x: Double, y: Double): Double = this.globalMatrix.fastTransformY(x, y)

    // Version with View.Reference as reference
    /** Converts a point [p] in the nearest ancestor marked as [View.Reference] into the local coordinate system. Allows to define [out] to avoid allocation. */
    fun renderToLocal(p: IPoint, out: Point = Point()): Point = renderToLocalXY(p.x, p.y, out)

    /** Converts a point [x], [y] in the nearest ancestor marked as [View.Reference] into the local coordinate system. Allows to define [out] to avoid allocation. */
    fun renderToLocalXY(x: Double, y: Double, out: Point = Point()): Point = this.globalMatrixInv.transform(x, y, out)

    /** Converts a point [x], [y] in the nearest ancestor marked as [View.Reference] into the local X coordinate. */
    fun renderToLocalX(x: Double, y: Double): Double = this.globalMatrixInv.fastTransformX(x, y)

    /** Converts a point [x], [y] in the nearest ancestor marked as [View.Reference] into the local Y coordinate. */
    fun renderToLocalY(x: Double, y: Double): Double = this.globalMatrixInv.fastTransformY(x, y)

    /** Converts the local point [p] into a point in the nearest ancestor masked as [View.Reference]. Allows to define [out] to avoid allocation. */
    fun localToRender(p: IPoint, out: Point = Point()): Point = localToRenderXY(p.x, p.y, out)

    /** Converts the local point [x],[y] into a point in the nearest ancestor masked as [View.Reference]. Allows to define [out] to avoid allocation. */
    fun localToRenderXY(x: Double, y: Double, out: Point = Point()): Point = this.globalMatrix.transform(x, y, out)

    /** Converts the local point [x],[y] into a X coordinate in the nearest ancestor masked as [View.Reference]. */
    fun localToRenderX(x: Double, y: Double): Double = this.globalMatrix.fastTransformX(x, y)

    /** Converts the local point [x],[y] into a Y coordinate in the nearest ancestor masked as [View.Reference]. */
    fun localToRenderY(x: Double, y: Double): Double = this.globalMatrix.fastTransformY(x, y)

    /**
     * Determines the view at the global point defined by [x] and [y] if any, or null
     *
     * When a container, recursively finds the [View] displayed the given global [x], [y] coordinates.
     *
     * @returns The (visible) [View] displayed at the given coordinates or `null` if none is found.
     */
    fun hitTest(x: Double, y: Double): View? {
        _children?.fastForEachReverse { child ->
            if (child.visible) {
                child.hitTest(x, y)?.let {
                    return it
                }
            }
        }
        val res = hitTestInternal(x, y)
        if (res != null) return res
        return if (this is Stage) this else null
    }

    fun hitTestAny(x: Double, y: Double): Boolean = hitTest(x, y) != null

    var hitTestUsingShapes: Boolean? = null

    /** [x] and [y] coordinates are global */
    protected fun hitTestInternal(x: Double, y: Double): View? {
        //println("x,y: $x,$y")
        //println("bounds: ${getGlobalBounds(_localBounds)}")
        //if (!getGlobalBounds(_localBounds).contains(x, y)) return null

        val bounds = getLocalBounds()

        // Adjusted coordinates to compensate anchoring
        val llx = globalToLocalX(x, y)
        val lly = globalToLocalY(x, y)

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


        if (hitTestUsingShapes == true || (hitTestUsingShapes == null && hitShape != null)) {
            return if (hitShape!!.containsPoint(lx, ly)) this else null
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
        pos.setTo(0.0, 0.0)
        _scaleX = 1.0; _scaleY = 1.0
        _skewX = 0.0; _skewY = 0.0
        _rotation = 0.radians
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

    /**
     * Gets the concatenated [Matrix] of this [View] up to the [target] view.
     * Allows to define an [out] matrix that will hold the result to prevent allocations.
     */
    fun getConcatMatrix(target: View, out: Matrix = Matrix()): Matrix {
        var current: View? = this
        out.identity()

        while (current != null) {
            //out.premultiply(current.localMatrix)
            out.multiply(out, current.localMatrix)
            if (current == target) break
            current = current.parent
        }

        return out
    }

    /** Returns the global bounds of this object. Note this incurs in allocations. Use [getGlobalBounds] (out) to avoid it */
    val globalBounds: Rectangle get() = getGlobalBounds()

    /** Returns the global bounds of this object. Allows to specify an [out] [Rectangle] to prevent allocations. */
    fun getGlobalBounds(out: Rectangle = Rectangle()): Rectangle = getBounds(this.root, out)

    /** Get the bounds of this view, using the [target] view as coordinate system. Not providing a [target] will return the local bounds. Allows to specify [out] [Rectangle] to prevent allocations. */
    fun getBounds(target: View? = this, out: Rectangle = Rectangle()): Rectangle {
        //val concat = (parent ?: this).getConcatMatrix(target ?: this)
        val concat = (this).getConcatMatrix(target ?: this)
        val bb = BoundsBuilder()

        getLocalBoundsInternal(out)

        val p1x = out.left
        val p1y = out.top

        val p2x = out.right
        val p2y = out.top

        val p3x = out.right
        val p3y = out.bottom

        val p4x = out.left
        val p4y = out.bottom

        bb.add(concat.fastTransformX(p1x, p1y), concat.fastTransformY(p1x, p1y))
        bb.add(concat.fastTransformX(p2x, p2y), concat.fastTransformY(p2x, p2y))
        bb.add(concat.fastTransformX(p3x, p3y), concat.fastTransformY(p3x, p3y))
        bb.add(concat.fastTransformX(p4x, p4y), concat.fastTransformY(p4x, p4y))

        bb.getBounds(out)
        return out
    }

    /**
     * Get local bounds of the view. Allows to specify [out] [Rectangle] if you want to reuse an object.
     * **NOTE:** that if [out] is not provided, the [Rectangle] returned shouldn't stored and modified since it is owned by this class.
     */
    fun getLocalBounds(out: Rectangle = _localBounds) = out.apply { getLocalBoundsInternal(out) }

    private val _localBounds: Rectangle = Rectangle()
    open fun getLocalBoundsInternal(out: Rectangle = _localBounds): Unit {
        out.setTo(0, 0, 0, 0)
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
    fun findViewByName(name: String): View? {
        if (this.name == name) return this
        if (this.isContainer) {
            //(this as Container).children.fastForEach { child ->
            (this as Container).forEachChildren { child ->
                val named = child.findViewByName(name)
                if (named != null) return named
            }
        }
        return null
    }

    /**
     * Allows to clone this view.
     * This method is inadvisable in normal circumstances.
     * This might not work property if the [View] doesn't override the [createInstance] method.
     */
    open fun clone(): View = createInstance().apply {
        this@apply.copyPropsFrom(this@View)
    }
}

// Doesn't seems to work
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
 * Determines if the local coords [x], [y], hits this view or any of this descendants.
 * Returns the view hitting or null
 */
fun View.hitTest(x: Int, y: Int): View? = hitTest(x.toDouble(), y.toDouble())

/**
 * Determines if the local coords [pos], hits this view or any of this descendants.
 * Returns the view hitting or null
 */
fun View.hitTest(pos: IPoint): View? = hitTest(pos.x, pos.y)
//fun View.hitTest(pos: Point): View? = hitTest(pos.x, pos.y)

/**
 * Checks if this view has an [ancestor].
 */
fun View.hasAncestor(ancestor: View): Boolean {
    return if (this == ancestor) true else this.parent?.hasAncestor(ancestor) ?: false
}

/**
 * Replaces this view in its parent with [view].
 * Returns true if the replacement was successful.
 * If this view doesn't have a parent or [view] is the same as [this], returns null.
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
    val component = object : UpdateComponentV2 {
        override val view: View get() = this@addUpdater
        override fun update(dt: HRTimeSpan) {
            updatable(this@addUpdater, dt.timeSpan)
        }
    }.attach()
    component.update(HRTimeSpan.ZERO)
    return Cancellable { component.detach() }
}

/** Adds a block that will be executed per frame to this view. As parameter the block will receive a [TimeSpan] with the time elapsed since the previous frame. */
fun <T : View> T.addHrUpdater(updatable: T.(dt: HRTimeSpan) -> Unit): Cancellable {
    val component = object : UpdateComponentV2 {
        override val view: View get() = this@addHrUpdater
        override fun update(dt: HRTimeSpan) {
            updatable(this@addHrUpdater, dt)
        }
    }.attach()
    component.update(HRTimeSpan.ZERO)
    return Cancellable { component.detach() }
}

fun <T : View> T.addFixedUpdater(
    time: TimeSpan,
    initial: Boolean = true,
    limitCallsPerFrame: Int = 16,
    updatable: T.() -> Unit
): Cancellable = addFixedUpdater(time.hr, initial, limitCallsPerFrame, updatable)

fun <T : View> T.addFixedUpdater(
    timesPerSecond: Frequency,
    initial: Boolean = true,
    limitCallsPerFrame: Int = 16,
    updatable: T.() -> Unit
): Cancellable = addFixedUpdater(timesPerSecond.timeSpan.hr, initial, limitCallsPerFrame, updatable)

/**
 * Adds an [updatable] block that will be executed every [time] time, the calls will be discretized on each frame and will handle accumulations.
 * The [initial] properly allows to adjust if the [updatable] will be called immediately after calling this function.
 * To avoid executing too much blocks, when there is a long pause, [limitCallsPerFrame] limits the number of times the block can be executed in a single frame.
 */
fun <T : View> T.addFixedUpdater(
    time: HRTimeSpan,
    initial: Boolean = true,
    limitCallsPerFrame: Int = 16,
    updatable: T.() -> Unit
): Cancellable {
    val tickTime = time
    var accum = 0.hrNanoseconds
    val component = object : UpdateComponentV2 {
        override val view: View get() = this@addFixedUpdater
        override fun update(dt: HRTimeSpan) {
            accum += dt
            //println("UPDATE: accum=$accum, tickTime=$tickTime")
            var calls = 0
            while (accum >= tickTime * 0.75) {
                accum -= tickTime
                updatable(this@addFixedUpdater)
                calls++
                if (calls >= limitCallsPerFrame) {
                    // We do not accumulate for the next frame in this case
                    accum = 0.hrNanoseconds
                    break
                }
            }
            if (calls > 0) {
                // Do not accumulate for small fractions since this would cause hiccups!
                if (accum < tickTime * 0.25) {
                    accum = 0.hrNanoseconds
                }
            }
        }
    }.attach()
    if (initial) {
        updatable(this@addFixedUpdater)
    }
    return Cancellable { component.detach() }
}

fun <T : View> T.onNextFrame(updatable: T.(views: Views) -> Unit) {
    object : UpdateComponentWithViews {
        override val view: View get() = this@onNextFrame
        override fun update(views: Views, ms: Double) {
            removeFromView()
            updatable(this@onNextFrame, views)
        }
    }.attach()
}

/**
 * Returns the number of ancestors of this view.
 * Views without parents return 0.
 */
val View?.ancestorCount: Int get() = this?.parent?.ancestorCount?.plus(1) ?: 0

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
        this.forEachChildren { child ->
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
 * Iterates all the descendants [View]s including this calling the [handler].
 * Iteration happens in [Pre-order (NLR)](https://en.wikipedia.org/wiki/Tree_traversal#Pre-order_(NLR)).
 */
fun View?.foreachDescendant(handler: (View) -> Unit) {
    if (this != null) {
        handler(this)
        if (this.isContainer) {
            this.forEachChildren { child ->
                child.foreachDescendant(handler)
            }
        }
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

/** Indexer that allows to get a descendant marked with the name [name]. */
operator fun View?.get(name: String): View? = firstDescendantWith { it.name == name }

/** Sets the position [point] of the view and returns this (chaineable). */
inline fun <T : View> T.position(point: IPoint): T = position(point.x, point.y)
inline fun <T : View> T.visible(visible: Boolean): T = this.also { it.visible = visible }
inline fun <T : View> T.name(name: String?): T = this.also { it.name = name }
inline fun <T : View> T.hitShape(crossinline block: VectorBuilder.() -> Unit): T {
    buildPath { block() }.also {
        this.hitShape = it
    }
    return this
}

fun <T : View> T.size(width: Double, height: Double): T {
    this.width = width
    this.height = height
    return this
}

fun <T : View> T.size(width: Int, height: Int): T = size(width.toDouble(), height.toDouble())

@Deprecated("", ReplaceWith("this[name]", "com.soywiz.korge.view.get"))
fun View?.firstDescendantWithName(name: String): View? = this[name]

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
        this.forEachChildren { child ->
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
            this.forEachChildren { child ->
                child.descendantsWith(out, check)
            }
        }
    }
    return out
}

/** Chainable method returning this that sets [View.x] and [View.y] */
fun <T : View> T.xy(x: Double, y: Double): T {
    this.x = x
    this.y = y
    return this
}

fun <T : View> T.xy(x: Int, y: Int): T = xy(x.toDouble(), y.toDouble())

/** Chainable method returning this that sets [View.x] and [View.y] */
fun <T : View> T.position(x: Int, y: Int): T = xy(x.toDouble(), y.toDouble())
fun <T : View> T.position(x: Double, y: Double): T = xy(x, y)

fun <T : View> T.positionX(x: Double): T {
    this.x = x
    return this
}

fun <T : View> T.positionY(y: Double): T {
    this.y = y
    return this
}

/** Chainable method returning this that sets [View.x] */
fun <T : View> T.positionX(x: Int): T = positionX(x.toDouble())

/** Chainable method returning this that sets [View.y] */
fun <T : View> T.positionY(y: Int): T = positionY(y.toDouble())

/** Chainable method returning this that sets [this] View in the middle between [x1] and [x2] */
fun <T : View> T.centerXBetween(x1: Double, x2: Double): T {
    this.x = (x2 + x1 - this.width) / 2
    return this
}

/** Chainable method returning this that sets [this] View in the middle between [y1] and [y2] */
fun <T : View> T.centerYBetween(y1: Double, y2: Double): T {
    this.y = (y2 + y1 - this.height) / 2
    return this
}

/**
 * Chainable method returning this that sets [this] View
 * in the middle between [x1] and [x2] and in the middle between [y1] and [y2]
 */
fun <T : View> T.centerBetween(x1: Double, y1: Double, x2: Double, y2: Double): T =
    this.centerXBetween(x1, x2).centerYBetween(y1, y2)

/**
 * Chainable method returning this that sets [View.x] so that
 * [this] View is centered on the [other] View horizontally
 */
fun <T : View> T.centerXOn(other: View): T = this.centerXBetween(other.x, other.x + other.width)

/**
 * Chainable method returning this that sets [View.y] so that
 * [this] View is centered on the [other] View vertically
 */
fun <T : View> T.centerYOn(other: View): T = this.centerYBetween(other.y, other.y + other.height)

/**
 * Chainable method returning this that sets [View.x] and [View.y]
 * so that [this] View is centered on the [other] View
 */
fun <T : View> T.centerOn(other: View): T = this.centerXOn(other).centerYOn(other)

/**
 * Chainable method returning this that sets [View.x] so that
 * [this] View's left side is aligned with the [other] View's left side
 */
fun <T : View> T.alignLeftToLeftOf(other: View, padding: Double = 0.0): T {
    x = other.x + padding
    return this
}

/**
 * Chainable method returning this that sets [View.x] so that
 * [this] View's left side is aligned with the [other] View's right side
 */
fun <T : View> T.alignLeftToRightOf(other: View, padding: Double = 0.0): T {
    x = other.x + other.width + padding
    return this
}

/**
 * Chainable method returning this that sets [View.x] so that
 * [this] View's right side is aligned with the [other] View's left side
 */
fun <T : View> T.alignRightToLeftOf(other: View, padding: Double = 0.0): T {
    x = other.x - width - padding
    return this
}

/**
 * Chainable method returning this that sets [View.x] so that
 * [this] View's right side is aligned with the [other] View's right side
 */
fun <T : View> T.alignRightToRightOf(other: View, padding: Double = 0.0): T {
    x = other.x + other.width - width - padding
    return this
}

/**
 * Chainable method returning this that sets [View.y] so that
 * [this] View's top side is aligned with the [other] View's top side
 */
fun <T : View> T.alignTopToTopOf(other: View, padding: Double = 0.0): T {
    y = other.y + padding
    return this
}

/**
 * Chainable method returning this that sets [View.y] so that
 * [this] View's top side is aligned with the [other] View's bottom side
 */
fun <T : View> T.alignTopToBottomOf(other: View, padding: Double = 0.0): T {
    y = other.y + other.height + padding
    return this
}

/**
 * Chainable method returning this that sets [View.y] so that
 * [this] View's bottom side is aligned with the [other] View's top side
 */
fun <T : View> T.alignBottomToTopOf(other: View, padding: Double = 0.0): T {
    y = other.y - height - padding
    return this
}

/**
 * Chainable method returning this that sets [View.y] so that
 * [this] View's bottom side is aligned with the [other] View's bottom side
 */
fun <T : View> T.alignBottomToBottomOf(other: View, padding: Double = 0.0): T {
    y = other.y + other.height - height - padding
    return this
}

/** Chainable method returning this that sets [View.rotation] */
fun <T : View> T.rotation(rot: Angle): T {
    this.rotationRadians = rot.radians
    return this
}

/** Chainable method returning this that sets [View.skewX] and [View.skewY] */
fun <T : View> T.skew(sx: Double, sy: Double): T {
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

/** Chainable method returning this that sets [View.alpha] */
fun <T : View> T.alpha(alpha: Double): T {
    this.alpha = alpha
    return this
}

@Deprecated("Kotlin/Native boxes inline+Number")
inline fun <T : View> T.skew(sx: Number, sy: Number): T = skew(sx.toDouble(), sy.toDouble())
@Deprecated("Kotlin/Native boxes inline+Number")
inline fun <T : View> T.scale(sx: Number, sy: Number = sx): T = scale(sx.toDouble(), sy.toDouble())
@Deprecated("Kotlin/Native boxes inline+Number")
inline fun <T : View> T.alpha(alpha: Number): T = alpha(alpha.toDouble())
@Deprecated("Kotlin/Native boxes inline+Number")
inline fun <T : View> T.rotation(rot: Number): T = this.rotation(rot.toDouble().radians)
@Deprecated("Kotlin/Native boxes inline+Number")
inline fun <T : View> T.rotationDegrees(degs: Number): T = rotation(degs.toDouble().degrees)

@Deprecated("Kotlin/Native boxes inline+Number")
inline fun <T : View> T.xy(x: Number, y: Number): T = xy(x.toDouble(), y.toDouble())
@Deprecated("Kotlin/Native boxes inline+Number")
inline fun <T : View> T.position(x: Number, y: Number): T = xy(x.toDouble(), y.toDouble())
@Deprecated("Kotlin/Native boxes inline+Number")
inline fun <T : View> T.positionX(x: Number): T = positionX(x.toDouble())
@Deprecated("Kotlin/Native boxes inline+Number")
inline fun <T : View> T.positionY(y: Number): T = positionY(y.toDouble())
@Deprecated("Kotlin/Native boxes inline+Number")
inline fun <T : View> T.size(width: Number, height: Number): T = size(width.toDouble(), height.toDouble())

@Deprecated("Kotlin/Native boxes inline+Number")
inline fun View.hitTest(x: Number, y: Number): View? = hitTest(x.toDouble(), y.toDouble())

@Deprecated("Kotlin/Native boxes inline+Number")
inline fun <T : View> T.centerXBetween(x1: Number, x2: Number): T = centerXBetween(x1.toDouble(), x2.toDouble())
@Deprecated("Kotlin/Native boxes inline+Number")
inline fun <T : View> T.centerYBetween(y1: Number, y2: Number): T = centerYBetween(y1.toDouble(), y2.toDouble())
@Deprecated("Kotlin/Native boxes inline+Number")
inline fun <T : View> T.centerBetween(x1: Number, y1: Number, x2: Number, y2: Number): T =
    centerBetween(x1.toDouble(), y1.toDouble(), x2.toDouble(), y2.toDouble())

@Deprecated("Kotlin/Native boxes inline+Number")
inline fun <T : View> T.alignLeftToLeftOf(other: View, padding: Number): T =
    alignLeftToLeftOf(other, padding.toDouble())

@Deprecated("Kotlin/Native boxes inline+Number")
inline fun <T : View> T.alignLeftToRightOf(other: View, padding: Number): T =
    alignLeftToRightOf(other, padding.toDouble())

@Deprecated("Kotlin/Native boxes inline+Number")
inline fun <T : View> T.alignRightToLeftOf(other: View, padding: Number): T =
    alignRightToLeftOf(other, padding.toDouble())

@Deprecated("Kotlin/Native boxes inline+Number")
inline fun <T : View> T.alignRightToRightOf(other: View, padding: Number): T =
    alignRightToRightOf(other, padding.toDouble())

@Deprecated("Kotlin/Native boxes inline+Number")
inline fun <T : View> T.alignTopToTopOf(other: View, padding: Number): T = alignTopToTopOf(other, padding.toDouble())
@Deprecated("Kotlin/Native boxes inline+Number")
inline fun <T : View> T.alignTopToBottomOf(other: View, padding: Number): T =
    alignTopToBottomOf(other, padding.toDouble())

@Deprecated("Kotlin/Native boxes inline+Number")
inline fun <T : View> T.alignBottomToTopOf(other: View, padding: Number): T =
    alignBottomToTopOf(other, padding.toDouble())

@Deprecated("Kotlin/Native boxes inline+Number")
inline fun <T : View> T.alignBottomToBottomOf(other: View, padding: Number): T =
    alignBottomToBottomOf(other, padding.toDouble())
