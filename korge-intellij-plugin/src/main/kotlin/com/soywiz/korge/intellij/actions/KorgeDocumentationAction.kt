package com.soywiz.korge.intellij.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import java.awt.Desktop
import java.net.URI

class KorgeDocumentationAction : AnAction() {
	override fun actionPerformed(p0: AnActionEvent) {
		Desktop.getDesktop().browse(URI.create("https://korlibs.soywiz.com/korge/"))
	}
}
