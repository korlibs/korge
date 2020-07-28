package com.soywiz.korge.intellij.debug.actions

import com.intellij.debugger.actions.*
import com.intellij.debugger.engine.*
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.project.*
import com.intellij.openapi.ui.*
import com.intellij.xdebugger.impl.ui.tree.actions.*
import com.intellij.xdebugger.impl.ui.tree.nodes.*
import com.soywiz.korge.intellij.debug.*
import com.soywiz.korim.awt.*
import com.soywiz.korim.bitmap.*
import com.sun.jdi.*
import java.awt.*
import javax.swing.*

class ShowKorimBitmapAction : XDebuggerTreeActionBase(), DumbAware {
	class BitmapDialog(val project: Project, val bitmap: Bitmap32) : DialogWrapper(project) {
		init {
			init()
			title = "KorIM Bitmap"
		}

		override fun createCenterPanel(): JComponent {
			val dialogPanel = JPanel(BorderLayout())
			val label = JLabel(ImageIcon(bitmap.clone().toAwt()))
			label.preferredSize = Dimension(bitmap.width, bitmap.height)
			dialogPanel.add(label, BorderLayout.CENTER)
			return dialogPanel
		}
	}

	override fun perform(node: XValueNodeImpl, nodeName: String, e: AnActionEvent?) {
		val tree = node.tree
		val javaValue = (node.valueContainer as JavaValue)
		val descriptor = javaValue.descriptor
		val value = descriptor.value
		val type = value.type()
		if (type.isKorimBitmapOrDrawable()) {
			val thread = javaValue.evaluationContext.suspendContext.thread?.threadReference
			val value = descriptor.value as ObjectReference
			val bmp32 = value.readKorimBitmap32(512, 512, thread)

			val dialog = BitmapDialog(tree.project, bmp32)
			dialog.show()

			/*
			val frame = JFrame()
			frame.add()
			frame.pack()
			val dim: Dimension = Toolkit.getDefaultToolkit().screenSize
			frame.setLocation(dim.width / 2 - frame.size.width / 2, dim.height / 2 - frame.size.height / 2)
			frame.isVisible = true
			*/
		}
	}

	override fun update(e: AnActionEvent) {
		val values = ViewAsGroup.getSelectedValues(e)
		e.presentation.isEnabledAndVisible = when {
			values.size >= 1 -> {
				val javaValue = values[0]
				val value = javaValue.descriptor.value
				value.type().isKorimBitmapOrDrawable()
			}
			else -> {
				false
			}
		}
	}

	/*
	override fun actionPerformed(e: AnActionEvent) {
		val session = e.dataContext.getData(XDebugSession.DATA_KEY) ?: error("Can't find debug session")
		val debuggerTree = e.dataContext.getData(XDebuggerTree.XDEBUGGER_TREE_KEY) ?: error("Can't find debugger tree")
		val selectionPath = debuggerTree.selectionPath
		println("ACTION!")
	}
	 */
}