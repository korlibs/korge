package com.soywiz.korge.view.filter

import com.soywiz.korge.debug.uiEditableValue
import com.soywiz.korge.view.Views
import com.soywiz.korge.view.property.*
import com.soywiz.korma.geom.degrees
import com.soywiz.korui.UiContainer
import kotlin.math.log2

class BlurFilter(
    radius: Double = 4.0,
    expandBorder: Boolean = true,
    @ViewProperty
    var optimize: Boolean = true
) : ComposedFilter() {
    companion object {
        @Deprecated("", ReplaceWith("BlurFilter(radius = initialRadius)"))
        operator fun invoke(initialRadius: Double, dummy: Unit = Unit): BlurFilter = BlurFilter(radius = initialRadius)
    }
    private val horizontal = DirectionalBlurFilter(angle = 0.degrees, radius, expandBorder).also { filters.add(it) }
    private val vertical = DirectionalBlurFilter(angle = 90.degrees, radius, expandBorder).also { filters.add(it) }
    @ViewProperty
    var expandBorder: Boolean
        get() = horizontal.expandBorder
        set(value) {
            horizontal.expandBorder = value
            vertical.expandBorder = value
        }
    @ViewProperty
    var radius: Double = radius
        set(value) {
            field = value
            horizontal.radius = radius
            vertical.radius = radius
        }
    override val recommendedFilterScale: Double get() = if (!optimize || radius <= 2.0) 1.0 else 1.0 / log2(radius * 0.5)

    override val isIdentity: Boolean get() = radius == 0.0
}
