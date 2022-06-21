package com.soywiz.korim.atlas

import com.soywiz.kds.ListReader
import com.soywiz.korim.format.ImageOrientation
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

        fun toRectangleInt() : RectangleInt = RectangleInt(x, y, w, h)
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
        val virtFrame: Rect? = null,
        val imageOrientation: ImageOrientation = ImageOrientation.ORIGINAL,
    ) {

        val srcWidth = if (imageOrientation.isRotatedDeg90CwOrCcw) frame.h else frame.w

        val srcHeight = if (imageOrientation.isRotatedDeg90CwOrCcw) frame.w else frame.h

        @Deprecated("Use primary constructor")
        constructor(
            name: String,
            frame: Rect,
            rotated: Boolean,
            sourceSize: Size,
            spriteSourceSize: Rect,
            trimmed: Boolean,
            orig: Size = Size(0, 0),
            offset: Point = Point(),
        ) : this(
            name = name,
            frame = frame,
            virtFrame = when {
                offset.x != 0.0 || offset.y != 0.0 || orig.width != 0 || orig.height != 0 ->
                    Rect(offset.x.toInt(), offset.y.toInt(), orig.width, orig.height)
                spriteSourceSize.x != 0 || spriteSourceSize.y != 0 || sourceSize.width != frame.h || sourceSize.height != frame.w ->
                    Rect(spriteSourceSize.x, spriteSourceSize.y, sourceSize.width, sourceSize.height)
                else -> null
            },
            imageOrientation = if (rotated) ImageOrientation.ROTATE_90 else ImageOrientation.ORIGINAL
        ) {
            @Suppress("DEPRECATION")
            this.rotated = rotated
            @Suppress("DEPRECATION")
            this.sourceSize = sourceSize
            @Suppress("DEPRECATION")
            this.spriteSourceSize = spriteSourceSize
            @Suppress("DEPRECATION")
            this.trimmed = trimmed
            @Suppress("DEPRECATION")
            this.orig = orig
            @Suppress("DEPRECATION")
            this.offset = offset
        }

        @Deprecated("Use imageOrientation", ReplaceWith("imageOrientation == ImageOrientation.ROTATE_90", imports = ["com.soywiz.korim.format.ImageOrientation"]))
        var rotated: Boolean = imageOrientation.isRotatedDeg90CwOrCcw
            private set

        @Deprecated("Use virtFrame", ReplaceWith("Size(virtFrame?.w ?: frame.w, virtFrame?.h ?: frame.h)"))
        var sourceSize: Size = Size(virtFrame?.w ?: frame.w, virtFrame?.h ?: frame.h)
            private set

        @Deprecated("Use virtFrame", ReplaceWith("if (virtFrame == null) frame else Rect(virtFrame.x, virtFrame.y, frame.w, frame.h)"))
        var spriteSourceSize: Rect = if (virtFrame == null) frame else Rect(virtFrame.x, virtFrame.y, frame.w, frame.h)
            private set

        @Deprecated("Use virtFrame", ReplaceWith("virtFrame != null"))
        var trimmed: Boolean = virtFrame != null
            private set

        @Deprecated("Use virtFrame", ReplaceWith("Size(virtFrame?.w ?: frame.w, virtFrame?.h ?: frame.h)"))
        @Suppress("DEPRECATION")
        var orig: Size = sourceSize
            private set

        @Deprecated("Use virtFrame", ReplaceWith("Point(virtFrame?.x ?: 0, virtFrame?.y ?: 0)"))
        var offset: Point = Point(virtFrame?.x ?: 0, virtFrame?.y ?: 0)
            private set

        // @TODO: Rename to path or name
        val filename get() = name

        fun applyRotation() = if (imageOrientation == ImageOrientation.ROTATE_90) {
            this.copy(
                frame = frame.copy(w = frame.h, h = frame.w),
                virtFrame = virtFrame?.copy(
                    x = virtFrame.y,
                    y = virtFrame.x,
                    w = virtFrame.h,
                    h = virtFrame.w
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
        private fun KDynamic.createEntry(name: String, it: Any?): Region {
            val rotated = it["rotated"].bool
            val sourceSize = it["sourceSize"].toSize()
            val spriteSourceSize = it["spriteSourceSize"].toRect()
            return Region(name = name,
                frame = it["frame"].toRect(),
                virtFrame = Rect(spriteSourceSize.x, spriteSourceSize.y, sourceSize.width, sourceSize.height),
                imageOrientation = if (rotated) ImageOrientation.ROTATE_270 else ImageOrientation.ORIGINAL)
        }

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
            val size = Size(xml.int("width", -1), xml.int("height", -1))

            return AtlasInfo(
                (xml.children("SubTexture") + xml.children("sprite")).map {
                    val rotated = it.boolean("rotated", false)
                    var rect = Rect(
                        it.int("x"),
                        it.int("y"),
                        it.intNull("width") ?: it.int("w"),
                        it.intNull("height") ?: it.int("h")
                    )
                    val imageOrientation: ImageOrientation
                    if (rotated) {
                        rect = rect.copy(w = rect.h, h = rect.w)
                        imageOrientation = ImageOrientation.ROTATE_270
                    } else {
                        imageOrientation = ImageOrientation.ORIGINAL
                    }

                    val virtFrame = Rect(
                        it.int("frameX", 0) * -1,
                        it.int("frameY", 0) * -1,
                        it.int("frameWidth", 0),
                        it.int("frameHeight", 0)
                    )
                    Region(
                        name = it.strNull("name") ?: it.str("n"),
                        frame = rect,
                        virtFrame = if (virtFrame.w != 0 || virtFrame.h != 0) virtFrame else null,
                        imageOrientation = imageOrientation
                    )
                }, Meta(
                    app = "Unknown",
                    format = "xml",
                    image = imagePath,
                    scale = 1.0,
                    size = size,
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

                    currentEntryList.add(Region(
                        name = name,
                        frame = Rect(xy.x.toInt(), xy.y.toInt(), size.width, size.height),
                        virtFrame = Rect(offset.x.toInt(), orig.height - size.height - offset.y.toInt(), orig.width, orig.height), // In Spine atlas format offset is defined from left and bottom
                        imageOrientation = if (rotate) {
                            ImageOrientation.ROTATE_90
                        } else {
                            ImageOrientation.ORIGINAL
                        }
                    ))
                }
            }
            val firstPage = pages.first()
            return AtlasInfo(Meta("unknown", firstPage.format, firstPage.fileName, 1.0, firstPage.size, "1.0"), pages)
        }
    }
}
