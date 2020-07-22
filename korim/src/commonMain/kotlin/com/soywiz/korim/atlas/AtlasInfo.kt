package com.soywiz.korim.atlas

import com.soywiz.kds.*
import com.soywiz.korio.dynamic.*
import com.soywiz.korio.serialization.json.*
import com.soywiz.korio.serialization.xml.*
import com.soywiz.korma.geom.*

//e: java.lang.UnsupportedOperationException: Class literal annotation arguments are not yet supported: Factory
//@AsyncFactoryClass(AtlasInfo.Factory::class)
data class AtlasInfo(
    val meta: Meta,
    val pages: List<Page>
) {
    val frames = pages.flatMap { it.regions }
    val framesMap = frames.associateBy { it.name }

    constructor(
        frames: List<Region>,
        meta: Meta,
    ) : this(meta, listOf(Page(
        meta.image,
        meta.size,
        meta.format,
        true, true,
        false, false, frames
    )))

	data class Rect(val x: Int, val y: Int, val w: Int, val h: Int) {
        val rect get() = Rectangle(x, y, w, h)
	}

	data class Size(val w: Int, val h: Int) {
		val size get() = com.soywiz.korma.geom.Size(w, h)
	}

	data class Meta(
        val app: String,
        val format: String,
        val image: String,
        val scale: Double,
        val size: Size,
        val version: String
	) {
		companion object {
			val VERSION = "1.0.0"
		}
	}

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
        // @TODO: Renemae to path or name
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
					it["frames"].let { frames ->
                        when (frames) {
                            // Hash-based
                            is Map<*, *> -> frames.keys.map { createEntry(it.str, frames[it.str]) }
                            // Array-based
                            else -> frames.list.map { createEntry(it["filename"].str, it) }
                        }
					},
					it["meta"].let {
						Meta(
							app = it["app"].str,
							format = it["format"].str,
							image = it["image"].str,
							scale = it["scale"].double,
							size = it["size"].toSize(),
							version = it["version"].str
						)
					}
				)
			}
			return info.copy(pages = info.pages.map { it.copy(regions = it.regions.map { it.apply {  } }) })
		}

        fun loadXml(content: String): AtlasInfo {
            val xml = Xml(content)
            val imagePath = xml.str("imagePath")

            return AtlasInfo( (xml.children("SubTexture") + xml.children("sprite")).map {
                val rect = Rect(it.int("x"), it.int("y"), it.intNull("width") ?: it.int("w"), it.intNull("height") ?: it.int("h"))
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
            ))
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
                    val rect = Rect(xy.x.toInt(), xy.y.toInt(), size.w, size.h)
                    currentEntryList.add(Region(name, rect, rotate, size, rect, false, orig, offset))
                }
            }
            val firstPage = pages.first()
            return AtlasInfo(Meta("unknown", firstPage.format, firstPage.fileName, 1.0, firstPage.size, "1.0"), pages)
        }
    }
}
