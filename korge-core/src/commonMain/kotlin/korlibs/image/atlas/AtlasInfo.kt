package korlibs.image.atlas

import korlibs.datastructure.*
import korlibs.image.format.*
import korlibs.io.dynamic.*
import korlibs.io.serialization.json.*
import korlibs.io.serialization.xml.*
import korlibs.math.geom.*

//e: java.lang.UnsupportedOperationException: Class literal annotation arguments are not yet supported: Factory
//@AsyncFactoryClass(AtlasInfo.Factory::class)
data class AtlasInfo(
    val meta: Meta = Meta(),
    val pages: List<Page> = listOf()
) {
    val frames: List<Region> = pages.flatMap { it.regions }
    val framesMap: Map<String, Region> = frames.associateBy { it.name }

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

    fun RectangleInt.toMap() = mapOf("x" to x, "y" to y, "w" to width, "h" to height)
    fun SizeInt.toMap() = mapOf("w" to width, "h" to height)

    data class Meta(
        val app: String = "app",
        val format: String = "format",
        val image: String = "image",
        val scale: Double = 1.0,
        val size: SizeInt = SizeInt(1, 1),
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
        val bounds: RectangleInt
    )

    data class Page(
        val fileName: String,
        var size: SizeInt,
        var format: String,
        var filterMin: Boolean,
        var filterMag: Boolean,
        var repeatX: Boolean,
        var repeatY: Boolean,
        val regions: List<Region>
    )

    data class Region(
        val name: String,
        val frame: RectangleInt,
        val virtFrame: RectangleInt? = null,
        val imageOrientation: ImageOrientation = ImageOrientation.NORMAL,
    ) {
        @Deprecated("Use primary constructor")
        internal constructor(
            name: String,
            frame: RectangleInt,
            rotated: Boolean,
            sourceSize: SizeInt,
            spriteSourceSize: RectangleInt,
            trimmed: Boolean,
            orig: SizeInt = SizeInt(0, 0),
            offset: Point = Point.ZERO,
        ) : this(
            name = name,
            frame = frame,
            virtFrame = when {
                offset.x != 0.0 || offset.y != 0.0 || orig.width != 0 || orig.height != 0 ->
                    RectangleInt(offset.x.toInt(), offset.y.toInt(), orig.width, orig.height)
                spriteSourceSize.x != 0 || spriteSourceSize.y != 0 || sourceSize.width != frame.height || sourceSize.height != frame.width ->
                    RectangleInt(spriteSourceSize.x, spriteSourceSize.y, sourceSize.width, sourceSize.height)
                else -> null
            },
            imageOrientation = if (rotated) ImageOrientation.ROTATE_90 else ImageOrientation.ROTATE_0
        )

        @Deprecated("Use imageOrientation", ReplaceWith("imageOrientation == ImageOrientation.ROTATE_90", imports = ["korlibs.image.format.ImageOrientation"]))
        val rotated: Boolean get() = imageOrientation.isRotatedDeg90CwOrCcw

        @Deprecated("Use virtFrame", ReplaceWith("Size(virtFrame?.w ?: frame.w, virtFrame?.h ?: frame.h)"))
        val sourceSize: SizeInt get() = SizeInt(virtFrame?.width ?: frame.width, virtFrame?.height ?: frame.height)

        @Deprecated("Use virtFrame", ReplaceWith("if (virtFrame == null) frame else Rect(virtFrame.x, virtFrame.y, frame.w, frame.h)"))
        val spriteSourceSize: RectangleInt get() = if (virtFrame == null) frame else RectangleInt(virtFrame.x, virtFrame.y, frame.width, frame.height)

        @Deprecated("Use virtFrame", ReplaceWith("virtFrame != null"))
        val trimmed: Boolean get() = virtFrame != null

        @Deprecated("Use virtFrame", ReplaceWith("Size(virtFrame?.w ?: frame.w, virtFrame?.h ?: frame.h)"))
        @Suppress("DEPRECATION")
        val orig: SizeInt get() = sourceSize

        @Deprecated("Use virtFrame", ReplaceWith("Point(virtFrame?.x ?: 0, virtFrame?.y ?: 0)"))
        val offset: Point get() = Point(virtFrame?.x ?: 0, virtFrame?.y ?: 0)

        // @TODO: Rename to path or name
        //@IgnoreSerialization
        val filename: String get() = name

        // Used by Spine
        @Suppress("unused")
        val srcWidth: Int = if (imageOrientation.isRotatedDeg90CwOrCcw) frame.height else frame.width

        // Used by Spine
        @Suppress("unused")
        val srcHeight: Int = if (imageOrientation.isRotatedDeg90CwOrCcw) frame.width else frame.height
    }

    val app: String get() = meta.app
    val format: String get() = meta.format
    val image: String get() = meta.image
    val scale: Double get() = meta.scale
    val size: SizeInt get() = meta.size
    val version: String get() = meta.version

    fun toJsonString(): String = Json.stringify(toMap(), pretty = true)

    fun toMap(): Map<String, Any?> = mapOf(
        "meta" to mapOf(
            "app" to app,
            "version" to version,
            "image" to image,
            "format" to format,
            "size" to size.toMap(),
            "scale" to scale.toString(),
        ),
        "frames" to framesMap.entries.associate { (key, value) ->
            key to mapOf(
                "frame" to value.frame.toMap(),
                "rotated" to value.rotated,
                "trimmed" to value.trimmed,
                "spriteSourceSize" to value.spriteSourceSize.toMap(),
                "sourceSize" to value.sourceSize.toMap(),
            )
        }
    )

    companion object {
        private fun Dyn.toRect(): RectangleInt = RectangleInt(this["x"].int, this["y"].int, this["w"].int, this["h"].int)
        private fun Dyn.toSize(): SizeInt = SizeInt(this["w"].int, this["h"].int)
        private fun createEntry(name: String, it: Dyn): Region {
            val rotated = it["rotated"].bool
            val sourceSize = it["sourceSize"].toSize()
            val spriteSourceSize = it["spriteSourceSize"].toRect()

            val sW = sourceSize.width
            val sH = sourceSize.height
            val frame = it["frame"].toRect()

            return Region(name = name,
                frame = if (!rotated) frame else RectangleInt(frame.x, frame.y, frame.height, frame.width),
                virtFrame = RectangleInt(spriteSourceSize.x, spriteSourceSize.y, sW, sH),
                imageOrientation = if (rotated) ImageOrientation.ROTATE_270 else ImageOrientation.ROTATE_0)
        }

        // @TODO: kotlinx-serialization?
        fun loadJsonSpriter(json: String): AtlasInfo {
            val it = Json.parse(json).dyn
            val info =
                AtlasInfo(
                    frames = it["frames"].let { framesDyn ->
                        val frames = framesDyn.value
                        when (frames) {
                            // Hash-based
                            is Map<*, *> -> frames.keys.map { createEntry(it.dyn.str, framesDyn[it.dyn.str]) }
                            // Array-based
                            else -> framesDyn.list.map {
                                createEntry(
                                    it["name"].orNull?.str ?: it["filename"].orNull?.str ?: "unknown",
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
                            frameTags = it["frameTags"].let { frameTagsDyn ->
                                val frameTags = frameTagsDyn.value
                                if (frameTags is List<*>) frameTagsDyn.list.map {
                                    FrameTag(
                                        name = it["name"].str,
                                        from = it["from"].int,
                                        to = it["to"].int,
                                        direction = it["direction"].str
                                    )
                                }
                                else listOf()
                            },
                            layers = it["layers"].let { layersDyn ->
                                val layers = layersDyn.value
                                if (layers is List<*>) layersDyn.list.map {
                                    Layer(
                                        name = it["name"].str,
                                        opacity = it["opacity"].int,
                                        blendMode = it["blendMode"].str
                                    )
                                }
                                else listOf()
                            },
                            slices = it["slices"].let { slicesDyn ->
                                val slices = slicesDyn.value
                                if (slices is List<*>) slicesDyn.list.map {
                                    Slice(
                                        name = it["name"].str,
                                        color = it["color"].str,
                                        keys = it["keys"].let { keysDyn ->
                                            val keys = keysDyn.value
                                            if (keys is List<*>) keysDyn.list.map {
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

            return info.copy(pages = info.pages.map { it.copy(regions = it.regions.map { it.apply { } }) })
        }

        fun loadXml(content: String): AtlasInfo {
            val xml = Xml(content)
            val imagePath = xml.str("imagePath")
            val size = SizeInt(xml.int("width", -1), xml.int("height", -1))

            return AtlasInfo(
                (xml.children("SubTexture") + xml.children("sprite")).map {
                    val virtFrame = RectangleInt(
                        it.int("frameX", 0) * -1,
                        it.int("frameY", 0) * -1,
                        it.int("frameWidth", 0),
                        it.int("frameHeight", 0)
                    )
                    Region(
                        name = it.strNull("name") ?: it.str("n"),
                        frame = RectangleInt(
                            it.int("x"),
                            it.int("y"),
                            it.intNull("width") ?: it.int("w"),
                            it.intNull("height") ?: it.int("h")
                        ),
                        virtFrame = if (virtFrame.width != 0 || virtFrame.height != 0) virtFrame else null,
                        imageOrientation = if (it.boolean("rotated", false)) ImageOrientation.ROTATE_270 else ImageOrientation.ROTATE_0
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

            fun String.size(): SizeInt = point().let { SizeInt(it.x.toInt(), it.y.toInt()) }

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
                    var size = SizeInt(0, 0)
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
                    var xy = Point.ZERO
                    var size = SizeInt(0, 0)
                    var orig = SizeInt(0, 0)
                    var offset = Point.ZERO
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
                    val orientation = when {
                        rotate -> ImageOrientation.ROTATE_90
                        else -> ImageOrientation.ROTATE_0
                    }

                    val w = if (!rotate) size.width else size.height
                    val h = if (!rotate) size.height else size.width
                    val oW = orig.width
                    val oH = orig.height

                    currentEntryList.add(Region(
                        name = name,
                        frame = RectangleInt(xy.x.toInt(), xy.y.toInt(), w, h),
                        virtFrame = RectangleInt(offset.x.toInt(), orig.height - size.height - offset.y.toInt(), oW, oH), // In Spine atlas format offset is defined from left and bottom
                        imageOrientation = orientation
                    ))
                }
            }
            val firstPage = pages.first()
            return AtlasInfo(Meta("unknown", firstPage.format, firstPage.fileName, 1.0, firstPage.size, "1.0"), pages)
        }
    }
}
