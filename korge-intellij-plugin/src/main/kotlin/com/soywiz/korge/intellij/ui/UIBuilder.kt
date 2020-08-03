package com.soywiz.korge.intellij.ui

import com.intellij.openapi.ui.*
import com.soywiz.korge.awt.*
import java.awt.*
import javax.imageio.*
import javax.swing.*

fun icon(path: String) = ImageIcon(ImageIO.read(UIBuilderSample::class.java.getResource(path)))
fun toolbarIcon(path: String) = icon("/com/soywiz/korge/intellij/toolbar/$path")

fun showDialog(title: String = "Dialog", settings: DialogSettings = DialogSettings(), block: Styled<JPanel>.(wrapper: DialogWrapper) -> Unit): Boolean {
	class MyDialogWrapper : DialogWrapper(true) {
		override fun createCenterPanel(): JComponent? {
			val dialogPanel = JPanel(FillLayout())
			dialogPanel.preferredSize = Dimension(200, 200)
			block(dialogPanel.styled, this)
			return dialogPanel
		}

        override fun createActions(): Array<Action> {
            if (settings.onlyCancelButton) {
                return arrayOf(cancelAction)
            } else {
                return super.createActions()
            }
        }

        init {
			init()
            this.title = title
            this.isOK
		}
	}

	return MyDialogWrapper().showAndGet()
}
