package com.soywiz.korge.intellij.editor.tiled.editor

import com.intellij.ui.components.*
import com.soywiz.kmem.*
import com.soywiz.korge.intellij.*
import com.soywiz.korge.intellij.editor.tiled.*
import com.soywiz.korge.intellij.editor.tiled.dialog.*
import com.soywiz.korge.intellij.ui.*
import com.soywiz.korge.intellij.util.*
import com.soywiz.korge.view.tiles.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korim.color.*
import com.soywiz.korim.format.*
import com.soywiz.korio.file.*
import com.soywiz.korma.geom.*
import kotlinx.coroutines.*
import javax.swing.*
import kotlin.math.*

fun Styled<out JTabbedPane>.tilesetTab(
	ctx: MapContext
) = ctx.apply {
	tab("Tileset") {
		verticalStack {
			tabs {
				fill()
                component.addChangeListener {
                    selectedTilesetIndex.value = (it.source as JBTabbedPane).selectedIndex
                }
				uiSequence({ tilemap.tilesets }, tilesetsUpdated) { tileset ->
					tab(tileset.data.name) {
						val tilemap = tileset.pickerTilemap()
						val mapComponent = MapComponent(tilemap)
						val tileLayer = tilemap.tileLayers.first()
						mapComponent.selectedRange(0, 0)
						var downStart: PointInt? = null
						val zoomLevel =
							ObservableProperty(zoomLevels.indexOf(100)) { it.clamp(0, zoomLevels.size - 1) }

						fun zoomRatio(): Double = zoomLevels[zoomLevel.value].toDouble() / 100.0
						zoomLevel {
							mapComponent.scale = zoomRatio()
						}
						zoomLevel.trigger()
						mapComponent.onZoom {
							zoomLevel.value += it
						}
						mapComponent.upTileSignal {
							downStart = null
						}
						mapComponent.outTileSignal {
							//downStart = null
						}
						mapComponent.downTileSignal {
							if (downStart == null) {
								downStart = it
							}
							val start = downStart!!
							val xmin = min(start.x, it.x)
							val xmax = max(start.x, it.x)
							val ymin = min(start.y, it.y)
							val ymax = max(start.y, it.y)
							val width = xmax - xmin + 1
							val height = ymax - ymin + 1
							val bmp =
								Bitmap32(width, height) { x, y -> RGBA(tileLayer.map[xmin + x, ymin + y].value) }
							picked.value = PickedSelection(bmp)
							mapComponent.selectedRange(xmin, ymin, bmp.width, bmp.height)
						}
						component.add(JBScrollPane(mapComponent))
					}
				}
                selectedTilesetIndex {
                    val current = component.selectedIndex
                    val new = selectedTilesetIndex.value
                    if (current != new) {
                        component.selectedIndex = new
                    }
                }
			}
			toolbar {
				iconButton(toolbarIcon("add.png"), "Add tileset file or image") {
					click {
						val vfsFile = projectContext.chooseFile()?.toVfs()
						if (vfsFile != null) {
							runBlocking {
                                val firstgid = tilemap.nextGid
                                // TODO: copy file to current directory
								val tileset = if (vfsFile.extensionLC == "tsx") {
									vfsFile.readTileSetData(firstgid).toTiledSet(vfsFile.parent)
								} else {
									val bmp = vfsFile.readBitmapOptimized()
                                    // TODO: show dialog to specify tile width and height
									tiledsetFromBitmap(vfsFile, 16, 16, bmp, firstgid)
								}

								history.addAndDo("ADD TILESET") { redo ->
									if (redo) {
                                        val index = tilemap.tilesets.size
										tilemap.data.tilesets += tileset.data
										tilemap.tilesets.add(tileset)
										tilesetsUpdated(Unit)
                                        selectedTilesetIndex.value = index
									} else {
                                        val index = tilemap.tilesets.indexOf(tileset)
										tilemap.data.tilesets -= tileset.data
                                        tilemap.tilesets.remove(tileset)
                                        tilesetsUpdated(Unit)
                                        selectedTilesetIndex.value = index
									}
								}
							}
						}
					}
				}
				iconButton(toolbarIcon("openDisk.png")) {
					click {
					}
				}
				iconButton(toolbarIcon("edit.png")) {
					click {
					}
				}
				iconButton(toolbarIcon("delete.png"), "Remove tileset") {
                    tilesetsUpdated {
                        component.isEnabled = tilemap.tilesets.size > 1
                    }
					click {
                        val index = selectedTilesetIndex.value
                        val tileset = tilemap.tilesets[index]
                        history.addAndDo("REMOVE TILESET") { redo ->
                            if (redo) {
                                tilemap.data.tilesets.remove(tileset.data)
                                tilemap.tilesets.removeAt(index)
                                tilesetsUpdated(Unit)
                                selectedTilesetIndex.value = max(0, index - 1)
                            } else {
                                tilemap.data.tilesets.add(tileset.data)
                                tilemap.tilesets.add(index, tileset)
                                tilesetsUpdated(Unit)
                                selectedTilesetIndex.value = index
                            }
                        }
					}
				}
			}
		}
	}
}

data class PickedSelection(val data: Bitmap32)

private fun TiledMap.TiledTileset.pickerTilemap(): TiledMap {
	val mapWidth = data.columns.takeIf { it >= 0 } ?: (tileset.width / data.tileWidth)
	val mapHeight = ceil(data.tileCount.toDouble() / data.columns.toDouble()).toInt()

	return TiledMap(TiledMapData(
		width = mapWidth, height = mapHeight,
		tilewidth = tileset.width, tileheight = tileset.height,
		allLayers = arrayListOf(TiledMap.Layer.Tiles(
            Bitmap32(mapWidth.coerceAtLeast(1), mapHeight.coerceAtLeast(1)) { x, y -> RGBA(y * mapWidth + x + firstgid) }
        ))
	), mutableListOf(this))
}

private suspend fun tiledsetFromBitmap(file: VfsFile, tileWidth: Int, tileHeight: Int, bmp: Bitmap, firstgid: Int): TiledMap.TiledTileset {
    val tileset = TileSet(bmp.slice(), tileWidth, tileHeight)
    return TileSetData(
        name = file.baseName.substringBeforeLast("."),
        firstgid = firstgid,
        tileWidth = tileset.width,
        tileHeight = tileset.height,
        tileCount = tileset.textures.size,
		//TODO: provide these values as params
		spacing = 0,
		margin = 0,
        columns = tileset.base.width / tileset.width,
        image = TiledMap.Image.External(file.baseName, bmp.width, bmp.height),
        tilesetSource = null,
        terrains = listOf(),
        tiles = listOf()
    ).toTiledSet(file.parent)
}
