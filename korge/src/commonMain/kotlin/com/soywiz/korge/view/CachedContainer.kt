package com.soywiz.korge.view

import com.soywiz.kmem.*
import com.soywiz.korge.baseview.*
import com.soywiz.korge.render.*
import com.soywiz.korge.view.filter.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korio.lang.*
import com.soywiz.korma.geom.*

inline fun Container.cachedContainer(cache: Boolean = true, callback: @ViewDslMarker CachedContainer.() -> Unit = {}) =
    CachedContainer(cache).addTo(this, callback)

class CachedContainer(cache: Boolean = true) : Container(), InvalidateNotifier {
    inner class CacheTexture(val ctx: RenderContext) : Closeable {
        val rb = ctx.ag.unsafeAllocateFrameRenderBuffer(16, 16, onlyThisFrame = false)
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
            ctx.ag.unsafeFreeFrameRenderBuffer(rb)
        }
    }

    var cache: Boolean = cache
    private var _cacheTex: CacheTexture? = null
    private val tempMat2d = Matrix()
    private var dirty = true
    private var lbounds = Rectangle()

    override fun invalidateRender() {
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

        // @TODO: Do this only when required so it is cached
        //dirty = true
        if (dirty) {
            lbounds.copyFrom(getLocalBoundsOptimizedAnchored(includeFilters = false))
            dirty = false
            val texWidth = (lbounds.width).toInt().coerceAtLeast(1)
            val texHeight = (lbounds.height).toInt().coerceAtLeast(1)
            //println("texWidth=$texWidth, texHeight=$texHeight")
            cache.resize(texWidth, texHeight)
            ctx.renderToFrameBuffer(cache.rb) {
                @Suppress("DEPRECATION")
                ctx.batch.setViewMatrixTemp(tempMat2d.also {
                    it.copyFrom(globalMatrixInv)
                    it.translate(-lbounds.x, -lbounds.y)
                }) {
                    renderChildrenInternal(ctx)
                }
            }
        }

        //println("cache.tex=${cache.tex}")

        ctx.flush()
        ctx.useBatcher { batch ->
            batch.drawQuad(
                cache.tex,
                m = tempMat2d.also {
                    it.copyFrom(globalMatrix)
                    it.pretranslate(lbounds.x, lbounds.y)
                },
                colorAdd = renderColorAdd,
                colorMul = renderColorMul,
                blendMode = blendMode,
            )
        }
        ctx.flush()
    }

    override fun setInvalidateNotifier() {
        _invalidateNotifier = this
    }

    init {
        _invalidateNotifier = this
    }

    override fun invalidatedView(view: BaseView?) {
        dirty = true
        parent?._invalidateNotifier?.invalidatedView(view)
    }
}
