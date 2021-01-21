package com.soywiz.korge.component.length

import com.soywiz.klock.*
import com.soywiz.kmem.toInt
import com.soywiz.korge.baseview.*
import com.soywiz.korge.component.*
import com.soywiz.korge.view.*
import com.soywiz.korio.lang.*
import com.soywiz.korma.geom.Matrix
import com.soywiz.korui.layout.*
import kotlin.reflect.*

fun View.bindLength(prop: KMutableProperty0<Double>, horizontal: Boolean = prop.isHorizontal, value: LengthExtensions.() -> Length): Cancellable {
    return bindLength({ prop.set(it) }, horizontal, value)
}

fun <T> View.bindLength(receiver: T, prop: KMutableProperty1<T, Double>, horizontal: Boolean = prop.isHorizontal, value: LengthExtensions.() -> Length): Cancellable {
    return bindLength({ prop.set(receiver, it) }, prop.isHorizontal, value)
}

fun View.bindLength(setProp: (Double) -> Unit, horizontal: Boolean, value: LengthExtensions.() -> Length): Cancellable {
    val component = getOrCreateComponentUpdateWithViews { BindLengthComponent(it) }
    component.setBind(horizontal, setProp, value(LengthExtensions))
    return Cancellable {
        component.removeBind(horizontal, setProp)
    }
}

//var View.widthLength: Length by LengthDelegatedProperty(View::scaledWidth)
//var View.heightLength: Length by LengthDelegatedProperty(View::scaledHeight)
//var View.xLength: Length by LengthDelegatedProperty(View::x)
//var View.yLength: Length by LengthDelegatedProperty(View::y)
//var View.scaleLength: Length by LengthDelegatedProperty(View::scale)
//var View.scaleXLength: Length by LengthDelegatedProperty(View::scaleX)
//var View.scaleYLength: Length by LengthDelegatedProperty(View::scaleY)

class LengthDelegatedProperty(val prop: KMutableProperty1<View, Double>) {
    operator fun getValue(view: View, property: KProperty<*>): Length = Length.ZERO
    operator fun setValue(view: View, property: KProperty<*>, length: Length) {
        (view.parent ?: view).bindLength(view, prop) { length }
    }
}

val KProperty<*>.isHorizontal get() = when (name) {
    "x", "width" -> true
    else -> name.contains("x") || name.contains("X") || name.contains("width") || name.contains("Width")
}

internal class BindLengthComponent(override val view: BaseView) : UpdateComponentWithViews {
    private val binds = Array(2) { LinkedHashMap<(Double) -> Unit, Length>() }
    private lateinit var views: Views

    private open class BaseLengthContext : Length.Context() {
        override var pixelRatio: Double = 1.0
    }

    private val context = BaseLengthContext()

    fun setBind(x: Boolean, prop: (Double) -> Unit, value: Length) {
        binds[x.toInt()][prop] = value
    }

    fun removeBind(x: Boolean, prop: (Double) -> Unit) {
        binds[x.toInt()].remove(prop)
        if (binds[0].isEmpty() && binds[1].isEmpty()) {
            removeFromView()
        }
    }

    private val tempTransform = Matrix.Transform()
    override fun update(views: Views, dt: TimeSpan) {
        this.views = views
        val container = view as? View? ?: views.stage
        tempTransform.setMatrix(container.globalMatrix)
        val scaleAvgInv = 1.0 / tempTransform.scaleAvg

        context.fontSize = 16.0 // @TODO: Can we store something in the views?
        context.viewportWidth = views.actualVirtualWidth.toDouble()
        context.viewportHeight = views.actualVirtualHeight.toDouble()
        context.pixelRatio = views.ag.devicePixelRatio * scaleAvgInv
        context.pixelsPerInch = views.ag.pixelsPerInch * scaleAvgInv
        for (horizontal in arrayOf(false, true)) {
            val size = if (horizontal) container.width else container.height
            context.size = size.toInt()
            for ((prop, value) in binds[horizontal.toInt()]) {
                val pointValue = value.calc(context).toDouble()
                //println("$prop -> $pointValue [$value]")
                prop(pointValue)
            }
        }
    }
}
