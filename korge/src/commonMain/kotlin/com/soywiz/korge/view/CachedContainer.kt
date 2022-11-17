package com.soywiz.korge.view

import com.soywiz.korge.baseview.*
import com.soywiz.korge.render.*
import com.soywiz.korge.view.property.*
import com.soywiz.korio.lang.*
import com.soywiz.korma.geom.*

inline fun Container.cachedContainer(cache: Boolean = true, callback: @ViewDslMarker CachedContainer.() -> Unit = {}) =
    CachedContainer(cache).addTo(this, callback)

open class FixedSizeCachedContainer(
    override var width: Double = 100.0,
    override var height: Double = 100.0,
    cache: Boolean = true
) : CachedContainer(cache), View.Reference {
    override fun getLocalBoundsInternal(out: Rectangle) {
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

    private var _cacheTex: CacheTexture? = null
    private val tempMat2d = Matrix()
    private var dirty = true
    private var lbounds = Rectangle()

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

        if (dirty) {
            lbounds.copyFrom(getLocalBoundsOptimizedAnchored(includeFilters = false))
            dirty = false
            val texWidth = (lbounds.width).toInt().coerceAtLeast(1)
            val texHeight = (lbounds.height).toInt().coerceAtLeast(1)
            cache.resize(texWidth, texHeight)
            ctx.renderToFrameBuffer(cache.rb) {
                //ctx.ag.clear(Colors.TRANSPARENT, clearColor = true)
                ctx.setViewMatrixTemp(tempMat2d.also {
                    it.copyFrom(globalMatrixInv)
                    it.translate(-lbounds.x, -lbounds.y)
                }) {
                    super.renderInternal(ctx)
                }
            }
        }

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
