package com.soywiz.korge.animate.serialization

object AniFile {
	const val MAGIC = "KORGEANI"
	const val VERSION = 16

	const val SYMBOL_TYPE_EMPTY = 0
	const val SYMBOL_TYPE_SOUND = 1
	const val SYMBOL_TYPE_TEXT = 2
	const val SYMBOL_TYPE_SHAPE = 3
	const val SYMBOL_TYPE_BITMAP = 4
	const val SYMBOL_TYPE_MOVIE_CLIP = 5
	const val SYMBOL_TYPE_MORPH_SHAPE = 6
}
