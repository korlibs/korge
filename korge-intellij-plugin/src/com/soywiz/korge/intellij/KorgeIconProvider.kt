package com.soywiz.korge.intellij

import com.intellij.ide.IconProvider
import com.intellij.openapi.util.IconLoader
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import javax.swing.Icon

/*
class KorgeIconProvider : IconProvider() {
	override fun getIcon(psi: PsiElement, i: Int): Icon? {
		if (psi is PsiFile) {
			when {
				psi.name.endsWith(".pex", ignoreCase = true) -> return KorgePluginIcons.PARTICLE_ICON
				psi.name.endsWith(".fnt", ignoreCase = true) -> return KorgePluginIcons.BITMAP_FONT_ICON
				psi.name.endsWith(".swf", ignoreCase = true) -> return KorgePluginIcons.SWF_ICON
				psi.name.endsWith(".tmx", ignoreCase = true) -> return KorgePluginIcons.TILED_ICON
				psi.name.endsWith(".ani", ignoreCase = true) -> return KorgePluginIcons.KORGE_ICON
			}
		}
		return null
	}
}
*/
