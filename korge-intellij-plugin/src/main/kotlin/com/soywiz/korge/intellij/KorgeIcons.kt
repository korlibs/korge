package com.soywiz.korge.intellij

import com.intellij.openapi.util.IconLoader
import javax.swing.*

object KorgeIcons {
    val KTREE by lazy { getIcon("/com/soywiz/korge/intellij/icon/korge.png") }
	val PARTICLE by lazy { getIcon("/com/soywiz/korge/intellij/icon/particle.png") }
	val BITMAP_FONT by lazy { getIcon("/com/soywiz/korge/intellij/icon/bitmap_font.png") }
	val KRITA by lazy { getIcon("/com/soywiz/korge/intellij/icon/krita.png") }
	val SWF by lazy { getIcon("/com/soywiz/korge/intellij/icon/swf.png") }
	val TILED by lazy { getIcon("/com/soywiz/korge/intellij/icon/tiled.png") }
	val KORGE by lazy { getIcon("/com/soywiz/korge/intellij/icon/korge.png") }
	val VOICE by lazy { getIcon("/com/soywiz/korge/intellij/icon/lips.png") }
	val SPRITER by lazy { getIcon("/com/soywiz/korge/intellij/icon/spriter.png") }
	val SOUND by lazy { getIcon("/com/soywiz/korge/intellij/icon/sound.png") }
	val ATLAS by lazy { getIcon("/com/soywiz/korge/intellij/icon/atlas.png") }
    val SPINE by lazy { getIcon("/com/soywiz/korge/intellij/icon/spine.png") }
    val DRAGONBONES by lazy { getIcon("/com/soywiz/korge/intellij/icon/dragonbones.png") }

    private fun getIcon(path: String): Icon = IconLoader.getIcon(path, KorgeIcons::class.java)
}
