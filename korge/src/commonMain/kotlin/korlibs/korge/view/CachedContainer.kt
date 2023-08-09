@file:OptIn(ExperimentalStdlibApi::class)

package korlibs.korge.view

import korlibs.datastructure.*
import korlibs.image.*
import korlibs.korge.internal.*
import korlibs.korge.render.*
import korlibs.korge.view.property.*
import korlibs.math.geom.*
import korlibs.render.*

inline fun Container.fixedSizeCachedContainer(size: Size, cache: Boolean = true, clip: Boolean = true, callback: @ViewDslMarker CachedContainer.() -> Unit = {}) =
    FixedSizeCachedContainer(size, cache, clip).addTo(this, callback)
inline fun Container.fixedSizeCachedContainer(width: Double, height: Double, cache: Boolean = true, clip: Boolean = true, callback: @ViewDslMarker CachedContainer.() -> Unit = {}) =
    FixedSizeCachedContainer(Size(width, height), cache, clip).addTo(this, callback)

inline fun Container.cachedContainer(cache: Boolean = true, callback: @ViewDslMarker CachedContainer.() -> Unit = {}) =
    CachedContainer(cache).addTo(this, callback)

open class FixedSizeCachedContainer(
    size: Size = Size(100, 100),
    cache: Boolean = true,
    @property:ViewProperty
    var clip: Boolean = true,
) : CachedContainer(cache), View.Reference {
    override var unscaledSize: Size = size

    override fun getLocalBoundsInternal(): Rectangle = Rectangle(0f, 0f, width, height)

    private var renderingInternalRef = Ref(false)

    @OptIn(KorgeInternal::class)
    override fun renderInternal(ctx: RenderContext) {
        FixedSizeContainer.renderClipped(this, ctx, clip, renderingInternalRef) { super.renderInternal(ctx) }
    }
}

open class CachedContainer(
    /** Indicates if we are going to render this container into a texture, and reuse its content in following frames. */
    @property:ViewProperty
    var cache: Boolean = true,
    /** Affects the size of the cached texture (bigger textures on hidpi screens for high quality and smaller for low quality), when null, uses the configured [GameWindow.quality]. */
    renderQuality: Quality? = null,
) : Container(), InvalidateNotifier {
    //@ViewProperty
    //var cache: Boolean = cache

    @property:ViewProperty
    @property:ViewPropertyProvider(QualityProvider::class)
    var renderQuality: Quality? = renderQuality
        set(value) {
            if (field != value) {
                field = value
                dirty = true
            }
        }

    inner class CacheTexture(val ctx: RenderContext) : AutoCloseable {
        val rb = ctx.unsafeAllocateFrameBuffer(16, 16, onlyThisFrame = false)
        val texBase = TextureBase(rb.tex, 16, 16)
        var tex = Texture(texBase)

        fun resize(width: Int, height: Int) {
            rb.setSize(0, 0, width, height)
            texBase.width = width
            texBase.height = height
            tex = Texture(texBase)
        }
        override fun close() {
            _cacheTex = null
            ctx.unsafeFreeFrameBuffer(rb)
        }
    }

    private var _cacheTex: CacheTexture? = null
    private var dirty = true
    private var scaledCache = -1f
    private var lbounds = Rectangle()
    private var windowLocalRatio: Scale = Scale(1)

    override fun invalidateRender() {
        super.invalidateRender()
        dirty = true
    }

    override fun renderInternal(ctx: RenderContext) {
        if (!visible) return
        if (!cache) {
            return super.renderInternal(ctx)
        }

        if (_cacheTex == null) {
            _cacheTex = CacheTexture(ctx)
            dirty = true
        }
        val cache = _cacheTex!!
        ctx.refGcCloseable(cache)

        val renderScale: Float = when (ctx.quality) {
            GameWindow.Quality.PERFORMANCE -> 1f
            else -> ctx.devicePixelRatio
        }

        val doExpensiveScaling = !(renderQuality ?: ctx.quality).isLow
        //val renderScale = 1.0

        if (dirty || scaledCache != renderScale) {
            scaledCache = renderScale
            lbounds = getLocalBounds(includeFilters = false)
            windowLocalRatio = when {
                doExpensiveScaling -> (windowBounds.size / lbounds.size)
                else -> Scale(1)
            }

            dirty = false
            val texWidth = (lbounds.width * renderScale * windowLocalRatio.scaleX).toInt().coerceAtLeast(1)
            val texHeight = (lbounds.height * renderScale * windowLocalRatio.scaleY).toInt().coerceAtLeast(1)
            cache.resize(texWidth, texHeight)
            ctx.flush()
            ctx.renderToFrameBuffer(cache.rb) {
                //ctx.ag.clear(Colors.TRANSPARENT, clearColor = true)
                ctx.setViewMatrixTemp(globalMatrixInv
                    .translated(-lbounds.x, -lbounds.y)
                    .scaled(renderScale * windowLocalRatio.scaleX, renderScale * windowLocalRatio.scaleY)
                ) {
                    super.renderInternal(ctx)
                }
            }
        }

        ctx.useBatcher { batch ->
            batch.drawQuad(
                cache.tex,
                m = globalMatrix
                    .pretranslated(lbounds.x, lbounds.y)
                    .prescaled(1.0 / (renderScale * windowLocalRatio.scaleX), 1.0 / (renderScale * windowLocalRatio.scaleY))
                ,
                colorMul = renderColorMul,
                blendMode = blendMode,
            )
        }
    }

    override val _invalidateNotifierForChildren: InvalidateNotifier get() = this

    override fun invalidatedView(view: BaseView?) {
        dirty = true
        parent?._invalidateNotifier?.invalidatedView(view)
    }
}
