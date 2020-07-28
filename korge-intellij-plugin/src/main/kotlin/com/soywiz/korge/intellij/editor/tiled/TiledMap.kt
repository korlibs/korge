// @TODO: @WARNING: Duplicated from KorGE to be able to modify it. Please, copy again to KorGE once this is stable
package com.soywiz.korge.intellij.editor.tiled

import com.soywiz.klogger.*
import com.soywiz.korge.view.tiles.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korim.color.*
import com.soywiz.korma.geom.*
import java.util.*
import kotlin.collections.LinkedHashMap

class TiledMapData(
	var orientation: TiledMap.Orientation = TiledMap.Orientation.ORTHOGONAL,
	//TODO: support render order
	var renderOrder: TiledMap.RenderOrder = TiledMap.RenderOrder.RIGHT_DOWN,
	var compressionLevel: Int = -1,
	var width: Int = 0,
	var height: Int = 0,
	var tilewidth: Int = 0,
	var tileheight: Int = 0,
	var hexSideLength: Int? = null,
	var staggerAxis: TiledMap.StaggerAxis? = null,
	var staggerIndex: TiledMap.StaggerIndex? = null,
	var backgroundColor: RGBA? = null,
	var nextLayerId: Int = 1,
	var nextObjectId: Int = 1,
	var infinite: Boolean = false,
	val properties: MutableMap<String, TiledMap.Property> = mutableMapOf(),
	val allLayers: MutableList<TiledMap.Layer> = arrayListOf(),
	val tilesets: MutableList<TileSetData> = arrayListOf(),
	var editorSettings: TiledMap.EditorSettings? = null
) {
	val pixelWidth: Int get() = width * tilewidth
	val pixelHeight: Int get() = height * tileheight
	inline val tileLayers get() = allLayers.tiles
	inline val imageLayers get() = allLayers.images
	inline val objectLayers get() = allLayers.objects

	val maxGid get() = tilesets.map { it.firstgid + it.tileCount }.max() ?: 0

	fun getObjectByName(name: String) = objectLayers.mapNotNull { it.getByName(name) }.firstOrNull()

	fun clone() = TiledMapData(
		orientation, renderOrder, compressionLevel,
		width, height, tilewidth, tileheight,
		hexSideLength, staggerAxis, staggerIndex,
		backgroundColor, nextLayerId, nextObjectId, infinite,
		LinkedHashMap(properties),
		allLayers.map { it.clone() }.toMutableList(),
		tilesets.map { it.clone() }.toMutableList()
	)
}

fun TiledMap.Object.getPos(map: TiledMapData) = Point(bounds.x / map.tilewidth, bounds.y / map.tileheight)

fun TiledMapData.getObjectPosByName(name: String) = getObjectByName(name)?.getPos(this)

data class TerrainData(
	val name: String,
	val tile: Int,
	val properties: Map<String, TiledMap.Property> = mapOf()
)

data class AnimationFrameData(
	val tileId: Int, val duration: Int
)

data class TerrainInfo(val info: List<Int?>) {
	operator fun get(x: Int, y: Int): Int? = if (x in 0..1 && y in 0..1) info[y * 2 + x] else null
}

class WangSet(
	val name: String,
	val tileId: Int,
	val properties: Map<String, TiledMap.Property> = mapOf(),
	val cornerColors: List<WangColor> = listOf(),
	val edgeColors: List<WangColor> = listOf(),
	val wangtiles: List<WangTile> = listOf()
) {
	class WangColor(
		val name: String,
		val color: RGBA,
		val tileId: Int,
		val probability: Double = 0.0
	)

	class WangTile(
		val tileId: Int,
		val wangId: Int,
		val hflip: Boolean = false,
		val vflip: Boolean = false,
		val dflip: Boolean = false
	)
}

data class TileData(
	val id: Int,
	val type: Int = -1,
	val terrain: List<Int?>? = null,
	val probability: Double = 0.0,
	val image: TiledMap.Image? = null,
	val properties: Map<String, TiledMap.Property> = mapOf(),
	val objectGroup: TiledMap.Layer.Objects? = null,
	val frames: List<AnimationFrameData>? = null
) {
	val terrainInfo = TerrainInfo(terrain ?: listOf(null, null, null, null))
}

data class TileSetData(
	val name: String,
	val firstgid: Int,
	val tileWidth: Int,
	val tileHeight: Int,
	val tileCount: Int,
	val spacing: Int,
	val margin: Int,
	val columns: Int,
	val image: TiledMap.Image?,
	val tileOffsetX: Int = 0,
	val tileOffsetY: Int = 0,
	val grid: TiledMap.Grid? = null,
	val tilesetSource: String? = null,
	val objectAlignment: TiledMap.ObjectAlignment = TiledMap.ObjectAlignment.UNSPECIFIED,
	val terrains: List<TerrainData> = listOf(),
	val wangsets: List<WangSet> = listOf(),
	val tiles: List<TileData> = listOf(),
	val properties: Map<String, TiledMap.Property> = mapOf()
) {
	val width: Int get() = image?.width ?: 0
	val height: Int get() = image?.height ?: 0
	fun clone() = copy()
}

//e: java.lang.UnsupportedOperationException: Class literal annotation arguments are not yet supported: Factory
//@AsyncFactoryClass(TiledMapFactory::class)
class TiledMap constructor(
	var data: TiledMapData,
	var tilesets: MutableList<TiledTileset>
) {
	val width get() = data.width
	val height get() = data.height
	val tilewidth get() = data.tilewidth
	val tileheight get() = data.tileheight
	val pixelWidth: Int get() = data.pixelWidth
	val pixelHeight: Int get() = data.pixelHeight
	val allLayers get() = data.allLayers
	val tileLayers get() = data.tileLayers
	val imageLayers get() = data.imageLayers
	val objectLayers get() = data.objectLayers
	val nextGid get() = tilesets.map { it.firstgid + it.tileset.textures.size }.max() ?: 1

	fun clone() = TiledMap(data.clone(), tilesets.map { it.clone() }.toMutableList())

	enum class Orientation(val value: String) {
		ORTHOGONAL("orthogonal"),
		ISOMETRIC("isometric"),
		STAGGERED("staggered"),
		HEXAGONAL("hexagonal")
	}

	enum class RenderOrder(val value: String) {
		RIGHT_DOWN("right-down"),
		RIGHT_UP("right-up"),
		LEFT_DOWN("left-down"),
		LEFT_UP("left-up")
	}

	enum class StaggerAxis(val value: String) {
		X("x"), Y("y")
	}

	enum class StaggerIndex(val value: String) {
		EVEN("even"), ODD("odd")
	}

	enum class ObjectAlignment(val value: String) {
		UNSPECIFIED("unspecified"),
		TOP_LEFT("topleft"),
		TOP("top"),
		TOP_RIGHT("topright"),
		LEFT("left"),
		CENTER("center"),
		RIGHT("right"),
		BOTTOM_LEFT("bottomleft"),
		BOTTOM("bottom"),
		BOTTOM_RIGHT("bottomright")
	}

	class Grid(
		val cellWidth: Int,
		val cellHeight: Int,
		val orientation: Orientation = Orientation.ORTHOGONAL
	) {
		enum class Orientation(val value: String) {
			ORTHOGONAL("orthogonal"),
			ISOMETRIC("isometric")
		}
	}

	data class Object(
		val id: Int,
		var gid: Int?,
		var name: String,
		var type: String,
		var bounds: Rectangle,
		var rotation: Double, // in degrees
		var visible: Boolean,
		var objectType: Type = Type.Rectangle,
		val properties: MutableMap<String, Property> = mutableMapOf()
	) {
		enum class DrawOrder(val value: String) {
			INDEX("index"), TOP_DOWN("topdown")
		}

		sealed class Type {
			object Rectangle : Type()
			object Ellipse : Type()
			object PPoint : Type()
			class Polygon(val points: List<Point>) : Type()
			class Polyline(val points: List<Point>) : Type()
			class Text(
				val fontFamily: String,
				val pixelSize: Int,
				val wordWrap: Boolean,
				val color: RGBA,
				val bold: Boolean,
				val italic: Boolean,
				val underline: Boolean,
				val strikeout: Boolean,
				val kerning: Boolean,
				val hAlign: TextHAlignment,
				val vAlign: TextVAlignment
			) : Type()
		}
	}

	enum class TextHAlignment(val value: String) {
		LEFT("left"), CENTER("center"), RIGHT("right"), JUSTIFY("justify")
	}

	enum class TextVAlignment(val value: String) {
		TOP("top"), CENTER("center"), BOTTOM("bottom")
	}

	sealed class Image(val width: Int, val height: Int, val transparent: RGBA? = null) {
		class Embedded(
			val format: String,
			val image: Bitmap32,
			val encoding: Encoding,
			val compression: Compression,
			transparent: RGBA? = null
		) : Image(image.width, image.height, transparent)

		class External(
			val source: String,
			width: Int,
			height: Int,
			transparent: RGBA? = null
		) : Image(width, height, transparent)
	}

	enum class Encoding(val value: String?) {
		BASE64("base64"), CSV("csv"), XML(null)
	}

	enum class Compression(val value: String?) {
		NO(null), GZIP("gzip"), ZLIB("zlib"), ZSTD("zstd")
	}

	sealed class Property {
		class StringT(var value: String) : Property()
		class IntT(var value: Int) : Property()
		class FloatT(var value: Double) : Property()
		class BoolT(var value: Boolean) : Property()
		class ColorT(var value: RGBA) : Property()
		class FileT(var path: String) : Property()
		class ObjectT(var id: Int) : Property()
	}

	data class TiledTileset(
		val tileset: TileSet,
		val data: TileSetData = TileSetData(
			name = "unknown",
			firstgid = 1,
			tileWidth = tileset.width,
			tileHeight = tileset.height,
			tileCount = tileset.textures.size,
			spacing = 0,
			margin = 0,
			columns = tileset.base.width / tileset.width,
			image = TODO(), //null
			//width = tileset.base.width,
			//height = tileset.base.height,
			terrains = listOf(),
			tiles = tileset.textures.mapIndexed { index, bmpSlice -> TileData(index) }
		),
		val firstgid: Int = 1
	) {
		fun clone(): TiledTileset = TiledTileset(tileset.clone(), data.clone(), firstgid)
	}

	sealed class Layer {
		var id: Int = 1
		var name: String = ""
		var visible: Boolean = true
		var locked: Boolean = false
		var opacity = 1.0
		var tintColor: RGBA? = null
		var offsetx: Double = 0.0
		var offsety: Double = 0.0
		val properties: MutableMap<String, Property> = mutableMapOf()

		open fun copyFrom(other: Layer) {
			this.id = other.id
			this.name = other.name
			this.visible = other.visible
			this.locked = other.locked
			this.opacity = other.opacity
			this.tintColor = other.tintColor
			this.offsetx = other.offsetx
			this.offsety = other.offsety
			this.properties.clear()
			this.properties.putAll(other.properties)
		}

		abstract fun clone(): Layer

		class Tiles(
			var map: Bitmap32 = Bitmap32(0, 0),
			var encoding: Encoding = Encoding.XML,
			var compression: Compression = Compression.NO
		) : Layer() {
			val width: Int get() = map.width
			val height: Int get() = map.height
			val area: Int get() = width * height
			operator fun set(x: Int, y: Int, value: Int) = run { map.setInt(x, y, value) }
			operator fun get(x: Int, y: Int): Int = map.getInt(x, y)
			override fun clone(): Tiles = Tiles(map.clone(), encoding, compression).also { it.copyFrom(this) }
		}

		class Objects(
			var color: RGBA = Colors.WHITE,
			var drawOrder: Object.DrawOrder = Object.DrawOrder.TOP_DOWN,
			val objects: MutableList<Object> = arrayListOf()
		) : Layer() {
			val objectsById by lazy { objects.associateBy { it.id } }
			val objectsByName by lazy { objects.associateBy { it.name } }

			fun getById(id: Int): Object? = objectsById[id]
			fun getByName(name: String): Object? = objectsByName[name]

			override fun clone() = Objects(color, drawOrder, ArrayList(objects)).also { it.copyFrom(this) }
		}

		class Image(var image: TiledMap.Image? = null) : Layer() {
			override fun clone(): Image = Image(image).also { it.copyFrom(this) }
		}

		class Group(
			val layers: MutableList<Layer> = arrayListOf()
		) : Layer() {
			override fun clone(): Group = Group(ArrayList(layers)).also { it.copyFrom(this) }
		}
	}

	class EditorSettings(
		val chunkWidth: Int = 16,
		val chunkHeight: Int = 16
	)
}

private fun TileSet.clone(): TileSet = TileSet(this.textures, this.width, this.height, this.base)

inline val Iterable<TiledMap.Layer>.tiles get() = this.filterIsInstance<TiledMap.Layer.Tiles>()
inline val Iterable<TiledMap.Layer>.images get() = this.filterIsInstance<TiledMap.Layer.Image>()
inline val Iterable<TiledMap.Layer>.objects get() = this.filterIsInstance<TiledMap.Layer.Objects>()

val tilemapLog = Logger("tilemap")
