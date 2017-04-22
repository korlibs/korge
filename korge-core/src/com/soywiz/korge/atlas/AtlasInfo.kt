package com.soywiz.korge.atlas

import com.soywiz.korge.resources.Path
import com.soywiz.korge.resources.ResourcesRoot
import com.soywiz.korge.view.Views
import com.soywiz.korio.inject.AsyncFactory
import com.soywiz.korio.inject.AsyncFactoryClass
import com.soywiz.korio.serialization.json.Json
import com.soywiz.korio.util.asDynamicNode
import com.soywiz.korma.geom.Rectangle
import com.soywiz.korma.geom.Size
import org.intellij.lang.annotations.Language

@AsyncFactoryClass(AtlasInfo.Factory::class)
class AtlasInfo(
	val app: String,
	val version: String,
	val image: String,
	val scale: Double,
	val size: Size,
	val format: String,
	val frames: Map<String, Frame>
) {
	data class Frame(
		val name: String,
		val frame: Rectangle,
		val sourceSize: Size,
		val spriteSourceSize: Rectangle,
		val rotated: Boolean,
		val trimmed: Boolean
	)

	companion object {
		fun loadJsonSpriter(@Language("json") json: String): AtlasInfo {
			val info = Json.decode(json).asDynamicNode()
			val frames = info["frames"]
			val outFrames = arrayListOf<Frame>()

			for ((name, item) in frames.getEntries()) {
				val frame = item["frame"]
				val sourceSize = item["sourceSize"]
				val spriteSourceSize = item["spriteSourceSize"]
				val rotated = item["rotated"].toBoolean(false)
				val trimmed = item["trimmed"].toBoolean(false)

				outFrames += Frame(
					name = name,
					frame = if (!rotated) {
						Rectangle(frame["x"].toFloat(0f), frame["y"].toFloat(0f), frame["w"].toFloat(0f), frame["h"].toFloat(0f))
					} else {
						Rectangle(frame["x"].toFloat(0f), frame["y"].toFloat(0f), frame["h"].toFloat(0f), frame["w"].toFloat(0f))
					},
					sourceSize = Size(sourceSize["w"].toFloat(0f), sourceSize["h"].toFloat(0f)),
					spriteSourceSize = if (!rotated) {
						Rectangle(spriteSourceSize["x"].toFloat(0f), spriteSourceSize["y"].toFloat(0f), spriteSourceSize["w"].toFloat(0f), spriteSourceSize["h"].toFloat(0f))
					} else {
						Rectangle(spriteSourceSize["y"].toFloat(0f), spriteSourceSize["x"].toFloat(0f), spriteSourceSize["h"].toFloat(0f), spriteSourceSize["w"].toFloat(0f))
					},
					rotated = rotated,
					trimmed = trimmed
				)
			}
			val meta = info["meta"]
			val app = meta["app"].toString("Spriter")
			val version = meta["version"].toString("unknown")
			val format = meta["format"].toString("RGBA8888")
			val image = meta["image"].toString("")
			val scale = meta["scale"].toDouble(1.0)
			val size = meta["size"]

			return AtlasInfo(
				app = app,
				version = version,
				format = format,
				image = image,
				scale = scale,
				size = Size(size["w"].toFloat(0f), size["h"].toFloat(0f)),
				frames = outFrames.map { it.name to it }.toMap()
			)
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
