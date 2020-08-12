package com.soywiz.korge.view

import com.soywiz.kds.iterators.*
import com.soywiz.korge.animate.*
import com.soywiz.korim.color.*
import com.soywiz.korma.geom.*
import kotlin.reflect.*

class QView(val views: List<View>) {
    val firstOrNull: View? = views.firstOrNull()
    val first: View by lazy { firstOrNull ?: DummyView() }

    constructor(view: View?) : this(if (view != null) listOf(view) else emptyList())

    operator fun get(name: String): QView = QView(views.mapNotNull { it.firstDescendantWith { it.name == name } })

    fun alpha(value: Double) = views.fastForEach { it.alpha(value) }

    fun position(): Point = first.pos.copy()

    fun <T> setProperty(prop: KMutableProperty1<View, T>, value: T) {
        views.fastForEach { prop.set(it, value) }
    }

    inline fun fastForEach(callback: (View) -> Unit) {
        views.fastForEach { callback(it) }
    }

    var alpha: Double
        get() = first.alpha
        set(value) = fastForEach { it.alpha = value }

    var scale: Double
        get() = first.scale
        set(value) = fastForEach { it.scale = value }

    var scaleX: Double
        get() = first.scaleX
        set(value) = fastForEach { it.scaleX = value }

    var scaleY: Double
        get() = first.scaleY
        set(value) = fastForEach { it.scaleY = value }

    var x: Double
        get() = first.x
        set(value) = fastForEach { it.x = value }

    var y: Double
        get() = first.y
        set(value) = fastForEach { it.y = value }

    var rotation: Angle
        get() = first.rotation
        set(value) = fastForEach { it.rotation = value }

    var skewX: Angle
        get() = first.skewX
        set(value) = fastForEach { it.skewX = value }

    var skewY: Angle
        get() = first.skewY
        set(value) = fastForEach { it.skewY = value }

    var colorMul: RGBA
        get() = first.colorMul
        set(value) = fastForEach { it.colorMul = value }

    /** Sets the state (if available) of all the views in this query */
    fun state(name: String) {
        fastForEach { it.play(name) }
    }
}

/** Indexer that allows to get a descendant marked with the name [name]. */
operator fun View?.get(name: String): QView = QView(this)[name]
