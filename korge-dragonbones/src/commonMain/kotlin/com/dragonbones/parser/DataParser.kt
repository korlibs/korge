/**
 * The MIT License (MIT)
 *
 * Copyright (c) 2012-2018 DragonBones team and other contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.dragonbones.parser

import com.dragonbones.core.*
import com.dragonbones.model.*
import com.soywiz.korio.serialization.json.*

/**
 * @private
 */
@Suppress("unused", "MayBeConstant", "MemberVisibilityCanBePrivate", "FunctionName")
abstract class DataParser(val pool: BaseObjectPool) {
	companion object {
		val DATA_VERSION_2_3: String = "2.3"
		val DATA_VERSION_3_0: String = "3.0"
		val DATA_VERSION_4_0: String = "4.0"
		val DATA_VERSION_4_5: String = "4.5"
		val DATA_VERSION_5_0: String = "5.0"
		val DATA_VERSION_5_5: String = "5.5"
		val DATA_VERSION_5_6: String = "5.6"
		val DATA_VERSION: String = DataParser.DATA_VERSION_5_6

		val DATA_VERSIONS: Array<String> = arrayOf(
			DataParser.DATA_VERSION_4_0,
			DataParser.DATA_VERSION_4_5,
			DataParser.DATA_VERSION_5_0,
			DataParser.DATA_VERSION_5_5,
			DataParser.DATA_VERSION_5_6
		)

		val TEXTURE_ATLAS: String = "textureAtlas"
		val SUB_TEXTURE: String = "SubTexture"
		val FORMAT: String = "format"
		val IMAGE_PATH: String = "imagePath"
		val WIDTH: String = "width"
		val HEIGHT: String = "height"
		val ROTATED: String = "rotated"
		val FRAME_X: String = "frameX"
		val FRAME_Y: String = "frameY"
		val FRAME_WIDTH: String = "frameWidth"
		val FRAME_HEIGHT: String = "frameHeight"

		val DRADON_BONES: String = "dragonBones"
		val USER_DATA: String = "userData"
		val ARMATURE: String = "armature"
		val CANVAS: String = "canvas"
		val BONE: String = "bone"
		val SURFACE: String = "surface"
		val SLOT: String = "slot"
		val CONSTRAINT: String = "constraint"
		val SKIN: String = "skin"
		val DISPLAY: String = "display"
		val FRAME: String = "frame"
		val IK: String = "ik"
		val PATH_CONSTRAINT: String = "path"

		val ANIMATION: String = "animation"
		val TIMELINE: String = "timeline"
		val FFD: String = "ffd"
		val TRANSLATE_FRAME: String = "translateFrame"
		val ROTATE_FRAME: String = "rotateFrame"
		val SCALE_FRAME: String = "scaleFrame"
		val DISPLAY_FRAME: String = "displayFrame"
		val COLOR_FRAME: String = "colorFrame"
		val DEFAULT_ACTIONS: String = "defaultActions"
		val ACTIONS: String = "actions"
		val EVENTS: String = "events"

		val INTS: String = "ints"
		val FLOATS: String = "floats"
		val STRINGS: String = "strings"

		val TRANSFORM: String = "transform"
		val PIVOT: String = "pivot"
		val AABB: String = "aabb"
		val COLOR: String = "color"

		val VERSION: String = "version"
		val COMPATIBLE_VERSION: String = "compatibleVersion"
		val FRAME_RATE: String = "frameRate"
		val TYPE: String = "type"
		val SUB_TYPE: String = "subType"
		val NAME: String = "name"
		val PARENT: String = "parent"
		val TARGET: String = "target"
		val STAGE: String = "stage"
		val SHARE: String = "share"
		val PATH: String = "path"
		val LENGTH: String = "length"
		val DISPLAY_INDEX: String = "displayIndex"
		val Z_ORDER: String = "zOrder"
		val Z_INDEX: String = "zIndex"
		val BLEND_MODE: String = "blendMode"
		val INHERIT_TRANSLATION: String = "inheritTranslation"
		val INHERIT_ROTATION: String = "inheritRotation"
		val INHERIT_SCALE: String = "inheritScale"
		val INHERIT_REFLECTION: String = "inheritReflection"
		val INHERIT_ANIMATION: String = "inheritAnimation"
		val INHERIT_DEFORM: String = "inheritDeform"
		val SEGMENT_X: String = "segmentX"
		val SEGMENT_Y: String = "segmentY"
		val BEND_POSITIVE: String = "bendPositive"
		val CHAIN: String = "chain"
		val WEIGHT: String = "weight"

		val BLEND_TYPE: String = "blendType"
		val FADE_IN_TIME: String = "fadeInTime"
		val PLAY_TIMES: String = "playTimes"
		val SCALE: String = "scale"
		val OFFSET: String = "offset"
		val POSITION: String = "position"
		val DURATION: String = "duration"
		val TWEEN_EASING: String = "tweenEasing"
		val TWEEN_ROTATE: String = "tweenRotate"
		val TWEEN_SCALE: String = "tweenScale"
		val CLOCK_WISE: String = "clockwise"
		val CURVE: String = "curve"
		val SOUND: String = "sound"
		val EVENT: String = "event"
		val ACTION: String = "action"

		val X: String = "x"
		val Y: String = "y"
		val SKEW_X: String = "skX"
		val SKEW_Y: String = "skY"
		val SCALE_X: String = "scX"
		val SCALE_Y: String = "scY"
		val VALUE: String = "value"
		val ROTATE: String = "rotate"
		val SKEW: String = "skew"
		val ALPHA: String = "alpha"

		val ALPHA_OFFSET: String = "aO"
		val RED_OFFSET: String = "rO"
		val GREEN_OFFSET: String = "gO"
		val BLUE_OFFSET: String = "bO"
		val ALPHA_MULTIPLIER: String = "aM"
		val RED_MULTIPLIER: String = "rM"
		val GREEN_MULTIPLIER: String = "gM"
		val BLUE_MULTIPLIER: String = "bM"

		val UVS: String = "uvs"
		val VERTICES: String = "vertices"
		val TRIANGLES: String = "triangles"
		val WEIGHTS: String = "weights"
		val SLOT_POSE: String = "slotPose"
		val BONE_POSE: String = "bonePose"

		val BONES: String = "bones"
		val POSITION_MODE: String = "positionMode"
		val SPACING_MODE: String = "spacingMode"
		val ROTATE_MODE: String = "rotateMode"
		val SPACING: String = "spacing"
		val ROTATE_OFFSET: String = "rotateOffset"
		val ROTATE_MIX: String = "rotateMix"
		val TRANSLATE_MIX: String = "translateMix"

		val TARGET_DISPLAY: String = "targetDisplay"
		val CLOSED: String = "closed"
		val CONSTANT_SPEED: String = "constantSpeed"
		val VERTEX_COUNT: String = "vertexCount"
		val LENGTHS: String = "lengths"

		val GOTO_AND_PLAY: String = "gotoAndPlay"

		val DEFAULT_NAME: String = "default"

		fun _getArmatureType(value: String?): ArmatureType {
			return when (value?.toLowerCase()) {
				null -> ArmatureType.Armature
				"stage" -> ArmatureType.Stage
				"armature" -> ArmatureType.Armature
				"movieclip" -> ArmatureType.MovieClip
				else -> ArmatureType.Armature
			}
		}

		fun _getBoneTypeIsSurface(value: String?): Boolean {
			return when (value?.toLowerCase()) {
				null -> false
				"bone" -> false
				"surface" -> true
				else -> false
			}
		}

		fun _getPositionMode(value: String?): PositionMode {
			return when (value?.toLowerCase()) {
				null -> PositionMode.Percent
				"percent" -> PositionMode.Percent
				"fixed" -> PositionMode.Fixed
				else -> PositionMode.Percent
			}
		}

		fun _getSpacingMode(value: String?): SpacingMode {
			return when (value?.toLowerCase()) {
				null -> SpacingMode.Length
				"length" -> SpacingMode.Length
				"percent" -> SpacingMode.Percent
				"fixed" -> SpacingMode.Fixed
				else -> SpacingMode.Length
			}
		}

		fun _getRotateMode(value: String?): RotateMode {
			return when (value?.toLowerCase()) {
				null -> RotateMode.Tangent
				"tangent" -> RotateMode.Tangent
				"chain" -> RotateMode.Chain
				"chainscale" -> RotateMode.ChainScale
				else -> RotateMode.Tangent
			}
		}

		fun _getDisplayType(value: String?): DisplayType {
			return when (value?.toLowerCase()) {
				null -> DisplayType.Image
				"image" -> DisplayType.Image
				"mesh" -> DisplayType.Mesh
				"armature" -> DisplayType.Armature
				"boundingbox" -> DisplayType.BoundingBox
				"path" -> DisplayType.Path
				else -> DisplayType.Image
			}
		}

		fun _getBoundingBoxType(value: String?): BoundingBoxType {
			return when (value?.toLowerCase()) {
				null -> BoundingBoxType.Rectangle
				"rectangle" -> BoundingBoxType.Rectangle
				"ellipse" -> BoundingBoxType.Ellipse
				"polygon" -> BoundingBoxType.Polygon
				else -> BoundingBoxType.Rectangle
			}
		}

		fun _getBlendMode(value: String?): BlendMode {
			return when (value?.toLowerCase()) {
				null -> BlendMode.Normal
				"normal" -> BlendMode.Normal
				"add" -> BlendMode.Add
				"alpha" -> BlendMode.Alpha
				"darken" -> BlendMode.Darken
				"difference" -> BlendMode.Difference
				"erase" -> BlendMode.Erase
				"hardlight" -> BlendMode.HardLight
				"invert" -> BlendMode.Invert
				"layer" -> BlendMode.Layer
				"lighten" -> BlendMode.Lighten
				"multiply" -> BlendMode.Multiply
				"overlay" -> BlendMode.Overlay
				"screen" -> BlendMode.Screen
				"subtract" -> BlendMode.Subtract
				else -> BlendMode.Normal
			}
		}

		fun _getAnimationBlendType(value: String?): AnimationBlendType {
			return when (value?.toLowerCase()) {
				null -> AnimationBlendType.None
				"none" -> AnimationBlendType.None
				"1d" -> AnimationBlendType.E1D
				else -> AnimationBlendType.None
			}
		}

		fun _getActionType(value: String?): ActionType {
			return when (value?.toLowerCase()) {
				null -> ActionType.Play
				"play" -> ActionType.Play
				"frame" -> ActionType.Frame
				"sound" -> ActionType.Sound
				else -> ActionType.Play
			}
		}

		fun parseDragonBonesDataJson(data: String, scale: Double = 1.0, pool: BaseObjectPool = BaseObjectPool()): DragonBonesData? {
			return ObjectDataParser(pool).parseDragonBonesData(Json.parseFast(data), scale)
		}
	}

	abstract fun parseDragonBonesData(rawData: Any?, scale: Double = 1.0): DragonBonesData?
	abstract fun parseTextureAtlasData(rawData: Any?, textureAtlasData: TextureAtlasData, scale: Double = 1.0): Boolean
}
