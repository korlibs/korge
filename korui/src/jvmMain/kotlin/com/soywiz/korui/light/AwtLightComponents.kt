package com.soywiz.korui.light

import com.soywiz.korag.*
import com.soywiz.korev.*
import com.soywiz.korev.ChangeEvent
import com.soywiz.korim.awt.*
import com.soywiz.korio.file.*
import com.soywiz.korio.file.std.*
import com.soywiz.korio.lang.*
import com.soywiz.korio.lang.Closeable
import com.soywiz.korev.Event
import java.awt.*
import java.awt.datatransfer.*
import java.awt.dnd.*
import java.awt.event.*
import java.awt.event.KeyEvent
import java.awt.event.MouseEvent
import java.awt.image.*
import java.io.*
import java.net.*
import java.util.concurrent.*
import javax.swing.*
import javax.swing.border.*
import javax.swing.event.*
import javax.swing.text.*
import kotlin.reflect.*

typealias KoruiMouseEvent = com.soywiz.korev.MouseEvent
typealias KoruiMouseEventType = com.soywiz.korev.MouseEvent.Type
typealias KoruiChangeEvent = ChangeEvent

@Suppress("EXPERIMENTAL_FEATURE_WARNING")
class AwtLightComponents : LightComponents() {
	init {
		if (UIManager.getLookAndFeel().name == UIManager.getCrossPlatformLookAndFeelClassName()) {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
		}
	}

	override fun create(type: LightType, config: Any?): LightComponentInfo {
		var agg: AG? = null
		@Suppress("REDUNDANT_ELSE_IN_WHEN")
		val handle: Component = when (type) {
			LightType.FRAME -> JFrame2().apply {
				defaultCloseOperation = JFrame.EXIT_ON_CLOSE
			}
			LightType.CONTAINER -> JPanel2().apply {
				layout = null
			}
			LightType.BUTTON -> JButton()
			LightType.IMAGE -> JImage()
			LightType.PROGRESS -> JProgressBar(0, 100)
			LightType.LABEL -> JLabel()
			LightType.TEXT_FIELD -> JTextField()
			LightType.TEXT_AREA -> JScrollableTextArea()
			LightType.CHECK_BOX -> JCheckBox()
			LightType.COMBO_BOX -> JComboBox<ComboBoxItem>()
			LightType.RADIO_BUTTON -> JRadioButton()
			LightType.SLIDER -> JSlider()
			LightType.SCROLL_PANE -> JScrollPane2()
			LightType.TABPANE -> JTabbedPane()
			LightType.TABPAGE -> JTabbedPage()
			LightType.AGCANVAS -> {
				agg = AGOpenglFactory.create(null).create(null, (config as? AGConfig) ?: AGConfig())
				agg.nativeComponent as Component
			}
			else -> throw UnsupportedOperationException("Type: $type")
		}
		return LightComponentInfo(handle).apply {
			if (agg != null) {
				this.ag = agg
			}
		}
	}

	override fun <T : Event> registerEventKind(c: Any, clazz: KClass<T>, ed: EventDispatcher): Closeable {
		when (clazz) {
			KoruiMouseEvent::class -> {
				val cc = c as Component

				val adapter = object : MouseAdapter() {
					private val info = KoruiMouseEvent()

					private fun dispatch(ntype: KoruiMouseEventType, e: MouseEvent) {
						info.apply {
							type = ntype
							x = e.x
							y = e.y
							buttons = 1 shl e.button
							isAltDown = e.isAltDown
							isCtrlDown = e.isControlDown
							isShiftDown = e.isShiftDown
							isMetaDown = e.isMetaDown
							//scaleCoords = false
						}
						ed.dispatch(info)
					}

					override fun mouseReleased(e: MouseEvent) = dispatch(KoruiMouseEventType.UP, e)
					override fun mousePressed(e: MouseEvent) = dispatch(KoruiMouseEventType.DOWN, e)
					override fun mouseClicked(e: MouseEvent) = dispatch(KoruiMouseEventType.CLICK, e)
					override fun mouseMoved(e: MouseEvent) = dispatch(KoruiMouseEventType.MOVE, e)
					override fun mouseDragged(e: MouseEvent) = dispatch(KoruiMouseEventType.DRAG, e)
					override fun mouseEntered(e: MouseEvent) = dispatch(KoruiMouseEventType.ENTER, e)
					override fun mouseExited(e: MouseEvent) = dispatch(KoruiMouseEventType.EXIT, e)
				}

				cc.addMouseListener(adapter)
				cc.addMouseMotionListener(adapter)

				return Closeable {
					cc.removeMouseListener(adapter)
					cc.removeMouseMotionListener(adapter)
				}
			}
			ChangeEvent::class -> {
				var rc = c as Component
				if (rc is JScrollableTextArea) rc = rc.textArea
				val cc = rc as? JTextComponent

				if (cc != null) {
					val adaptor = object : DocumentListener {
						val info = KoruiChangeEvent(null, null)

						override fun changedUpdate(e: DocumentEvent?) = ed.dispatch(info)
						override fun insertUpdate(e: DocumentEvent?) = ed.dispatch(info)
						override fun removeUpdate(e: DocumentEvent?) = ed.dispatch(info)
					}

					cc?.document?.addDocumentListener(adaptor)

					return Closeable {
						cc?.document?.removeDocumentListener(adaptor)
					}
				} else if (rc is JComboBox<*>) {
					val adaptor = ActionListener {
						ed.dispatch(KoruiChangeEvent(null, rc.selectedIndex))
					}
					rc.addActionListener(adaptor)
					return Closeable {
						rc.removeActionListener(adaptor)
					}
				} else if (rc is JSlider) {
					val adaptor = ChangeListener {
						if (!rc.valueIsAdjusting) {
							ed.dispatch(KoruiChangeEvent(null, rc.value))
						}
					}
					rc.addChangeListener(adaptor)
					return Closeable {
						rc.removeChangeListener(adaptor)
					}
				}
			}
			DropFileEvent::class -> {
				val cc = c as JFrame

				val oldTH = cc.transferHandler
				cc.transferHandler = object : TransferHandler() {
					override fun canImport(support: TransferHandler.TransferSupport): Boolean {
						return support.isDataFlavorSupported(DataFlavor.javaFileListFlavor)
					}

					override fun importData(support: TransferHandler.TransferSupport): Boolean {
						if (!canImport(support)) return false
						val l = support.transferable.getTransferData(DataFlavor.javaFileListFlavor) as List<File>
						ed.dispatch(DropFileEvent(DropFileEvent.Type.DROP, l.map { localVfs(it) }))
						return true
					}
				}
				val adapter = object : DropTargetAdapter() {
					override fun dragEnter(dtde: DropTargetDragEvent) {
						ed.dispatch(DropFileEvent(DropFileEvent.Type.ENTER, null))
						//if (listener.enter(LightDropHandler.EnterInfo())) {
						dtde.acceptDrag(DnDConstants.ACTION_COPY_OR_MOVE)
						//}
					}

					override fun dragExit(dte: DropTargetEvent) {
						ed.dispatch(DropFileEvent(DropFileEvent.Type.EXIT, null))
					}

					override fun drop(dtde: DropTargetDropEvent?) {
					}
				}
				cc.dropTarget.addDropTargetListener(adapter)

				return Closeable {
					cc.transferHandler = oldTH
					cc.dropTarget.removeDropTargetListener(adapter)
				}
			}
			ReshapeEvent::class -> {
				val info = ReshapeEvent(0, 0)
				val cc = c as? Container?

				fun send() {
					val cc2 = (c as? JFrame2?)
					val cp = cc2?.contentPane
					if (cp != null) {
						ed.dispatch(info.apply {
							width = cp.width
							height = cp.height
						})
					}
				}

				val adapter = object : ComponentAdapter() {
					override fun componentResized(e: ComponentEvent) {
						send()
					}
				}

				cc?.addComponentListener(adapter)
				send()

				return Closeable {
					cc?.removeComponentListener(adapter)
				}
			}
			com.soywiz.korev.KeyEvent::class -> {
				val cc = c as Component
				val ev = com.soywiz.korev.KeyEvent()

				val adapter = object : KeyAdapter() {
					private fun populate(type: com.soywiz.korev.KeyEvent.Type, e: KeyEvent) = ev.apply {
						this.type = type
						this.key = when (e.keyCode) {
							KeyEvent.VK_ENTER          -> Key.ENTER
							KeyEvent.VK_BACK_SPACE     -> Key.BACKSPACE
							KeyEvent.VK_TAB            -> Key.TAB
							KeyEvent.VK_CANCEL         -> Key.CANCEL
							KeyEvent.VK_CLEAR          -> Key.CLEAR
							KeyEvent.VK_SHIFT          -> Key.LEFT_SHIFT
							KeyEvent.VK_CONTROL        -> Key.LEFT_CONTROL
							KeyEvent.VK_ALT            -> Key.LEFT_ALT
							KeyEvent.VK_PAUSE          -> Key.PAUSE
							KeyEvent.VK_CAPS_LOCK      -> Key.CAPS_LOCK
							KeyEvent.VK_ESCAPE         -> Key.ESCAPE
							KeyEvent.VK_SPACE          -> Key.SPACE
							KeyEvent.VK_PAGE_UP        -> Key.PAGE_UP
							KeyEvent.VK_PAGE_DOWN      -> Key.PAGE_DOWN
							KeyEvent.VK_END            -> Key.END
							KeyEvent.VK_HOME           -> Key.HOME
							KeyEvent.VK_LEFT           -> Key.LEFT
							KeyEvent.VK_UP             -> Key.UP
							KeyEvent.VK_RIGHT          -> Key.RIGHT
							KeyEvent.VK_DOWN           -> Key.DOWN
							KeyEvent.VK_COMMA          -> Key.COMMA
							KeyEvent.VK_MINUS          -> Key.MINUS
							KeyEvent.VK_PERIOD         -> Key.PERIOD
							KeyEvent.VK_SLASH          -> Key.SLASH
							KeyEvent.VK_0              -> Key.N0
							KeyEvent.VK_1              -> Key.N1
							KeyEvent.VK_2              -> Key.N2
							KeyEvent.VK_3              -> Key.N3
							KeyEvent.VK_4              -> Key.N4
							KeyEvent.VK_5              -> Key.N5
							KeyEvent.VK_6              -> Key.N6
							KeyEvent.VK_7              -> Key.N7
							KeyEvent.VK_8              -> Key.N8
							KeyEvent.VK_9              -> Key.N9
							KeyEvent.VK_SEMICOLON      -> Key.SEMICOLON
							KeyEvent.VK_EQUALS         -> Key.EQUAL
							KeyEvent.VK_A              -> Key.A
							KeyEvent.VK_B              -> Key.B
							KeyEvent.VK_C              -> Key.C
							KeyEvent.VK_D              -> Key.D
							KeyEvent.VK_E              -> Key.E
							KeyEvent.VK_F              -> Key.F
							KeyEvent.VK_G              -> Key.G
							KeyEvent.VK_H              -> Key.H
							KeyEvent.VK_I              -> Key.I
							KeyEvent.VK_J              -> Key.J
							KeyEvent.VK_K              -> Key.K
							KeyEvent.VK_L              -> Key.L
							KeyEvent.VK_M              -> Key.M
							KeyEvent.VK_N              -> Key.N
							KeyEvent.VK_O              -> Key.O
							KeyEvent.VK_P              -> Key.P
							KeyEvent.VK_Q              -> Key.Q
							KeyEvent.VK_R              -> Key.R
							KeyEvent.VK_S              -> Key.S
							KeyEvent.VK_T              -> Key.T
							KeyEvent.VK_U              -> Key.U
							KeyEvent.VK_V              -> Key.V
							KeyEvent.VK_W              -> Key.W
							KeyEvent.VK_X              -> Key.X
							KeyEvent.VK_Y              -> Key.Y
							KeyEvent.VK_Z              -> Key.Z
							KeyEvent.VK_OPEN_BRACKET   -> Key.OPEN_BRACKET
							KeyEvent.VK_BACK_SLASH     -> Key.BACKSLASH
							KeyEvent.VK_CLOSE_BRACKET  -> Key.CLOSE_BRACKET
							KeyEvent.VK_NUMPAD0        -> Key.NUMPAD0
							KeyEvent.VK_NUMPAD1        -> Key.NUMPAD1
							KeyEvent.VK_NUMPAD2        -> Key.NUMPAD2
							KeyEvent.VK_NUMPAD3        -> Key.NUMPAD3
							KeyEvent.VK_NUMPAD4        -> Key.NUMPAD4
							KeyEvent.VK_NUMPAD5        -> Key.NUMPAD5
							KeyEvent.VK_NUMPAD6        -> Key.NUMPAD6
							KeyEvent.VK_NUMPAD7        -> Key.NUMPAD7
							KeyEvent.VK_NUMPAD8        -> Key.NUMPAD8
							KeyEvent.VK_NUMPAD9        -> Key.NUMPAD9
							KeyEvent.VK_MULTIPLY       -> Key.KP_MULTIPLY
							KeyEvent.VK_ADD            -> Key.KP_ADD
							KeyEvent.VK_SEPARATER      -> Key.KP_SEPARATOR
							//KeyEvent.VK_SEPARATOR      -> Key.KP_SEPARATOR
							KeyEvent.VK_SUBTRACT       -> Key.KP_SUBTRACT
							KeyEvent.VK_DECIMAL        -> Key.KP_DECIMAL
							KeyEvent.VK_DIVIDE         -> Key.KP_DIVIDE
							KeyEvent.VK_DELETE         -> Key.DELETE
							KeyEvent.VK_NUM_LOCK       -> Key.NUM_LOCK
							KeyEvent.VK_SCROLL_LOCK    -> Key.SCROLL_LOCK
							KeyEvent.VK_F1             -> Key.F1
							KeyEvent.VK_F2             -> Key.F2
							KeyEvent.VK_F3             -> Key.F3
							KeyEvent.VK_F4             -> Key.F4
							KeyEvent.VK_F5             -> Key.F5
							KeyEvent.VK_F6             -> Key.F6
							KeyEvent.VK_F7             -> Key.F7
							KeyEvent.VK_F8             -> Key.F8
							KeyEvent.VK_F9             -> Key.F9
							KeyEvent.VK_F10            -> Key.F10
							KeyEvent.VK_F11            -> Key.F11
							KeyEvent.VK_F12            -> Key.F12
							KeyEvent.VK_F13            -> Key.F13
							KeyEvent.VK_F14            -> Key.F14
							KeyEvent.VK_F15            -> Key.F15
							KeyEvent.VK_F16            -> Key.F16
							KeyEvent.VK_F17            -> Key.F17
							KeyEvent.VK_F18            -> Key.F18
							KeyEvent.VK_F19            -> Key.F19
							KeyEvent.VK_F20            -> Key.F20
							KeyEvent.VK_F21            -> Key.F21
							KeyEvent.VK_F22            -> Key.F22
							KeyEvent.VK_F23            -> Key.F23
							KeyEvent.VK_F24            -> Key.F24
							KeyEvent.VK_PRINTSCREEN    -> Key.PRINT_SCREEN
							KeyEvent.VK_INSERT         -> Key.INSERT
							KeyEvent.VK_HELP           -> Key.HELP
							KeyEvent.VK_META           -> Key.META
							KeyEvent.VK_BACK_QUOTE     -> Key.BACKQUOTE
							KeyEvent.VK_QUOTE          -> Key.QUOTE
							KeyEvent.VK_KP_UP          -> Key.KP_UP
							KeyEvent.VK_KP_DOWN        -> Key.KP_DOWN
							KeyEvent.VK_KP_LEFT        -> Key.KP_LEFT
							KeyEvent.VK_KP_RIGHT       -> Key.KP_RIGHT
							//KeyEvent.VK_DEAD_GRAVE               -> Key.DEAD_GRAVE
							//KeyEvent.VK_DEAD_ACUTE               -> Key.DEAD_ACUTE
							//KeyEvent.VK_DEAD_CIRCUMFLEX          -> Key.DEAD_CIRCUMFLEX
							//KeyEvent.VK_DEAD_TILDE               -> Key.DEAD_TILDE
							//KeyEvent.VK_DEAD_MACRON              -> Key.DEAD_MACRON
							//KeyEvent.VK_DEAD_BREVE               -> Key.DEAD_BREVE
							//KeyEvent.VK_DEAD_ABOVEDOT            -> Key.DEAD_ABOVEDOT
							//KeyEvent.VK_DEAD_DIAERESIS           -> Key.DEAD_DIAERESIS
							//KeyEvent.VK_DEAD_ABOVERING           -> Key.DEAD_ABOVERING
							//KeyEvent.VK_DEAD_DOUBLEACUTE         -> Key.DEAD_DOUBLEACUTE
							//KeyEvent.VK_DEAD_CARON               -> Key.DEAD_CARON
							//KeyEvent.VK_DEAD_CEDILLA             -> Key.DEAD_CEDILLA
							//KeyEvent.VK_DEAD_OGONEK              -> Key.DEAD_OGONEK
							//KeyEvent.VK_DEAD_IOTA                -> Key.DEAD_IOTA
							//KeyEvent.VK_DEAD_VOICED_SOUND        -> Key.DEAD_VOICED_SOUND
							//KeyEvent.VK_DEAD_SEMIVOICED_SOUND    -> Key.DEAD_SEMIVOICED_SOUND
							//KeyEvent.VK_AMPERSAND                -> Key.AMPERSAND
							//KeyEvent.VK_ASTERISK                 -> Key.ASTERISK
							//KeyEvent.VK_QUOTEDBL                 -> Key.QUOTEDBL
							//KeyEvent.VK_LESS                     -> Key.LESS
							//KeyEvent.VK_GREATER                  -> Key.GREATER
							//KeyEvent.VK_BRACELEFT                -> Key.BRACELEFT
							//KeyEvent.VK_BRACERIGHT               -> Key.BRACERIGHT
							//KeyEvent.VK_AT                       -> Key.AT
							//KeyEvent.VK_COLON                    -> Key.COLON
							//KeyEvent.VK_CIRCUMFLEX               -> Key.CIRCUMFLEX
							//KeyEvent.VK_DOLLAR                   -> Key.DOLLAR
							//KeyEvent.VK_EURO_SIGN                -> Key.EURO_SIGN
							//KeyEvent.VK_EXCLAMATION_MARK         -> Key.EXCLAMATION_MARK
							//KeyEvent.VK_INVERTED_EXCLAMATION_MARK -> Key.INVERTED_EXCLAMATION_MARK
							//KeyEvent.VK_LEFT_PARENTHESIS         -> Key.LEFT_PARENTHESIS
							//KeyEvent.VK_NUMBER_SIGN              -> Key.NUMBER_SIGN
							KeyEvent.VK_PLUS                     -> Key.PLUS
							//KeyEvent.VK_RIGHT_PARENTHESIS        -> Key.RIGHT_PARENTHESIS
							//KeyEvent.VK_UNDERSCORE               -> Key.UNDERSCORE
							//KeyEvent.VK_WINDOWS                  -> Key.WINDOWS
							//KeyEvent.VK_CONTEXT_MENU             -> Key.CONTEXT_MENU
							//KeyEvent.VK_FINAL                    -> Key.FINAL
							//KeyEvent.VK_CONVERT                  -> Key.CONVERT
							//KeyEvent.VK_NONCONVERT               -> Key.NONCONVERT
							//KeyEvent.VK_ACCEPT                   -> Key.ACCEPT
							//KeyEvent.VK_MODECHANGE               -> Key.MODECHANGE
							//KeyEvent.VK_KANA                     -> Key.KANA
							//KeyEvent.VK_KANJI                    -> Key.KANJI
							//KeyEvent.VK_ALPHANUMERIC             -> Key.ALPHANUMERIC
							//KeyEvent.VK_KATAKANA                 -> Key.KATAKANA
							//KeyEvent.VK_HIRAGANA                 -> Key.HIRAGANA
							//KeyEvent.VK_FULL_WIDTH               -> Key.FULL_WIDTH
							//KeyEvent.VK_HALF_WIDTH               -> Key.HALF_WIDTH
							//KeyEvent.VK_ROMAN_CHARACTERS         -> Key.ROMAN_CHARACTERS
							//KeyEvent.VK_ALL_CANDIDATES           -> Key.ALL_CANDIDATES
							//KeyEvent.VK_PREVIOUS_CANDIDATE       -> Key.PREVIOUS_CANDIDATE
							//KeyEvent.VK_CODE_INPUT               -> Key.CODE_INPUT
							//KeyEvent.VK_JAPANESE_KATAKANA        -> Key.JAPANESE_KATAKANA
							//KeyEvent.VK_JAPANESE_HIRAGANA        -> Key.JAPANESE_HIRAGANA
							//KeyEvent.VK_JAPANESE_ROMAN           -> Key.JAPANESE_ROMAN
							//KeyEvent.VK_KANA_LOCK                -> Key.KANA_LOCK
							//KeyEvent.VK_INPUT_METHOD_ON_OFF      -> Key.INPUT_METHOD_ON_OFF
							//KeyEvent.VK_CUT                      -> Key.CUT
							//KeyEvent.VK_COPY                     -> Key.COPY
							//KeyEvent.VK_PASTE                    -> Key.PASTE
							//KeyEvent.VK_UNDO                     -> Key.UNDO
							//KeyEvent.VK_AGAIN                    -> Key.AGAIN
							//KeyEvent.VK_FIND                     -> Key.FIND
							//KeyEvent.VK_PROPS                    -> Key.PROPS
							//KeyEvent.VK_STOP                     -> Key.STOP
							//KeyEvent.VK_COMPOSE                  -> Key.COMPOSE
							//KeyEvent.VK_ALT_GRAPH                -> Key.ALT_GRAPH
							//KeyEvent.VK_BEGIN                    -> Key.BEGIN
							KeyEvent.VK_UNDEFINED      -> Key.UNDEFINED
							else -> Key.UNKNOWN
						}
						this.keyCode = e.keyCode
					}

					override fun keyTyped(e: KeyEvent) =
						ed.dispatch(populate(com.soywiz.korev.KeyEvent.Type.TYPE, e))

					override fun keyPressed(e: KeyEvent) =
						ed.dispatch(populate(com.soywiz.korev.KeyEvent.Type.DOWN, e))

					override fun keyReleased(e: KeyEvent) =
						ed.dispatch(populate(com.soywiz.korev.KeyEvent.Type.UP, e))
				}

				cc.addKeyListener(adapter)

				return Closeable {
					cc.removeKeyListener(adapter)
				}
			}
		}
		return DummyCloseable
	}

	val Any.actualComponent: Component get() = if (this is JFrame2) this.panel else (this as Component)
	val Any.actualContainer: Container? get() = if (this is JFrame2) this.panel else (this as? Container)

	override fun setParent(c: Any, parent: Any?) {
		val cc = c as? Component
		val actualParent = (parent as? ChildContainer)?.childContainer ?: parent?.actualContainer
		cc?.parent?.remove(cc)
		//if (cc is JTabbedPage && actualParent is JTabbedPane) {
		//	actualParent?.addTab(cc.title, cc)
		//} else {
			actualParent?.add(cc, 0)
		//}
		//println("$parent <- $c")
	}

	override fun setBounds(c: Any, x: Int, y: Int, width: Int, height: Int) {
		//println("setBounds[${c.javaClass.simpleName}]($x, $y, $width, $height) : Thread(${Thread.currentThread().id})")
		when (c) {
			is JFrame2 -> {
				c.panel.preferredSize = Dimension(width, height)
				//c.preferredSize = Dimension(width, height)
				c.pack()
				//c.contentPane.setBounds(x, y, width, height)
			}
			is Component -> {
				if (c is JScrollPane2) {
					//c.preferredSize = Dimension(100, 100)
					//c.viewport.viewSize = Dimension(100, 100)
					c.viewport.setSize(width, height)
					val rightMost = c.childContainer.components.map { it.bounds.x + it.bounds.width }.max() ?: 0
					val bottomMost = c.childContainer.components.map { it.bounds.y + it.bounds.height }.max() ?: 0
					c.childContainer.preferredSize = Dimension(rightMost, bottomMost)
					c.setBounds(x, y, width, height)
					c.revalidate()
				} else {
					c.setBounds(x, y, width, height)
				}
			}
		}
	}

	override fun <T> callAction(c: Any, key: LightAction<T>, param: T) {
		when (key) {
			LightAction.FOCUS -> {
				(c as Component).requestFocus()
			}
		}
	}

	override fun <T> setProperty(c: Any, key: LightProperty<T>, value: T) {
		when (key) {
			LightProperty.VISIBLE -> {
				val visible = key[value]
				if (c is JFrame2) {
					if (!c.isVisible && visible) {
						c.setLocationRelativeTo(null)
					}
				}
				(c as Component).isVisible = visible
			}
			LightProperty.TEXT -> {
				val text = key[value]
				(c as? JLabel)?.text = text
				(c as? JScrollableTextArea)?.text = text
				(c as? JTextComponent)?.text = text
				(c as? AbstractButton)?.text = text
				(c as? Frame)?.title = text
			}
			LightProperty.NAME -> {
				val text = key[value]
				(c as? JComponent)?.name = text
			}
			LightProperty.IMAGE -> {
				val bmp = key[value]
				val image = (c as? JImage)
				if (image != null) {
					if (bmp == null) {
						image.image = null
					} else {
						if (bmp is AwtNativeImage) {
							image.image = bmp.awtImage.clone()

						} else {
							if ((image.width != bmp.width) || (image.height != bmp.height)) {
								//println("*********************** RECREATED NATIVE IMAGE!")
								image.image = bmp.toAwt()
							}
							bmp.toBMP32().transferTo(image.image!!)
						}
					}
					image.repaint()
				}
			}
			LightProperty.ICON -> {
				val bmp = key[value]
				when (c) {
					is JFrame2 -> {
						c.iconImage = bmp?.toBMP32()?.toAwt()
					}
				}
			}
			LightProperty.IMAGE_SMOOTH -> {
				val v = key[value]
				when (c) {
					is JImage -> {
						c.smooth = v
					}
				}
			}
			LightProperty.BGCOLOR -> {
				val v = key[value]
				(c as? Component)?.background = Color(v.value, true)
			}
			LightProperty.PROGRESS_CURRENT -> {
				(c as? JProgressBar)?.value = key[value]
				(c as? JSlider)?.value = key[value]
			}
			LightProperty.PROGRESS_MAX -> {
				(c as? JProgressBar)?.maximum = key[value]
				(c as? JSlider)?.maximum = key[value]
			}
			LightProperty.CHECKED -> {
				(c as? JToggleButton)?.isSelected = key[value]
			}
			//LightProperty.RADIO_GROUP -> {
			//	val lg = value as LightRadioButtonGroup
			//	val group = lg.extra?.getOrPut("group") { ButtonGroup() } as ButtonGroup
			//	val but = c as? AbstractButton
			//	if (but != null) group.add(but)
			//}
			LightProperty.COMBO_BOX_ITEMS -> {
				val cb = (c as? JComboBox<ComboBoxItem>)
				if (cb != null) {
					cb.removeAllItems()
					for (item in (value as List<ComboBoxItem>)) cb.addItem(item)
				}
			}
			LightProperty.SELECTED_INDEX -> {
				val cb = (c as? JComboBox<ComboBoxItem>)
				if (cb != null) {
					cb.selectedIndex = cb.selectedIndex
				}
			}
		}
	}

	@Suppress("UNCHECKED_CAST")
	override fun <T> getProperty(c: Any, key: LightProperty<T>): T {
		return when (key) {
			LightProperty.CHECKED -> {
				(c as? JComponent)?.name
			}
			LightProperty.CHECKED -> {
				(c as? JToggleButton)?.isSelected ?: false
			}
			LightProperty.TEXT -> {
				(c as? JLabel)?.text ?: (c as? JScrollableTextArea)?.text ?: (c as? JTextComponent)?.text
				?: (c as? AbstractButton)?.text ?: (c as? Frame)?.title
			}
			LightProperty.SELECTED_INDEX -> {
				(c as? JComboBox<ComboBoxItem>)?.selectedIndex ?: super.getProperty(c, key)
			}
			LightProperty.PROGRESS_CURRENT -> {
				(c as? JSlider)?.value ?: super.getProperty(c, key)
			}
			LightProperty.PROGRESS_MAX -> {
				(c as? JSlider)?.maximum ?: super.getProperty(c, key)
			}
			else -> super.getProperty(c, key)
		} as T
	}

	override suspend fun dialogAlert(c: Any, message: String) {
		JOptionPane.showMessageDialog(null, message)
	}

	override suspend fun dialogPrompt(c: Any, message: String, initialValue: String): String {
		val jpf = JTextField()
		jpf.addAncestorListener(RequestFocusListener())
		jpf.text = initialValue
		jpf.selectAll()
		val result =
			JOptionPane.showConfirmDialog(null, arrayOf(JLabel(message), jpf), "Reply:", JOptionPane.OK_CANCEL_OPTION)
		if (result != JFileChooser.APPROVE_OPTION) throw CancellationException()
		return jpf.text
	}

	override suspend fun dialogOpenFile(c: Any, filter: String): VfsFile {
		val fd = FileDialog(c as JFrame2, "Open file", FileDialog.LOAD)
		fd.isVisible = true
		return if (fd.files.isNotEmpty()) {
			localVfs(fd.files.first())
		} else {
			throw CancelException()
		}
	}

	override fun repaint(c: Any) {
		(c as? Component)?.repaint()
	}

	override fun openURL(url: String): Unit {
		Desktop.getDesktop().browse(URI(url))
	}

	override fun open(file: VfsFile): Unit {
		Desktop.getDesktop().open(File(file.absolutePath))
	}

	override fun getDpi(): Double {
		val sr = Toolkit.getDefaultToolkit().screenResolution
		return sr.toDouble()
	}

	override fun getDevicePixelRatio(): Double {
		// screenResolution: 108
		// screenResolution: java.awt.Dimension[width=2048,height=1152]
		// Try to get devicePixelRatio on MAC the old way
		try {
			val obj = Toolkit.getDefaultToolkit().getDesktopProperty("apple.awt.contentScaleFactor")
			if (obj is Number) return obj.toDouble()
		} catch (e: Throwable) {
			e.printStackTrace()
		}

		if (true) {
			return (BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB).graphics as Graphics2D)
				.fontRenderContext.transform.scaleX
		} else {

			// Try to get using the new way
			val gfxConfig = GraphicsEnvironment.getLocalGraphicsEnvironment().defaultScreenDevice.defaultConfiguration
			val transform = gfxConfig.defaultTransform
			return transform.scaleX
		}
	}

	override fun configuredFrame(handle: Any) {
	}
}

class JFrame2 : JFrame() {
	val panel = JPanel2().apply {
		layout = null
	}

	init {
		add(panel)
	}

    override fun createRootPane(): JRootPane = super.createRootPane().apply {
        putClientProperty("apple.awt.fullscreenable", true)
    }
}

interface ChildContainer {
	val childContainer: Container
}

class JScrollableTextArea(val textArea: JTextArea = JTextArea()) : JScrollPane(textArea) {
	var text: String
		get() = textArea.text;
		set(value) {
			textArea.text = value
		}
}

class JScrollPane2(override val childContainer: JPanel = JPanel().apply { layout = null }) : JScrollPane(
	childContainer,
	ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
	ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
), ChildContainer {
	init {
		isOpaque = false
		val unitIncrement = 16
		verticalScrollBar.unitIncrement = unitIncrement
		horizontalScrollBar.unitIncrement = unitIncrement
		border = EmptyBorder(0, 0, 0, 0)
	}

	override fun paintComponent(g: Graphics) {
		g.clearRect(0, 0, width, height)
	}
}

class JPanel2 : JPanel() {
	init {
		isOpaque = false
	}

	//override fun paintComponent(g: Graphics) {
	//	g.clearRect(0, 0, width, height)
	//}
	//override fun paintComponent(g: Graphics) {
	//g.clearRect(0, 0, width, height)
	//}
}

class JImage : JComponent() {
	var image: BufferedImage? = null
	var smooth: Boolean = false

	override fun paintComponent(g: Graphics) {
		val g2 = (g as? Graphics2D)
		if (image != null) {
			g2?.setRenderingHint(
				RenderingHints.KEY_INTERPOLATION,
				if (smooth) RenderingHints.VALUE_INTERPOLATION_BILINEAR else RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR
			)
			g.drawImage(image, 0, 0, width, height, null)
		} else {
			g.clearRect(0, 0, width, height)
		}
		//super.paintComponent(g)
	}
}

class JTabbedPage : JComponent() {
	init {
		name = "Page"
	}
}

class RequestFocusListener(private val removeListener: Boolean = true) : AncestorListener {
	override fun ancestorAdded(e: AncestorEvent) {
		val component = e.component
		component.requestFocusInWindow()
		if (removeListener) component.removeAncestorListener(this)
	}

	override fun ancestorMoved(e: AncestorEvent) {}

	override fun ancestorRemoved(e: AncestorEvent) {}
}


