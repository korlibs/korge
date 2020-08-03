package com.soywiz.korge.intellij.ui

import com.soywiz.korge.awt.*
import com.soywiz.korge.intellij.editor.tiled.createTileMapEditor
import java.awt.Dimension
import javax.swing.JFrame
import javax.swing.UIManager
import javax.swing.WindowConstants

object UIBuilderSample {
	@JvmStatic
	fun main(args: Array<String>) {
		UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
		val frame = JFrame("UIBuilderSample")
		frame.preferredSize = Dimension(1280, 720)
		frame.defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
		frame.contentPane.layout = FillLayout()
		frame.contentPane.styled.createTileMapEditor()

		frame.pack()
		frame.setLocationRelativeTo(null)
		frame.isVisible = true
	}
}
