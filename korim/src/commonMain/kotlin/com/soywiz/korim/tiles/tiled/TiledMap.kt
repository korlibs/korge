package com.soywiz.korim.tiles.tiled

import com.soywiz.kds.*
import com.soywiz.kds.iterators.fastForEach
import com.soywiz.klogger.Logger
import com.soywiz.korim.bitmap.Bitmap32
import com.soywiz.korim.color.Colors
import com.soywiz.korim.color.RGBA
import com.soywiz.korim.text.TextAlignment
import com.soywiz.korim.tiles.TileMapObjectAlignment
import com.soywiz.korim.tiles.TileMapOrientation
import com.soywiz.korim.tiles.TileMapRenderOrder
import com.soywiz.korim.tiles.TileMapStaggerAxis
import com.soywiz.korim.tiles.TileMapStaggerIndex
import com.soywiz.korim.tiles.TileSet
import com.soywiz.korim.tiles.TileSetTileInfo
import com.soywiz.korim.tiles.TileShapeInfo
import com.soywiz.korio.lang.invalidArgument
import com.soywiz.korma.geom.*
import com.soywiz.korma.geom.shape.Shape2d
import com.soywiz.korma.geom.shape.buildVectorPath
import com.soywiz.korma.geom.shape.toShape2dNew
import com.soywiz.korma.geom.vector.VectorPath
import com.soywiz.korma.geom.vector.applyTransform
import com.soywiz.korma.geom.vector.ellipse
import com.soywiz.korma.geom.vector.polygon
import com.soywiz.korma.geom.vector.rect

class TiledMapData(
    var orientation: TileMapOrientation = TileMapOrientation.ORTHOGONAL,
    var renderOrder: TileMapRenderOrder = TileMapRenderOrder.RIGHT_DOWN,
    var compressionLevel: Int = -1,
    var width: Int = 0,
    var height: Int = 0,
    var tilewidth: Int = 0,
    var tileheight: Int = 0,
    var hexSideLength: Int? = null,
    var staggerAxis: TileMapStaggerAxis? = null,
    var staggerIndex: TileMapStaggerIndex? = null,
    var backgroundColor: RGBA? = null,
    var nextLayerId: Int = 1,
    var nextObjectId: Int = 1,
    var infinite: Boolean = false,
    val properties: MutableMap<String, TiledMap.Property> = mutableMapOf(),
    val allLayers: MutableList<TiledMap.Layer> = arrayListOf(),
    val tilesets: MutableList<TileSetData> = arrayListOf(),
    var editorSettings: TiledMap.EditorSettings? = null
) {
    val pixelWidth: Int
        get() {
            return when(orientation) {
                TileMapOrientation.ORTHOGONAL -> width * tilewidth
                TileMapOrientation.STAGGERED -> {
                    when(staggerAxis) {
                        TileMapStaggerAxis.X -> (tilewidth * (width / 2 + 0.5)).toInt()
                        TileMapStaggerAxis.Y -> tilewidth * width + tilewidth / 2
                        else -> invalidArgument("staggeraxis missing on staggered tiled map")
                    }
                }
                else -> TODO()
            }
        }
    val pixelHeight: Int
        get() {
            return when(orientation) {
                TileMapOrientation.ORTHOGONAL -> height * tileheight
                TileMapOrientation.STAGGERED -> {
                    (tileheight * (height / (if (staggerAxis == TileMapStaggerAxis.Y) 2.0 else 1.0) + 0.5)).toInt()
                }
                else -> TODO()
            }
        }
    inline val tileLayers get() = allLayers.tiles
    inline val imageLayers get() = allLayers.images
    inline val objectLayers get() = allLayers.objects

    val maxGid get() = tilesets.map { it.firstgid + it.tileCount }.maxOrNull() ?: 0

    fun getObjectByName(name: String) = objectLayers.firstNotNullOfOrNull { it.getByName(name) }
    fun getObjectByType(type: String) = objectLayers.flatMap { it.getByType(type) }

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
    val type: String? = null,
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
    val objectAlignment: TileMapObjectAlignment = TileMapObjectAlignment.UNSPECIFIED,
    val terrains: List<TerrainData> = listOf(),
    val wangsets: List<WangSet> = listOf(),
    val tiles: List<TileData> = listOf(),
    val properties: Map<String, TiledMap.Property> = mapOf()
) {
    val tilesById = tiles.associateByInt { _, it -> it.id }

    val width: Int get() = image?.width ?: 0
    val height: Int get() = image?.height ?: 0
    fun clone() = copy()
}

fun List<TiledMap.TiledTileset>.toTileSet(): TileSet {
    val tilesets = this
    val maxGid = tilesets.map { it.maxgid }.maxOrNull() ?: 0
    val tiles = IntMap<TileSetTileInfo>(maxGid * 2)
    val collisions = IntMap<TileShapeInfo>(maxGid * 2)
    tilesets.fastForEach { tileset ->
        tileset.tileset.texturesMap.fastForEach { key, value ->
            val id = tileset.firstgid + key
            tiles[id] = value.copy(id = id)
        }
        tileset.tileset.collisionsMap.fastForEach { key, value ->
            collisions[tileset.firstgid + key] = value
        }
    }
    return TileSet(tiles, collisionsMap = collisions)
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
    val nextGid get() = tilesets.map { it.firstgid + it.tileset.textures.size }.maxOrNull() ?: 1

    fun clone() = TiledMap(data.clone(), tilesets.map { it.clone() }.toMutableList())

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

    data class Object constructor(
        val id: Int,
        var gid: Int?,
        var name: String,
        var type: String,
        var bounds: Rectangle,
        var rotation: Double, // in degrees
        var visible: Boolean,
        var objectShape: Shape = Shape.PPoint,
        val properties: MutableMap<String, Property> = mutableMapOf()
    ) {
        val x: Double get() = bounds.x
        val y: Double get() = bounds.y

        //val transform get() = getTransform()

        fun strOrNull(propName: String) = properties[propName]?.string
        fun str(propName: String, default: String = "") = strOrNull(propName) ?: default
        fun int(propName: String, default: Int = 0) = properties[propName]?.int ?: default
        fun double(propName: String, default: Double = 0.0) = properties[propName]?.double ?: default
        fun bool(propName: String, default: Boolean = false) = properties[propName]?.bool ?: default

        enum class DrawOrder(val value: String) {
            INDEX("index"), TOP_DOWN("topdown")
        }

        fun getTransform() = Matrix()
            .pretranslate(bounds.x, bounds.y)
            .prerotate(rotation.degrees)

        fun toVectorPath(): VectorPath = objectShape.toVectorPath().clone().apply {
            applyTransform(getTransform())
        }

        fun toShape2dNoTransformed(): Shape2d = objectShape.toShape2d()

        sealed class Shape {
            abstract fun toVectorPath(): VectorPath
            open fun toShape2d(): Shape2d = toVectorPath().toShape2dNew()

            data class Rectangle(val width: Double, val height: Double) : Shape() {
                override fun toVectorPath(): VectorPath = buildVectorPath(VectorPath(), fun VectorPath.() {
                    rect(0.0, 0.0, width, height)
                })

                override fun toShape2d(): Shape2d = Shape2d.Rectangle(0.0, 0.0, width, height)
            }
            data class Ellipse(val width: Double, val height: Double) : Shape() {
                override fun toVectorPath(): VectorPath = buildVectorPath(VectorPath(), fun VectorPath.() {
                    ellipse(0.0, 0.0, width, height)
                })

                override fun toShape2d() = Shape2d.EllipseOrCircle(0.0, 0.0, width, height)
            }
            object PPoint : Shape() {
                override fun toVectorPath(): VectorPath = buildVectorPath(VectorPath(), fun VectorPath.() {
                })
            }
            data class Polygon(val points: List<Point>) : Shape() {
                override fun toVectorPath(): VectorPath = buildVectorPath(VectorPath(), fun VectorPath.() {
                    polygon(points)
                })
            }
            data class Polyline(val points: List<Point>) : Shape() {
                override fun toVectorPath(): VectorPath = buildVectorPath(VectorPath(), fun VectorPath.() {
                    polygon(points)
                })
            }
            data class Text(
                val fontFamily: String,
                val pixelSize: Int,
                val wordWrap: Boolean,
                val color: RGBA,
                val bold: Boolean,
                val italic: Boolean,
                val underline: Boolean,
                val strikeout: Boolean,
                val kerning: Boolean,
                val align: TextAlignment,
            ) : Shape() {
                val hAlign get() = align.horizontal
                val vAlign get() = align.vertical
                override fun toVectorPath(): VectorPath = buildVectorPath(VectorPath(), fun VectorPath.() {
                })
            }
        }
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
        abstract val string: String
        val int: Int get() = number.toInt()
        val double: Double get() = number.toDouble()
        val bool: Boolean get() = double != 0.0
        open val number: Number get() = string.toDoubleOrNull() ?: 0.0
        override fun toString(): String = string

        class StringT(var value: String) : Property() {
            override val string: String get() = value
        }
        class IntT(var value: Int) : Property() {
            override val number: Number get() = value
            override val string: String get() = "$value"
        }
        class FloatT(var value: Double) : Property() {
            override val number: Number get() = value
            override val string: String get() = "$value"
        }
        class BoolT(var value: Boolean) : Property() {
            override val number: Number get() = if (value) 1 else 0
            override val string: String get() = "$value"
        }
        class ColorT(var value: RGBA) : Property() {
            override val string: String get() = "$value"
        }
        class FileT(var path: String) : Property() {
            override val string: String get() = path
        }
        class ObjectT(var id: Int) : Property() {
            override val string: String get() = "$id"
        }
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
            image = null,
            terrains = listOf(),
            tiles = tileset.textures.mapIndexed { index, bmpSlice -> TileData(index) }
        ),
        val firstgid: Int = 1
    ) {
        val maxgid get() = firstgid + tileset.textures.size
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
            var map: IStackedIntArray2 = StackedIntArray2(0, 0),
            var encoding: Encoding = Encoding.XML,
            var compression: Compression = Compression.NO
        ) : Layer() {
            val width: Int get() = map.width
            val height: Int get() = map.height
            val area: Int get() = width * height
            operator fun set(x: Int, y: Int, value: Int) { map.setFirst(x, y, value) }
            operator fun get(x: Int, y: Int): Int = map.getFirst(x, y)
            override fun clone(): Tiles = Tiles(map.clone(), encoding, compression).also { it.copyFrom(this) }
        }

        data class Objects(
            var color: RGBA = Colors.WHITE,
            var drawOrder: Object.DrawOrder = Object.DrawOrder.TOP_DOWN,
            val objects: FastArrayList<Object> = FastArrayList()
        ) : Layer() {
            val objectsById by lazy { objects.associateBy { it.id } }
            val objectsByName by lazy { objects.associateBy { it.name } }
            val objectsByType by lazy { objects.groupBy { it.type } }

            fun getById(id: Int): Object? = objectsById[id]
            fun getByName(name: String): Object? = objectsByName[name]
            //fun getByType(type: String): List<Object> = objectsByType[type] ?: emptyList()
            fun getByType(type: String): List<Object> = objects.filter { it.type == type }

            override fun clone() = Objects(color, drawOrder, FastArrayList(objects)).also { it.copyFrom(this) }
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

inline val Iterable<TiledMap.Layer>.tiles get() = this.filterIsInstance<TiledMap.Layer.Tiles>()
inline val Iterable<TiledMap.Layer>.images get() = this.filterIsInstance<TiledMap.Layer.Image>()
inline val Iterable<TiledMap.Layer>.objects get() = this.filterIsInstance<TiledMap.Layer.Objects>()

val tilemapLog = Logger("tilemap")
