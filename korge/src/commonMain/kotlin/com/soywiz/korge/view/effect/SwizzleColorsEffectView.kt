package com.soywiz.korge.view.effect

import com.soywiz.korag.shader.*
import com.soywiz.korge.internal.*
import com.soywiz.korge.view.*

@KorgeDeprecated
@Deprecated("Use View.filter instead")
class SwizzleColorsEffectView(initialSwizzle: String = "rgba") : EffectView() {
	var swizzle: String = ""
		set(value) {
			field = value
			fragment = fragment.appending {
				out setTo out[value]
			}
		}

	init {
		swizzle = initialSwizzle
	}
}

@KorgeDeprecated
@Deprecated("Use View.filter instead")
inline fun Container.swizzleColorsEffectView(swizzle: String = "rgba", callback: SwizzleColorsEffectView.() -> Unit = {}) =
	SwizzleColorsEffectView(swizzle).addTo(this, callback)
