package com.soywiz.korge.view.filter

import com.soywiz.kds.*
import com.soywiz.korag.shader.*
import com.soywiz.korge.view.property.*

/**
 * Allows to swizzle (interchange) color components via the [swizzle] property.
 *
 * - swizzle="rgba" would be the identity
 * - swizzle="bgra" would interchange red and blue channels
 * - swizzle="rrra" would show as greyscale the red component
 */
class SwizzleColorsFilter(initialSwizzle: String = "rgba") : ShaderFilter() {
    companion object {
        class SwizzleProgram(val swizzle: String) : BaseProgramProvider() {
            override val fragment = Filter.DEFAULT_FRAGMENT.appending {
                SET(out, out[swizzle])
                //BatchBuilder2D.DO_INPUT_OUTPUT(this, out)
            }
        }

        private val CACHE = CopyOnWriteFrozenMap<String, SwizzleProgram>()
    }

    var _programProvider: ProgramProvider? = null

    /**
     * The swizzling string
     *
     * - swizzle="rgba" would be the identity
     * - swizzle="bgra" would interchange red and blue channels
     * - swizzle="rrra" would show as greyscale the red component
     * */
    @ViewProperty
    var swizzle: String = initialSwizzle
        set(value) {
            field = value
            _programProvider = null
        }

    override val programProvider: ProgramProvider get() {
        if (_programProvider == null) {
            _programProvider = CACHE.getOrPut(swizzle) { SwizzleProgram(swizzle) }
        }
        return _programProvider!!
    }
}
