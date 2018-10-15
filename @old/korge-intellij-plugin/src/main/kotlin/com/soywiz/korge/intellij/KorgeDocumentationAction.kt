package com.soywiz.korge.intellij

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import java.awt.Desktop
import java.net.URI

class KorgeDocumentationAction : AnAction() {
	override fun actionPerformed(p0: AnActionEvent?) {
		Desktop.getDesktop().browse(URI.create("http://docs.korge.soywiz.com/"))
	}
}
