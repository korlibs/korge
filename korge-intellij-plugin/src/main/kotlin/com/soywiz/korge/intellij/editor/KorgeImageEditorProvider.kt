package com.soywiz.korge.intellij.editor

import com.intellij.diff.util.*
import com.intellij.openapi.fileEditor.*
import com.intellij.openapi.project.*
import com.intellij.openapi.vfs.*
import com.soywiz.korge.intellij.imageformat.KRA
import com.soywiz.korim.awt.*
import com.soywiz.korim.format.*
import com.soywiz.korio.file.std.*
import kotlinx.coroutines.*
import java.io.*
import javax.swing.*

class KorgeImageEditorProvider : FileEditorProvider, DumbAware {
	companion object {
		val FORMATS = ImageFormats(PSD, KRA, DDS, DXT1, DXT2, DXT3, DXT4, DXT5)
	}

	override fun accept(
		project: Project,
		virtualFile: VirtualFile
	): Boolean {
		val name = virtualFile.name
		return when {
			name.endsWith(".kra", ignoreCase = true) -> true
			name.endsWith(".psd", ignoreCase = true) -> true
			name.endsWith(".dds", ignoreCase = true) -> true
			name.endsWith(".dxt1", ignoreCase = true) -> true
			name.endsWith(".dxt2", ignoreCase = true) -> true
			name.endsWith(".dxt3", ignoreCase = true) -> true
			name.endsWith(".dxt4", ignoreCase = true) -> true
			name.endsWith(".dxt5", ignoreCase = true) -> true
			else -> false
		}
	}

	override fun getEditorTypeId(): String = "KORGE_IMAGE_TYPE_ID"

	override fun createEditor(project: Project, file: VirtualFile): FileEditor {
		return object : FileEditorBase() {
			override fun getComponent(): JComponent {
				val image = JLabel()
				runBlocking {
					val bmp = File(file.canonicalPath!!).toVfs().readImageData(FORMATS).mainBitmap
					image.icon = ImageIcon(bmp.toAwt())
				}
				return image
			}

			override fun getName(): String {
				return "KorgeImageEditor"
			}

			override fun getPreferredFocusedComponent(): JComponent? {
				return null
			}
		}
	}

	override fun getPolicy(): FileEditorPolicy = FileEditorPolicy.PLACE_BEFORE_DEFAULT_EDITOR
}
