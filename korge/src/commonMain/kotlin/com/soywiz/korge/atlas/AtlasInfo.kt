package com.soywiz.korge.atlas

import com.soywiz.korio.dynamic.*
import com.soywiz.korio.serialization.json.*
import com.soywiz.korio.serialization.xml.*
import com.soywiz.korma.geom.*

//e: java.lang.UnsupportedOperationException: Class literal annotation arguments are not yet supported: Factory
//@AsyncFactoryClass(AtlasInfo.Factory::class)
data class AtlasInfo(
	val frames: List<Entry>,
	val meta: Meta
) {
    val framesMap = frames.associateBy { it.filename }

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

	data class Entry(
        val filename: String,
		val frame: Rect,
		val rotated: Boolean,
		val sourceSize: Size,
		val spriteSourceSize: Rect,
		val trimmed: Boolean
	) {
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
        private fun KDynamic.createEntry(name: String, it: Any?) = Entry(
            filename = name,
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
			return info.copy(frames = info.frames.map { it.applyRotation() })
		}

        fun loadXml(content: String): AtlasInfo {
            val xml = Xml(content)
            val imagePath = xml.str("imagePath")
            return AtlasInfo(xml.children("SubTexture").map {
                val rect = Rect(it.int("x"), it.int("y"), it.int("width"), it.int("height"))
                Entry(
                    filename = it.str("name"),
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
	}
}
