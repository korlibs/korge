package com.soywiz.korge.view

import com.soywiz.korge.render.*
import com.soywiz.korge.view.property.*
import com.soywiz.korgw.*
import com.soywiz.korio.lang.*
import com.soywiz.korma.geom.*

inline fun Container.cachedContainer(cache: Boolean = true, callback: @ViewDslMarker CachedContainer.() -> Unit = {}) =
    CachedContainer(cache).addTo(this, callback)

open class FixedSizeCachedContainer(
    override var width: Double = 100.0,
    override var height: Double = 100.0,
    cache: Boolean = true
) : CachedContainer(cache), View.Reference {
    override fun getLocalBoundsInternal(out: MRectangle) {
        out.setTo(0.0, 0.0, width, height)
    }
}

open class CachedContainer(
    @property:ViewProperty
    var cache: Boolean = true
) : Container(), InvalidateNotifier {
    //@ViewProperty
    //var cache: Boolean = cache

    inner class CacheTexture(val ctx: RenderContext) : Closeable {
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
    private val tempMat2d = MMatrix()
    private var dirty = true
    private var scaledCache = -1.0
    private var lbounds = MRectangle()

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

        val renderScale = when (ctx.views?.gameWindow?.quality) {
            GameWindow.Quality.PERFORMANCE -> 1.0
            else -> ctx.devicePixelRatio
        }
        //val renderScale = 1.0

        if (dirty || scaledCache != renderScale) {
            scaledCache = renderScale
            lbounds.copyFrom(getLocalBoundsOptimizedAnchored(includeFilters = false))
            dirty = false
            val texWidth = (lbounds.width * renderScale).toInt().coerceAtLeast(1)
            val texHeight = (lbounds.height * renderScale).toInt().coerceAtLeast(1)
            cache.resize(texWidth, texHeight)
            ctx.renderToFrameBuffer(cache.rb) {
                //ctx.ag.clear(Colors.TRANSPARENT, clearColor = true)
                ctx.setViewMatrixTemp(tempMat2d.also {
                    it.copyFrom(globalMatrixInv)
                    it.translate(-lbounds.x, -lbounds.y)
                    it.scale(renderScale)
                }.immutable) {
                    super.renderInternal(ctx)
                }
            }
        }

        ctx.useBatcher { batch ->
            batch.drawQuad(
                cache.tex,
                m = globalMatrix
                    .pretranslated(Point(lbounds.x, lbounds.y))
                    .prescaled(Scale(1.0 / renderScale))
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
