package com.soywiz.korge.view.filter

import com.soywiz.korag.shader.*

// Can't be an object since it contains a Matrix and would mutate on native
val IdentityFilter = object : Filter() {
	init {
		fragment = FragmentShader {
			apply {
				out setTo tex(fragmentCoords)
			}
		}
	}
}

val DummyFilter get() = IdentityFilter
