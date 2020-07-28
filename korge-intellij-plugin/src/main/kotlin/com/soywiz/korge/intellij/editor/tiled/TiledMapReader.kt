package com.soywiz.korge.intellij.editor.tiled

import com.soywiz.kds.iterators.*
import com.soywiz.kmem.*
import com.soywiz.korge.intellij.editor.tiled.TiledMap.*
import com.soywiz.korge.view.tiles.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korim.color.*
import com.soywiz.korim.format.*
import com.soywiz.korio.compression.*
import com.soywiz.korio.compression.deflate.*
import com.soywiz.korio.file.*
import com.soywiz.korio.lang.*
import com.soywiz.korio.serialization.xml.*
import com.soywiz.korio.util.encoding.*
import com.soywiz.korma.geom.*
import kotlin.collections.set

suspend fun VfsFile.readTiledMap(
	hasTransparentColor: Boolean = false,
	transparentColor: RGBA = Colors.FUCHSIA,
	createBorder: Int = 1
): TiledMap {
	val folder = this.parent.jail()
	val data = readTiledMapData()

	val tiledTilesets = arrayListOf<TiledTileset>()

	data.tilesets.fastForEach { tileset ->
		tiledTilesets += tileset.toTiledSet(folder, hasTransparentColor, transparentColor, createBorder)
	}

	return TiledMap(data, tiledTilesets)
}

suspend fun VfsFile.readTileSetData(firstgid: Int = 1): TileSetData {
	return parseTileSetData(this.readXml(), firstgid, this.baseName)
}

suspend fun TileSetData.toTiledSet(
	folder: VfsFile,
	hasTransparentColor: Boolean = false,
	transparentColor: RGBA = Colors.FUCHSIA,
	createBorder: Int = 1
): TiledTileset {
	val tileset = this
	var bmp = try {
		when (tileset.image) {
			is Image.Embedded -> TODO()
			is Image.External -> folder[tileset.image.source].readBitmapOptimized()
			null -> Bitmap32(0, 0)
		}
	} catch (e: Throwable) {
		e.printStackTrace()
		Bitmap32(tileset.width, tileset.height)
	}

	// @TODO: Preprocess this, so in JS we don't have to do anything!
	if (hasTransparentColor) {
		bmp = bmp.toBMP32()
		for (n in 0 until bmp.area) {
			if (bmp.data[n] == transparentColor) bmp.data[n] = Colors.TRANSPARENT_BLACK
		}
	}

	val ptileset = if (createBorder > 0) {
		bmp = bmp.toBMP32()

		if (tileset.spacing >= createBorder) {
			// There is already separation between tiles, use it as it is
			val slices = TileSet.extractBmpSlices(
				bmp,
				tileset.tileWidth,
				tileset.tileHeight,
				tileset.columns,
				tileset.tileCount,
				tileset.spacing,
				tileset.margin
			)
			TileSet(slices, tileset.tileWidth, tileset.tileHeight, bmp)
		} else {
			// No separation between tiles: create a new Bitmap adding that separation
			val bitmaps = TileSet.extractBitmaps(
				bmp,
				tileset.tileWidth,
				tileset.tileHeight,
				tileset.columns,
				tileset.tileCount,
				tileset.spacing,
				tileset.margin
			)
			TileSet.fromBitmaps(tileset.tileWidth, tileset.tileHeight, bitmaps, border = createBorder, mipmaps = false)
		}
	} else {
		TileSet(bmp.slice(), tileset.tileWidth, tileset.tileHeight, tileset.columns, tileset.tileCount)
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
	val folder = this.parent.jail()
	val tiledMap = TiledMapData()
	val mapXml = file.readXml()

	if (mapXml.nameLC != "map") error("Not a TiledMap XML TMX file starting with <map>")

	//TODO: Support different orientations
	val orientation = mapXml.getString("orientation")
	tiledMap.orientation = when (orientation) {
		"orthogonal" -> TiledMap.Orientation.ORTHOGONAL
		else -> unsupported("Orientation \"$orientation\" is not supported")
	}
	val renderOrder = mapXml.getString("renderorder")
	tiledMap.renderOrder = when (renderOrder) {
		"right-down" -> RenderOrder.RIGHT_DOWN
		"right-up" -> RenderOrder.RIGHT_UP
		"left-down" -> RenderOrder.LEFT_DOWN
		"left-up" -> RenderOrder.LEFT_UP
		else -> RenderOrder.RIGHT_DOWN
	}
	tiledMap.compressionLevel = mapXml.getInt("compressionlevel") ?: -1
	tiledMap.width = mapXml.getInt("width") ?: 0
	tiledMap.height = mapXml.getInt("height") ?: 0
	tiledMap.tilewidth = mapXml.getInt("tilewidth") ?: 32
	tiledMap.tileheight = mapXml.getInt("tileheight") ?: 32
	tiledMap.hexSideLength = mapXml.getInt("hexsidelength")
	val staggerAxis = mapXml.getString("staggeraxis")
	tiledMap.staggerAxis = when (staggerAxis) {
		"x" -> StaggerAxis.X
		"y" -> StaggerAxis.Y
		else -> null
	}
	val staggerIndex = mapXml.getString("staggerindex")
	tiledMap.staggerIndex = when (staggerIndex) {
		"even" -> StaggerIndex.EVEN
		"odd" -> StaggerIndex.ODD
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
	val objectAlignment = ObjectAlignment.values().find { it.value == alignment } ?: ObjectAlignment.UNSPECIFIED
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
		type = tile.int("type", -1),
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

	val map: Bitmap32
	val encoding: Encoding
	val compression: Compression

	if (data == null) {
		map = Bitmap32(width, height)
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
				children("tile").map { it.uint("gid").toInt() }.toIntArray()
			}
			Encoding.CSV -> {
				text.replace(spaces, "").split(',').map { it.toUInt().toInt() }.toIntArray()
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

		val tiles: IntArray
		if (infinite) {
			tiles = IntArray(count)
			data.children("chunk").forEach { chunk ->
				val offsetX = chunk.int("x")
				val offsetY = chunk.int("y")
				val cwidth = chunk.int("width")
				val cheight = chunk.int("height")
				chunk.encodeGids().forEachIndexed { i, gid ->
					val x = offsetX + i % cwidth
					val y = offsetY + i / cwidth
					tiles[x + y * (offsetX + cwidth)] = gid
				}
			}
		} else {
			tiles = data.encodeGids()
		}
		map = Bitmap32(width, height, RgbaArray(tiles))
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
		val objectType: Object.Type = when {
			ellipse != null -> Object.Type.Ellipse
			point != null -> Object.Type.PPoint
			polygon != null -> Object.Type.Polygon(polygon.readPoints())
			polyline != null -> Object.Type.Polyline(polyline.readPoints())
			text != null -> Object.Type.Text(
				fontFamily = text.str("fontfamily", "sans-serif"),
				pixelSize = text.int("pixelsize", 16),
				wordWrap = text.int("wrap", 0) == 1,
				color = colorFromARGB(text.str("color"), Colors.BLACK),
				bold = text.int("bold") == 1,
				italic = text.int("italic") == 1,
				underline = text.int("underline") == 1,
				strikeout = text.int("strikeout") == 1,
				kerning = text.int("kerning", 1) == 1,
				hAlign = text.str("halign", "left").let { align ->
					TextHAlignment.values().find { it.value == align } ?: TextHAlignment.LEFT
				},
				vAlign = text.str("valign", "top").let { align ->
					TextVAlignment.values().find { it.value == align } ?: TextVAlignment.TOP
				}
			)
			else -> Object.Type.Rectangle
		}

		objInstance.objectType = objectType
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
		val bitmap = Bitmap32(width, height)
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
		val rawValue = property.str("value")
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

//TODO: move to korio
private fun Xml.uint(name: String, defaultValue: UInt = 0u): UInt =
	this.attributesLC[name]?.toUIntOrNull() ?: defaultValue

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
