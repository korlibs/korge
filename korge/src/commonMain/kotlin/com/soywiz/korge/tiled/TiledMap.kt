package com.soywiz.korge.tiled

import com.soywiz.kds.iterators.*
import com.soywiz.klogger.*
import com.soywiz.kmem.*
import com.soywiz.korge.view.tiles.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korim.color.*
import com.soywiz.korim.format.*
import com.soywiz.korio.compression.*
import com.soywiz.korio.compression.deflate.*
import com.soywiz.korio.file.*
import com.soywiz.korio.lang.*
import com.soywiz.korio.serialization.xml.*
import com.soywiz.korio.util.*
import com.soywiz.korio.util.encoding.*
import com.soywiz.korma.geom.*
import kotlin.collections.set

class TiledMapData {
	var width = 0
	var height = 0
	var tilewidth = 0
	var tileheight = 0
	val pixelWidth: Int get() = width * tilewidth
	val pixelHeight: Int get() = height * tileheight
	val allLayers = arrayListOf<TiledMap.Layer>()
	inline val patternLayers get() = allLayers.patterns
	inline val imageLayers get() = allLayers.images
	inline val objectLayers get() = allLayers.objects
	val tilesets = arrayListOf<TileSetData>()

	fun getObjectByName(name: String) = objectLayers.mapNotNull { it.getByName(name) }.firstOrNull()

	val maxGid get() = tilesets.map { it.firstgid + it.tilecount }.max() ?: 0
}

fun TiledMap.Layer.Objects.Object.getPos(map: TiledMapData): IPoint =
	IPoint(bounds.x / map.tilewidth, bounds.y / map.tileheight)

fun TiledMapData?.getObjectPosByName(name: String): IPoint? {
	val obj = this?.getObjectByName(name) ?: return null
	return obj.getPos(this)
}

data class TileSetData(
	val name: String,
	val firstgid: Int,
	val tilewidth: Int,
	val tileheight: Int,
	val tilecount: Int,
	val columns: Int,
	val image: Xml?,
	val source: String,
	val width: Int,
	val height: Int
)

//e: java.lang.UnsupportedOperationException: Class literal annotation arguments are not yet supported: Factory
//@AsyncFactoryClass(TiledMapFactory::class)
class TiledMap(
	val data: TiledMapData,
	val tilesets: List<TiledTileset>,
	val tileset: TileSet
) {
	val width get() = data.width
	val height get() = data.height
	val tilewidth get() = data.tilewidth
	val tileheight get() = data.tileheight
	val pixelWidth: Int get() = data.pixelWidth
	val pixelHeight: Int get() = data.pixelHeight
	val allLayers get() = data.allLayers
	val patternLayers get() = data.patternLayers
	val imageLayers get() = data.imageLayers
	val objectLayers get() = data.objectLayers

	class TiledTileset(val tileset: TileSet, val firstgid: Int = 0) {
	}

	sealed class Layer {
		var name: String = ""
		var visible: Boolean = true
		var draworder: String = ""
		var color: RGBA = Colors.WHITE
		var opacity = 1.0
		var offsetx: Double = 0.0
		var offsety: Double = 0.0
		val properties = hashMapOf<String, Any>()

		class Patterns : Layer() {
			//val tilemap = TileMap(Bitmap32(0, 0), )
			var map: Bitmap32 = Bitmap32(0, 0)
		}

		data class ObjectInfo(
			val id: Int, val name: String, val type: String,
			val bounds: IRectangleInt,
			val objprops: Map<String, Any>
		)

		class Objects : Layer() {
			interface Object {
				val info: ObjectInfo
			}

			interface Poly : Object {
				val points: List<IPoint>
			}

			data class Rect(override val info: ObjectInfo) : Object
			data class Ellipse(override val info: ObjectInfo) : Object
			data class Polyline(override val info: ObjectInfo, override val points: List<IPoint>) : Poly
			data class Polygon(override val info: ObjectInfo, override val points: List<IPoint>) : Poly

			val objects = arrayListOf<Object>()
			val objectsById by lazy { objects.associateBy { it.id } }
			val objectsByName by lazy { objects.associateBy { it.name } }

			fun getById(id: Int): Object? = objectsById[id]
			fun getByName(name: String): Object? = objectsByName[name]
		}

		class Image : Layer() {
			var width = 0
			var height = 0
			var source = ""
			var image: Bitmap = Bitmap32(0, 0)
		}
	}
}

val TiledMap.Layer.Objects.Object.id get() = this.info.id
val TiledMap.Layer.Objects.Object.name get() = this.info.name
val TiledMap.Layer.Objects.Object.bounds get() = this.info.bounds
val TiledMap.Layer.Objects.Object.objprops get() = this.info.objprops

inline val Iterable<TiledMap.Layer>.patterns get() = this.filterIsInstance<TiledMap.Layer.Patterns>()
inline val Iterable<TiledMap.Layer>.images get() = this.filterIsInstance<TiledMap.Layer.Image>()
inline val Iterable<TiledMap.Layer>.objects get() = this.filterIsInstance<TiledMap.Layer.Objects>()

private val spaces = Regex("\\s+")

val tilemapLog = Logger("tilemap")

class TiledFile(val name: String)

private fun Xml.parseProperties(): Map<String, Any> {
	val out = LinkedHashMap<String, Any>()
	for (property in this.children("property")) {
		val pname = property.str("name")
		val rawValue = if (property.hasAttribute("value")) property.str("value") else property.text
		val type = property.str("type", "text")
		val pvalue: Any = when (type) {
			"bool" -> rawValue == "true"
			"color" -> Colors[rawValue]
			"text" -> rawValue
			"int" -> rawValue.toIntOrNull() ?: 0
			"float" -> rawValue.toDoubleOrNull() ?: 0.0
			"file" -> TiledFile(pname)
			else -> rawValue
		}
		out[pname] = pvalue
		//println("$pname: $pvalue")
	}
	return out
}

suspend fun VfsFile.readTiledMapData(): TiledMapData {
	val log = tilemapLog
	val file = this
	val folder = this.parent.jail()
	val tiledMap = TiledMapData()
	val mapXml = file.readXml()

	if (mapXml.nameLC != "map") error("Not a TiledMap XML TMX file starting with <map>")

	tiledMap.width = mapXml.getInt("width") ?: 0
	tiledMap.height = mapXml.getInt("height") ?: 0
	tiledMap.tilewidth = mapXml.getInt("tilewidth") ?: 32
	tiledMap.tileheight = mapXml.getInt("tileheight") ?: 32

	tilemapLog.trace { "tilemap: width=${tiledMap.width}, height=${tiledMap.height}, tilewidth=${tiledMap.tilewidth}, tileheight=${tiledMap.tileheight}" }
	tilemapLog.trace { "tilemap: $tiledMap" }

	val elements = mapXml.allChildrenNoComments

	tilemapLog.trace { "tilemap: elements=${elements.size}" }
	tilemapLog.trace { "tilemap: elements=$elements" }

	var maxGid = 1
	//var lastBaseTexture = views.transparentTexture.base

	elements.fastForEach { element ->
		val elementName = element.nameLC
		@Suppress("IntroduceWhenSubject") // @TODO: BUG IN KOTLIN-JS with multicase in suspend functions
		when {
			elementName == "tileset" -> {
				tilemapLog.trace { "tileset" }
				val firstgid = element.int("firstgid")

				// TSX file
				val element = if (element.hasAttribute("source")) {
					folder[element.str("source")].readXml()
				} else {
					element
				}

				val name = element.str("name")
				val tilewidth = element.int("tilewidth")
				val tileheight = element.int("tileheight")
				val tilecount = element.int("tilecount", -1)
				val columns = element.int("columns", -1)
				val image = element.child("image")
				val source = image?.str("source") ?: ""
				val width = image?.int("width", 0) ?: 0
				val height = image?.int("height", 0) ?: 0

				tiledMap.tilesets += TileSetData(
					name = name,
					firstgid = firstgid,
					tilewidth = tilewidth,
					tileheight = tileheight,
					tilecount = tilecount,
					columns = columns,
					image = image,
					source = source,
					width = width,
					height = height
				)

				//lastBaseTexture = tex.base
			}
			elementName == "layer" || elementName == "objectgroup" || elementName == "imagelayer" -> {
				tilemapLog.trace { "layer:$elementName" }
				val layer = when (element.nameLC) {
					"layer" -> TiledMap.Layer.Patterns()
					"objectgroup" -> TiledMap.Layer.Objects()
					"imagelayer" -> TiledMap.Layer.Image()
					else -> invalidOp
				}
				tiledMap.allLayers += layer
				layer.name = element.str("name")
				layer.visible = element.int("visible", 1) != 0
				layer.draworder = element.str("draworder", "")
				layer.color = Colors[element.str("color", "#ffffff")]
				layer.opacity = element.double("opacity", 1.0)
				layer.offsetx = element.double("offsetx", 0.0)
				layer.offsety = element.double("offsety", 0.0)

				val properties = element.child("properties")?.parseProperties()
				if (properties != null) {
					layer.properties.putAll(properties)
				}

				when (layer) {
					is TiledMap.Layer.Patterns -> {
						val width = element.int("width")
						val height = element.int("height")
						val count = width * height
						val data = element.child("data")
						val encoding = data?.str("encoding", "") ?: ""
						val compression = data?.str("compression", "") ?: ""
						@Suppress("IntroduceWhenSubject") // @TODO: BUG IN KOTLIN-JS with multicase in suspend functions
						val tilesArray: IntArray = when {
							encoding == "" || encoding == "xml" -> {
								val items = data?.children("tile")?.map { it.uint("gid") } ?: listOf()
								items.toIntArray()
							}
							encoding == "csv" -> {
								val content = data?.text ?: ""
								val items = content.replace(spaces, "").split(',').map { it.toUInt().toInt() }
								items.toIntArray()
							}
							encoding == "base64" -> {
								val base64Content = (data?.text ?: "").trim()
								val rawContent = base64Content.fromBase64()

								val content = when (compression) {
									"" -> rawContent
									"gzip" -> rawContent.uncompress(GZIP)
									"zlib" -> rawContent.uncompress(ZLib)
									else -> invalidOp
								}
								content.readIntArrayLE(0, count)
							}
							else -> invalidOp("Unhandled encoding '$encoding'")
						}
						if (tilesArray.size != count) invalidOp("")
						layer.map = Bitmap32(width, height, RgbaArray(tilesArray))
					}
					is TiledMap.Layer.Image -> {
						for (image in element.children("image")) {
							layer.source = image.str("source")
							layer.width = image.int("width")
							layer.height = image.int("height")
						}
					}
					is TiledMap.Layer.Objects -> {
						for (obj in element.children("object")) {
							val id = obj.int("id")
							val name = obj.str("name")
							val type = obj.str("type")
							val bounds = obj.run { IRectangleInt(int("x"), int("y"), int("width"), int("height")) }
							var rkind = RKind.RECT
							var points = listOf<IPoint>()
							var objprops: Map<String, Any> = LinkedHashMap()

							for (kind in obj.allNodeChildren) {
								val kindType = kind.nameLC
								@Suppress("IntroduceWhenSubject") // @TODO: BUG IN KOTLIN-JS with multicase in suspend functions
								when {
									kindType == "ellipse" -> {
										rkind = RKind.ELLIPSE
									}
									kindType == "polyline" || kindType == "polygon" -> {
										val pointsStr = kind.str("points")
										points = pointsStr.split(spaces).map {
											val parts = it.split(',').map { it.trim().toDoubleOrNull() ?: 0.0 }
											IPoint(parts[0], parts[1])
										}

										rkind = (if (kindType == "polyline") RKind.POLYLINE else RKind.POLYGON)
									}
									kindType == "properties" -> {
										objprops = kind.parseProperties()
									}
									else -> invalidOp("Invalid object kind '$kindType'")
								}
							}

							val info = TiledMap.Layer.ObjectInfo(id, name, type, bounds, objprops)
							layer.objects += when (rkind) {
								RKind.RECT -> TiledMap.Layer.Objects.Rect(info)
								RKind.ELLIPSE -> TiledMap.Layer.Objects.Ellipse(info)
								RKind.POLYLINE -> TiledMap.Layer.Objects.Polyline(info, points)
								RKind.POLYGON -> TiledMap.Layer.Objects.Polygon(info, points)
							}
						}
					}
				}
			}
		}
	}

	return tiledMap
}

private fun Xml.uint(name: String, defaultValue: Int = 0): Int = this.attributesLC[name]?.toUIntOrNull()?.toInt() ?: defaultValue

suspend fun VfsFile.readTiledMap(
	hasTransparentColor: Boolean = false,
	transparentColor: RGBA = Colors.FUCHSIA,
	createBorder: Int = 1
): TiledMap {
	val folder = this.parent.jail()
	val data = readTiledMapData()

	//val combinedTileset = kotlin.arrayOfNulls<Texture>(data.maxGid + 1)
	val combinedTileset = arrayOfNulls<BmpSlice>(data.maxGid + 1)

	data.imageLayers.fastForEach { layer ->
		layer.image = folder[layer.source].readBitmapOptimized()
	}

	val tiledTilesets = arrayListOf<TiledMap.TiledTileset>()

	data.tilesets.fastForEach { tileset ->
		var bmp = folder[tileset.source].readBitmapOptimized()

		// @TODO: Preprocess this, so in JS we don't have to do anything!
		if (hasTransparentColor) {
			bmp = bmp.toBMP32()
			for (n in 0 until bmp.area) {
				if (bmp.data[n] == transparentColor) bmp.data[n] = Colors.TRANSPARENT_BLACK
			}
		}

		val ptileset = if (createBorder > 0) {
			bmp = bmp.toBMP32()

			val slices =
				TileSet.extractBitmaps(bmp, tileset.tilewidth, tileset.tileheight, tileset.columns, tileset.tilecount)

			TileSet.fromBitmaps(
				tileset.tilewidth, tileset.tileheight,
				slices,
				border = createBorder,
				mipmaps = false
			)
		} else {
			TileSet(bmp.slice(), tileset.tilewidth, tileset.tileheight, tileset.columns, tileset.tilecount)
		}

		val tiledTileset = TiledMap.TiledTileset(
			tileset = ptileset,
			firstgid = tileset.firstgid
		)

		tiledTilesets += tiledTileset

		//lastBaseTexture = tex.base
		tilemapLog.trace { "tileset:$tiledTileset" }

		for (n in 0 until ptileset.textures.size) {
			combinedTileset[tileset.firstgid + n] = ptileset.textures[n]
		}
	}

	return TiledMap(data, tiledTilesets, TileSet(combinedTileset.toList(), data.tilewidth, data.tileheight))
}

private enum class RKind {
	RECT, ELLIPSE, POLYLINE, POLYGON
}
