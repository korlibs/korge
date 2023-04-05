package korlibs.korge.view.filter

import korlibs.datastructure.*
import korlibs.image.color.*
import korlibs.korge.render.*
import korlibs.korge.view.*
import korlibs.korge.view.property.*
import korlibs.math.geom.*

/**
 * Allows to create a single [Filter] that will render several [filters] in order.
 */
open class ComposedFilter private constructor(
    @ViewProperty
    @ViewPropertySubTree
    val filters: FastArrayList<Filter>,
    unit: Unit = Unit
) : Filter {
    companion object {
        fun combine(left: Filter?, right: Filter?): Filter? = when {
            left == null && right == null -> null
            left == null -> right
            right == null -> left
            left is ComposedFilter && right is ComposedFilter -> ComposedFilter(left.filters + right.filters)
            left is ComposedFilter -> ComposedFilter(left.filters + right)
            right is ComposedFilter -> ComposedFilter(listOf(left) + right)
            else -> ComposedFilter(left, right)
        }

        fun combine(left: Filter?, right: List<Filter>): Filter? = when {
            left == null -> when {
                right.isEmpty() -> null
                right.size == 1 -> right.first()
                else -> ComposedFilter(right)
            }
            left is ComposedFilter -> ComposedFilter(left.filters + right)
            else -> ComposedFilter(listOf(left) + right)
        }
    }

    constructor() : this(mutableListOf())
    constructor(filters: List<Filter>) : this(if (filters is FastArrayList<Filter>) filters else FastArrayList(filters))
	constructor(vararg filters: Filter) : this(filters.toList())

    override val allFilters: List<Filter> get() = filters.flatMap { it.allFilters }

    override val recommendedFilterScale: Float get() {
        var out = 1f
        filters.fastForEach { out *= it.recommendedFilterScale  }
        return out
    }

    override fun computeBorder(texWidth: Int, texHeight: Int): MarginInt {
        var sumLeft = 0
        var sumTop = 0
        var sumRight = 0
        var sumBottom = 0
        filters.fastForEach {
            val out = it.computeBorder(texWidth, texHeight)
            sumLeft += out.left
            sumRight += out.right
            sumTop += out.top
            sumBottom += out.bottom
        }
        return MarginInt(sumTop, sumRight, sumBottom, sumLeft)
        //println(out)
    }

    open val isIdentity: Boolean get() = false

    final override fun render(
        ctx: RenderContext,
        matrix: Matrix,
        texture: Texture,
        texWidth: Int,
        texHeight: Int,
        renderColorMul: RGBA,
        blendMode: BlendMode,
        filterScale: Float,
	) {
        var mat = matrix
        var tex = texture
        var last: RenderToTextureResult? = null
        var texWidth = texWidth
        var texHeight = texHeight
        var filterScale = filterScale
        val resultPool = ctx.renderToTextureResultPool
        if (!isIdentity) {
            filters.fastForEach { filter ->
                //if (n != 0) filterScale = filter.recommendedFilterScale
                val result = resultPool.alloc()
                stepBefore()
                //println("n=$n, texWidth=$texWidth, texHeight=$texHeight")
                filter.renderToTextureWithBorderUnsafe(ctx, mat, tex, texWidth, texHeight, filterScale, result)
                result.render()
                ctx.flush()
                resultPool.freeNotNull(last)
                last = result
                mat = result.matrix
                tex = result.newtex!!
                texWidth = result.newTexWidth
                texHeight = result.newTexHeight
            }
        }
        IdentityFilter.render(ctx, mat, tex, texWidth, texHeight, renderColorMul, blendMode, filterScale)
        resultPool.freeNotNull(last)
	}

    /*
    final override fun render(
        ctx: RenderContext,
        matrix: Matrix,
        texture: Texture,
        texWidth: Int,
        texHeight: Int,
        renderColorMul: RGBA,
        blendMode: BlendMode,
        filterScale: Float,
    ) {
        if (isIdentity) return IdentityFilter.render(ctx, matrix, texture, texWidth, texHeight, renderColorMul, blendMode, filterScale)
        renderIndex(ctx, matrix, texture, texWidth, texHeight, renderColorMul, blendMode, filterScale, filters.size - 1)
    }

    fun renderIndex(
        ctx: RenderContext,
        matrix: Matrix,
        texture: Texture,
        texWidth: Int,
        texHeight: Int,
        renderColorMul: RGBA,
        blendMode: BlendMode,
        filterScale: Float,
        level: Int,
    ) {
        stepBefore()
        if (level < 0 || filters.isEmpty()) {
            return IdentityFilter.render(ctx, matrix, texture, texWidth, texHeight, renderColorMul, blendMode, filterScale)
        }
        //println("ComposedFilter.renderIndex: $level")
        val filter = filters[filters.size - level - 1]

        filter.renderToTextureWithBorder(ctx, matrix, texture, texWidth, texHeight, filterScale) { newtex, newmatrix ->
            renderIndex(ctx, newmatrix, newtex, newtex.width, newtex.height, renderColorMul, blendMode, filterScale, level - 1)
        }
    }
    */

    protected open fun stepBefore() {
    }
}
