package korlibs.korge.view.filter

import korlibs.korge.view.property.*
import korlibs.math.geom.*
import kotlin.math.*

class BlurFilter(
    radius: Float = 4f,
    expandBorder: Boolean = true,
    @ViewProperty
    var optimize: Boolean = true
) : ComposedFilter(), FilterWithFiltering {
    private val horizontal = DirectionalBlurFilter(angle = 0.degrees, radius, expandBorder).also { filters.add(it) }
    private val vertical = DirectionalBlurFilter(angle = 90.degrees, radius, expandBorder).also { filters.add(it) }
    override var filtering: Boolean
        get() = horizontal.filtering
        set(value) {
            horizontal.filtering = value
            vertical.filtering = value
        }
    @ViewProperty
    var expandBorder: Boolean
        get() = horizontal.expandBorder
        set(value) {
            horizontal.expandBorder = value
            vertical.expandBorder = value
        }
    @ViewProperty
    var radius: Float = radius
        set(value) {
            field = value
            horizontal.radius = radius
            vertical.radius = radius
        }
    override val recommendedFilterScale: Double get() = if (!optimize || radius <= 2.0) 1.0 else 1.0 / log2(radius * 0.5)

    override val isIdentity: Boolean get() = radius == 0f
}
