package com.soywiz.kgl

import com.soywiz.kmem.*
import com.soywiz.kgl.internal.*

class KmlGlCached(parent: KmlGl) : KmlGlFastProxy(parent) {
	private var lastLineWidth = CachedFloat(-1f)

	override fun lineWidth(width: Float) {
		lastLineWidth(width) {
			super.lineWidth(width)
		}
	}

	private var lastActiveTexture = CachedInt(-1)
	override fun activeTexture(texture: Int) {
		lastActiveTexture(texture) {
			super.activeTexture(texture)
		}
	}

	private var lastColorMask = CachedInt(-1)
	override fun colorMask(red: Boolean, green: Boolean, blue: Boolean, alpha: Boolean) {
		lastColorMask((red.toInt() shl 0) or (green.toInt() shl 1) or (blue.toInt() shl 2) or (alpha.toInt() shl 3)) {
			super.colorMask(red, green, blue, alpha)
		}
	}

	private var lastDepthMask = CachedInt(-1)
	override fun depthMask(flag: Boolean) {
		lastDepthMask(flag.toInt()) {
			super.depthMask(flag)
		}
	}

	private val lastDepth = CachedFloat2(-1f, -1f)
	override fun depthRangef(n: Float, f: Float) {
		lastDepth(n, f) {
			super.depthRangef(n, f)
		}
	}

	private val lastEquation = CachedInt(-1)
	override fun blendEquation(mode: Int) {
		lastEquation(mode) {
			super.blendEquation(mode)
		}
	}

	private val lastEquationSeparate = CachedInt2(-1, -1)
	override fun blendEquationSeparate(modeRGB: Int, modeAlpha: Int) {
		lastEquationSeparate(modeRGB, modeAlpha) {
			super.blendEquationSeparate(modeRGB, modeAlpha)
		}
	}

	private val lastBlendFunc = CachedInt2(-1, -1)
	override fun blendFunc(sfactor: Int, dfactor: Int) {
		lastBlendFunc(sfactor, dfactor) {
			super.blendFunc(sfactor, dfactor)
		}
	}

	private val lastBlendFuncSeparate = CachedInt4(-1, -1, -1, -1)
	override fun blendFuncSeparate(sfactorRGB: Int, dfactorRGB: Int, sfactorAlpha: Int, dfactorAlpha: Int) {
		lastBlendFuncSeparate(sfactorRGB, dfactorRGB, sfactorAlpha, dfactorAlpha) {
			super.blendFuncSeparate(sfactorRGB, dfactorRGB, sfactorAlpha, dfactorAlpha)
		}
	}

	private val lastStencilMask = CachedInt(-1)
	override fun stencilMask(mask: Int) {
		lastStencilMask(mask) {
			super.stencilMask(mask)
		}
	}

	private val lastFrontFace = CachedInt(-1)
	override fun frontFace(mode: Int) {
		lastFrontFace(mode) {
			super.frontFace(mode)
		}
	}

	private val enables = Array<Boolean?>(1024) { null }
	private fun enableDisablePriv(cap: Int, enable: Boolean) {
		val index = cap - BLEND
		if (index !in enables.indices) return
		if (enables[index] != enable) {
			enables[index] = enable
			if (enable) {
				parent.enable(cap)
			} else {
				parent.disable(cap)
			}
		}
	}

	override fun enable(cap: Int) = enableDisablePriv(cap, true)
	override fun disable(cap: Int) = enableDisablePriv(cap, false)

	private val lastScissor = CachedInt4(-1, -1, -1, -1)
	override fun scissor(x: Int, y: Int, width: Int, height: Int) {
		lastScissor(x, y, width, height) {
			super.scissor(x, y, width, height)
		}
	}

	private val lastUseProgram = CachedInt(-1)
	override fun useProgram(program: Int) {
		lastUseProgram(program) {
			super.useProgram(program)
		}
	}

    var _isFloatTextureSupported: Boolean? = null

    override val isFloatTextureSupported: Boolean get() {
        if (_isFloatTextureSupported == null) {
            _isFloatTextureSupported = super.isFloatTextureSupported
        }
        return _isFloatTextureSupported!!
    }
}
