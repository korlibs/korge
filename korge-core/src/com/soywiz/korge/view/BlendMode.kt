package com.soywiz.korge.view

import com.soywiz.korag.AG

enum class BlendMode(val factors: AG.BlendFactors) {
	INHERIT(AG.BlendFactors.NORMAL),
	NORMAL(AG.BlendFactors.NORMAL),
	ADD(AG.BlendFactors.ADD)
	;
}
