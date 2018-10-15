package com.soywiz.korge.view.effect

import com.soywiz.korag.shader.*
import com.soywiz.korge.view.*

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

inline fun Container.swizzleColorsEffectView(swizzle: String = "rgba", callback: @ViewsDslMarker SwizzleColorsEffectView.() -> Unit = {}) =
	SwizzleColorsEffectView(swizzle).addTo(this).apply(callback)
