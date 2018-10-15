package com.soywiz.korge.intellij

import com.intellij.ide.FileIconProvider
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import javax.swing.Icon

class KorgeFileIconProvider : FileIconProvider {
	override fun getIcon(file: VirtualFile, p1: Int, p2: Project?): Icon? {
		return when {
			file.name.endsWith(".pex", ignoreCase = true) -> KorgeIcons.PARTICLE
			file.name.endsWith(".fnt", ignoreCase = true) -> KorgeIcons.BITMAP_FONT
			file.name.endsWith(".swf", ignoreCase = true) -> KorgeIcons.SWF
			file.name.endsWith(".tmx", ignoreCase = true) -> KorgeIcons.TILED
			file.name.endsWith(".scml", ignoreCase = true) -> KorgeIcons.SPRITER
			file.name.endsWith(".ani", ignoreCase = true) -> KorgeIcons.KORGE
			file.name.endsWith(".voice.lipsync", ignoreCase = true) -> KorgeIcons.VOICE
			file.name.endsWith(".voice.wav", ignoreCase = true) -> KorgeIcons.VOICE
			file.name.endsWith(".voice.mp3", ignoreCase = true) -> KorgeIcons.VOICE
			file.name.endsWith(".voice.ogg", ignoreCase = true) -> KorgeIcons.VOICE

			file.name.endsWith(".wav", ignoreCase = true) -> KorgeIcons.SOUND
			file.name.endsWith(".mp3", ignoreCase = true) -> KorgeIcons.SOUND
			file.name.endsWith(".ogg", ignoreCase = true) -> KorgeIcons.SOUND

			file.name.endsWith(".atlas", ignoreCase = true) -> KorgeIcons.ATLAS
			else -> null
		}
	}
}
