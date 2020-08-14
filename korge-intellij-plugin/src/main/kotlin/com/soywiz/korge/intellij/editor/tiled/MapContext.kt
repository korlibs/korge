package com.soywiz.korge.intellij.editor.tiled

import com.soywiz.korge.intellij.editor.*
import com.soywiz.korge.intellij.editor.tiled.dialog.*
import com.soywiz.korge.intellij.editor.tiled.editor.*
import com.soywiz.korge.intellij.util.ObservableProperty
import com.soywiz.korim.bitmap.*
import com.soywiz.korim.color.*
import com.soywiz.korio.async.*
import com.soywiz.korge.tiled.*

data class MapContext(
	val tilemap: TiledMap,
	val projectContext: ProjectContext?,
	val history: HistoryManager
) {
	val picked: ObservableProperty<PickedSelection> = ObservableProperty(PickedSelection(Bitmap32(1, 1) { _, _ -> RGBA(0) }))
	val zoomLevels: List<Int> = listOf(25, 50, 75, 100, 150, 200, 300, 400, 700, 1000, 1500, 2000, 2500, 3000)
	val tilesetsUpdated = Signal<Unit>()
	val selectedTilesetIndex = ObservableProperty(0)
}
