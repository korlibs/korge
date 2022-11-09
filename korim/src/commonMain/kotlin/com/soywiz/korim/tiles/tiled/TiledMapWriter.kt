package com.soywiz.korim.tiles.tiled

import com.soywiz.kds.*
import com.soywiz.kmem.toInt
import com.soywiz.korim.tiles.tiled.TiledMap.Encoding
import com.soywiz.korim.tiles.tiled.TiledMap.Image
import com.soywiz.korim.tiles.tiled.TiledMap.Layer
import com.soywiz.korim.tiles.tiled.TiledMap.Object
import com.soywiz.korim.tiles.tiled.TiledMap.Property
import com.soywiz.korim.color.RGBA
import com.soywiz.korim.text.HorizontalAlign
import com.soywiz.korim.text.VerticalAlign
import com.soywiz.korim.tiles.TileMapObjectAlignment
import com.soywiz.korio.file.VfsFile
import com.soywiz.korio.lang.format
import com.soywiz.korio.serialization.xml.Xml
import com.soywiz.korio.serialization.xml.XmlBuilder
import com.soywiz.korio.serialization.xml.buildXml
import com.soywiz.korio.util.niceStr
import com.soywiz.korma.geom.Point

suspend fun VfsFile.writeTiledMap(map: TiledMap) {
	writeString(map.toXml().toString())
}

fun TiledMap.toXml(): Xml {
	val map = this
	val mapData = map.data
	return buildXml(
		"map",
		"version" to 1.2,
		"tiledversion" to "1.3.1",
		"orientation" to mapData.orientation.value,
		"renderorder" to mapData.renderOrder.value,
		"compressionlevel" to mapData.compressionLevel,
		"width" to mapData.width,
		"height" to mapData.height,
		"tilewidth" to mapData.tilewidth,
		"tileheight" to mapData.tileheight,
		"hexsidelength" to mapData.hexSideLength,
		"staggeraxis" to mapData.staggerAxis,
		"staggerindex" to mapData.staggerIndex,
		"backgroundcolor" to mapData.backgroundColor?.toStringARGB(),
		"infinite" to mapData.infinite.toInt(),
		"nextlayerid" to mapData.nextLayerId,
		"nextobjectid" to mapData.nextObjectId
	) {
		propertiesToXml(mapData.properties)
		for (tileset in map.tilesets) {
			val tilesetData = tileset.data
			if (tilesetData.tilesetSource != null) {
				node("tileset", "firstgid" to tilesetData.firstgid, "source" to tilesetData.tilesetSource)
			} else {
				node(tilesetData.toXml())
			}
		}
		for (layer in map.allLayers) {
			when (layer) {
				is Layer.Tiles -> tileLayerToXml(
					layer,
					mapData.infinite,
					mapData.editorSettings?.chunkWidth ?: 16,
					mapData.editorSettings?.chunkHeight ?: 16
				)
				is Layer.Objects -> objectLayerToXml(layer)
				is Layer.Image -> imageLayerToXml(layer)
				is Layer.Group -> groupLayerToXml(
					layer,
					mapData.infinite,
					mapData.editorSettings?.chunkWidth ?: 16,
					mapData.editorSettings?.chunkHeight ?: 16
				)
			}
		}
		val editorSettings = mapData.editorSettings
		if (editorSettings != null && (editorSettings.chunkWidth != 16 || editorSettings.chunkHeight != 16)) {
			node("editorsettings") {
				node(
					"chunksize",
					"width" to editorSettings.chunkWidth,
					"height" to editorSettings.chunkHeight
				)
			}
		}
	}
}

private fun TileSetData.toXml(): Xml {
	return buildXml(
		"tileset",
		"firstgid" to firstgid,
		"name" to name,
		"tilewidth" to tileWidth,
		"tileheight" to tileHeight,
		"spacing" to spacing.takeIf { it > 0 },
		"margin" to margin.takeIf { it > 0 },
		"tilecount" to tileCount,
		"columns" to columns,
		"objectalignment" to objectAlignment.takeIf { it != TileMapObjectAlignment.UNSPECIFIED }?.value
	) {
		imageToXml(image)
		if (tileOffsetX != 0 || tileOffsetY != 0) {
			node("tileoffset", "x" to tileOffsetX, "y" to tileOffsetY)
		}
		grid?.let { grid ->
			node(
				"grid",
				"orientation" to grid.orientation.value,
				"width" to grid.cellWidth,
				"height" to grid.cellHeight
			)
		}
		propertiesToXml(properties)
		if (terrains.isNotEmpty()) {
			node("terraintypes") {
				for (terrain in terrains) {
					node("terrain", "name" to terrain.name, "tile" to terrain.tile) {
						propertiesToXml(terrain.properties)
					}
				}
			}
		}
		if (tiles.isNotEmpty()) {
			for (tile in tiles) {
				node(tile.toXml())
			}
		}
		if (wangsets.isNotEmpty()) {
			node("wangsets") {
				for (wangset in wangsets) node(wangset.toXml())
			}
		}
	}
}

private fun WangSet.toXml(): Xml {
	return buildXml("wangset", "name" to name, "tile" to tileId) {
		propertiesToXml(properties)
		if (cornerColors.isNotEmpty()) {
			for (color in cornerColors) {
				node(
					"wangcornercolor",
					"name" to color.name,
					"color" to color.color,
					"tile" to color.tileId,
					"probability" to color.probability.takeIf { it != 0.0 }?.niceStr
				)
			}
		}
		if (edgeColors.isNotEmpty()) {
			for (color in edgeColors) {
				node(
					"wangedgecolor",
					"name" to color.name,
					"color" to color.color.toStringARGB(),
					"tile" to color.tileId,
					"probability" to color.probability.takeIf { it != 0.0 }?.niceStr
				)
			}
		}
		if (wangtiles.isNotEmpty()) {
			for (wangtile in wangtiles) {
				node(
					"wangtile",
					"tileid" to wangtile.tileId,
					"wangid" to wangtile.wangId.toUInt().toString(16).toUpperCase(),
					"hflip" to wangtile.hflip.takeIf { it },
					"vflip" to wangtile.vflip.takeIf { it },
					"dflip" to wangtile.dflip.takeIf { it }
				)
			}
		}
	}
}

private fun TileData.toXml(): Xml {
	return buildXml("tile",
		"id" to id,
		"type" to type,
		"terrain" to terrain?.joinToString(",") { it?.toString() ?: "" },
		"probability" to probability.takeIf { it != 0.0 }?.niceStr
	) {
		propertiesToXml(properties)
		imageToXml(image)
		objectLayerToXml(objectGroup)
		if (frames != null && frames.isNotEmpty()) {
			node("animation") {
				for (frame in frames) {
					node("frame", "tileid" to frame.tileId, "duration" to frame.duration)
				}
			}
		}
	}
}

private fun IStackedIntArray2.toFinalChunks(): List<StackedIntArray2> {
    return when (this) {
        is SparseChunkedStackedIntArray2 -> this.findAllChunks().flatMap { it.toFinalChunks() }
        is StackedIntArray2 -> listOf(this)
        else -> emptyList()
    }
}

private fun XmlBuilder.tileLayerToXml(
	layer: Layer.Tiles,
	infinite: Boolean,
	chunkWidth: Int,
	chunkHeight: Int
) {
	node(
		"layer",
		"id" to layer.id,
		"name" to layer.name.takeIf { it.isNotEmpty() },
		"width" to layer.width,
		"height" to layer.height,
		"opacity" to layer.opacity.takeIf { it != 1.0 },
		"visible" to layer.visible.toInt().takeIf { it != 1 },
		"locked" to layer.locked.toInt().takeIf { it != 0 },
		"tintcolor" to layer.tintColor,
		"offsetx" to layer.offsetx.takeIf { it != 0.0 },
		"offsety" to layer.offsety.takeIf { it != 0.0 }
	) {
		propertiesToXml(layer.properties)
		node("data", "encoding" to layer.encoding.value, "compression" to layer.compression.value) {
			if (infinite) {
                val chunks = layer.map.toFinalChunks()
				chunks.forEach { chunk ->
                    val chunkIds = chunk.data.first().data
					node(
						"chunk",
						"x" to chunk.startX,
						"y" to chunk.startY,
						"width" to chunkWidth,
						"height" to chunkHeight
					) {
						when (layer.encoding) {
							Encoding.XML -> {
                                chunkIds.forEach { gid ->
									node("tile", "gid" to gid.toUInt().takeIf { it != 0u })
								}
							}
							Encoding.CSV -> {
								text(buildString(chunkWidth * chunkHeight * 4) {
									append("\n")
									for (y in 0 until chunkHeight) {
										for (x in 0 until chunkWidth) {
											append(chunkIds[x + y * chunkWidth].toUInt())
											if (y != chunkHeight - 1 || x != chunkWidth - 1) append(',')
										}
										append("\n")
									}
								})
							}
							Encoding.BASE64 -> {
								//TODO: convert int array of gids into compressed string
							}
						}
					}
				}
			} else {
				when (layer.encoding) {
					Encoding.XML -> {
                        val tileIds = (layer.map as StackedIntArray2).data.first().data
                        tileIds.forEach { gid ->
							node("tile", "gid" to gid.toUInt().takeIf { it != 0u })
						}
					}
					Encoding.CSV -> {
						text(buildString(layer.area * 4) {
							append("\n")
							for (y in 0 until layer.height) {
								for (x in 0 until layer.width) {
									append(layer.map[x, y, 0].toUInt())
									if (y != layer.height - 1 || x != layer.width - 1) append(',')
								}
								append("\n")
							}
						})
					}
					Encoding.BASE64 -> {
						//TODO: convert int array of gids into compressed string
					}
				}
			}
		}
	}
}

private fun XmlBuilder.objectLayerToXml(layer: Layer.Objects?) {
	if (layer == null) return
	node(
		"objectgroup",
		"draworder" to layer.drawOrder.value,
		"id" to layer.id,
		"name" to layer.name.takeIf { it.isNotEmpty() },
		"color" to layer.color.toStringARGB().takeIf { it != "#a0a0a4" },
		"opacity" to layer.opacity.takeIf { it != 1.0 },
		"visible" to layer.visible.toInt().takeIf { it != 1 },
		"locked" to layer.locked.toInt().takeIf { it != 0 },
		"tintcolor" to layer.tintColor,
		"offsetx" to layer.offsetx.takeIf { it != 0.0 },
		"offsety" to layer.offsety.takeIf { it != 0.0 }
	) {
		propertiesToXml(layer.properties)
		layer.objects.forEach { obj ->
			node(
				"object",
				"id" to obj.id,
				"gid" to obj.gid,
				"name" to obj.name.takeIf { it.isNotEmpty() },
				"type" to obj.type.takeIf { it.isNotEmpty() },
				"x" to obj.bounds.x.takeIf { it != 0.0 },
				"y" to obj.bounds.y.takeIf { it != 0.0 },
				"width" to obj.bounds.width.takeIf { it != 0.0 },
				"height" to obj.bounds.height.takeIf { it != 0.0 },
				"rotation" to obj.rotation.takeIf { it != 0.0 },
				"visible" to obj.visible.toInt().takeIf { it != 1 }
				//TODO: support object template
				//"template" to obj.template
			) {
				propertiesToXml(obj.properties)

				fun List<Point>.toXml() = joinToString(" ") { p -> "${p.x.niceStr},${p.y.niceStr}" }

				when (val type = obj.objectShape) {
					is Object.Shape.Rectangle -> Unit
					is Object.Shape.Ellipse -> node("ellipse")
					is Object.Shape.PPoint -> node("point")
					is Object.Shape.Polygon -> node("polygon", "points" to type.points.toXml())
					is Object.Shape.Polyline -> node("polyline", "points" to type.points.toXml())
					is Object.Shape.Text -> node(
						"text",
						"fontfamily" to type.fontFamily,
						"pixelsize" to type.pixelSize.takeIf { it != 16 },
						"wrap" to type.wordWrap.toInt().takeIf { it != 0 },
						"color" to type.color.toStringARGB(),
						"bold" to type.bold.toInt().takeIf { it != 0 },
						"italic" to type.italic.toInt().takeIf { it != 0 },
						"underline" to type.underline.toInt().takeIf { it != 0 },
						"strikeout" to type.strikeout.toInt().takeIf { it != 0 },
						"kerning" to type.kerning.toInt().takeIf { it != 1 },
						"halign" to when (type.hAlign) {
                            HorizontalAlign.LEFT -> "left"
                            HorizontalAlign.CENTER -> "center"
                            HorizontalAlign.RIGHT -> "right"
                            HorizontalAlign.JUSTIFY -> "justify"
                            else -> "left"
                        },
						"valign" to when (type.vAlign) {
                            VerticalAlign.TOP -> "top"
                            VerticalAlign.MIDDLE -> "center"
                            VerticalAlign.BOTTOM -> "bottom"
                            else -> "top"
                        }
					)
				}
			}
		}
	}
}

private fun XmlBuilder.imageLayerToXml(layer: Layer.Image) {
	node(
		"imagelayer",
		"id" to layer.id,
		"name" to layer.name.takeIf { it.isNotEmpty() },
		"opacity" to layer.opacity.takeIf { it != 1.0 },
		"visible" to layer.visible.toInt().takeIf { it != 1 },
		"locked" to layer.locked.toInt().takeIf { it != 0 },
		"tintcolor" to layer.tintColor,
		"offsetx" to layer.offsetx.takeIf { it != 0.0 },
		"offsety" to layer.offsety.takeIf { it != 0.0 }
	) {
		propertiesToXml(layer.properties)
		imageToXml(layer.image)
	}
}

private fun XmlBuilder.groupLayerToXml(layer: Layer.Group, infinite: Boolean, chunkWidth: Int, chunkHeight: Int) {
	node(
		"group",
		"id" to layer.id,
		"name" to layer.name.takeIf { it.isNotEmpty() },
		"opacity" to layer.opacity.takeIf { it != 1.0 },
		"visible" to layer.visible.toInt().takeIf { it != 1 },
		"locked" to layer.locked.toInt().takeIf { it != 0 },
		"tintcolor" to layer.tintColor,
		"offsetx" to layer.offsetx.takeIf { it != 0.0 },
		"offsety" to layer.offsety.takeIf { it != 0.0 }
	) {
		propertiesToXml(layer.properties)
		layer.layers.forEach {
			when (it) {
				is Layer.Tiles -> tileLayerToXml(it, infinite, chunkWidth, chunkHeight)
				is Layer.Objects -> objectLayerToXml(it)
				is Layer.Image -> imageLayerToXml(it)
				is Layer.Group -> groupLayerToXml(it, infinite, chunkWidth, chunkHeight)
			}
		}
	}
}

private fun XmlBuilder.imageToXml(image: Image?) {
	if (image == null) return
	node(
		"image",
		when (image) {
			is Image.Embedded -> "format" to image.format
			is Image.External -> "source" to image.source
		},
		"width" to image.width,
		"height" to image.height,
		"transparent" to image.transparent
	) {
		if (image is Image.Embedded) {
			node(
				"data",
				"encoding" to image.encoding.value,
				"compression" to image.compression.value
			) {
				//TODO: encode and compress image
				text(image.toString())
			}
		}
	}
}

private fun XmlBuilder.propertiesToXml(properties: Map<String, Property>) {
	if (properties.isEmpty()) return

	fun property(name: String, type: String, value: Any) =
		node("property", "name" to name, "type" to type, "value" to value)

	node("properties") {
		properties.forEach { (name, prop) ->
			when (prop) {
				is Property.StringT -> property(name, "string", prop.value)
				is Property.IntT -> property(name, "int", prop.value)
				is Property.FloatT -> property(name, "float", prop.value)
				is Property.BoolT -> property(name, "bool", prop.value.toString())
				is Property.ColorT -> property(name, "color", prop.value.toStringARGB())
				is Property.FileT -> property(name, "file", prop.path)
				is Property.ObjectT -> property(name, "object", prop.id)
			}
		}
	}
}

//TODO: move to korim
private fun RGBA.toStringARGB(): String {
	if (a == 0xFF) {
		return "#%02x%02x%02x".format(r, g, b)
	} else {
		return "#%02x%02x%02x%02x".format(a, r, g, b)
	}
}
