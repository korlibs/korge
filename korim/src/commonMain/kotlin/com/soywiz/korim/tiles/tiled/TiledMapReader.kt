package com.soywiz.korim.tiles.tiled

import com.soywiz.kds.*
import com.soywiz.kds.iterators.*
import com.soywiz.klock.*
import com.soywiz.kmem.*
import com.soywiz.korim.tiles.tiled.TiledMap.*
import com.soywiz.korim.tiles.tiled.TiledMap.Image
import com.soywiz.korim.atlas.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korim.color.*
import com.soywiz.korim.format.*
import com.soywiz.korim.text.HorizontalAlign
import com.soywiz.korim.text.TextAlignment
import com.soywiz.korim.text.VerticalAlign
import com.soywiz.korim.tiles.TileMapObjectAlignment
import com.soywiz.korim.tiles.TileMapOrientation
import com.soywiz.korim.tiles.TileMapRenderOrder
import com.soywiz.korim.tiles.TileMapStaggerAxis
import com.soywiz.korim.tiles.TileMapStaggerIndex
import com.soywiz.korim.tiles.TileSet
import com.soywiz.korim.tiles.TileSetAnimationFrame
import com.soywiz.korim.tiles.TileSetTileInfo
import com.soywiz.korim.tiles.TileShapeInfo
import com.soywiz.korim.tiles.TileShapeInfoImpl
import com.soywiz.korio.compression.*
import com.soywiz.korio.compression.deflate.*
import com.soywiz.korio.file.*
import com.soywiz.korio.lang.*
import com.soywiz.korio.serialization.xml.*
import com.soywiz.korma.geom.*
import com.soywiz.korma.geom.collider.HitTestDirection
import com.soywiz.korma.geom.collider.HitTestDirectionFlags
import com.soywiz.korma.geom.shape.*
import com.soywiz.krypto.encoding.*
import kotlin.collections.set

suspend fun VfsFile.readTiledMap(
	hasTransparentColor: Boolean = false,
	transparentColor: RGBA = Colors.FUCHSIA,
	createBorder: Int = 1,
    atlas: MutableAtlasUnit? = null
): TiledMap {
	val folder = this.parent.jail()
	val data = readTiledMapData()

	val tiledTilesets = arrayListOf<TiledTileset>()

	data.tilesets.fastForEach { tileset ->
		tiledTilesets += tileset.toTiledSet(folder, hasTransparentColor, transparentColor, createBorder, atlas = atlas)
	}

	return TiledMap(data, tiledTilesets)
}

suspend fun VfsFile.readTiledSet(
    firstgid: Int = 1,
    hasTransparentColor: Boolean = false,
    transparentColor: RGBA = Colors.FUCHSIA,
    createBorder: Int = 1,
    atlas: MutableAtlasUnit? = null,
): TiledTileset {
    return readTileSetData(firstgid).toTiledSet(this.parent, hasTransparentColor, transparentColor, createBorder, atlas)
}

suspend fun VfsFile.readTiledSetData(firstgid: Int = 1): TileSetData {
    return parseTileSetData(this.readXml(), firstgid, this.baseName)
}

@Deprecated("Use readTiledSetData", ReplaceWith("readTiledSetData(firstgid)"))
suspend fun VfsFile.readTileSetData(firstgid: Int = 1): TileSetData = readTiledSetData(firstgid)

suspend fun TileSetData.toTiledSet(
	folder: VfsFile,
	hasTransparentColor: Boolean = false,
	transparentColor: RGBA = Colors.FUCHSIA,
	createBorder: Int = 1,
    atlas: MutableAtlasUnit? = null
): TiledTileset {
    val atlas = when {
        atlas != null -> atlas
        createBorder > 0 -> MutableAtlas(2048, border = createBorder.clamp(1, 4))
        else -> null
    }

	val tileset = this
	var bmp = try {
		when (tileset.image) {
			is Image.Embedded -> TODO()
			is Image.External -> folder[tileset.image.source].readBitmap()
                .let { if (atlas != null) it.toBMP32IfRequired() else it }
			null -> Bitmap32.EMPTY
		}
	} catch (e: Throwable) {
		e.printStackTrace()
		Bitmap32(tileset.width, tileset.height, premultiplied = true)
	}

	// @TODO: Preprocess this, so in JS we don't have to do anything!
	if (hasTransparentColor) {
		bmp = bmp.toBMP32()
        val bmp: Bitmap32 = bmp
		for (n in 0 until bmp.area) {
			if (bmp.getRgbaAtIndex(n) == transparentColor) bmp.ints[n] = 0
		}
	}


    val collisionsMap = IntMap<TileShapeInfo>()
    tileset.tiles.fastForEach { tile ->
        val collisionType = HitTestDirectionFlags.fromString(tile.type, HitTestDirectionFlags.NONE)
        val vectorPaths = fastArrayListOf<TileShapeInfo>()
        if (tile.objectGroup != null) {
            tile.objectGroup.objects.fastForEach {
                vectorPaths.add(
                    TileShapeInfoImpl(
                        HitTestDirectionFlags.fromString(it.type),
                        it.toShape2dNoTransformed(),
                        it.getTransform(),
                        //it.toVectorPath()
                    )
                )
            }
        }
        //println("tile.objectGroup=${tile.objectGroup}")
        collisionsMap[tile.id] = object : TileShapeInfo {
            override fun hitTestAny(x: Double, y: Double, direction: HitTestDirection): Boolean {
                if (vectorPaths.isNotEmpty()) {
                    vectorPaths.fastForEach {
                        if (it.hitTestAny(x, y, direction)) return true
                    }
                }
                return collisionType.matches(direction)
            }

            override fun hitTestAny(shape2d: Shape2d, matrix: Matrix, direction: HitTestDirection): Boolean {
                if (vectorPaths.isNotEmpty()) {
                    vectorPaths.fastForEach {
                        if (it.hitTestAny(shape2d, matrix, direction)) return true
                    }
                }
                return collisionType.matches(direction)
            }

            override fun toString(): String = "HitTestable[id=${tile.id}]($vectorPaths)"
        }
    }


    val ptileset = when {
	    atlas != null -> {
            val tileSet = TileSet(bmp.slice(), tileset.tileWidth, tileset.tileHeight, tileset.columns, tileset.tileCount, collisionsMap)
            val map = IntMap<TileSetTileInfo>()
            tileSet.infos.fastForEachWithIndex { index, value ->
                if (value != null) {
                    val tile = tileset.tilesById[value.id]
                    map[index] = value.copy(
                        slice = atlas.add(value.slice, Unit).slice,
                        frames = (tile?.frames ?: emptyList()).map { TileSetAnimationFrame(it.tileId, it.duration.milliseconds) }
                    )
                }
            }
            TileSet(map, tileset.tileWidth, tileset.tileHeight, collisionsMap)
	    }
        //createBorder > 0 -> {
        //    bmp = bmp.toBMP32()
        //    if (tileset.spacing >= createBorder) {
        //        // There is already separation between tiles, use it as it is
        //        val slices = TileSet.extractBmpSlices(
        //            bmp,
        //            tileset.tileWidth,
        //            tileset.tileHeight,
        //            tileset.columns,
        //            tileset.tileCount,
        //            tileset.spacing,
        //            tileset.margin
        //        ).mapIndexed { index, bitmapSlice -> TileSetTileInfo(index, bitmapSlice) }
        //        TileSet(slices, tileset.tileWidth, tileset.tileHeight, collisionsMap)
        //    } else {
        //        // No separation between tiles: create a new Bitmap adding that separation
        //        val bitmaps = if (bmp.width != 0 && bmp.height != 0) {
        //            TileSet.extractBmpSlices(
        //                bmp,
        //                tileset.tileWidth,
        //                tileset.tileHeight,
        //                tileset.columns,
        //                tileset.tileCount,
        //                tileset.spacing,
        //                tileset.margin
        //            )
        //        } else if (tileset.tiles.isNotEmpty()) {
        //            tileset.tiles.map {
        //                when (it.image) {
        //                    is Image.Embedded -> TODO()
        //                    is Image.External -> {
        //                        val file = folder[it.image.source]
        //                        file.readBitmapOptimized().toBMP32().slice(name = file.baseName)
        //                    }
        //                    else -> Bitmap32(0, 0).slice()
        //                }
        //            }
        //        } else {
        //            emptyList()
        //        }
        //        TileSet.fromBitmapSlices(tileset.tileWidth, tileset.tileHeight, bitmaps, border = createBorder, mipmaps = false, collisionsMap = collisionsMap)
        //    }
        //}
        else -> {
            TileSet(bmp.slice(), tileset.tileWidth, tileset.tileHeight, tileset.columns, tileset.tileCount, collisionsMap)
        }
    }

	val tiledTileset = TiledTileset(
		tileset = ptileset,
		data = tileset,
		firstgid = tileset.firstgid
	)

	return tiledTileset
}

suspend fun VfsFile.readTiledMapData(): TiledMapData {
	val file = this
	val folder = this.parent
	val tiledMap = TiledMapData()
	val mapXml = file.readXml()

	if (mapXml.nameLC != "map") error("Not a TiledMap XML TMX file starting with <map>")

	//TODO: Support different orientations
	val orientation = mapXml.getString("orientation")
	tiledMap.orientation = when (orientation) {
		"orthogonal" -> TileMapOrientation.ORTHOGONAL
		"staggered" -> TileMapOrientation.STAGGERED
		else -> unsupported("Orientation \"$orientation\" is not supported")
	}
	val renderOrder = mapXml.getString("renderorder")
	tiledMap.renderOrder = when (renderOrder) {
		"right-down" -> TileMapRenderOrder.RIGHT_DOWN
		"right-up" -> TileMapRenderOrder.RIGHT_UP
		"left-down" -> TileMapRenderOrder.LEFT_DOWN
		"left-up" -> TileMapRenderOrder.LEFT_UP
		else -> TileMapRenderOrder.RIGHT_DOWN
	}
	tiledMap.compressionLevel = mapXml.getInt("compressionlevel") ?: -1
	tiledMap.width = mapXml.getInt("width") ?: 0
	tiledMap.height = mapXml.getInt("height") ?: 0
	tiledMap.tilewidth = mapXml.getInt("tilewidth") ?: 32
	tiledMap.tileheight = mapXml.getInt("tileheight") ?: 32
	tiledMap.hexSideLength = mapXml.getInt("hexsidelength")
	val staggerAxis = mapXml.getString("staggeraxis")
	tiledMap.staggerAxis = when (staggerAxis) {
		"x" -> TileMapStaggerAxis.X
		"y" -> TileMapStaggerAxis.Y
		else -> null
	}
	val staggerIndex = mapXml.getString("staggerindex")
	tiledMap.staggerIndex = when (staggerIndex) {
		"even" -> TileMapStaggerIndex.EVEN
		"odd" -> TileMapStaggerIndex.ODD
		else -> null
	}
	tiledMap.backgroundColor = mapXml.getString("backgroundcolor")?.let { colorFromARGB(it, Colors.TRANSPARENT_BLACK) }
	val nextLayerId = mapXml.getInt("nextlayerid")
	val nextObjectId = mapXml.getInt("nextobjectid")
	tiledMap.infinite = mapXml.getInt("infinite") == 1

	mapXml.child("properties")?.parseProperties()?.let {
		tiledMap.properties.putAll(it)
	}

	tilemapLog.trace { "tilemap: width=${tiledMap.width}, height=${tiledMap.height}, tilewidth=${tiledMap.tilewidth}, tileheight=${tiledMap.tileheight}" }
	tilemapLog.trace { "tilemap: $tiledMap" }

	val elements = mapXml.allChildrenNoComments

	tilemapLog.trace { "tilemap: elements=${elements.size}" }
	tilemapLog.trace { "tilemap: elements=$elements" }

	elements.fastForEach { element ->
		when (element.nameLC) {
			"tileset" -> {
				tilemapLog.trace { "tileset" }
				val firstgid = element.int("firstgid", 1)
				val sourcePath = element.getString("source")
				val tileset = if (sourcePath != null) folder[sourcePath].readXml() else element
				tiledMap.tilesets += parseTileSetData(tileset, firstgid, sourcePath)
			}
			"layer" -> {
				val layer = element.parseTileLayer(tiledMap.infinite)
				tiledMap.allLayers += layer
			}
			"objectgroup" -> {
				val layer = element.parseObjectLayer()
				tiledMap.allLayers += layer
			}
			"imagelayer" -> {
				val layer = element.parseImageLayer()
				tiledMap.allLayers += layer
			}
			"group" -> {
				val layer = element.parseGroupLayer(tiledMap.infinite)
				tiledMap.allLayers += layer
			}
			"editorsettings" -> {
				val chunkSize = element.child("chunksize")
				tiledMap.editorSettings = EditorSettings(
					chunkWidth = chunkSize?.int("width", 16) ?: 16,
					chunkHeight = chunkSize?.int("height", 16) ?: 16
				)
			}
		}
	}

	tiledMap.nextLayerId = nextLayerId ?: run {
		var maxLayerId = 0
		for (layer in tiledMap.allLayers) {
			if (layer.id > maxLayerId) maxLayerId = layer.id
		}
		maxLayerId + 1
	}
	tiledMap.nextObjectId = nextObjectId ?: run {
		var maxObjectId = 0
		for (objects in tiledMap.objectLayers) {
			for (obj in objects.objects) {
				if (obj.id > maxObjectId) maxObjectId = obj.id
			}
		}
		maxObjectId + 1
	}

	return tiledMap
}

fun parseTileSetData(tileset: Xml, firstgid: Int, tilesetSource: String? = null): TileSetData {
	val alignment = tileset.str("objectalignment", "unspecified")
	val objectAlignment = TileMapObjectAlignment.values().find { it.value == alignment } ?: TileMapObjectAlignment.UNSPECIFIED
	val tileOffset = tileset.child("tileoffset")

	return TileSetData(
		name = tileset.str("name"),
		firstgid = firstgid,
		tileWidth = tileset.int("tilewidth"),
		tileHeight = tileset.int("tileheight"),
		tileCount = tileset.int("tilecount", 0),
		spacing = tileset.int("spacing", 0),
		margin = tileset.int("margin", 0),
		columns = tileset.int("columns", 0),
		image = tileset.child("image")?.parseImage(),
		tileOffsetX = tileOffset?.int("x") ?: 0,
		tileOffsetY = tileOffset?.int("y") ?: 0,
		grid = tileset.child("grid")?.parseGrid(),
		tilesetSource = tilesetSource,
		objectAlignment = objectAlignment,
		terrains = tileset.children("terraintypes").children("terrain").map { it.parseTerrain() },
		wangsets = tileset.children("wangsets").children("wangset").map { it.parseWangSet() },
		properties = tileset.child("properties")?.parseProperties() ?: mapOf(),
		tiles = tileset.children("tile").map { it.parseTile() }
	)
}

private fun Xml.parseTile(): TileData {
	val tile = this
	fun Xml.parseFrame(): AnimationFrameData {
		return AnimationFrameData(this.int("tileid"), this.int("duration"))
	}
	return TileData(
		id = tile.int("id"),
		type = tile.strNull("type"),
		terrain = tile.str("terrain").takeIf { it.isNotEmpty() }?.split(',')?.map { it.toIntOrNull() },
		probability = tile.double("probability"),
		image = tile.child("image")?.parseImage(),
		properties = tile.child("properties")?.parseProperties() ?: mapOf(),
		objectGroup = tile.child("objectgroup")?.parseObjectLayer(),
		frames = tile.child("animation")?.children("frame")?.map { it.parseFrame() }
	)
}

private fun Xml.parseTerrain(): TerrainData {
	return TerrainData(
		name = str("name"),
		tile = int("tile"),
		properties = parseProperties()
	)
}

private fun Xml.parseWangSet(): WangSet {
	fun Xml.parseWangColor(): WangSet.WangColor {
		val wangcolor = this
		return WangSet.WangColor(
            name = wangcolor.str("name"),
            color = Colors[wangcolor.str("color")],
            tileId = wangcolor.int("tile"),
            probability = wangcolor.double("probability")
        )
	}

	fun Xml.parseWangTile(): WangSet.WangTile {
		val wangtile = this
		val hflip = wangtile.str("hflip")
		val vflip = wangtile.str("vflip")
		val dflip = wangtile.str("dflip")
		return WangSet.WangTile(
            tileId = wangtile.int("tileid"),
            wangId = wangtile.int("wangid"),
            hflip = hflip == "1" || hflip == "true",
            vflip = vflip == "1" || vflip == "true",
            dflip = dflip == "1" || dflip == "true"
        )
	}

	val wangset = this
	return WangSet(
		name = wangset.str("name"),
		tileId = wangset.int("tile"),
		properties = wangset.parseProperties(),
		cornerColors = wangset.children("wangcornercolor").map { it.parseWangColor() },
		edgeColors = wangset.children("wangedgecolor").map { it.parseWangColor() },
		wangtiles = wangset.children("wangtile").map { it.parseWangTile() }
	)
}

private fun Xml.parseGrid(): Grid {
	val grid = this
	val orientation = grid.str("orientation")
	return Grid(
		cellWidth = grid.int("width"),
		cellHeight = grid.int("height"),
		orientation = Grid.Orientation.values().find { it.value == orientation } ?: Grid.Orientation.ORTHOGONAL
	)
}

private fun Xml.parseCommonLayerData(layer: Layer) {
	val element = this
	layer.id = element.int("id")
	layer.name = element.str("name")
	layer.opacity = element.double("opacity", 1.0)
	layer.visible = element.int("visible", 1) == 1
	layer.locked = element.int("locked", 0) == 1
	layer.tintColor = element.strNull("tintcolor")?.let { colorFromARGB(it, Colors.WHITE) }
	layer.offsetx = element.double("offsetx")
	layer.offsety = element.double("offsety")

	element.child("properties")?.parseProperties()?.let {
		layer.properties.putAll(it)
	}
}

private fun Xml.parseTileLayer(infinite: Boolean): Layer.Tiles {
	val layer = Layer.Tiles()
	parseCommonLayerData(layer)

	val element = this
	val width = element.int("width")
	val height = element.int("height")
	val data = element.child("data")

	val map: IStackedIntArray2
	val encoding: Encoding
	val compression: Compression

	if (data == null) {
		map = StackedIntArray2(width, height)
		encoding = Encoding.CSV
		compression = Compression.NO
	} else {
		val enc = data.strNull("encoding")
		val com = data.strNull("compression")
		encoding = Encoding.values().find { it.value == enc } ?: Encoding.XML
		compression = Compression.values().find { it.value == com } ?: Compression.NO
		val count = width * height

		fun Xml.encodeGids(): IntArray = when (encoding) {
			Encoding.XML -> {
                // @TODO: Bug on Kotlin-JS 1.4.0 release
				//children("tile").map { it.uint("gid").toInt() }.toIntArray()
                children("tile").map { it.double("gid").toInt() }.toIntArray()
			}
			Encoding.CSV -> {
                // @TODO: Bug on Kotlin-JS 1.4.0 release
				//text.replace(spaces, "").split(',').map { it.toUInt().toInt() }.toIntArray()
                text.replace(spaces, "").split(',').map { it.toDouble().toInt() }.toIntArray()
			}
			Encoding.BASE64 -> {
				val rawContent = text.trim().fromBase64()
				val content = when (compression) {
					Compression.NO -> rawContent
					Compression.GZIP -> rawContent.uncompress(GZIP)
					Compression.ZLIB -> rawContent.uncompress(ZLib)
					//TODO: support "zstd" compression
					//Data.Compression.ZSTD -> rawContent.uncompress(ZSTD)
					else -> invalidOp("Unknown compression '$compression'")
				}
				//TODO: read UIntArray
				content.readIntArrayLE(0, count)
			}
		}

		if (infinite) {
            val sparse = SparseChunkedStackedIntArray2()
            map = sparse
			data.children("chunk").forEach { chunk ->
				val offsetX = chunk.int("x")
				val offsetY = chunk.int("y")
				val cwidth = chunk.int("width")
				val cheight = chunk.int("height")
                sparse.putChunk(
                    StackedIntArray2(
                        IntArray2(cwidth, cheight, chunk.encodeGids()),
                        startX = offsetX,
                        startY = offsetY
                    )
                )
			}
		} else {
            map = StackedIntArray2(IntArray2(width, height, data.encodeGids()))
		}
	}

	layer.map = map
	layer.encoding = encoding
	layer.compression = compression

	return layer
}

private fun Xml.parseObjectLayer(): Layer.Objects {
	val layer = Layer.Objects()
	parseCommonLayerData(layer)

	val element = this
	val order = element.str("draworder", "topdown")
	layer.color = colorFromARGB(element.str("color"), Colors["#a0a0a4"])
	layer.drawOrder = Object.DrawOrder.values().find { it.value == order } ?: Object.DrawOrder.TOP_DOWN

	for (obj in element.children("object")) {
		val objInstance = Object(
			id = obj.int("id"),
			gid = obj.intNull("gid"),
			name = obj.str("name"),
			type = obj.str("type"),
			bounds = obj.run { Rectangle(double("x"), double("y"), double("width"), double("height")) },
			rotation = obj.double("rotation"),
			visible = obj.int("visible", 1) != 0
			//TODO: support object templates
			//templatePath = obj.strNull("template")
		)
		obj.child("properties")?.parseProperties()?.let {
			objInstance.properties.putAll(it)
		}

		fun Xml.readPoints() = str("points").split(spaces).map { xy ->
			val parts = xy.split(',').map { it.trim().toDoubleOrNull() ?: 0.0 }
			Point(parts[0], parts[1])
		}

		val ellipse = obj.child("ellipse")
		val point = obj.child("point")
		val polygon = obj.child("polygon")
		val polyline = obj.child("polyline")
		val text = obj.child("text")
		val objectShape: Object.Shape = when {
			ellipse != null -> Object.Shape.Ellipse(objInstance.bounds.width, objInstance.bounds.height)
			point != null -> Object.Shape.PPoint
			polygon != null -> Object.Shape.Polygon(polygon.readPoints())
			polyline != null -> Object.Shape.Polyline(polyline.readPoints())
			text != null -> Object.Shape.Text(
				fontFamily = text.str("fontfamily", "sans-serif"),
				pixelSize = text.int("pixelsize", 16),
				wordWrap = text.int("wrap", 0) == 1,
				color = colorFromARGB(text.str("color"), Colors.BLACK),
				bold = text.int("bold") == 1,
				italic = text.int("italic") == 1,
				underline = text.int("underline") == 1,
				strikeout = text.int("strikeout") == 1,
				kerning = text.int("kerning", 1) == 1,
				align = TextAlignment(
                    HorizontalAlign(text.str("halign", "left")),
                    VerticalAlign(text.str("valign", "top")),
                ),
			)
			else -> Object.Shape.Rectangle(objInstance.bounds.width, objInstance.bounds.height)
		}

		objInstance.objectShape = objectShape
		layer.objects.add(objInstance)
	}

	return layer
}

private fun Xml.parseImageLayer(): Layer.Image {
	val layer = Layer.Image()
	parseCommonLayerData(layer)
	layer.image = child("image")?.parseImage()
	return layer
}

private fun Xml.parseGroupLayer(infinite: Boolean): Layer.Group {
	val layer = Layer.Group()
	parseCommonLayerData(layer)

	allChildrenNoComments.fastForEach { element ->
		when (element.nameLC) {
			"layer" -> {
				val tileLayer = element.parseTileLayer(infinite)
				layer.layers += tileLayer
			}
			"objectgroup" -> {
				val objectLayer = element.parseObjectLayer()
				layer.layers += objectLayer
			}
			"imagelayer" -> {
				val imageLayer = element.parseImageLayer()
				layer.layers += imageLayer
			}
			"group" -> {
				val groupLayer = element.parseGroupLayer(infinite)
				layer.layers += groupLayer
			}
		}
	}
	return layer
}

private fun Xml.parseImage(): Image? {
	val image = this
	val width = image.int("width")
	val height = image.int("height")
	val trans = image.str("trans")
	val transparent = when {
		trans.isEmpty() -> null
		trans.startsWith("#") -> Colors[trans]
		else -> Colors["#$trans"]
	}
	val source = image.str("source")
	return if (source.isNotEmpty()) {
		Image.External(
			source = source,
			width = width,
			height = height,
			transparent = transparent
		)
	} else {
		val data = image.child("data") ?: return null
		val enc = data.strNull("encoding")
		val com = data.strNull("compression")
		val encoding = Encoding.values().find { it.value == enc } ?: Encoding.XML
		val compression = Compression.values().find { it.value == com } ?: Compression.NO
		//TODO: read embedded image (png, jpg, etc.) and convert to bitmap
		val bitmap = Bitmap32(width, height, premultiplied = true)
		Image.Embedded(
			format = image.str("format"),
			image = bitmap,
			encoding = encoding,
			compression = compression,
			transparent = transparent
		)
	}
}

private fun Xml.parseProperties(): Map<String, Property> {
	val out = LinkedHashMap<String, Property>()
	for (property in this.children("property")) {
		val pname = property.str("name")
		val rawValue = property.strNull("value") ?: property.text
		val type = property.str("type", "string")
		val pvalue = when (type) {
			"string" -> Property.StringT(rawValue)
			"int" -> Property.IntT(rawValue.toIntOrNull() ?: 0)
			"float" -> Property.FloatT(rawValue.toDoubleOrNull() ?: 0.0)
			"bool" -> Property.BoolT(rawValue == "true")
			"color" -> Property.ColorT(colorFromARGB(rawValue, Colors.TRANSPARENT_BLACK))
			"file" -> Property.FileT(if (rawValue.isEmpty()) "." else rawValue)
			"object" -> Property.ObjectT(rawValue.toIntOrNull() ?: 0)
			else -> Property.StringT(rawValue)
		}
		out[pname] = pvalue
	}
	return out
}

//TODO: move to korim
fun colorFromARGB(color: String, default: RGBA): RGBA {
	if (!color.startsWith('#') || color.length != 9 && color.length != 7) return default
	val hex = color.substring(1)
	val start = if (color.length == 7) 0 else 2
	val a = if (color.length == 9) hex.substr(0, 2).toInt(16) else 0xFF
	val r = hex.substr(start + 0, 2).toInt(16)
	val g = hex.substr(start + 2, 2).toInt(16)
	val b = hex.substr(start + 4, 2).toInt(16)
	return RGBA(r, g, b, a)
}

private val spaces = Regex("\\s+")
