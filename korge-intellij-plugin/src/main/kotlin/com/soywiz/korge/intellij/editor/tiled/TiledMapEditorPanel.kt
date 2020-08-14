package com.soywiz.korge.intellij.editor.tiled

import com.soywiz.korge.awt.*
import com.soywiz.korge.intellij.editor.HistoryManager
import com.soywiz.korge.intellij.editor.tiled.dialog.ProjectContext
import com.soywiz.korio.file.VfsFile
import kotlinx.coroutines.runBlocking
import java.awt.BorderLayout
import javax.swing.JPanel
import com.soywiz.korge.tiled.*

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
}
