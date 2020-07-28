package com.soywiz.korge.intellij.editor.tiled

import com.soywiz.korge.intellij.editor.HistoryManager
import com.soywiz.korge.intellij.editor.tiled.dialog.ProjectContext
import com.soywiz.korge.intellij.ui.styled
import com.soywiz.korio.file.VfsFile
import kotlinx.coroutines.runBlocking
import java.awt.BorderLayout
import javax.swing.JPanel

class TiledMapEditorPanel(
    val tmxFile: VfsFile,
    val history: HistoryManager = HistoryManager(),
    val registerHistoryShortcuts: Boolean = true,
    val projectCtx: ProjectContext? = null,
    val onSaveXml: (String) -> Unit = {}
) : JPanel(BorderLayout()) {
	val tmx = runBlocking { tmxFile.readTiledMap() }
	init {
		styled.createTileMapEditor(tmx, history, registerHistoryShortcuts, projectCtx)
		history.onSave {
			runBlocking {
				val xmlString = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + tmx.toXml().toOuterXmlIndented().toString()
				onSaveXml(xmlString)
				//tmxFile.writeString(xmlString)
			}
		}
		//history.onAdd {
		history.onChange {
			history.save()
		}
	}
/*
	val realPanel = tileMapEditor.contentPanel

	val mapComponent = MapComponent(tmx)

	var scale: Double
		get() = mapComponent.scale
		set(value) = run { mapComponent.scale = value }

	val mapComponentScroll = JBScrollPane(mapComponent).also { scroll ->
		//scroll.verticalScrollBar.unitIncrement = 16
	}

	fun updatedSize() {
		tileMapEditor.leftSplitPane.dividerLocation = 200
		tileMapEditor.rightSplitPane.dividerLocation = tileMapEditor.rightSplitPane.width - 200
	}

	val layersController = LayersController(tileMapEditor.layersPane)
	val propertiesController = PropertiesController(tileMapEditor.propertiesPane)

	init {

		add(realPanel, BorderLayout.CENTER)

		tileMapEditor.mapPanel.add(mapComponentScroll, GridConstraints().also { it.fill = GridConstraints.FILL_BOTH })

		tileMapEditor.zoomInButton.addActionListener { scale *= 1.5 }
		tileMapEditor.zoomOutButton.addActionListener { scale /= 1.5 }


		updatedSize()
		addComponentListener(object : ComponentAdapter() {
			override fun componentResized(e: ComponentEvent) {
				updatedSize()
			}
		})
	}
 */
}

/*
class PropertiesController(val panel: PropertiesPane) {
	var width = 100
	var height = 100
	val propertyTable = KorgePropertyTable(KorgePropertyTable.Properties().register(::width,::height)).also {
		panel.tablePane.add(JScrollPane(it), BorderLayout.CENTER)
	}
}

class LayersController(val panel: LayersPane) {
	init {
		val menu = JPopupMenu("Menu").apply {
			add("Tile Layer")
			add("Object Layer")
			add("Image Layer")
		}

		panel.newButton.addActionListener {
			menu.show(panel.newButton, 0, panel.newButton.height)
		}
	}
}
 */