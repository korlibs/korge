package com.codeazur.as3swf.timeline

import com.codeazur.as3swf.utils.FlashByteArray

@Suppress("unused")
class Frame(var frameNumber: Int = 0, var tagIndexStart: Int = 0) {
	var tagIndexEnd: Int = 0
	var label: String? = null

	var objects = hashMapOf<Int, com.codeazur.as3swf.timeline.FrameObject>() // Key is string?
	var _objectsSortedByDepth: java.util.ArrayList<com.codeazur.as3swf.timeline.FrameObject>? = null
	var characters = arrayListOf<Int>()

	//fun get objects():Dictionary { return _objects; }
	//fun get characters():Array { return _characters; }

	fun getObjectsSortedByDepth(): java.util.ArrayList<com.codeazur.as3swf.timeline.FrameObject> {
		val depths = arrayListOf<Int>()
		if (_objectsSortedByDepth == null) {
			depths += objects.keys
			depths.sort()
			_objectsSortedByDepth = arrayListOf()
			for (i in 0 until depths.size) _objectsSortedByDepth!!.add(objects[depths[i]]!!)
		}
		return _objectsSortedByDepth!!
	}

	val tagCount: Int get() {
		return tagIndexEnd - tagIndexStart + 1
	}

	fun placeObject(tagIndex: Int, tag: com.codeazur.as3swf.tags.TagPlaceObject) {
		val frameObject = objects[tag.depth]
		if (frameObject != null) {
			// A character is already available at the specified depth
			if (tag.characterId == 0) {
				// The PlaceObject tag has no character id defined:
				// This means that the previous character is reused
				// and most likely modified by transforms
				frameObject.lastModifiedAtIndex = tagIndex
				frameObject.isKeyframe = false
			} else {
				// A character id is defined:
				// This means that the previous character is replaced
				// (possible transforms defined in previous frames are discarded)
				frameObject.lastModifiedAtIndex = 0
				frameObject.placedAtIndex = tagIndex
				frameObject.isKeyframe = true
				if (tag.characterId != frameObject.characterId) {
					// The character id does not match the previous character:
					// An entirely character is placed at this depth.
					frameObject.characterId = tag.characterId
				}
			}
		} else {
			// No character defined at specified depth. Create one.
			objects[tag.depth] = com.codeazur.as3swf.timeline.FrameObject(tag.depth, tag.characterId, tag.className, tagIndex, 0, true)
		}
		_objectsSortedByDepth = null
	}

	fun removeObject(tag: com.codeazur.as3swf.tags.TagRemoveObject) {
		objects.remove(tag.depth)
		_objectsSortedByDepth = null
	}

	fun clone(): com.codeazur.as3swf.timeline.Frame {
		val frame: com.codeazur.as3swf.timeline.Frame = com.codeazur.as3swf.timeline.Frame()
		for (depth in objects.keys) {
			frame.objects[depth] = (objects[depth] as com.codeazur.as3swf.timeline.FrameObject).clone()
		}
		return frame
	}

	fun toString(indent: Int = 0): String {
		var str: String = " ".repeat(indent) + "[" + frameNumber + "] " +
			"Start: " + tagIndexStart + ", " +
			"Length: " + tagCount
		if (label != null && label != "") {
			str += ", Label: " + label
		}
		if (characters.size > 0) {
			str += "\n" + " ".repeat(indent + 2) + "Defined CharacterIDs: " + characters.joinToString(", ")
		}
		for (depth in objects.keys) {
			str += (objects[depth] as com.codeazur.as3swf.timeline.FrameObject).toString(indent)
		}
		return str
	}
}

class FrameObject(
	// The depth of this display object
	var depth: Int,
	// The character id of this display object
	var characterId: Int,
	// The class name of this display object
	var className: String?,
	// The tag index of the PlaceObject tag that placed this object on the display list
	var placedAtIndex: Int,
	// The tag index of the PlaceObject tag that modified this object (optional)
	var lastModifiedAtIndex: Int = 0,
	// Whether this is a keyframe or not
	var isKeyframe: Boolean = false
) {
	// The index of the layer this object resides on
	var layer: Int = -1

	fun clone(): com.codeazur.as3swf.timeline.FrameObject {
		return com.codeazur.as3swf.timeline.FrameObject(depth, characterId, className, placedAtIndex, lastModifiedAtIndex, false)
	}

	fun toString(indent: Int = 0): String {
		var str: String = "\n" + " ".repeat(indent + 2) +
			"Depth: " + depth + (if (layer > -1) " (Layer $layer)" else "") + ", " +
			"CharacterId: " + characterId + ", "
		if (className != null) {
			str += "ClassName: $className, "
		}
		str += "PlacedAt: $placedAtIndex"
		if (lastModifiedAtIndex != 0) {
			str += ", LastModifiedAt: $lastModifiedAtIndex"
		}
		if (isKeyframe) {
			str += ", IsKeyframe"
		}
		return str
	}
}

@Suppress("unused")
class Layer(var depth: Int, var frameCount: Int) {
	var frameStripMap = arrayListOf<Int>()
	var strips = arrayListOf<com.codeazur.as3swf.timeline.LayerStrip>()

	fun appendStrip(type: Int, start: Int, end: Int) {
		if (type != com.codeazur.as3swf.timeline.LayerStrip.Companion.TYPE_EMPTY) {
			var stripIndex = strips.size
			if (stripIndex == 0 && start > 0) {
				for (i in 0 until start) {
					frameStripMap[i] = stripIndex
				}
				strips[stripIndex++] = com.codeazur.as3swf.timeline.LayerStrip(com.codeazur.as3swf.timeline.LayerStrip.Companion.TYPE_SPACER, 0, start - 1)
			} else if (stripIndex > 0) {
				val prevStrip: com.codeazur.as3swf.timeline.LayerStrip = strips[stripIndex - 1]
				if (prevStrip.endFrameIndex + 1 < start) {
					for (i in prevStrip.endFrameIndex + 1 until start) {
						frameStripMap[i] = stripIndex
					}
					strips[stripIndex++] = com.codeazur.as3swf.timeline.LayerStrip(com.codeazur.as3swf.timeline.LayerStrip.Companion.TYPE_SPACER, prevStrip.endFrameIndex + 1, start - 1)
				}
			}
			for (i in start..end) {
				frameStripMap[i] = stripIndex
			}
			strips[stripIndex] = com.codeazur.as3swf.timeline.LayerStrip(type, start, end)
		}
	}

	fun getStripsForFrameRegion(start: Int, end: Int): List<com.codeazur.as3swf.timeline.LayerStrip> {
		if (start >= frameStripMap.size || end < start) return listOf()
		val startStripIndex = frameStripMap[start]
		val endStripIndex = if (end >= frameStripMap.size) strips.size - 1 else frameStripMap[end]
		return strips.slice(startStripIndex until endStripIndex + 1)
	}

	fun toString(indent: Int = 0): String {
		var str: String = "Depth: $depth, Frames: $frameCount"
		if (strips.size > 0) {
			str += "\n" + " ".repeat(indent + 2) + "Strips:"
			for (i in 0 until strips.size) {
				val strip: com.codeazur.as3swf.timeline.LayerStrip = strips[i]
				str += "\n" + " ".repeat(indent + 4) + "[" + i + "] " + strip.toString()
			}
		}
		return str
	}
}

class LayerStrip(
	var type: Int = com.codeazur.as3swf.timeline.LayerStrip.Companion.TYPE_EMPTY,
	var startFrameIndex: Int = 0,
	var endFrameIndex: Int = 0
) {
	companion object {
		const val TYPE_EMPTY = 0
		const val TYPE_SPACER = 1
		const val TYPE_STATIC = 2
		const val TYPE_MOTIONTWEEN = 3
		const val TYPE_SHAPETWEEN = 4
	}

	override fun toString(): String {
		var str: String
		if (startFrameIndex == endFrameIndex) {
			str = "Frame: $startFrameIndex"
		} else {
			str = "Frames: $startFrameIndex-$endFrameIndex"
		}
		str += ", Type: "
		when (type) {
			com.codeazur.as3swf.timeline.LayerStrip.Companion.TYPE_EMPTY -> str += "EMPTY"
			com.codeazur.as3swf.timeline.LayerStrip.Companion.TYPE_SPACER -> str += "SPACER"
			com.codeazur.as3swf.timeline.LayerStrip.Companion.TYPE_STATIC -> str += "STATIC"
			com.codeazur.as3swf.timeline.LayerStrip.Companion.TYPE_MOTIONTWEEN -> str += "MOTIONTWEEN"
			com.codeazur.as3swf.timeline.LayerStrip.Companion.TYPE_SHAPETWEEN -> str += "SHAPETWEEN"
			else -> str += "unknown"
		}
		return str
	}
}

class Scene(var frameNumber: Int, var name: String) {

	fun toString(indent: Int = 0): String {
		return "${" ".repeat(indent)}Name: $name, Frame: $frameNumber"
	}
}

class SoundStream {
	var startFrame = 0
	var numFrames = 0
	var numSamples = 0

	var compression = 0
	var rate = 0
	var size = 0
	var type = 0

	var data = FlashByteArray(); protected set

	override fun toString() = "[SoundStream] StartFrame: $startFrame, Frames: $numFrames, Samples: $numSamples, Bytes: ${data.length}"
}
