package com.soywiz.korui.light


import java.awt.*
import java.awt.datatransfer.*
import java.awt.event.*
import java.io.*
import java.net.*
import javax.swing.*
import javax.swing.event.*

/**
 * Demonstration of the top-level `TransferHandler`
 * support on `JFrame`.
 *
 * @author Shannon Hickey
 */
class TopLevelTransferHandlerDemo : JFrame("TopLevelTransferHandlerDemo") {

	private val dp = JDesktopPane()
	private val listModel = DefaultListModel<Any>()
	private val list = JList(listModel)
	private var copyItem: JCheckBoxMenuItem? = null
	private var nullItem: JCheckBoxMenuItem? = null
	private var thItem: JCheckBoxMenuItem? = null

	private val handler = object : TransferHandler() {
		override fun canImport(support: TransferHandler.TransferSupport): Boolean {
			if (!support.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
				return false
			}

			if (copyItem!!.isSelected) {
				val copySupported = TransferHandler.COPY and support.sourceDropActions == TransferHandler.COPY

				if (!copySupported) {
					return false
				}

				support.dropAction = TransferHandler.COPY
			}

			return true
		}

		override fun importData(support: TransferHandler.TransferSupport): Boolean {
			if (!canImport(support)) {
				return false
			}

			val t = support.transferable

			try {
				val l = t.getTransferData(DataFlavor.javaFileListFlavor) as List<File>

				for (f in l) {
					Doc(f)
				}
			} catch (e: UnsupportedFlavorException) {
				return false
			} catch (e: IOException) {
				return false
			}

			return true
		}
	}

	private inner class Doc : InternalFrameAdapter, ActionListener {
		internal var name: String
		internal lateinit var frame: JInternalFrame
		internal lateinit var th: TransferHandler
		internal lateinit var area: JTextArea

		constructor(file: File) {
			this.name = file.name
			try {
				init(file.toURI().toURL())
			} catch (e: MalformedURLException) {
				e.printStackTrace()
			}

		}

		constructor(name: String) {
			this.name = name
			init(javaClass.getResource(name))
		}

		private fun init(url: URL) {
			frame = JInternalFrame(name)
			frame.addInternalFrameListener(this)
			listModel.add(listModel.size(), this)

			area = JTextArea()
			area.margin = Insets(5, 5, 5, 5)

			try {
				val reader = BufferedReader(InputStreamReader(url.openStream()))
				var `in`: String?
				while (true) {
					`in` = reader.readLine()
					if (`in` == null) break
					area.append(`in`)
					area.append("\n")
				}
				reader.close()
			} catch (e: Exception) {
				e.printStackTrace()
				return
			}

			th = area.transferHandler
			area.font = Font("monospaced", Font.PLAIN, 12)
			area.caretPosition = 0
			area.dragEnabled = true
			area.dropMode = DropMode.INSERT
			frame.contentPane.add(JScrollPane(area))
			dp.add(frame)
			frame.show()
			if (DEMO) {
				frame.setSize(300, 200)
			} else {
				frame.setSize(400, 300)
			}
			frame.isResizable = true
			frame.isClosable = true
			frame.isIconifiable = true
			frame.isMaximizable = true
			frame.setLocation(left, top)
			incr()
			SwingUtilities.invokeLater { select() }
			nullItem!!.addActionListener(this)
			setNullTH()
		}

		override fun internalFrameClosing(event: InternalFrameEvent?) {
			listModel.removeElement(this)
			nullItem!!.removeActionListener(this)
		}

		override fun internalFrameOpened(event: InternalFrameEvent?) {
			val index = listModel.indexOf(this)
			list.getSelectionModel().setSelectionInterval(index, index)
		}

		override fun internalFrameActivated(event: InternalFrameEvent?) {
			val index = listModel.indexOf(this)
			list.getSelectionModel().setSelectionInterval(index, index)
		}

		override fun toString(): String {
			return name
		}

		fun select() {
			try {
				frame.toFront()
				frame.isSelected = true
			} catch (e: java.beans.PropertyVetoException) {
			}

		}

		override fun actionPerformed(ae: ActionEvent) {
			setNullTH()
		}

		fun setNullTH() {
			if (nullItem!!.isSelected) {
				area.transferHandler = null
			} else {
				area.transferHandler = th
			}
		}
	}

	init {
		jMenuBar = createDummyMenuBar()
		contentPane.add(createDummyToolBar(), BorderLayout.NORTH)

		val sp = JSplitPane(JSplitPane.HORIZONTAL_SPLIT, list, dp)
		sp.dividerLocation = 120
		contentPane.add(sp)
		//new Doc("sample.txt");
		//new Doc("sample.txt");
		//new Doc("sample.txt");

		list.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION)

		list.addListSelectionListener(ListSelectionListener { e ->
			if (e.valueIsAdjusting) {
				return@ListSelectionListener
			}

			val `val` = list.getSelectedValue() as Doc
			`val`?.select()
		})

		val th = list.getTransferHandler()

		nullItem!!.addActionListener {
			if (nullItem!!.isSelected) {
				list.setTransferHandler(null)
			} else {
				list.setTransferHandler(th)
			}
		}
		thItem!!.addActionListener {
			if (thItem!!.isSelected) {
				transferHandler = handler
			} else {
				transferHandler = null
			}
		}
		dp.transferHandler = handler
	}

	private fun createDummyToolBar(): JToolBar {
		val tb = JToolBar()
		var b: JButton
		b = JButton("New")
		b.isRequestFocusEnabled = false
		tb.add(b)
		b = JButton("Open")
		b.isRequestFocusEnabled = false
		tb.add(b)
		b = JButton("Save")
		b.isRequestFocusEnabled = false
		tb.add(b)
		b = JButton("Print")
		b.isRequestFocusEnabled = false
		tb.add(b)
		b = JButton("Preview")
		b.isRequestFocusEnabled = false
		tb.add(b)
		tb.isFloatable = false
		return tb
	}

	private fun createDummyMenuBar(): JMenuBar {
		val mb = JMenuBar()
		mb.add(createDummyMenu("File"))
		mb.add(createDummyMenu("Edit"))
		mb.add(createDummyMenu("Search"))
		mb.add(createDummyMenu("View"))
		mb.add(createDummyMenu("Tools"))
		mb.add(createDummyMenu("Help"))

		val demo = JMenu("Demo")
		demo.mnemonic = KeyEvent.VK_D
		mb.add(demo)

		thItem = JCheckBoxMenuItem("Use Top-Level TransferHandler")
		thItem!!.mnemonic = KeyEvent.VK_T
		demo.add(thItem)

		nullItem = JCheckBoxMenuItem("Remove TransferHandler from List and Text")
		nullItem!!.mnemonic = KeyEvent.VK_R
		demo.add(nullItem)

		copyItem = JCheckBoxMenuItem("Use COPY Action")
		copyItem!!.mnemonic = KeyEvent.VK_C
		demo.add(copyItem)

		return mb
	}

	private fun createDummyMenu(str: String): JMenu {
		val menu = JMenu(str)
		val item = JMenuItem("[Empty]")
		item.isEnabled = false
		menu.add(item)
		return menu
	}

	companion object {

		private val DEMO = false
		private var left: Int = 0
		private var top: Int = 0

		private fun incr() {
			left += 30
			top += 30
			if (top == 150) {
				top = 0
			}
		}

		private fun createAndShowGUI(args: Array<String>) {
			try {
				UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
			} catch (e: Exception) {
			}

			val test = TopLevelTransferHandlerDemo()
			test.defaultCloseOperation = JFrame.EXIT_ON_CLOSE
			if (DEMO) {
				test.setSize(493, 307)
			} else {
				test.setSize(800, 600)
			}
			test.setLocationRelativeTo(null)
			test.isVisible = true
			test.list.requestFocus()
		}

		@JvmStatic
		fun main(args: Array<String>) {
			SwingUtilities.invokeLater {
				//Turn off metal's use of bold fonts
				UIManager.put("swing.boldMetal", java.lang.Boolean.FALSE)
				createAndShowGUI(args)
			}
		}
	}
}