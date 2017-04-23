package com.soywiz.korge.view

import com.soywiz.korag.AG
import com.soywiz.korge.render.MyBlendFactors

enum class BlendMode(val factors: AG.BlendFactors) {
	INHERIT(MyBlendFactors.NORMAL),
	NORMAL(MyBlendFactors.NORMAL),
	ADD(MyBlendFactors.ADD)
	;
}
