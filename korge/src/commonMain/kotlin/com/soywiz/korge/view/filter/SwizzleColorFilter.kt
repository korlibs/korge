package com.soywiz.korge.view.filter

import com.soywiz.korag.shader.*

class SwizzleColorsFilter(initialSwizzle: String = "rgba") : Filter() {
	var swizzle: String = initialSwizzle
		set(value) {
			field = value
			fragment = fragment.appending {
				out setTo out[value]
			}
		}
}
