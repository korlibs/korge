package com.codeazur.as3swf.data

import com.codeazur.as3swf.SWFData
import com.codeazur.as3swf.data.actions.IAction
import com.codeazur.as3swf.data.consts.*
import com.codeazur.as3swf.data.etc.CurvedEdge
import com.codeazur.as3swf.data.etc.IEdge
import com.codeazur.as3swf.data.etc.StraightEdge
import com.codeazur.as3swf.exporters.ShapeExporter
import com.codeazur.as3swf.utils.*
import com.soywiz.korim.vector.Context2d
import com.soywiz.korio.lang.format
import com.soywiz.korio.util.toString
import com.soywiz.korma.Matrix2d
import com.soywiz.korma.geom.Point2d
import com.soywiz.korma.geom.Rectangle
import kotlin.collections.set
import kotlin.math.*

class SWFActionValue {
	var type: Int = 0
	var string: String? = null
	var number: Double = 0.0
	var register: Int = 0
	var boolean: Boolean = false
	var integer: Int = 0
	var constant: Int = 0

	companion object {
		private var ba = SWFActionValue.initTmpBuffer()

		private fun initTmpBuffer(): FlashByteArray {
			val baTmp = FlashByteArray()
			baTmp.endian = Endian.LITTLE_ENDIAN
			baTmp.length = 8
			return baTmp
		}
	}

	fun parse(data: SWFData): Unit {
		type = data.readUI8()
		when (type) {
			ActionValueType.STRING -> string = data.readString()
			ActionValueType.FLOAT -> number = data.readFLOAT()
			ActionValueType.NULL -> Unit
			ActionValueType.UNDEFINED -> Unit
			ActionValueType.REGISTER -> register = data.readUI8()
			ActionValueType.BOOLEAN -> boolean = (data.readUI8() != 0)
			ActionValueType.DOUBLE -> {
				SWFActionValue.ba.position = 0
				SWFActionValue.ba[4] = data.readUI8()
				SWFActionValue.ba[5] = data.readUI8()
				SWFActionValue.ba[6] = data.readUI8()
				SWFActionValue.ba[7] = data.readUI8()
				SWFActionValue.ba[0] = data.readUI8()
				SWFActionValue.ba[1] = data.readUI8()
				SWFActionValue.ba[2] = data.readUI8()
				SWFActionValue.ba[3] = data.readUI8()
				number = SWFActionValue.ba.readDouble()
			}
			ActionValueType.INTEGER -> integer = data.readUI32()
			ActionValueType.CONSTANT_8 -> constant = data.readUI8()
			ActionValueType.CONSTANT_16 -> constant = data.readUI16()
			else ->
				throw(Error("Unknown ActionValueType: " + type))
		}
	}

	override fun toString(): String {
		return when (type) {
			ActionValueType.STRING -> "$string (string)"
			ActionValueType.FLOAT -> "$number (float)"
			ActionValueType.NULL -> "null"
			ActionValueType.UNDEFINED -> "undefined"
			ActionValueType.REGISTER -> "$register (register)"
			ActionValueType.BOOLEAN -> "$boolean (boolean)"
			ActionValueType.DOUBLE -> "$number (double)"
			ActionValueType.INTEGER -> "$integer (integer)"
			ActionValueType.CONSTANT_8 -> "$constant (constant8)"
			ActionValueType.CONSTANT_16 -> "$constant (constant16)"
			else -> "unknown"
		}
	}

	fun toBytecodeString(cpool: List<String>): String {
		return when (type) {
			ActionValueType.STRING -> "\"" + (string) + "\""
			ActionValueType.FLOAT, ActionValueType.DOUBLE -> {
				val str = number.toString()
				if (str.indexOf(".") == -1) {
					str + ".0"
				} else {
					str
				}
			}
			ActionValueType.NULL -> "null"
			ActionValueType.UNDEFINED -> "undefined"
			ActionValueType.REGISTER -> "$" + register
			ActionValueType.BOOLEAN -> boolean.toString()
			ActionValueType.INTEGER -> integer.toString()
			ActionValueType.CONSTANT_8, ActionValueType.CONSTANT_16 -> "\"" + (cpool[constant]) + "\""
			else -> "UNKNOWN"
		}
	}
}

class SWFButtonCondAction {
	var condActionSize = 0
	var condIdleToOverDown = false
	var condOutDownToIdle = false
	var condOutDownToOverDown = false
	var condOverDownToOutDown = false
	var condOverDownToOverUp = false
	var condOverUpToOverDown = false
	var condOverUpToIdle = false
	var condIdleToOverUp = false
	var condOverDownToIdle = false
	var condKeyPress = 0

	protected var actions = ArrayList<IAction>()

	protected var labelCount: Int = 0

	fun parse(data: SWFData): Unit {
		val flags: Int = (data.readUI8() shl 8) or data.readUI8()
		condIdleToOverDown = ((flags and 0x8000) != 0)
		condOutDownToIdle = ((flags and 0x4000) != 0)
		condOutDownToOverDown = ((flags and 0x2000) != 0)
		condOverDownToOutDown = ((flags and 0x1000) != 0)
		condOverDownToOverUp = ((flags and 0x0800) != 0)
		condOverUpToOverDown = ((flags and 0x0400) != 0)
		condOverUpToIdle = ((flags and 0x0200) != 0)
		condIdleToOverUp = ((flags and 0x0100) != 0)
		condOverDownToIdle = ((flags and 0x0001) != 0)
		condKeyPress = (flags and 0xff) ushr 1
		var action: IAction?
		while (true) {
			action = data.readACTIONRECORD()
			if (action == null) break
			actions.add(action)
		}
		labelCount = com.codeazur.as3swf.data.actions.Action.resolveOffsets(actions)
	}

	fun toString(indent: Int = 0, flags: Int = 0): String {
		val a = arrayListOf<String>()
		if (condIdleToOverDown) a.add("idleToOverDown")
		if (condOutDownToIdle) a.add("outDownToIdle")
		if (condOutDownToOverDown) a.add("outDownToOverDown")
		if (condOverDownToOutDown) a.add("overDownToOutDown")
		if (condOverDownToOverUp) a.add("overDownToOverUp")
		if (condOverUpToOverDown) a.add("overUpToOverDown")
		if (condOverUpToIdle) a.add("overUpToIdle")
		if (condIdleToOverUp) a.add("idleToOverUp")
		if (condOverDownToIdle) a.add("overDownToIdle")
		var str: String = "CondActionRecord (" + a.joinToString(", ") + ")"
		if (condKeyPress > 0) str += ", KeyPress: " + condKeyPress
		if ((flags and com.codeazur.as3swf.SWF.TOSTRING_FLAG_AVM1_BYTECODE) == 0) {
			for (i in 0 until actions.size) {
				str += "\n" + " ".repeat(indent + 2) + "[" + i + "] " + actions[i].toString(indent + 2)
			}
		} else {
			val context: com.codeazur.as3swf.data.actions.ActionExecutionContext = com.codeazur.as3swf.data.actions.ActionExecutionContext(actions, arrayListOf(), labelCount)
			for (i in 0 until actions.size) {
				str += "\n" + " ".repeat(indent + 4) + actions[i].toBytecode(indent + 4, context)
			}
			if (context.endLabel != null) {
				str += "\n" + " ".repeat(indent + 4) + context.endLabel + ":"
			}
		}
		return str
	}
}

class SWFButtonRecord {
	var hasBlendMode: Boolean = false
	var hasFilterList: Boolean = false
	var stateHitTest: Boolean = false
	var stateDown: Boolean = false
	var stateOver: Boolean = false
	var stateUp: Boolean = false

	var characterId: Int = 0
	var placeDepth: Int = 0
	var placeMatrix: SWFMatrix? = null
	var colorTransform: SWFColorTransformWithAlpha? = null
	var blendMode: Int = 0

	protected var filterList = ArrayList<com.codeazur.as3swf.data.filters.IFilter>()

	fun parse(data: SWFData, level: Int = 1): Unit {
		val flags: Int = data.readUI8()
		stateHitTest = ((flags and 0x08) != 0)
		stateDown = ((flags and 0x04) != 0)
		stateOver = ((flags and 0x02) != 0)
		stateUp = ((flags and 0x01) != 0)
		characterId = data.readUI16()
		placeDepth = data.readUI16()
		placeMatrix = data.readMATRIX()
		if (level >= 2) {
			colorTransform = data.readCXFORMWITHALPHA()
			hasFilterList = ((flags and 0x10) != 0)
			if (hasFilterList) {
				val numberOfFilters: Int = data.readUI8()
				for (i in 0 until numberOfFilters) {
					filterList.add(data.readFILTER())
				}
			}
			hasBlendMode = ((flags and 0x20) != 0)
			if (hasBlendMode) {
				blendMode = data.readUI8()
			}
		}
	}

	fun toString(indent: Int = 0): String {
		var str: String = "Depth: $placeDepth, CharacterID: $characterId, States: "
		val states = arrayListOf<String>()
		if (stateUp) states.add("up")
		if (stateOver) states.add("over")
		if (stateDown) states.add("down")
		if (stateHitTest) states.add("hit")
		str += states.joinToString(",")
		if (hasBlendMode) str += ", BlendMode: " + com.codeazur.as3swf.data.consts.BlendMode.toString(blendMode)
		if (placeMatrix != null && !placeMatrix!!.isIdentity()) str += "\n" + " ".repeat(indent + 2) + "Matrix: " + placeMatrix
		if (colorTransform != null && !colorTransform!!.isIdentity()) str += "\n" + " ".repeat(indent + 2) + "ColorTransform: " + colorTransform
		if (hasFilterList) {
			str += "\n" + " ".repeat(indent + 2) + "Filters:"
			for (i in 0 until filterList.size) {
				str += "\n" + " ".repeat(indent + 4) + "[" + i + "] " + filterList[i].toString(indent + 4)
			}
		}
		return str
	}
}

class SWFClipActionRecord {
	lateinit var eventFlags: com.codeazur.as3swf.data.SWFClipEventFlags
	var keyCode: Int = 0

	var actions = ArrayList<IAction>()

	var labelCount: Int = 0

	fun parse(data: SWFData, version: Int): Unit {
		eventFlags = data.readCLIPEVENTFLAGS(version)
		data.readUI32() // actionRecordSize, not needed here
		if (eventFlags.keyPressEvent) {
			keyCode = data.readUI8()
		}
		while (true) {
			actions.add(data.readACTIONRECORD() ?: break)
		}
		labelCount = com.codeazur.as3swf.data.actions.Action.resolveOffsets(actions)
	}

	fun toString(indent: Int = 0, flags: Int = 0): String {
		var str: String = "ClipActionRecord (" + eventFlags.toString() + ")"
		if (keyCode > 0) {
			str += ", KeyCode: " + keyCode
		}
		if ((flags and com.codeazur.as3swf.SWF.TOSTRING_FLAG_AVM1_BYTECODE) == 0) {
			for (i in 0 until actions.size) {
				str += "\n" + " ".repeat(indent + 2) + "[" + i + "] " + actions[i].toString(indent + 2)
			}
		} else {
			val context: com.codeazur.as3swf.data.actions.ActionExecutionContext = com.codeazur.as3swf.data.actions.ActionExecutionContext(actions, arrayListOf(), labelCount)
			for (i in 0 until actions.size) {
				str += "\n" + " ".repeat(indent + 4) + actions[i].toBytecode(indent + 4, context)
			}
			if (context.endLabel != null) {
				str += "\n" + " ".repeat(indent + 4) + context.endLabel + ":"
			}
		}
		return str
	}
}

class SWFClipActions {
	lateinit var eventFlags: com.codeazur.as3swf.data.SWFClipEventFlags

	protected var records = arrayListOf<com.codeazur.as3swf.data.SWFClipActionRecord>()

	fun parse(data: SWFData, version: Int): Unit {
		data.readUI16() // reserved, always 0
		eventFlags = data.readCLIPEVENTFLAGS(version)
		while (true) records.add(data.readCLIPACTIONRECORD(version) ?: break)
	}

	fun toString(indent: Int = 0, flags: Int = 0): String {
		var str: String = "ClipActions (" + eventFlags.toString() + "):"
		for (i in 0 until records.size) {
			str += "\n" + " ".repeat(indent + 2) + "[" + i + "] " + records[i].toString(indent + 2, flags)
		}
		return str
	}
}

class SWFClipEventFlags {
	var keyUpEvent = false
	var keyDownEvent = false
	var mouseUpEvent = false
	var mouseDownEvent = false
	var mouseMoveEvent = false
	var unloadEvent = false
	var enterFrameEvent = false
	var loadEvent = false
	var dragOverEvent = false // SWF6
	var rollOutEvent = false // SWF6
	var rollOverEvent = false // SWF6
	var releaseOutsideEvent = false // SWF6
	var releaseEvent = false // SWF6
	var pressEvent = false // SWF6
	var initializeEvent = false // SWF6
	var dataEvent = false
	var constructEvent = false // SWF7
	var keyPressEvent = false // SWF6
	var dragOutEvent = false // SWF6

	fun parse(data: SWFData, version: Int): Unit {
		val flags1: Int = data.readUI8()
		keyUpEvent = ((flags1 and 0x80) != 0)
		keyDownEvent = ((flags1 and 0x40) != 0)
		mouseUpEvent = ((flags1 and 0x20) != 0)
		mouseDownEvent = ((flags1 and 0x10) != 0)
		mouseMoveEvent = ((flags1 and 0x08) != 0)
		unloadEvent = ((flags1 and 0x04) != 0)
		enterFrameEvent = ((flags1 and 0x02) != 0)
		loadEvent = ((flags1 and 0x01) != 0)
		val flags2: Int = data.readUI8()
		dragOverEvent = ((flags2 and 0x80) != 0)
		rollOutEvent = ((flags2 and 0x40) != 0)
		rollOverEvent = ((flags2 and 0x20) != 0)
		releaseOutsideEvent = ((flags2 and 0x10) != 0)
		releaseEvent = ((flags2 and 0x08) != 0)
		pressEvent = ((flags2 and 0x04) != 0)
		initializeEvent = ((flags2 and 0x02) != 0)
		dataEvent = ((flags2 and 0x01) != 0)
		if (version >= 6) {
			val flags3: Int = data.readUI8()
			constructEvent = ((flags3 and 0x04) != 0)
			keyPressEvent = ((flags3 and 0x02) != 0)
			dragOutEvent = ((flags3 and 0x01) != 0)
			data.readUI8() // reserved, always 0
		}
	}

	override fun toString(): String {
		val a = arrayListOf<String>()
		if (keyUpEvent) a.add("keyup")
		if (keyDownEvent) a.add("keydown")
		if (mouseUpEvent) a.add("mouseup")
		if (mouseDownEvent) a.add("mousedown")
		if (mouseMoveEvent) a.add("mousemove")
		if (unloadEvent) a.add("unload")
		if (enterFrameEvent) a.add("enterframe")
		if (loadEvent) a.add("load")
		if (dragOverEvent) a.add("dragover")
		if (rollOutEvent) a.add("rollout")
		if (rollOverEvent) a.add("rollover")
		if (releaseOutsideEvent) a.add("releaseoutside")
		if (releaseEvent) a.add("release")
		if (pressEvent) a.add("press")
		if (initializeEvent) a.add("initialize")
		if (dataEvent) a.add("data")
		if (constructEvent) a.add("construct")
		if (keyPressEvent) a.add("keypress")
		if (dragOutEvent) a.add("dragout")
		return a.joinToString(",")
	}
}

open class SWFColorTransform {
	protected var _rMult = 256
	protected var _gMult = 256
	protected var _bMult = 256
	protected var _aMult = 256

	protected var _rAdd = 0
	protected var _gAdd = 0
	protected var _bAdd = 0
	protected var _aAdd = 0

	var hasMultTerms = false
	var hasAddTerms = false

	var rMult: Double get() = (_rMult.toDouble() / 256); set(value) = run { _rMult = clamp((value * 256).toInt()); updateHasMultTerms() }
	var gMult: Double get() = (_gMult.toDouble() / 256); set(value) = run { _gMult = clamp((value * 256).toInt()); updateHasMultTerms() }
	var bMult: Double get() = (_bMult.toDouble() / 256); set(value) = run { _bMult = clamp((value * 256).toInt()); updateHasMultTerms() }
	open var aMult: Double get() = (_aMult.toDouble() / 256); set(value) = run { _aMult = clamp((value * 256).toInt()); updateHasMultTerms() }

	var rAdd: Int get() = _rAdd; set(value) = run { _rAdd = clamp(value); updateHasAddTerms() }
	var gAdd: Int get() = _gAdd; set(value) = run { _gAdd = clamp(value); updateHasAddTerms() }
	var bAdd: Int get() = _bAdd; set(value) = run { _bAdd = clamp(value); updateHasAddTerms() }
	open var aAdd: Int get() = _aAdd; set(value) = run { _aAdd = clamp(value); updateHasAddTerms() }

	open fun parse(data: SWFData): Unit {
		data.resetBitsPending()
		hasAddTerms = (data.readUB(1) == 1)
		hasMultTerms = (data.readUB(1) == 1)
		val bits = data.readUB(4)
		if (hasMultTerms) {
			_rMult = data.readSB(bits)
			_gMult = data.readSB(bits)
			_bMult = data.readSB(bits)
		} else {
			_rMult = 256
			_gMult = 256
			_bMult = 256
		}
		if (hasAddTerms) {
			_rAdd = data.readSB(bits)
			_gAdd = data.readSB(bits)
			_bAdd = data.readSB(bits)
		} else {
			_rAdd = 0
			_gAdd = 0
			_bAdd = 0
		}
	}

	protected open fun updateHasMultTerms(): Unit = run { hasMultTerms = (_rMult != 256) || (_gMult != 256) || (_bMult != 256) }
	protected open fun updateHasAddTerms(): Unit = run { hasAddTerms = (_rAdd != 0) || (_gAdd != 0) || (_bAdd != 0) }

	protected fun clamp(value: Int): Int = min(max(value, -32768), 32767)
	fun isIdentity(): Boolean = !hasMultTerms && !hasAddTerms
	override fun toString(): String = "($rMult,$gMult,$bMult,$rAdd,$gAdd,$bAdd)"
}

class SWFColorTransformWithAlpha : SWFColorTransform() {
	//override var aMult: Double get() = _aMult.toDouble() / 256; set(value) = run { _aMult = clamp((value * 256).toInt()); updateHasMultTerms() }
	//override var aAdd: Int get() = _aAdd; set(value) = run { _aAdd = clamp(value); updateHasAddTerms() }

	//override val colorTransform: flash.geom.ColorTransform get() = flash.geom.ColorTransform(rMult, gMult, bMult, aMult, rAdd, gAdd, bAdd, aAdd)

	override fun parse(data: SWFData): Unit {
		data.resetBitsPending()
		hasAddTerms = (data.readUB(1) == 1)
		hasMultTerms = (data.readUB(1) == 1)
		val bits: Int = data.readUB(4)
		if (hasMultTerms) {
			_rMult = data.readSB(bits)
			_gMult = data.readSB(bits)
			_bMult = data.readSB(bits)
			_aMult = data.readSB(bits)
		} else {
			_rMult = 256
			_gMult = 256
			_bMult = 256
			_aMult = 256
		}
		if (hasAddTerms) {
			_rAdd = data.readSB(bits)
			_gAdd = data.readSB(bits)
			_bAdd = data.readSB(bits)
			_aAdd = data.readSB(bits)
		} else {
			_rAdd = 0
			_gAdd = 0
			_bAdd = 0
			_aAdd = 0
		}
	}

	override fun updateHasMultTerms(): Unit = run { hasMultTerms = (_rMult != 256) || (_gMult != 256) || (_bMult != 256) || (_aMult != 256) }
	override fun updateHasAddTerms(): Unit = run { hasAddTerms = (_rAdd != 0) || (_gAdd != 0) || (_bAdd != 0) || (_aAdd != 0) }

	override fun toString(): String = "($rMult,$gMult,$bMult,$aMult,$rAdd,$gAdd,$bAdd,$aAdd)"
}

class SWFFillStyle {
	var type: Int = 0

	var rgb: Int = 0
	var gradient: com.codeazur.as3swf.data.SWFGradient? = null
	var gradientMatrix: SWFMatrix? = null
	var bitmapId: Int = 0
	var bitmapMatrix: SWFMatrix? = null

	protected var _level: Int = 0

	fun parse(data: SWFData, level: Int = 1): Unit {
		_level = level
		type = data.readUI8()
		when (type) {
			0x00 -> {
				rgb = if (level <= 2) data.readRGB() else data.readRGBA()
			}
			0x10, 0x12, 0x13 -> {
				gradientMatrix = data.readMATRIX(); gradient = if (type == 0x13) data.readFOCALGRADIENT(level) else data.readGRADIENT(level)
			}
			0x40, 0x41, 0x42, 0x43 -> {
				bitmapId = data.readUI16(); bitmapMatrix = data.readMATRIX()
			}
			else -> throw Error("Unknown fill style type: 0x${type.toString(16)}")
		}
	}

	override fun toString(): String {
		var str: String = "[SWFFillStyle] Type: " + "%02x".format(type)
		when (type) {
			0x00 -> str += " (solid), Color: " + (if (_level <= 2) ColorUtils.rgbToString(rgb) else ColorUtils.rgbaToString(rgb))
			0x10 -> str += " (linear gradient), Gradient: $gradient, Matrix: $gradientMatrix"
			0x12 -> str += " (radial gradient), Gradient: $gradient, Matrix: $gradientMatrix"
			0x13 -> str += " (focal radial gradient), Gradient: $gradient, Matrix: $gradientMatrix, FocalPoint: ${gradient?.focalPoint}"
			0x40 -> str += " (repeating bitmap), BitmapID: $bitmapId"
			0x41 -> str += " (clipped bitmap), BitmapID: $bitmapId"
			0x42 -> str += " (non-smoothed repeating bitmap), BitmapID: $bitmapId"
			0x43 -> str += " (non-smoothed clipped bitmap), BitmapID: $bitmapId"
		}
		return str
	}
}

class SWFFocalGradient : SWFGradient() {
	override fun parse(data: SWFData, level: Int): Unit {
		super.parse(data, level)
		focalPoint = data.readFIXED8()
	}

	override fun toString(): String = "(" + records.joinToString(",") + ")"
}

class SWFFrameLabel(var frameNumber: Int, var name: String) {
	override fun toString() = "Frame: $frameNumber, Name: $name"
}

class SWFGlyphEntry {
	var index: Int = 0
	var advance: Int = 0

	fun parse(data: SWFData, glyphBits: Int, advanceBits: Int): Unit {
		// GLYPHENTRYs are not byte aligned
		index = data.readUB(glyphBits)
		advance = data.readSB(advanceBits)
	}

	override fun toString(): String {
		return "[SWFGlyphEntry] Index: " + index.toString() + ", Advance: " + advance.toString()
	}
}

open class SWFGradient {
	var spreadMode: GradientSpreadMode = GradientSpreadMode.PAD
	var interpolationMode: GradientInterpolationMode = GradientInterpolationMode.NORMAL

	// Forward declarations of properties in SWFFocalGradient
	var focalPoint: Double = 0.0

	var records = ArrayList<com.codeazur.as3swf.data.SWFGradientRecord>()

	open fun parse(data: SWFData, level: Int): Unit {
		data.resetBitsPending()
		spreadMode = GradientSpreadMode[data.readUB(2)]
		interpolationMode = GradientInterpolationMode[data.readUB(2)]
		val numGradients: Int = data.readUB(4)
		for (i in 0 until numGradients) records.add(data.readGRADIENTRECORD(level))
	}

	override fun toString(): String = "(${records.joinToString(",")}), SpreadMode: $spreadMode, InterpolationMode: $interpolationMode"
}

class SWFGradientRecord {
	var ratio: Int = 0
	var color: Int = 0

	protected var _level: Int = 0

	fun parse(data: SWFData, level: Int): Unit {
		_level = level
		ratio = data.readUI8()
		color = if (level <= 2) data.readRGB() else data.readRGBA()
	}

	override fun toString(): String = "[" + ratio + "," + (if (_level <= 2) ColorUtils.rgbToString(color) else ColorUtils.rgbaToString(color)) + "]"
}

class SWFKerningRecord {
	var code1: Int = 0
	var code2: Int = 0
	var adjustment: Int = 0

	fun parse(data: SWFData, wideCodes: Boolean): Unit {
		code1 = if (wideCodes) data.readUI16() else data.readUI8()
		code2 = if (wideCodes) data.readUI16() else data.readUI8()
		adjustment = data.readSI16()
	}

	fun toString(indent: Int = 0): String = "Code1: $code1, Code2: $code2, Adjustment: $adjustment"
}

open class SWFLineStyle {
	var width: Int = 0
	var color: Int = 0

	var _level: Int = 0

	// Forward declaration of SWFLineStyle2 properties
	var startCapsStyle: LineCapsStyle = LineCapsStyle.ROUND
	var endCapsStyle: LineCapsStyle = LineCapsStyle.ROUND
	var jointStyle: Int = LineJointStyle.ROUND
	var hasFillFlag: Boolean = false
	var noHScaleFlag: Boolean = false
	var noVScaleFlag: Boolean = false
	var pixelHintingFlag: Boolean = false
	var noClose: Boolean = false
	var miterLimitFactor: Double = 3.0
	var fillType: SWFFillStyle? = null

	open fun parse(data: SWFData, level: Int = 1): Unit {
		_level = level
		width = data.readUI16()
		color = if (level <= 2) data.readRGB() else data.readRGBA()
	}

	override fun toString(): String = "[SWFLineStyle] Width: " + width + " Color: " + (if (_level <= 2) ColorUtils.rgbToString(color) else ColorUtils.rgbaToString(color))
}

class SWFLineStyle2 : SWFLineStyle() {
	override fun parse(data: SWFData, level: Int): Unit {
		width = data.readUI16()
		startCapsStyle = LineCapsStyle[data.readUB(2)]
		jointStyle = data.readUB(2)
		hasFillFlag = (data.readUB(1) == 1)
		noHScaleFlag = (data.readUB(1) == 1)
		noVScaleFlag = (data.readUB(1) == 1)
		pixelHintingFlag = (data.readUB(1) == 1)
		data.readUB(5)
		noClose = (data.readUB(1) == 1)
		endCapsStyle = LineCapsStyle[data.readUB(2)]
		if (jointStyle == com.codeazur.as3swf.data.consts.LineJointStyle.MITER) miterLimitFactor = data.readFIXED8()
		if (hasFillFlag) {
			fillType = data.readFILLSTYLE(level)
		} else {
			color = data.readRGBA()
		}
	}

	override fun toString(): String {
		var str: String = "[SWFLineStyle2] Width: " + width + ", " +
			"StartCaps: " + (startCapsStyle) + ", " +
			"EndCaps: " + (endCapsStyle) + ", " +
			"Joint: " + LineJointStyle.toString(jointStyle) + ", "
		if (noClose) str += "NoClose, "
		if (noHScaleFlag) str += "NoHScale, "
		if (noVScaleFlag) str += "NoVScale, "
		if (pixelHintingFlag) str += "PixelHinting, "
		if (hasFillFlag) {
			str += "Fill: " + fillType.toString()
		} else {
			str += "Color: " + ColorUtils.rgbaToString(color)
		}
		return str
	}
}

class SWFMatrix {
	var scaleX: Double = 1.0
	var scaleY: Double = 1.0
	var rotateSkew0: Double = 0.0
	var rotateSkew1: Double = 0.0
	var translateX: Int = 0
	var translateY: Int = 0

	var xscale: Double = 0.0
	var yscale: Double = 0.0
	var rotation: Double = 0.0

	val matrix: Matrix2d get() = Matrix2d(scaleX, rotateSkew0, rotateSkew1, scaleY, translateX.toDouble() / 20.0, translateY.toDouble() / 20.0)

	fun parse(data: SWFData) {
		data.resetBitsPending()
		scaleX = 1.0
		scaleY = 1.0
		if (data.readUB(1) == 1) {
			val scaleBits = data.readUB(5)
			scaleX = data.readFB(scaleBits)
			scaleY = data.readFB(scaleBits)
		}
		rotateSkew0 = 0.0
		rotateSkew1 = 0.0
		if (data.readUB(1) == 1) {
			val rotateBits = data.readUB(5)
			rotateSkew0 = data.readFB(rotateBits)
			rotateSkew1 = data.readFB(rotateBits)
		}
		val translateBits = data.readUB(5)
		translateX = data.readSB(translateBits)
		translateY = data.readSB(translateBits)
		// conversion to rotation, xscale, yscale
		val px = matrix.deltaTransformPoint(Point2d(0.0, 1.0))
		rotation = ((180 / PI) * atan2(px.y, px.x) - 90)
		if (rotation < 0) rotation += 360
		xscale = sqrt(scaleX * scaleX + rotateSkew0 * rotateSkew0)
		yscale = sqrt(rotateSkew1 * rotateSkew1 + scaleY * scaleY)
	}

	fun isIdentity(): Boolean = (scaleX == 1.0 && scaleY == 1.0 && rotateSkew0 == 0.0 && rotateSkew1 == 0.0 && translateX == 0 && translateY == 0)
	override fun toString(): String = "($scaleX,$rotateSkew0,$rotateSkew1,$scaleY,$translateX,$translateY)"
}

class SWFMorphFillStyle {
	var type: Int = 0

	var startColor: Int = 0
	var endColor: Int = 0
	lateinit var startGradientMatrix: SWFMatrix
	lateinit var endGradientMatrix: SWFMatrix
	var gradient: SWFMorphGradient? = null
	var bitmapId: Int = 0
	lateinit var startBitmapMatrix: SWFMatrix
	lateinit var endBitmapMatrix: SWFMatrix

	fun parse(data: SWFData, level: Int = 1): Unit {
		type = data.readUI8()
		when (type) {
			0x00 -> {
				startColor = data.readRGBA()
				endColor = data.readRGBA()
			}
			0x10, 0x12, 0x13 -> {
				startGradientMatrix = data.readMATRIX()
				endGradientMatrix = data.readMATRIX()
				gradient = if (type == 0x13) data.readMORPHFOCALGRADIENT(level) else data.readMORPHGRADIENT(level)
			}
			0x40, 0x41, 0x42, 0x43 -> {
				bitmapId = data.readUI16()
				startBitmapMatrix = data.readMATRIX()
				endBitmapMatrix = data.readMATRIX()
			}
			else -> {
				throw(Error("Unknown fill style type: 0x" + type.toString(16)))
			}
		}
	}

	fun getMorphedFillStyle(ratio: Double): SWFFillStyle {
		val fillStyle: SWFFillStyle = SWFFillStyle()
		fillStyle.type = type
		when (type) {
			0x00 -> fillStyle.rgb = ColorUtils.interpolate(startColor, endColor, ratio)
			0x10, 0x12 -> {
				fillStyle.gradientMatrix = MatrixUtils.interpolate(startGradientMatrix, endGradientMatrix, ratio)
				fillStyle.gradient = gradient!!.getMorphedGradient(ratio)
			}
			0x40, 0x41, 0x42, 0x43 -> {
				fillStyle.bitmapId = bitmapId
				fillStyle.bitmapMatrix = MatrixUtils.interpolate(startBitmapMatrix, endBitmapMatrix, ratio)
			}
		}
		return fillStyle
	}

	override fun toString(): String {
		return "[SWFMorphFillStyle] Type: " + type.toString(16) + when (type) {
			0x00 -> " (solid), StartColor: " + ColorUtils.rgbaToString(startColor) + ", EndColor: " + ColorUtils.rgbaToString(endColor)
			0x10 -> " (linear gradient), Gradient: " + gradient
			0x12 -> " (radial gradient), Gradient: " + gradient
			0x13 -> " (focal radial gradient), Gradient: " + gradient
			0x40 -> " (repeating bitmap), BitmapID: " + bitmapId
			0x41 -> " (clipped bitmap), BitmapID: " + bitmapId
			0x42 -> " (non-smoothed repeating bitmap), BitmapID: " + bitmapId
			0x43 -> " (non-smoothed clipped bitmap), BitmapID: " + bitmapId
			else -> ""
		}
	}
}

class SWFMorphFocalGradient : SWFMorphGradient() {
	override fun parse(data: SWFData, level: Int): Unit {
		super.parse(data, level)
		startFocalPoint = data.readFIXED8()
		endFocalPoint = data.readFIXED8()
	}

	override fun getMorphedGradient(ratio: Double): com.codeazur.as3swf.data.SWFGradient {
		val gradient: com.codeazur.as3swf.data.SWFGradient = com.codeazur.as3swf.data.SWFGradient()
		// TODO: focalPoint
		for (i in 0 until records.size) {
			gradient.records.add(records[i].getMorphedGradientRecord(ratio))
		}
		return gradient
	}

	override fun toString(): String {
		return "FocalPoint: " + startFocalPoint + "," + endFocalPoint + " (" + records.joinToString(",") + ")"
	}
}

open class SWFMorphGradient {
	var spreadMode: Int = 0
	var interpolationMode: Int = 0

	// Forward declarations of properties in SWFMorphFocalGradient
	var startFocalPoint: Double = 0.0
	var endFocalPoint: Double = 0.0

	protected var records = ArrayList<com.codeazur.as3swf.data.SWFMorphGradientRecord>()

	open fun parse(data: SWFData, level: Int): Unit {
		data.resetBitsPending()
		spreadMode = data.readUB(2)
		interpolationMode = data.readUB(2)
		val numGradients: Int = data.readUB(4)
		for (i in 0 until numGradients) {
			records.add(data.readMORPHGRADIENTRECORD())
		}
	}

	open fun getMorphedGradient(ratio: Double = 0.0): com.codeazur.as3swf.data.SWFGradient {
		val gradient: com.codeazur.as3swf.data.SWFGradient = com.codeazur.as3swf.data.SWFGradient()
		for (i in 0 until records.size) {
			gradient.records.add(records[i].getMorphedGradientRecord(ratio))
		}
		return gradient
	}

	override fun toString(): String {
		return "(" + records.joinToString(",") + "), spread:" + spreadMode + ", interpolation:" + interpolationMode
	}
}

class SWFMorphGradientRecord {
	var startRatio: Int = 0
	var startColor: Int = 0
	var endRatio: Int = 0
	var endColor: Int = 0

	fun parse(data: SWFData): Unit {
		startRatio = data.readUI8()
		startColor = data.readRGBA()
		endRatio = data.readUI8()
		endColor = data.readRGBA()
	}

	fun getMorphedGradientRecord(ratio: Double = 0.0): com.codeazur.as3swf.data.SWFGradientRecord {
		val gradientRecord: com.codeazur.as3swf.data.SWFGradientRecord = com.codeazur.as3swf.data.SWFGradientRecord()
		gradientRecord.color = ColorUtils.interpolate(startColor, endColor, ratio)
		gradientRecord.ratio = (startRatio + (endRatio - startRatio) * ratio).toInt()
		return gradientRecord
	}

	override fun toString(): String = "[$startRatio,${ColorUtils.rgbaToString(startColor)},$endRatio,${ColorUtils.rgbaToString(endColor)}]"
}

open class SWFMorphLineStyle {
	var startWidth: Int = 0
	var endWidth: Int = 0
	var startColor: Int = 0
	var endColor: Int = 0

	// Forward declaration of SWFMorphLineStyle2 properties
	var startCapsStyle: LineCapsStyle = LineCapsStyle.ROUND
	var endCapsStyle: LineCapsStyle = LineCapsStyle.ROUND
	var jointStyle: Int = LineJointStyle.ROUND
	var hasFillFlag: Boolean = false
	var noHScaleFlag: Boolean = false
	var noVScaleFlag: Boolean = false
	var pixelHintingFlag: Boolean = false
	var noClose: Boolean = false
	var miterLimitFactor: Double = 3.0
	var fillType: com.codeazur.as3swf.data.SWFMorphFillStyle? = null

	open fun parse(data: SWFData, level: Int = 1): Unit {
		startWidth = data.readUI16()
		endWidth = data.readUI16()
		startColor = data.readRGBA()
		endColor = data.readRGBA()
	}

	fun getMorphedLineStyle(ratio: Double = 0.0): SWFLineStyle {
		val lineStyle: SWFLineStyle = SWFLineStyle()
		if (hasFillFlag) {
			lineStyle.fillType = fillType!!.getMorphedFillStyle(ratio)
		} else {
			lineStyle.color = ColorUtils.interpolate(startColor, endColor, ratio)
			lineStyle.width = (startWidth + (endWidth - startWidth) * ratio).toInt()
		}
		lineStyle.startCapsStyle = startCapsStyle
		lineStyle.endCapsStyle = endCapsStyle
		lineStyle.jointStyle = jointStyle
		lineStyle.hasFillFlag = hasFillFlag
		lineStyle.noHScaleFlag = noHScaleFlag
		lineStyle.noVScaleFlag = noVScaleFlag
		lineStyle.pixelHintingFlag = pixelHintingFlag
		lineStyle.noClose = noClose
		lineStyle.miterLimitFactor = miterLimitFactor
		return lineStyle
	}

	override fun toString(): String {
		return "[SWFMorphLineStyle] StartWidth: $startWidth, EndWidth: $endWidth, StartColor: ${ColorUtils.rgbaToString(startColor)}, EndColor: ${ColorUtils.rgbaToString(endColor)}"
	}
}

class SWFMorphLineStyle2 : SWFMorphLineStyle() {
	override fun parse(data: SWFData, level: Int): Unit {
		startWidth = data.readUI16()
		endWidth = data.readUI16()
		startCapsStyle = LineCapsStyle[data.readUB(2)]
		jointStyle = data.readUB(2)
		hasFillFlag = (data.readUB(1) == 1)
		noHScaleFlag = (data.readUB(1) == 1)
		noVScaleFlag = (data.readUB(1) == 1)
		pixelHintingFlag = (data.readUB(1) == 1)
		var reserved: Int = data.readUB(5)
		noClose = (data.readUB(1) == 1)
		endCapsStyle = LineCapsStyle[data.readUB(2)]
		if (jointStyle == LineJointStyle.MITER) miterLimitFactor = data.readFIXED8()
		if (hasFillFlag) {
			fillType = data.readMORPHFILLSTYLE(level)
		} else {
			startColor = data.readRGBA()
			endColor = data.readRGBA()
		}
	}

	override fun toString(): String {
		var str: String = "[SWFMorphLineStyle2] " +
			"StartWidth: " + startWidth + ", " +
			"EndWidth: " + endWidth + ", " +
			"StartCaps: " + (startCapsStyle) + ", " +
			"EndCaps: " + (endCapsStyle) + ", " +
			"Joint: " + (jointStyle)
		if (hasFillFlag) {
			str += ", Fill: " + fillType.toString()
		} else {
			str += ", StartColor: " + ColorUtils.rgbaToString(startColor)
			str += ", EndColor: " + ColorUtils.rgbaToString(endColor)
		}
		return str
	}
}

class SWFRawTag {
	lateinit var header: com.codeazur.as3swf.data.SWFRecordHeader
	lateinit var bytes: SWFData

	fun parse(data: SWFData): Unit {
		val pos: Int = data.position
		header = data.readTagHeader()
		bytes = SWFData()
		val posContent: Int = data.position
		data.position = pos
		bytes.writeBytes(data.readBytes(header.tagLength))
		data.position = posContent
	}
}

class SWFRecordHeader(
	var type: Int,
	var contentLength: Int,
	var headerLength: Int
) {
	val tagLength: Int get() = headerLength + contentLength

	override fun toString(): String = "[SWFRecordHeader] type: $type, headerLength: $headerLength, contentlength: $contentLength"
}

class SWFRectangle(
	var xmin: Int = 0,
	var xmax: Int = 11000,
	var ymin: Int = 0,
	var ymax: Int = 8000
) {
	protected var _rectangle = Rectangle()

	fun parse(data: SWFData) {
		data.resetBitsPending()
		val bits = data.readUB(5)
		xmin = data.readSB(bits)
		xmax = data.readSB(bits)
		ymin = data.readSB(bits)
		ymax = data.readSB(bits)
	}

	val rect: Rectangle
		get() {
			_rectangle.left = NumberUtils.roundPixels20(xmin.toDouble() / 20)
			_rectangle.right = NumberUtils.roundPixels20(xmax.toDouble() / 20)
			_rectangle.top = NumberUtils.roundPixels20(ymin.toDouble() / 20)
			_rectangle.bottom = NumberUtils.roundPixels20(ymax.toDouble() / 20)
			return _rectangle
		}

	override fun toString(): String = "($xmin,$xmax,$ymin,$ymax)"
	fun toStringSize(): String = "(" + ((xmax - xmin).toDouble() / 20) + "," + ((ymax - ymin).toDouble() / 20) + ")"
}

class SWFRegisterParam {
	var register: Int = 0
	var name: String? = null

	fun parse(data: SWFData): Unit {
		register = data.readUI8()
		name = data.readString()
	}

	override fun toString(): String = "$$register:$name"
}

class SWFScene(var offset: Int, var name: String) {
	override fun toString() = "Frame: $offset, Name: $name"
}

open class SWFShape(var unitDivisor: Double = 20.0) {
	var records = ArrayList<SWFShapeRecord>()

	var fillStyles = ArrayList<SWFFillStyle>()
	var lineStyles = ArrayList<SWFLineStyle>()
	var referencePoint = Point2d(0, 0)

	protected var fillEdgeMaps = ArrayList<HashMap<Int, ArrayList<IEdge>>>()
	protected var lineEdgeMaps = ArrayList<HashMap<Int, ArrayList<IEdge>>>()
	protected var currentFillEdgeMap = hashMapOf<Int, ArrayList<IEdge>>()
	protected var currentLineEdgeMap = hashMapOf<Int, ArrayList<IEdge>>()
	protected var numGroups: Int = 0
	protected var coordMap = hashMapOf<String, ArrayList<IEdge>>()

	protected var edgeMapsCreated: Boolean = false

	open fun parse(data: SWFData, level: Int = 1): Unit {
		data.resetBitsPending()
		val numFillBits: Int = data.readUB(4)
		val numLineBits: Int = data.readUB(4)
		readShapeRecords(data, numFillBits, numLineBits, level)
		determineReferencePoint()
	}

	protected fun readShapeRecords(data: SWFData, _fillBits: Int, _lineBits: Int, level: Int = 1): Unit {
		var fillBits: Int = _fillBits
		var lineBits: Int = _lineBits
		var shapeRecord: SWFShapeRecord? = null
		while (!(shapeRecord is SWFShapeRecordEnd)) {
			// The SWF10 spec says that shape records are byte aligned.
			// In reality they seem not to be?
			// bitsPending = 0;
			val edgeRecord = (data.readUB(1) == 1)
			if (edgeRecord) {
				val straightFlag = (data.readUB(1) == 1)
				val numBits: Int = data.readUB(4) + 2
				if (straightFlag) {
					shapeRecord = data.readSTRAIGHTEDGERECORD(numBits)
				} else {
					shapeRecord = data.readCURVEDEDGERECORD(numBits)
				}
			} else {
				val states: Int = data.readUB(5)
				if (states == 0) {
					shapeRecord = SWFShapeRecordEnd
				} else {
					val styleChangeRecord = data.readSTYLECHANGERECORD(states, fillBits, lineBits, level)
					if (styleChangeRecord.stateNewStyles) {
						fillBits = styleChangeRecord.numFillBits
						lineBits = styleChangeRecord.numLineBits
					}
					shapeRecord = styleChangeRecord
				}
			}
			//println(shapeRecord)
			records.add(shapeRecord)
		}
	}

	protected fun determineReferencePoint(): Unit {
		val styleChangeRecord = records[0] as? SWFShapeRecordStyleChange?
		if (styleChangeRecord != null && styleChangeRecord.stateMoveTo) {
			referencePoint.x = NumberUtils.roundPixels400(styleChangeRecord.moveDeltaX / unitDivisor)
			referencePoint.y = NumberUtils.roundPixels400(styleChangeRecord.moveDeltaY / unitDivisor)
		}
	}

	open fun export(_handler: ShapeExporter): Unit {
		val handler = _handler
		// Reset the flag so that shapes can be exported multiple times
		// TODO: This is a temporary bug fix. edgeMaps shouldn't need to be recreated for subsequent exports
		edgeMapsCreated = false
		// Create edge maps
		createEdgeMaps()
		// If no handler is passed, default to DefaultShapeExporter (does nothing)

		// Let the doc handler know that a shape export starts
		handler.beginShape()
		// Export fills and strokes for each group separately
		for (i in 0 until numGroups) {
			// Export fills first
			exportFillPath(handler, i)
			// Export strokes last
			exportLinePath(handler, i)
		}
		// Let the doc handler know that we're done exporting a shape
		handler.endShape()
	}

	protected fun createEdgeMaps(): Unit {
		if (!edgeMapsCreated) {
			var xPos: Double = 0.0
			var yPos: Double = 0.0
			var from: Point2d
			var to: Point2d
			var control: Point2d
			var fillStyleIdxOffset: Int = 0
			var lineStyleIdxOffset: Int = 0
			var currentFillStyleIdx0: Int = 0
			var currentFillStyleIdx1: Int = 0
			var currentLineStyleIdx: Int = 0
			var subPath: ArrayList<IEdge> = ArrayList<IEdge>()
			numGroups = 0
			fillEdgeMaps = arrayListOf<HashMap<Int, ArrayList<IEdge>>>()
			lineEdgeMaps = arrayListOf<HashMap<Int, ArrayList<IEdge>>>()
			currentFillEdgeMap = HashMap<Int, ArrayList<IEdge>>()
			currentLineEdgeMap = HashMap<Int, ArrayList<IEdge>>()
			for (i in 0 until records.size) {
				val shapeRecord: SWFShapeRecord = records[i]
				when (shapeRecord.type) {
					SWFShapeRecord.TYPE_STYLECHANGE -> {
						val styleChangeRecord: SWFShapeRecordStyleChange = shapeRecord as SWFShapeRecordStyleChange
						if (styleChangeRecord.stateLineStyle || styleChangeRecord.stateFillStyle0 || styleChangeRecord.stateFillStyle1) {
							processSubPath(subPath, currentLineStyleIdx, currentFillStyleIdx0, currentFillStyleIdx1)
							subPath = ArrayList<IEdge>()
						}
						if (styleChangeRecord.stateNewStyles) {
							fillStyleIdxOffset = fillStyles.size
							lineStyleIdxOffset = lineStyles.size
							fillStyles.addAll(styleChangeRecord.fillStyles)
							lineStyles.addAll(styleChangeRecord.lineStyles)
						}
						// Check if all styles are reset to 0.
						// This (probably) means that a group starts with the next record
						if (styleChangeRecord.stateLineStyle && styleChangeRecord.lineStyle == 0 &&
							styleChangeRecord.stateFillStyle0 && styleChangeRecord.fillStyle0 == 0 &&
							styleChangeRecord.stateFillStyle1 && styleChangeRecord.fillStyle1 == 0) {
							cleanEdgeMap(currentFillEdgeMap)
							cleanEdgeMap(currentLineEdgeMap)
							fillEdgeMaps.add(currentFillEdgeMap)
							lineEdgeMaps.add(currentLineEdgeMap)
							currentFillEdgeMap = hashMapOf()
							currentLineEdgeMap = hashMapOf()
							currentLineStyleIdx = 0
							currentFillStyleIdx0 = 0
							currentFillStyleIdx1 = 0
							numGroups++
						} else {
							if (styleChangeRecord.stateLineStyle) {
								currentLineStyleIdx = styleChangeRecord.lineStyle
								if (currentLineStyleIdx > 0) currentLineStyleIdx += lineStyleIdxOffset
							}
							if (styleChangeRecord.stateFillStyle0) {
								currentFillStyleIdx0 = styleChangeRecord.fillStyle0
								if (currentFillStyleIdx0 > 0) currentFillStyleIdx0 += fillStyleIdxOffset
							}
							if (styleChangeRecord.stateFillStyle1) {
								currentFillStyleIdx1 = styleChangeRecord.fillStyle1
								if (currentFillStyleIdx1 > 0) currentFillStyleIdx1 += fillStyleIdxOffset
							}
						}
						if (styleChangeRecord.stateMoveTo) {
							xPos = styleChangeRecord.moveDeltaX / unitDivisor
							yPos = styleChangeRecord.moveDeltaY / unitDivisor
						}
					}
					SWFShapeRecord.TYPE_STRAIGHTEDGE -> {
						val straightEdgeRecord: SWFShapeRecordStraightEdge = shapeRecord as SWFShapeRecordStraightEdge
						from = Point2d(NumberUtils.roundPixels400(xPos), NumberUtils.roundPixels400(yPos))
						if (straightEdgeRecord.generalLineFlag) {
							xPos += straightEdgeRecord.deltaX / unitDivisor
							yPos += straightEdgeRecord.deltaY / unitDivisor
						} else {
							if (straightEdgeRecord.vertLineFlag) {
								yPos += straightEdgeRecord.deltaY / unitDivisor
							} else {
								xPos += straightEdgeRecord.deltaX / unitDivisor
							}
						}
						to = Point2d(NumberUtils.roundPixels400(xPos), NumberUtils.roundPixels400(yPos))
						subPath.add(StraightEdge(from, to, currentLineStyleIdx, currentFillStyleIdx1))
					}
					SWFShapeRecord.TYPE_CURVEDEDGE -> {
						val curvedEdgeRecord: SWFShapeRecordCurvedEdge = shapeRecord as SWFShapeRecordCurvedEdge
						from = Point2d(NumberUtils.roundPixels400(xPos), NumberUtils.roundPixels400(yPos))
						val xPosControl: Double = xPos + curvedEdgeRecord.controlDeltaX / unitDivisor
						val yPosControl: Double = yPos + curvedEdgeRecord.controlDeltaY / unitDivisor
						xPos = xPosControl + curvedEdgeRecord.anchorDeltaX / unitDivisor
						yPos = yPosControl + curvedEdgeRecord.anchorDeltaY / unitDivisor
						control = Point2d(xPosControl, yPosControl)
						to = Point2d(NumberUtils.roundPixels400(xPos), NumberUtils.roundPixels400(yPos))
						subPath.add(CurvedEdge(from, control, to, currentLineStyleIdx, currentFillStyleIdx1))
					}
					SWFShapeRecord.TYPE_END -> {
						// We're done. Process the last subpath, if any
						processSubPath(subPath, currentLineStyleIdx, currentFillStyleIdx0, currentFillStyleIdx1)
						cleanEdgeMap(currentFillEdgeMap)
						cleanEdgeMap(currentLineEdgeMap)
						fillEdgeMaps.add(currentFillEdgeMap)
						lineEdgeMaps.add(currentLineEdgeMap)
						numGroups++
					}
				}
			}
			edgeMapsCreated = true
		}
	}

	protected fun processSubPath(subPath: ArrayList<IEdge>, lineStyleIdx: Int, fillStyleIdx0: Int, fillStyleIdx1: Int): Unit {
		if (fillStyleIdx0 != 0) {
			var path = currentFillEdgeMap[fillStyleIdx0]
			if (path == null) {
				path = ArrayList<IEdge>()
				currentFillEdgeMap[fillStyleIdx0] = path
			}
			for (_j in 0 until subPath.size) {
				val j = subPath.size - 1 - _j
				path.add(subPath[j].reverseWithNewFillStyle(fillStyleIdx0))
			}
		}
		if (fillStyleIdx1 != 0) {
			var path = currentFillEdgeMap[fillStyleIdx1]
			if (path == null) {
				path = ArrayList<IEdge>()
				currentFillEdgeMap[fillStyleIdx1] = path
			}
			path.addAll(subPath)
		}
		if (lineStyleIdx != 0) {
			var path = currentLineEdgeMap[lineStyleIdx]
			if (path == null) {
				path = ArrayList<IEdge>()
				currentLineEdgeMap[lineStyleIdx] = path
			}
			path.addAll(subPath)
		}
	}

	protected fun exportFillPath(handler: ShapeExporter, groupIndex: Int): Unit {
		val path: ArrayList<IEdge> = createPathFromEdgeMap(fillEdgeMaps[groupIndex])
		var pos: Point2d = Point2d(Int.MAX_VALUE, Int.MAX_VALUE)
		var fillStyleIdx: Int = Int.MAX_VALUE
		if (path.size > 0) {
			handler.beginFills()
			for (i in 0 until path.size) {
				val e = path[i]
				if (fillStyleIdx != e.fillStyleIdx) {
					if (fillStyleIdx != Int.MAX_VALUE) handler.endFill()
					fillStyleIdx = e.fillStyleIdx
					pos = Point2d(Int.MAX_VALUE, Int.MAX_VALUE)
					try {
						var matrix: Matrix2d
						val fillStyle: SWFFillStyle = fillStyles[fillStyleIdx - 1]
						when (fillStyle.type) {
							0x00 -> {
								// Solid fill
								handler.beginFill(ColorUtils.rgb(fillStyle.rgb), ColorUtils.alpha(fillStyle.rgb))
							}
							0x10,
							0x12,
							0x13 -> {
								// Gradient fill
								val colors = arrayListOf<Int>()
								val alphas = arrayListOf<Double>()
								val ratios = arrayListOf<Int>()
								matrix = fillStyle.gradientMatrix!!.matrix.clone()
								for (gri in 0 until fillStyle.gradient!!.records.size) {
									val gradientRecord = fillStyle.gradient!!.records[gri]
									colors.add(ColorUtils.rgb(gradientRecord.color))
									alphas.add(ColorUtils.alpha(gradientRecord.color))
									ratios.add(gradientRecord.ratio)
								}
								handler.beginGradientFill(
									if (fillStyle.type == 0x10) GradientType.LINEAR else GradientType.RADIAL,
									colors, alphas, ratios, matrix,
									fillStyle.gradient!!.spreadMode,
									fillStyle.gradient!!.interpolationMode,
									fillStyle.gradient!!.focalPoint
								)
							}
							0x40,
							0x41,
							0x42,
							0x43 -> {
								// Bitmap fill
								val m = fillStyle.bitmapMatrix!!
								matrix = Matrix2d()
								matrix.createBox(m.xscale / 20, m.yscale / 20, m.rotation, m.translateX / 20.0, m.translateY / 20.0)
								handler.beginBitmapFill(
									fillStyle.bitmapId,
									matrix,
									(fillStyle.type == 0x40 || fillStyle.type == 0x42),
									(fillStyle.type == 0x40 || fillStyle.type == 0x41)
								)
							}
						}
					} catch (e: Error) {
						// Font shapes define no fillstyles per se, but do reference fillstyle index 1,
						// which represents the font color. We just report solid black in this case.
						handler.beginFill(0)
					}
				}
				if (pos != e.from) handler.moveTo(e.from.x, e.from.y)
				if (e is CurvedEdge) {
					val c = e
					handler.curveTo(c.control.x, c.control.y, c.to.x, c.to.y)
				} else {
					handler.lineTo(e.to.x, e.to.y)
				}
				pos = e.to
			}
			if (fillStyleIdx != Int.MAX_VALUE) {
				handler.endFill()
			}
			handler.endFills()
		}
	}

	protected fun exportLinePath(handler: ShapeExporter, groupIndex: Int): Unit {
		val path = createPathFromEdgeMap(lineEdgeMaps[groupIndex])
		var pos = Point2d(Int.MAX_VALUE, Int.MAX_VALUE)
		var lineStyleIdx = Int.MAX_VALUE
		if (path.size > 0) {
			handler.beginLines()
			var basePoint: Point2d? = null
			for (i in 0 until path.size) {
				val e: IEdge = path[i]
				if (lineStyleIdx != e.lineStyleIdx) {
					lineStyleIdx = e.lineStyleIdx
					pos = Point2d(Int.MAX_VALUE, Int.MAX_VALUE)
					val lineStyle = try {
						lineStyles[lineStyleIdx - 1]
					} catch (e: Error) {
						null
					}
					if (lineStyle != null) {
						val scaleMode = when {
							(lineStyle.noHScaleFlag && lineStyle.noVScaleFlag) -> Context2d.ScaleMode.NONE
							lineStyle.noHScaleFlag -> Context2d.ScaleMode.HORIZONTAL
							lineStyle.noVScaleFlag -> Context2d.ScaleMode.VERTICAL
							else -> Context2d.ScaleMode.NORMAL
						}

						handler.lineStyle(
							lineStyle.width.toDouble() / 20,
							ColorUtils.rgb(lineStyle.color),
							ColorUtils.alpha(lineStyle.color),
							lineStyle.pixelHintingFlag,
							scaleMode,
							(lineStyle.startCapsStyle),
							(lineStyle.endCapsStyle),
							LineJointStyle.toString(lineStyle.jointStyle),
							lineStyle.miterLimitFactor)

						if (lineStyle.hasFillFlag) {
							val fillStyle: SWFFillStyle = lineStyle.fillType!!
							when (fillStyle.type) {
								0x10, 0x12, 0x13 -> {
									// Gradient fill
									val colors = arrayListOf<Int>()
									val alphas = arrayListOf<Double>()
									val ratios = arrayListOf<Int>()
									var gradientRecord: com.codeazur.as3swf.data.SWFGradientRecord
									val matrix = fillStyle.gradientMatrix!!.matrix.clone()
									for (gri in 0 until fillStyle.gradient!!.records.size) {
										gradientRecord = fillStyle.gradient!!.records[gri]
										colors.add(ColorUtils.rgb(gradientRecord.color))
										alphas.add(ColorUtils.alpha(gradientRecord.color))
										ratios.add(gradientRecord.ratio)
									}
									handler.lineGradientStyle(
										if (fillStyle.type == 0x10) GradientType.LINEAR else GradientType.RADIAL,
										colors, alphas, ratios, matrix,
										fillStyle.gradient!!.spreadMode,
										fillStyle.gradient!!.interpolationMode,
										fillStyle.gradient!!.focalPoint
									)
								}
							}
						}
					} else {
						// We should never get here
						handler.lineStyle(0.0)
					}
				}
				if (e.from != pos) {
					basePoint = e.from
					handler.moveTo(e.from.x, e.from.y)
				}
				if (e is CurvedEdge) {
					handler.curveTo(e.control.x, e.control.y, e.to.x, e.to.y)
				} else {
					handler.lineTo(e.to.x, e.to.y)
				}

				if (e.to == basePoint) {
					handler.closePath()
				}
				pos = e.to
			}
			handler.endLines()
		}
	}

	protected fun createPathFromEdgeMap(edgeMap: HashMap<Int, ArrayList<IEdge>>): ArrayList<IEdge> {
		val newPath: ArrayList<IEdge> = ArrayList<IEdge>()
		val styleIdxArray = arrayListOf<Int>()
		for (styleIdx in edgeMap.keys) {
			styleIdxArray.add(styleIdx)
		}
		styleIdxArray.sort()
		for (i in 0 until styleIdxArray.size) {
			newPath.addAll(edgeMap[styleIdxArray[i]] as ArrayList<IEdge>)
		}
		return newPath
	}

	protected fun cleanEdgeMap(edgeMap: HashMap<Int, ArrayList<IEdge>>): Unit {
		for (styleIdx in edgeMap.keys) {
			val subPath = edgeMap[styleIdx]
			if (subPath != null && subPath.size > 0) {
				var prevEdge: IEdge? = null
				val tmpPath = ArrayList<IEdge>()
				createCoordMap(subPath)
				while (subPath.size > 0) {
					var idx = 0
					while (idx < subPath.size) {
						if (prevEdge == null || prevEdge.to == subPath[idx].from) {
							val edge = subPath.removeAt(idx)
							tmpPath.add(edge)
							removeEdgeFromCoordMap(edge)
							prevEdge = edge
						} else {
							val edge = findNextEdgeInCoordMap(prevEdge)
							if (edge != null) {
								idx = subPath.indexOf(edge)
							} else {
								idx = 0
								prevEdge = null
							}
						}
					}
				}
				edgeMap[styleIdx] = tmpPath
			}
		}
	}

	protected fun createCoordMap(path: ArrayList<IEdge>): Unit {
		coordMap = hashMapOf()
		for (i in 0 until path.size) {
			val from: Point2d = path[i].from
			val key: String = "${from.x}_${from.y}"
			val coordMapArray = coordMap[key]
			if (coordMapArray == null) {
				coordMap[key] = arrayListOf(path[i])
			} else {
				coordMapArray.add(path[i])
			}
		}
	}

	protected fun removeEdgeFromCoordMap(edge: IEdge): Unit {
		val key: String = "" + edge.from.x + "_" + edge.from.y
		val coordMapArray = coordMap[key]
		if (coordMapArray != null) {
			if (coordMapArray.size == 1) {
				coordMap.remove(key)
			} else {
				val i = coordMapArray.indexOf(edge)
				if (i > -1) coordMapArray.removeAt(i)
			}
		}
	}

	protected fun findNextEdgeInCoordMap(edge: IEdge): IEdge? {
		val coordMapArray = coordMap["${edge.to.x}_${edge.to.y}"]
		return if (coordMapArray != null && coordMapArray.isNotEmpty()) coordMapArray[0] else null
	}

	open fun toString(indent: Int = 0): String {
		var str: String = "\n" + " ".repeat(indent) + "ShapeRecords:"
		for (i in 0 until records.size) {
			str += "\n" + " ".repeat(indent + 2) + "[" + i + "] " + records[i].toString()
		}
		return str
	}
}

open class SWFShapeRecord {
	companion object {
		const val TYPE_UNKNOWN = 0
		const val TYPE_END = 1
		const val TYPE_STYLECHANGE = 2
		const val TYPE_STRAIGHTEDGE = 3
		const val TYPE_CURVEDEDGE = 4
	}

	open val type = SWFShapeRecord.TYPE_UNKNOWN
	val isEdgeRecord: Boolean get() = (type == SWFShapeRecord.TYPE_STRAIGHTEDGE || type == SWFShapeRecord.TYPE_CURVEDEDGE)
	open fun parse(data: SWFData, level: Int = 1): Unit = Unit
	override fun toString(): String = "[SWFShapeRecord]"

	open fun clone(): SWFShapeRecord = SWFShapeRecord()
}

data class SWFShapeRecordCurvedEdge(
	var numBits: Int = 0,
	var controlDeltaX: Int = 0,
	var controlDeltaY: Int = 0,
	var anchorDeltaX: Int = 0,
	var anchorDeltaY: Int = 0
) : SWFShapeRecord() {
	override fun parse(data: SWFData, level: Int): Unit {
		controlDeltaX = data.readSB(numBits)
		controlDeltaY = data.readSB(numBits)
		anchorDeltaX = data.readSB(numBits)
		anchorDeltaY = data.readSB(numBits)
	}

	override val type = SWFShapeRecord.TYPE_CURVEDEDGE
	override fun toString(): String = "[SWFShapeRecordCurvedEdge] ControlDelta: $controlDeltaX,$controlDeltaY, AnchorDelta: $anchorDeltaX,$anchorDeltaY"

	override fun clone(): SWFShapeRecord = this.copy()

}

object SWFShapeRecordEnd : SWFShapeRecord() {
	override val type = SWFShapeRecord.TYPE_END
	override fun toString() = "[SWFShapeRecordEnd]"

	override fun clone(): SWFShapeRecord = this
}

data class SWFShapeRecordStraightEdge(
	var numBits: Int = 0,
	var generalLineFlag: Boolean = false,
	var vertLineFlag: Boolean = false,
	var deltaY: Int = 0,
	var deltaX: Int = 0
) : SWFShapeRecord() {

	override fun parse(data: SWFData, level: Int): Unit {
		generalLineFlag = (data.readUB(1) == 1)
		vertLineFlag = if (!generalLineFlag) (data.readUB(1) == 1) else false
		deltaX = if (generalLineFlag || !vertLineFlag) data.readSB(numBits) else 0
		deltaY = if (generalLineFlag || vertLineFlag) data.readSB(numBits) else 0
	}

	override val type = SWFShapeRecord.TYPE_STRAIGHTEDGE

	override fun toString(): String {
		var str: String = "[SWFShapeRecordStraightEdge] "
		if (generalLineFlag) {
			str += "General: $deltaX,$deltaY"
		} else {
			if (vertLineFlag) {
				str += "Vertical: " + deltaY
			} else {
				str += "Horizontal: " + deltaX
			}
		}
		return str
	}

	override fun clone(): SWFShapeRecord = this.copy()
}

data class SWFShapeRecordStyleChange(
	val states: Int = 0,
	var numFillBits: Int = 0,
	var numLineBits: Int = 0,
	var moveDeltaX: Int = 0,
	var moveDeltaY: Int = 0,
	var fillStyle0: Int = 0,
	var fillStyle1: Int = 0,
	var lineStyle: Int = 0,
	var fillStyles: ArrayList<SWFFillStyle> = ArrayList<SWFFillStyle>(),
	var lineStyles: ArrayList<SWFLineStyle> = ArrayList<SWFLineStyle>()
) : SWFShapeRecord() {
	val stateNewStyles: Boolean get() = ((states and 0x10) != 0)
	val stateLineStyle: Boolean get() = ((states and 0x08) != 0)
	val stateFillStyle1: Boolean get() = ((states and 0x04) != 0)
	val stateFillStyle0: Boolean get() = ((states and 0x02) != 0)
	val stateMoveTo: Boolean get() = ((states and 0x01) != 0)

	override val type = SWFShapeRecord.TYPE_STYLECHANGE

	override fun parse(data: SWFData, level: Int): Unit {
		if (stateMoveTo) {
			val moveBits = data.readUB(5)
			moveDeltaX = data.readSB(moveBits)
			moveDeltaY = data.readSB(moveBits)
		}
		fillStyle0 = if (stateFillStyle0) data.readUB(numFillBits) else 0
		fillStyle1 = if (stateFillStyle1) data.readUB(numFillBits) else 0
		lineStyle = if (stateLineStyle) data.readUB(numLineBits) else 0
		if (stateNewStyles) {
			data.resetBitsPending()
			val fillStylesLen: Int = readStyleArrayLength(data, level)
			for (i in 0 until fillStylesLen) {
				fillStyles.add(data.readFILLSTYLE(level))
			}
			val lineStylesLen: Int = readStyleArrayLength(data, level)
			for (i in 0 until lineStylesLen) {
				lineStyles.add(if (level <= 3) data.readLINESTYLE(level) else data.readLINESTYLE2(level))
			}
			data.resetBitsPending()
			numFillBits = data.readUB(4)
			numLineBits = data.readUB(4)
		}
	}

	protected fun readStyleArrayLength(data: SWFData, level: Int = 1): Int {
		var len: Int = data.readUI8()
		if (level >= 2 && len == 0xff) {
			len = data.readUI16()
		}
		return len
	}

	override fun toString(): String {
		val indent = 0
		var str: String = "[SWFShapeRecordStyleChange] "
		val cmds = arrayListOf<String>()
		if (stateMoveTo) cmds.add("MoveTo: $moveDeltaX,$moveDeltaY")
		if (stateFillStyle0) cmds.add("FillStyle0: $fillStyle0")
		if (stateFillStyle1) cmds.add("FillStyle1: $fillStyle1")
		if (stateLineStyle) cmds.add("LineStyle: $lineStyle")
		if (cmds.size > 0) str += cmds.joinToString(", ")
		if (stateNewStyles) {
			if (fillStyles.size > 0) {
				str += "\n" + " ".repeat(indent + 2) + "New FillStyles:"
				for (i in 0 until fillStyles.size) {
					str += "\n" + " ".repeat(indent + 4) + "[" + (i + 1) + "] " + fillStyles[i].toString()
				}
			}
			if (lineStyles.size > 0) {
				str += "\n" + " ".repeat(indent + 2) + "New LineStyles:"
				for (i in 0 until lineStyles.size) {
					str += "\n" + " ".repeat(indent + 4) + "[" + (i + 1) + "] " + lineStyles[i].toString()
				}
			}
		}
		return str
	}

	override fun clone(): SWFShapeRecord = this.copy()
}

class SWFShapeWithStyle(unitDivisor: Double = 20.0) : SWFShape(unitDivisor) {
	var initialFillStyles = ArrayList<SWFFillStyle>()
	var initialLineStyles = ArrayList<SWFLineStyle>()

	override fun parse(data: SWFData, level: Int): Unit {
		data.resetBitsPending()
		for (i in 0 until readStyleArrayLength(data, level)) initialFillStyles.add(data.readFILLSTYLE(level))
		for (i in 0 until readStyleArrayLength(data, level)) initialLineStyles.add(if (level <= 3) data.readLINESTYLE(level) else data.readLINESTYLE2(level))
		data.resetBitsPending()
		val numFillBits: Int = data.readUB(4)
		val numLineBits: Int = data.readUB(4)
		readShapeRecords(data, numFillBits, numLineBits, level)
	}

	override fun export(_handler: ShapeExporter): Unit {
		fillStyles = ArrayList(initialFillStyles)
		lineStyles = ArrayList(initialLineStyles)
		super.export(_handler)
	}

	override fun toString(indent: Int): String {
		var str: String = ""
		if (initialFillStyles.size > 0) {
			str += "\n" + " ".repeat(indent) + "FillStyles:"
			for (i in 0 until initialFillStyles.size) {
				str += "\n" + " ".repeat(indent + 2) + "[" + (i + 1) + "] " + initialFillStyles[i].toString()
			}
		}
		if (initialLineStyles.size > 0) {
			str += "\n" + " ".repeat(indent) + "LineStyles:"
			for (i in 0 until initialLineStyles.size) {
				str += "\n" + " ".repeat(indent + 2) + "[" + (i + 1) + "] " + initialLineStyles[i].toString()
			}
		}
		return str + super.toString(indent)
	}

	protected fun readStyleArrayLength(data: SWFData, level: Int = 1): Int {
		var len: Int = data.readUI8()
		if (level >= 2 && len == 0xff) len = data.readUI16()
		return len
	}
}

data class SWFSoundEnvelope(
	var pos44: Int = 0,
	var leftLevel: Int = 0,
	var rightLevel: Int = 0
) {
	constructor(data: SWFData) : this(data.readUI32(), data.readUI16(), data.readUI16())
}

class SWFSoundInfo {
	var syncStop: Boolean = false
	var syncNoMultiple: Boolean = false
	var hasEnvelope: Boolean = false
	var hasLoops: Boolean = false
	var hasOutPoint: Boolean = false
	var hasInPoint: Boolean = false

	var outPoint: Int = 0
	var inPoint: Int = 0
	var loopCount: Int = 0

	var envelopeRecords = ArrayList<com.codeazur.as3swf.data.SWFSoundEnvelope>()

	fun parse(data: SWFData): Unit {
		val flags = data.readUI8()
		syncStop = ((flags and 0x20) != 0)
		syncNoMultiple = ((flags and 0x10) != 0)
		hasEnvelope = ((flags and 0x08) != 0)
		hasLoops = ((flags and 0x04) != 0)
		hasOutPoint = ((flags and 0x02) != 0)
		hasInPoint = ((flags and 0x01) != 0)
		if (hasInPoint) inPoint = data.readUI32()
		if (hasOutPoint) outPoint = data.readUI32()
		if (hasLoops) loopCount = data.readUI16()
		if (hasEnvelope) {
			val envPoints: Int = data.readUI8()
			for (i in 0 until envPoints) envelopeRecords.add(data.readSOUNDENVELOPE())
		}
	}

	override fun toString(): String = "[SWFSoundInfo]"
}

data class SWFSymbol(var tagId: Int = 0, var name: String? = null) {
	constructor(data: SWFData) : this(data.readUI16(), data.readString())
}

class SWFTextRecord {
	var type = 0
	var hasFont = false
	var hasColor = false
	var hasXOffset = false
	var hasYOffset = false

	var fontId = 0
	var textColor = 0
	var textHeight = 0
	var xOffset = 0
	var yOffset = 0

	var glyphEntries = ArrayList<com.codeazur.as3swf.data.SWFGlyphEntry>()

	protected var _level = 0

	fun parse(data: SWFData, glyphBits: Int, advanceBits: Int, previousRecord: SWFTextRecord? = null, level: Int = 1): Unit {
		_level = level
		val styles: Int = data.readUI8()
		type = styles ushr 7
		hasFont = ((styles and 0x08) != 0)
		hasColor = ((styles and 0x04) != 0)
		hasYOffset = ((styles and 0x02) != 0)
		hasXOffset = ((styles and 0x01) != 0)
		fontId = if (hasFont) data.readUI16() else previousRecord?.fontId ?: fontId
		textColor = if (hasColor) (if (level < 2) data.readRGB() else data.readRGBA()) else previousRecord?.textColor ?: textColor
		xOffset = if (hasXOffset) data.readSI16() else previousRecord?.xOffset ?: xOffset
		yOffset = if (hasYOffset) data.readSI16() else previousRecord?.yOffset ?: yOffset
		textHeight = if (hasFont) data.readUI16() else previousRecord?.textHeight ?: textHeight
		for (i in 0 until data.readUI8()) glyphEntries.add(data.readGLYPHENTRY(glyphBits, advanceBits))
	}

	fun toString(indent: Int = 0): String {
		val params = arrayListOf("Glyphs: " + glyphEntries.size.toString())
		if (hasFont) params.add("FontID: " + fontId); params.add("Height: " + textHeight)
		if (hasColor) params.add("Color: " + (if (_level <= 2) ColorUtils.rgbToString(textColor) else ColorUtils.rgbaToString(textColor)))
		if (hasXOffset) params.add("XOffset: " + xOffset)
		if (hasYOffset) params.add("YOffset: " + yOffset)
		var str: String = params.joinToString(", ")
		for (i in 0 until glyphEntries.size) str += "\n" + " ".repeat(indent + 2) + "[" + i + "] " + glyphEntries[i].toString()
		return str
	}
}

data class SWFZoneData(var alignmentCoordinate: Double, var range: Double) {
	constructor(data: SWFData) : this(data.readFLOAT16(), data.readFLOAT16())
}

class SWFZoneRecord(var zoneData: List<SWFZoneData>, var mask: Int) {
	val maskX: Boolean get() = ((mask and 0x01) != 0)
	val maskY: Boolean get() = ((mask and 0x02) != 0)

	constructor(data: SWFData) : this(
		(0 until data.readUI8()).map { data.readZONEDATA() },
		data.readUI8()
	)
}

enum class GradientType { LINEAR, RADIAL }
