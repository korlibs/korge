package com.soywiz.korge.atlas

import com.soywiz.korge.resources.Path
import com.soywiz.korge.resources.ResourcesRoot
import com.soywiz.korge.view.Views
import com.soywiz.korio.inject.AsyncFactory
import com.soywiz.korio.inject.AsyncFactoryClass
import com.soywiz.korio.serialization.json.Json
import com.soywiz.korma.geom.Rectangle
import org.intellij.lang.annotations.Language

@AsyncFactoryClass(AtlasInfo.Factory::class)
data class AtlasInfo(
	val frames: Map<String, Entry>,
	val meta: Meta
) {
	data class Rect(val x: Int, val y: Int, val w: Int, val h: Int) {
		val rect get() = Rectangle(x, y, w, h)
	}

	data class Size(val w: Int, val h: Int) {
		val size get() = com.soywiz.korma.geom.Size(w, h)
	}

	data class Meta(val app: String, val format: String, val image: String, val scale: Double, val size: Size, val version: String)
	data class Entry(val frame: Rect, val rotated: Boolean, val sourceSize: Size, val spriteSourceSize: Rect, val trimmed: Boolean) {
		fun applyRotation() = if (rotated) {
			this.copy(
				frame = frame.copy(w = frame.h, h = frame.w),
				spriteSourceSize = spriteSourceSize.copy(x = spriteSourceSize.y, y = spriteSourceSize.x, w = spriteSourceSize.h, h = spriteSourceSize.w)
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
		fun loadJsonSpriter(@Language("json") json: String): AtlasInfo {
			val info = Json.decodeToType<AtlasInfo>(json)
			return info.copy(frames = info.frames.mapValues { it.value.applyRotation() })
		}
	}

	class Factory(
		val path: Path,
		val views: Views,
		val resourcesRoot: ResourcesRoot
	) : AsyncFactory<AtlasInfo> {
		suspend override fun create(): AtlasInfo = AtlasInfo.loadJsonSpriter(resourcesRoot[path].readString())
	}
}
