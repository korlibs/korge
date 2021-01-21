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
    val component = getOrCreateComponentUpdateWithViews { BindLengthComponent(it) }
    component.setBind(horizontal, prop, value(LengthExtensions))
    return Cancellable {
        component.removeBind(horizontal, prop)
    }
}

private val KProperty<*>.isHorizontal get() = when (name) {
    "x", "width" -> true
    else -> name.contains("x") || name.contains("X") || name.contains("width") || name.contains("Width")
}

internal class BindLengthComponent(override val view: BaseView) : UpdateComponentWithViews {
    private val binds = Array(2) { LinkedHashMap<KMutableProperty0<Double>, Length>() }
    private lateinit var views: Views

    private open class BaseLengthContext : Length.Context() {
        override var pixelRatio: Double = 1.0
    }

    private val context = BaseLengthContext()

    fun setBind(x: Boolean, prop: KMutableProperty0<Double>, value: Length) {
        binds[x.toInt()][prop] = value
    }

    fun removeBind(x: Boolean, prop: KMutableProperty0<Double>) {
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
                prop.set(pointValue)
            }
        }
    }
}
