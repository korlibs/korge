package com.soywiz.korim.atlas

import com.soywiz.kds.ListReader
import com.soywiz.korio.dynamic.KDynamic
import com.soywiz.korio.serialization.json.Json
import com.soywiz.korio.serialization.xml.Xml
import com.soywiz.korma.geom.*

//e: java.lang.UnsupportedOperationException: Class literal annotation arguments are not yet supported: Factory
//@AsyncFactoryClass(AtlasInfo.Factory::class)
data class AtlasInfo(
    val meta: Meta = Meta(),
    val pages: List<Page> = listOf()
) {
    val frames = pages.flatMap { it.regions }
    val framesMap = frames.associateBy { it.name }

    constructor(
        frames: List<Region>,
        meta: Meta,
    ) : this(
        meta, listOf(
            Page(
                meta.image,
                meta.size,
                meta.format,
                true, true,
                false, false, frames
            )
        )
    )

    data class Rect(val x: Int, val y: Int, val w: Int, val h: Int) {
        val rect get() = Rectangle(x, y, w, h)
    }

    data class Size(val width: Int, val height: Int) {
        val size get() = com.soywiz.korma.geom.Size(width, height)
    }

    data class Meta(
        val app: String = "app",
        val format: String = "format",
        val image: String = "image",
        val scale: Double = 1.0,
        val size: Size = Size(1, 1),
        val version: String = VERSION,
        val frameTags: List<FrameTag> = listOf(),
        val layers: List<Layer> = listOf(),
        val slices: List<Slice> = listOf()
    ) {
        companion object {
            val VERSION = "1.0.0"
        }
    }

    data class FrameTag(
        val name: String = "",
        val from: Int = 0,
        val to: Int = 0,
        val direction: String = ""
    )

    data class Layer(
        val name: String = "",
        val opacity: Int = 255,
        val blendMode: String = ""
    )

    data class Slice(
        val name: String = "slice",
        val color: String = "#0000ffff",
        val keys: List<Key>
    )

    data class Key(
        val frame: Int = 0,
        val bounds: Rect
    )

    data class Page(
        val fileName: String,
        var size: Size,
        var format: String,
        var filterMin: Boolean,
        var filterMag: Boolean,
        var repeatX: Boolean,
        var repeatY: Boolean,
        val regions: List<Region>
    )

    data class Region(
        val name: String,
        val frame: Rect,
        val rotated: Boolean,
        val sourceSize: Size,
        val spriteSourceSize: Rect,
        val trimmed: Boolean,
        val orig: Size = Size(0, 0),
        val offset: Point = Point(),
    ) {
        // @TODO: Rename to path or name
        val filename get() = name

        fun applyRotation() = if (rotated) {
            this.copy(
                frame = frame.copy(w = frame.h, h = frame.w),
                spriteSourceSize = spriteSourceSize.copy(
                    x = spriteSourceSize.y,
                    y = spriteSourceSize.x,
                    w = spriteSourceSize.h,
                    h = spriteSourceSize.w
                )
            )
        } else {
            this
        }
    }

    val app: String get() = meta.app
    val format: String get() = meta.format
    val image: String get() = meta.image
    val scale: Double get() = meta.scale
    val size: Size get() = meta.size
    val version: String get() = meta.version

    companion object {
        private fun Any?.toRect() = KDynamic(this) { Rect(it["x"].int, it["y"].int, it["w"].int, it["h"].int) }
        private fun Any?.toSize() = KDynamic(this) { Size(it["w"].int, it["h"].int) }
        private fun KDynamic.createEntry(name: String, it: Any?) = Region(
            name = name,
            frame = it["frame"].toRect(),
            rotated = it["rotated"].bool,
            sourceSize = it["sourceSize"].toSize(),
            spriteSourceSize = it["spriteSourceSize"].toRect(),
            trimmed = it["trimmed"].bool
        )

        // @TODO: kotlinx-serialization?
        fun loadJsonSpriter(json: String): AtlasInfo {
            val info = KDynamic(Json.parse(json)) {
                AtlasInfo(
                    frames = it["frames"].let { frames ->
                        when (frames) {
                            // Hash-based
                            is Map<*, *> -> frames.keys.map { createEntry(it.str, frames[it.str]) }
                            // Array-based
                            else -> frames.list.map {
                                createEntry(
                                    it["name"]?.str ?: it["filename"]?.str ?: "unknown",
                                    it
                                )
                            }
                        }
                    },
                    meta = it["meta"].let {
                        Meta(
                            app = it["app"].str,
                            format = it["format"].str,
                            image = it["image"].str,
                            scale = it["scale"].double,
                            size = it["size"].toSize(),
                            version = it["version"].str,
                            frameTags = it["frameTags"].let { frameTags ->
                                if (frameTags is List<*>) frameTags.list.map {
                                    FrameTag(
                                        name = it["name"].str,
                                        from = it["from"].int,
                                        to = it["to"].int,
                                        direction = it["direction"].str
                                    )
                                }
                                else listOf()
                            },
                            layers = it["layers"].let { layers ->
                                if (layers is List<*>) layers.list.map {
                                    Layer(
                                        name = it["name"].str,
                                        opacity = it["opacity"].int,
                                        blendMode = it["blendMode"].str
                                    )
                                }
                                else listOf()
                            },
                            slices = it["slices"].let { slices ->
                                if (slices is List<*>) slices.list.map {
                                    Slice(
                                        name = it["name"].str,
                                        color = it["color"].str,
                                        keys = it["keys"].let { keys ->
                                            if (keys is List<*>) keys.list.map {
                                                Key(
                                                    frame = it["frame"].int,
                                                    bounds = it["bounds"].toRect()
                                                )
                                            }
                                            else listOf()
                                        }
                                    )
                                }
                                else listOf()
                            }
                        )
                    }
                )
            }
            return info.copy(pages = info.pages.map { it.copy(regions = it.regions.map { it.apply { } }) })
        }

        fun loadXml(content: String): AtlasInfo {
            val xml = Xml(content)
            val imagePath = xml.str("imagePath")

            return AtlasInfo(
                (xml.children("SubTexture") + xml.children("sprite")).map {
                    val rect = Rect(
                        it.int("x"),
                        it.int("y"),
                        it.intNull("width") ?: it.int("w"),
                        it.intNull("height") ?: it.int("h")
                    )
                    Region(
                        name = it.strNull("name") ?: it.str("n"),
                        frame = rect,
                        rotated = false,
                        sourceSize = Size(rect.w, rect.h),
                        spriteSourceSize = rect,
                        trimmed = false
                    )
                }, Meta(
                    app = "Unknown",
                    format = "xml",
                    image = imagePath,
                    scale = 1.0,
                    size = Size(-1, -1),
                    version = "1.0"
                )
            )
        }

        fun loadText(content: String): AtlasInfo {
            val r = ListReader(content.lines())
            var pageImage: Any? = null

            fun String.point(): Point {
                val list = this.split(',', limit = 2)
                return Point(list.first().trim().toInt(), list.last().trim().toInt())
            }

            fun String.size(): Size = point().let { Size(it.x.toInt(), it.y.toInt()) }

            fun String.keyValue(): Pair<String, String> {
                val list = this.split(':', limit = 2)
                return list.first().trim().toLowerCase() to list.last().trim()
            }

            fun String.filter(): Boolean {
                return when (this.toLowerCase()) {
                    "nearest" -> false
                    "linear" -> true
                    "mipmap" -> true
                    "mipmapnearestnearest" -> false
                    "mipmaplinearnearest" -> false
                    "mipmapnearestlinear" -> false
                    "mipmaplinearlinear" -> false
                    else -> false
                }
            }

            var currentEntryList = arrayListOf<Region>()
            val pages = arrayListOf<Page>()

            while (r.hasMore) {
                val line = r.read().trim()
                if (line.isEmpty()) {
                    if (r.eof) break

                    val fileName = r.read().trim()
                    var size = Size(0, 0)
                    var format = "rgba8888"
                    var filterMin = false
                    var filterMag = false
                    var repeatX = false
                    var repeatY = false
                    while (r.hasMore && r.peek().contains(':')) {
                        val (key, value) = r.read().trim().keyValue()
                        when (key) {
                            "size" -> size = value.size()
                            "format" -> format = value
                            "filter" -> {
                                val filter = value.split(",").map { it.trim().toLowerCase() }
                                filterMin = filter.first().filter()
                                filterMag = filter.last().filter()
                            }
                            "repeat" -> {
                                repeatX = value.contains('x')
                                repeatY = value.contains('y')
                            }
                        }
                    }
                    currentEntryList = arrayListOf<Region>()
                    pages.add(Page(fileName, size, format, filterMin, filterMag, repeatX, repeatY, currentEntryList))
                } else {
                    val name = line
                    var rotate = false
                    var xy = Point()
                    var size = Size(0, 0)
                    var orig = Size(0, 0)
                    var offset = Point()
                    while (r.hasMore && r.peek().contains(':')) {
                        val (key, value) = r.read().trim().keyValue()
                        when (key) {
                            "rotate" -> rotate = value.toBoolean()
                            "xy" -> xy = value.point()
                            "size" -> size = value.size()
                            "orig" -> orig = value.size()
                            "offset" -> offset = value.point()
                        }
                    }
                    if (rotate) {
                        size = Size(size.height, size.width)
                    }
                    val rect = Rect(xy.x.toInt(), xy.y.toInt(), size.width, size.height)
                    currentEntryList.add(Region(name, rect, rotate, size, rect, false, orig, offset))
                }
            }
            val firstPage = pages.first()
            return AtlasInfo(Meta("unknown", firstPage.format, firstPage.fileName, 1.0, firstPage.size, "1.0"), pages)
        }
    }
}
