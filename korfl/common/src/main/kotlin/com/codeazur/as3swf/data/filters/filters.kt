package com.codeazur.as3swf.data.filters

import com.codeazur.as3swf.SWFData
import com.codeazur.as3swf.utils.ColorUtils
import kotlin.math.PI

interface IFilter {
	val id: Int

	fun parse(data: SWFData): Unit
	fun toString(indent: Int = 0): String
}

open class Filter(override val id: Int) : IFilter {
	override fun parse(data: SWFData): Unit = throw Error("Implement in subclasses!")
	override fun toString(indent: Int): String = "[Filter]"
	override fun toString(): String = toString(0)
}

enum class FilterType { FULL, INNER, OUTER }

class FilterBevel(id: Int) : Filter(id) {
	var shadowColor: Int = 0
	var highlightColor: Int = 0
	var blurX: Double = 0.0
	var blurY: Double = 0.0
	var angle: Double = 0.0
	var distance: Double = 0.0
	var strength: Double = 0.0
	var innerShadow: Boolean = false
	var knockout: Boolean = false
	var compositeSource: Boolean = false
	var onTop: Boolean = false
	var passes: Int = 0
	val filterType get() = if (onTop) FilterType.FULL else if (innerShadow) FilterType.INNER else FilterType.OUTER
	val angleDegrees get() = angle * 180 / PI


	//override val filter: BitmapFilter get() {
	//	val filterType: BitmapFilterType
	//	if (onTop) {
	//		filterType = BitmapFilterType.FULL
	//	} else {
	//		filterType = if (innerShadow) BitmapFilterType.INNER else BitmapFilterType.OUTER
	//	}
	//	return BevelFilter(
	//		distance,
	//		angle * 180 / PI,
	//		ColorUtils.rgb(highlightColor),
	//		ColorUtils.alpha(highlightColor),
	//		ColorUtils.rgb(shadowColor),
	//		ColorUtils.alpha(shadowColor),
	//		blurX,
	//		blurY,
	//		strength,
	//		passes,
	//		filterType,
	//		knockout
	//	)
	//}

	override fun parse(data: SWFData) {
		shadowColor = data.readRGBA()
		highlightColor = data.readRGBA()
		blurX = data.readFIXED()
		blurY = data.readFIXED()
		angle = data.readFIXED()
		distance = data.readFIXED()
		strength = data.readFIXED8()
		val flags = data.readUI8()
		innerShadow = ((flags and 0x80) != 0)
		knockout = ((flags and 0x40) != 0)
		compositeSource = ((flags and 0x20) != 0)
		onTop = ((flags and 0x10) != 0)
		passes = flags and 0x0f
	}

	override fun toString(indent: Int): String {
		var str: String = "[BevelFilter] " +
			"ShadowColor: " + ColorUtils.rgbToString(shadowColor) + ", " +
			"HighlightColor: " + ColorUtils.rgbToString(highlightColor) + ", " +
			"BlurX: " + blurX + ", " +
			"BlurY: " + blurY + ", " +
			"Angle: " + angle + ", " +
			"Distance: " + distance + ", " +
			"Strength: " + strength + ", " +
			"Passes: " + passes
		val flags = arrayListOf<String>()
		if (innerShadow) flags.add("InnerShadow")
		if (knockout) flags.add("Knockout")
		if (compositeSource) flags.add("CompositeSource")
		if (onTop) flags.add("OnTop")
		if (flags.size > 0) str += ", Flags: " + flags.joinToString(", ")
		return str
	}
}

class FilterBlur(id: Int) : Filter(id) {
	var blurX: Double = 0.0
	var blurY: Double = 0.0
	var passes: Int = 0


	//override val filter: BitmapFilter get() {
	//	return BlurFilter(
	//		blurX,
	//		blurY,
	//		passes
	//	)
	//}

	override fun parse(data: SWFData) {
		blurX = data.readFIXED()
		blurY = data.readFIXED()
		passes = data.readUI8() ushr 3
	}

	override fun toString(indent: Int): String = "[BlurFilter] BlurX: $blurX, BlurY: $blurY, Passes: $passes"
}

class FilterColorMatrix(id: Int) : Filter(id) {
	val colorMatrix = arrayListOf<Double>()

	//override val filter: BitmapFilter get() {
	//	return ColorMatrixFilter(listOf(
	//		colorMatrix[0], colorMatrix[1], colorMatrix[2], colorMatrix[3], colorMatrix[4],
	//		colorMatrix[5], colorMatrix[6], colorMatrix[7], colorMatrix[8], colorMatrix[9],
	//		colorMatrix[10], colorMatrix[11], colorMatrix[12], colorMatrix[13], colorMatrix[14],
	//		colorMatrix[15], colorMatrix[16], colorMatrix[17], colorMatrix[18], colorMatrix[19]
	//	))
	//}

	override fun parse(data: SWFData): Unit {
		for (i in 0 until 20) {
			colorMatrix.add(data.readFLOAT())
		}
	}

	override fun toString(indent: Int): String {
		val si = " ".repeat(indent + 2)
		return "[ColorMatrixFilter]" +
			"\n" + si + "[R] " + colorMatrix[0] + ", " + colorMatrix[1] + ", " + colorMatrix[2] + ", " + colorMatrix[3] + ", " + colorMatrix[4] +
			"\n" + si + "[G] " + colorMatrix[5] + ", " + colorMatrix[6] + ", " + colorMatrix[7] + ", " + colorMatrix[8] + ", " + colorMatrix[9] +
			"\n" + si + "[B] " + colorMatrix[10] + ", " + colorMatrix[11] + ", " + colorMatrix[12] + ", " + colorMatrix[13] + ", " + colorMatrix[14] +
			"\n" + si + "[A] " + colorMatrix[15] + ", " + colorMatrix[16] + ", " + colorMatrix[17] + ", " + colorMatrix[18] + ", " + colorMatrix[19]
	}
}

class FilterConvolution(id: Int) : Filter(id), IFilter {
	var matrixX: Int = 0
	var matrixY = 0
	var divisor = 0.0
	var bias = 0.0
	var defaultColor = 0
	var clamp = false
	var preserveAlpha = false

	var matrix = ArrayList<Double>()

	//override val filter: BitmapFilter get() {
	//	val convolutionMatrix = arrayListOf<Double>()
	//	for (i in 0 until matrix.size) {
	//		convolutionMatrix.add(matrix[i])
	//	}
	//	return ConvolutionFilter(
	//		matrixX,
	//		matrixY,
	//		convolutionMatrix,
	//		divisor,
	//		bias,
	//		preserveAlpha,
	//		clamp,
	//		ColorUtils.rgb(defaultColor),
	//		ColorUtils.alpha(defaultColor)
	//	)
	//}

	override fun parse(data: SWFData): Unit {
		matrixX = data.readUI8()
		matrixY = data.readUI8()
		divisor = data.readFLOAT()
		bias = data.readFLOAT()
		val len = matrixX * matrixY
		for (i in 0 until len) {
			matrix.add(data.readFLOAT())
		}
		defaultColor = data.readRGBA()
		val flags = data.readUI8()
		clamp = ((flags and 0x02) != 0)
		preserveAlpha = ((flags and 0x01) != 0)
	}

	override fun toString(indent: Int): String {
		var str: String = "[ConvolutionFilter] " +
			"DefaultColor: " + ColorUtils.rgbToString(defaultColor) + ", " +
			"Divisor: " + divisor + ", " +
			"Bias: " + bias
		val flags = arrayListOf<String>()
		if (clamp) flags.add("Clamp")
		if (preserveAlpha) flags.add("PreserveAlpha")
		if (flags.size > 0) str += ", Flags: " + flags.joinToString(", ")
		if (matrix.size > 0) {
			str += "\n" + " ".repeat(indent + 2) + "Matrix:"
			for (y in 0 until matrixY) {
				str += "\n" + " ".repeat(indent + 4) + "[" + y + "]"
				for (x in 0 until matrixX) {
					str += (if (x > 0) ", " else " ") + matrix[matrixX * y + x]
				}
			}
		}
		return str
	}
}

class FilterDropShadow(id: Int) : Filter(id), IFilter {
	var dropShadowColor = 0
	var blurX = 0.0
	var blurY = 0.0
	var angle = 0.0
	var distance = 0.0
	var strength = 0.0
	var innerShadow = false
	var knockout = false
	var compositeSource = false
	var passes = 0

	//override val filter: BitmapFilter get() = DropShadowFilter(
	//	distance,
	//	angle * 180 / PI,
	//	ColorUtils.rgb(dropShadowColor),
	//	ColorUtils.alpha(dropShadowColor),
	//	blurX,
	//	blurY,
	//	strength,
	//	passes,
	//	innerShadow,
	//	knockout
	//)

	override fun parse(data: SWFData): Unit {
		dropShadowColor = data.readRGBA()
		blurX = data.readFIXED()
		blurY = data.readFIXED()
		angle = data.readFIXED()
		distance = data.readFIXED()
		strength = data.readFIXED8()
		val flags = data.readUI8()
		innerShadow = ((flags and 0x80) != 0)
		knockout = ((flags and 0x40) != 0)
		compositeSource = ((flags and 0x20) != 0)
		passes = flags and 0x1f
	}

	override fun toString(indent: Int): String {
		var str: String = "[DropShadowFilter] " +
			"DropShadowColor: " + ColorUtils.rgbToString(dropShadowColor) + ", " +
			"BlurX: " + blurX + ", " +
			"BlurY: " + blurY + ", " +
			"Angle: " + angle + ", " +
			"Distance: " + distance + ", " +
			"Strength: " + strength + ", " +
			"Passes: " + passes
		val flags = arrayListOf<String>()
		if (innerShadow) flags.add("InnerShadow")
		if (knockout) flags.add("Knockout")
		if (compositeSource) flags.add("CompositeSource")
		if (flags.size > 0) str += ", Flags: " + flags.joinToString(", ")
		return str
	}
}

class FilterGlow(id: Int) : Filter(id), IFilter {
	var glowColor = 0
	var blurX = 0.0
	var blurY = 0.0
	var strength = 0.0
	var innerGlow = false
	var knockout = false
	var compositeSource = false
	var passes = 0

	//override val filter: BitmapFilter get() {
	//	return GlowFilter(
	//		ColorUtils.rgb(glowColor),
	//		ColorUtils.alpha(glowColor),
	//		blurX, blurY,
	//		strength, passes, innerGlow, knockout
	//	)
	//}

	override fun parse(data: SWFData): Unit {
		glowColor = data.readRGBA()
		blurX = data.readFIXED()
		blurY = data.readFIXED()
		strength = data.readFIXED8()
		val flags = data.readUI8()
		innerGlow = ((flags and 0x80) != 0)
		knockout = ((flags and 0x40) != 0)
		compositeSource = ((flags and 0x20) != 0)
		passes = flags and 0x1f
	}

	override fun toString(indent: Int): String {
		var str: String = "[GlowFilter] " +
			"GlowColor: " + ColorUtils.rgbToString(glowColor) + ", " +
			"BlurX: " + blurX + ", " +
			"BlurY: " + blurY + ", " +
			"Strength: " + strength + ", " +
			"Passes: " + passes
		val flags = arrayListOf<String>()
		if (innerGlow) flags.add("InnerGlow")
		if (knockout) flags.add("Knockout")
		if (compositeSource) flags.add("CompositeSource")
		if (flags.size > 0) str += ", Flags: " + flags.joinToString(", ")
		return str
	}
}

class FilterGradientBevel(id: Int) : FilterGradientGlow(id), IFilter {
	//override val filter: BitmapFilter get() {
	//	val gradientGlowColors = arrayListOf<Int>()
	//	val gradientGlowAlphas = arrayListOf<Double>()
	//	val gradientGlowRatios = arrayListOf<Int>()
	//	for (i in 0 until numColors) {
	//		gradientGlowColors.add(ColorUtils.rgb(gradientColors[i]))
	//		gradientGlowAlphas.add(ColorUtils.alpha(gradientColors[i]))
	//		gradientGlowRatios.add(gradientRatios[i])
	//	}
	//	val filterType: BitmapFilterType
	//	if (onTop) {
	//		filterType = BitmapFilterType.FULL
	//	} else {
	//		filterType = if (innerShadow) BitmapFilterType.INNER else BitmapFilterType.OUTER
	//	}
	//	return GradientBevelFilter(
	//		distance,
	//		angle,
	//		gradientGlowColors,
	//		gradientGlowAlphas,
	//		gradientGlowRatios,
	//		blurX,
	//		blurY,
	//		strength,
	//		passes,
	//		filterType,
	//		knockout
	//	)
	//}

	override val filterName: String = "GradientBevelFilter"
}

open class FilterGradientGlow(id: Int) : Filter(id), IFilter {
	var numColors = 0
	var blurX = 0.0
	var blurY = 0.0
	var angle = 0.0
	var distance = 0.0
	var strength = 0.0
	var innerShadow = false
	var knockout = false
	var compositeSource = false
	var onTop = false
	var passes = 0

	protected var gradientColors = arrayListOf<Int>()
	protected var gradientRatios = arrayListOf<Int>()

	//override val filter: BitmapFilter get() {
	//	val gradientGlowColors = arrayListOf<Int>()
	//	val gradientGlowAlphas = arrayListOf<Double>()
	//	val gradientGlowRatios = arrayListOf<Int>()
	//	for (i in 0 until numColors) {
	//		gradientGlowColors.add(ColorUtils.rgb(gradientColors[i]))
	//		gradientGlowAlphas.add(ColorUtils.alpha(gradientColors[i]))
	//		gradientGlowRatios.add(gradientRatios[i])
	//	}
	//	val filterType: BitmapFilterType
	//	if (onTop) {
	//		filterType = BitmapFilterType.FULL
	//	} else {
	//		filterType = if (innerShadow) BitmapFilterType.INNER else BitmapFilterType.OUTER
	//	}
	//	return GradientGlowFilter(
	//		distance,
	//		angle * 180 / PI,
	//		gradientGlowColors,
	//		gradientGlowAlphas,
	//		gradientGlowRatios,
	//		blurX,
	//		blurY,
	//		strength,
	//		passes,
	//		filterType,
	//		knockout
	//	)
	//}

	override fun parse(data: SWFData): Unit {
		numColors = data.readUI8()
		for (i in 0 until numColors) {
			gradientColors.add(data.readRGBA())
		}
		for (i in 0 until numColors) {
			gradientRatios.add(data.readUI8())
		}
		blurX = data.readFIXED()
		blurY = data.readFIXED()
		angle = data.readFIXED()
		distance = data.readFIXED()
		strength = data.readFIXED8()
		val flags = data.readUI8()
		innerShadow = ((flags and 0x80) != 0)
		knockout = ((flags and 0x40) != 0)
		compositeSource = ((flags and 0x20) != 0)
		onTop = ((flags and 0x10) != 0)
		passes = flags and 0x0f
	}

	override fun toString(indent: Int): String {
		var str: String = "[$filterName] BlurX: $blurX, BlurY: $blurY, Angle: $angle, Distance: $distance, Strength: $strength, Passes: $passes"
		val flags = arrayListOf<String>()
		if (innerShadow) flags.add("InnerShadow")
		if (knockout) flags.add("Knockout")
		if (compositeSource) flags.add("CompositeSource")
		if (onTop) flags.add("OnTop")
		if (flags.size > 0) str += ", Flags: " + flags.joinToString(", ")
		if (gradientColors.size > 0) {
			str += "\n" + " ".repeat(indent + 2) + "GradientColors:"
			for (i in 0 until gradientColors.size) {
				str += (if (i > 0) ", " else " ") + ColorUtils.rgbToString(gradientColors[i])
			}
		}
		if (gradientRatios.size > 0) {
			str += "\n" + " ".repeat(indent + 2) + "GradientRatios:"
			for (i in 0 until gradientRatios.size) {
				str += (if (i > 0) ", " else " ") + gradientRatios[i]
			}
		}
		return str
	}

	open val filterName = "GradientGlowFilter"
}
