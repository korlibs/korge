package com.soywiz.korge.intellij

import com.intellij.ide.FileIconProvider
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import javax.swing.Icon

class KorgeFileIconProvider : FileIconProvider {
	override fun getIcon(file: VirtualFile, p1: Int, p2: Project?): Icon? {
		return when {
			file.name.endsWith(".pex", ignoreCase = true) -> KorgePluginIcons.PARTICLE_ICON
			file.name.endsWith(".fnt", ignoreCase = true) -> KorgePluginIcons.BITMAP_FONT_ICON
			file.name.endsWith(".swf", ignoreCase = true) -> KorgePluginIcons.SWF_ICON
			file.name.endsWith(".tmx", ignoreCase = true) -> KorgePluginIcons.TILED_ICON
			file.name.endsWith(".ani", ignoreCase = true) -> KorgePluginIcons.KORGE_ICON
			file.name.endsWith(".voice.lipsync", ignoreCase = true) -> KorgePluginIcons.VOICE
			file.name.endsWith(".voice.wav", ignoreCase = true) -> KorgePluginIcons.VOICE
			file.name.endsWith(".voice.mp3", ignoreCase = true) -> KorgePluginIcons.VOICE
			file.name.endsWith(".voice.ogg", ignoreCase = true) -> KorgePluginIcons.VOICE
			else -> null
		}
	}
}
