package com.soywiz.korge.component.length

import com.soywiz.kds.*
import com.soywiz.klock.*
import com.soywiz.kmem.*
import com.soywiz.korge.annotations.*
import com.soywiz.korge.baseview.*
import com.soywiz.korge.component.*
import com.soywiz.korge.view.*
import com.soywiz.korio.lang.*
import com.soywiz.korma.geom.*
import com.soywiz.korma.length.*
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.set
import kotlin.native.concurrent.*
import kotlin.reflect.*

/**
 * Binds a property [prop] in this [View] to the [Length] value returned by [value].
 * The binding will register a component that will compute the [Length] for that view in its container/context
 * and will set the specific property.
 *
 * The binding requires to know if the property is vertical or horizontal.
 * The direction will be tried to be determined by its name by default, but you can explicitly specify it with [horizontal]
 *
 * Returns a [Cancellable] to stop the binding.
 */
@KorgeExperimental
fun View.bindLength(prop: KMutableProperty1<View, Double>, horizontal: Boolean = prop.isHorizontal, value: LengthExtensions.() -> Length): Cancellable {
    return bindLength(prop.name, { prop.set(this, it) }, horizontal, value)
}

@KorgeExperimental
fun View.bindLength(name: String, setProp: (Double) -> Unit, horizontal: Boolean, value: LengthExtensions.() -> Length): Cancellable {
    val component = getOrCreateComponentUpdateWithViews { BindLengthComponent(it) }
    component.setBind(horizontal, name, setProp, value(LengthExtensions))
    return Cancellable {
        component.removeBind(horizontal, name)
    }
}

// @TODO: Extension properties for inline classes seem to not be supported for now. This should be more efficient than the Extra.PropertyThis once supported
//val View.lengths get() = ViewWithLength(this)
//inline class ViewWithLength(val view: View) : LengthExtensions

/**
 * Supports setting/binding properties by using [Length] units, until they are set to null.
 *
 * Usage:
 *
 * ```
 * view.lengths { width = 50.vw }
 * ```
 *
 * The current implementation has some side effects to have into consideration:
 * - The component is using the view to hold a component that will update these properties on each frame
 * - If the original property is changed, it will be overriden by the computed [Length] on each frame
 * - To remove the binding, set the property to null
 */
@KorgeExperimental
@ThreadLocal
val View.lengths by Extra.PropertyThis { ViewWithLength(this) }
class ViewWithLength(val view: View) : LengthExtensions {
    inline operator fun <T> invoke(block: ViewWithLength.() -> T): T = block()
}

var ViewWithLength.width: Length? by ViewWithLengthProp(View::scaledWidth)
var ViewWithLength.height: Length? by ViewWithLengthProp(View::scaledHeight)
var ViewWithLength.x: Length? by ViewWithLengthProp(View::x)
var ViewWithLength.y: Length? by ViewWithLengthProp(View::y)
var ViewWithLength.scale: Length? by ViewWithLengthProp(View::scale)
var ViewWithLength.scaleX: Length? by ViewWithLengthProp(View::scaleX)
var ViewWithLength.scaleY: Length? by ViewWithLengthProp(View::scaleY)

fun ViewWithLengthProp(prop: KMutableProperty1<View, Double>, horizontal: Boolean? = null) = LengthDelegatedProperty<ViewWithLength>(prop, horizontal) { it.view }
fun LengthDelegatedProperty(prop: KMutableProperty1<View, Double>, horizontal: Boolean? = null) = LengthDelegatedProperty<View>(prop, horizontal) { it }

class LengthDelegatedProperty<T>(val prop: KMutableProperty1<View, Double>, horizontal: Boolean? = null, val getView: (T) -> View) {
    val horizontal = horizontal ?: prop.isHorizontal
    private fun getBind(view: View): BindLengthComponent =
        view.getOrCreateComponentUpdateWithViews { BindLengthComponent(it) }

    operator fun getValue(viewHolder: T, property: KProperty<*>): Length? {
        return getBind(getView(viewHolder)).getLength(prop.name, horizontal)
    }
    operator fun setValue(viewHolder: T, property: KProperty<*>, length: Length?) {
        val view = getView(viewHolder)
        val bind = getBind(view)
        if (length == null) {
            bind.removeBind(horizontal, prop.name)
        } else {
            bind.setBind(horizontal, prop.name, { prop.set(view, it) }, length)
        }
    }
}

internal val KCallable<*>.isHorizontal get() = when (name) {
    "x", "width" -> true
    else -> name.contains("x") || name.contains("X") || name.contains("width") || name.contains("Width")
}

internal class BindLengthComponent(override val view: BaseView) : UpdateComponentWithViews {
    private val binds = Array(2) { LinkedHashMap<String, Pair<(Double) -> Unit, Length>>() }
    private lateinit var views: Views

    fun getLength(key: String, horizontal: Boolean): Length? {
        return binds[horizontal.toInt()][key]?.second
    }

    private open class BaseLengthContext : Length.Context() {
        override var pixelRatio: Double = 1.0
    }

    private val context = BaseLengthContext()

    fun setBind(x: Boolean, name: String, prop: (Double) -> Unit, value: Length) {
        binds[x.toInt()][name] = prop to value
    }

    fun removeBind(x: Boolean, name: String) {
        binds[x.toInt()].remove(name)
        if (binds[0].isEmpty() && binds[1].isEmpty()) {
            removeFromView()
        }
    }

    private val tempTransform = MMatrix.Transform()
    override fun update(views: Views, dt: TimeSpan) {
        this.views = views
        val container = (view as? View?)?.parent ?: views.stage
        tempTransform.setMatrixNoReturn(container.globalMatrix)
        val scaleAvgInv = 1.0 / tempTransform.scaleAvg

        context.fontSize = 16.0 // @TODO: Can we store something in the views?
        context.viewportWidth = if (views.clipBorders || views.scaleAnchor != Anchor.TOP_LEFT) views.virtualWidthDouble else views.actualVirtualWidth.toDouble()
        context.viewportHeight = if (views.clipBorders || views.scaleAnchor != Anchor.TOP_LEFT) views.virtualHeightDouble else views.actualVirtualHeight.toDouble()
        context.pixelRatio = views.devicePixelRatio * scaleAvgInv
        context.pixelsPerInch = views.pixelsPerInch * scaleAvgInv
        for (horizontal in arrayOf(false, true)) {
            val size = if (horizontal) container.width else container.height
            context.size = size.toInt()
            for ((_, value) in binds[horizontal.toInt()]) {
                val pointValue = value.second.calc(context).toDouble()
                //println("$prop -> $pointValue [$value]")
                value.first(pointValue)
            }
        }
    }
}
