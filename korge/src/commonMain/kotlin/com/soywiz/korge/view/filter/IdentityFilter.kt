package com.soywiz.korge.view.filter

import com.soywiz.korag.shader.*

object IdentityFilter : Filter() {
	init {
		fragment = FragmentShader {
			apply {
				out setTo tex(fragmentCoords)
			}
		}
	}
}

val DummyFilter get() = IdentityFilter
