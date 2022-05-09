package com.soywiz.korge.view.filter

import com.soywiz.korge.debug.*
import com.soywiz.korge.view.*
import com.soywiz.korma.geom.*
import com.soywiz.korui.*
import kotlin.math.*

class BlurFilter(radius: Double = 4.0, expandBorder: Boolean = true) : ComposedFilter() {
    companion object {
        @Deprecated("", ReplaceWith("BlurFilter(radius = initialRadius)"))
        operator fun invoke(initialRadius: Double, dummy: Unit = Unit): BlurFilter = BlurFilter(radius = initialRadius)
    }
    private val horizontal = DirectionalBlurFilter(angle = 0.degrees, radius, expandBorder).also { filters.add(it) }
    private val vertical = DirectionalBlurFilter(angle = 90.degrees, radius, expandBorder).also { filters.add(it) }
    var expandBorder: Boolean
        get() = horizontal.expandBorder
        set(value) {
            horizontal.expandBorder = value
            vertical.expandBorder = value
        }
    var radius: Double = radius
        set(value) {
            field = value
            horizontal.radius = radius
            vertical.radius = radius
        }
    override val recommendedFilterScale: Double get() = if (radius <= 2.0) 1.0 else 1.0 / log2(radius * 0.5)
    //override val recommendedFilterScale: Double get() = 1.0

    override val isIdentity: Boolean get() = radius == 0.0

    override fun buildDebugComponent(views: Views, container: UiContainer) {
        container.uiEditableValue(::radius)
    }
}
