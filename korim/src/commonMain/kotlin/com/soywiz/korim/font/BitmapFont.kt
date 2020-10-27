package com.soywiz.korim.font

import com.soywiz.kds.*
import com.soywiz.kmem.insert
import com.soywiz.kmem.nextPowerOfTwo
import com.soywiz.kmem.toIntCeil
import com.soywiz.korim.bitmap.*
import com.soywiz.korim.atlas.MutableAtlas
import com.soywiz.korim.bitmap.effect.BitmapEffect
import com.soywiz.korim.color.Colors
import com.soywiz.korim.color.RGBA
import com.soywiz.korim.format.*
import com.soywiz.korim.paint.DefaultPaint
import com.soywiz.korim.paint.Paint
import com.soywiz.korim.text.*
import com.soywiz.korim.vector.*
import com.soywiz.korio.dynamic.KDynamic
import com.soywiz.korio.file.*
import com.soywiz.korio.lang.substr
import com.soywiz.korio.serialization.xml.Xml
import com.soywiz.korio.serialization.xml.get
import com.soywiz.korio.util.unquote
import com.soywiz.korma.geom.Rectangle
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.set
import kotlin.math.max
import kotlin.math.sqrt

class BitmapFont(
    val fontSize: Double,
    val lineHeight: Double,
    val base: Double,
    val glyphs: IntMap<Glyph>,
    val kernings: IntMap<Kerning>,
    val atlas: Bitmap = glyphs.values.iterator().next()?.texture?.bmp ?: Bitmaps.transparent.bmp,
    override val name: String = "BitmapFont"
) : Font, Extra by Extra.Mixin() {
    override fun getOrNull() = this
    override suspend fun get() = this

    private val naturalFontMetrics by lazy {
        FontMetrics(
            fontSize, lineHeight, lineHeight, 0.0, 0.0, 0.0, 0.0,
            maxWidth = run {
                var width = 0.0
                for (glyph in glyphs.values) if (glyph != null) width = max(width, glyph.texture.width.toDouble())
                width
            }
        )
    }

    override fun getFontMetrics(size: Double, metrics: FontMetrics): FontMetrics =
        metrics.copyFromNewSize(naturalFontMetrics, size)

    override fun getGlyphMetrics(size: Double, codePoint: Int, metrics: GlyphMetrics): GlyphMetrics =
        metrics.copyFromNewSize(glyphs[codePoint]?.naturalMetrics ?: naturalNonExistantGlyphMetrics, size, codePoint)

    private val naturalNonExistantGlyphMetrics = GlyphMetrics(fontSize, false, 0, Rectangle(), 0.0)

    override fun getKerning(size: Double, leftCodePoint: Int, rightCodePoint: Int): Double =
        getTextScale(size) * (getKerning(leftCodePoint, rightCodePoint)?.amount?.toDouble() ?: 0.0)

    override fun renderGlyph(ctx: Context2d, size: Double, codePoint: Int, x: Double, y: Double, fill: Boolean, metrics: GlyphMetrics) {
        val scale = getTextScale(size)
        val g = glyphs[codePoint] ?: return
        getGlyphMetrics(size, codePoint, metrics).takeIf { it.existing } ?: return
        if (metrics.width == 0.0 && metrics.height == 0.0) return
        //println("SCALE: $scale")
        val texX = x + metrics.left
        val texY = y + metrics.top
        val swidth = metrics.width
        val sheight = metrics.height

        val bmp = if (ctx.fillStyle == DefaultPaint) {
            g.bmp
        } else {
            val bmpFill = Bitmap32(g.bmp.width, g.bmp.height).context2d {
                this.keepTransform {
                    this.scale(1.0 / scale)
                    this.fillStyle = ctx.fillStyle
                    fillRect(0, 0, width * scale, height * scale)
                }
            }
            bmpFill.writeChannel(BitmapChannel.ALPHA, g.bmp, BitmapChannel.ALPHA)
            bmpFill
        }
        ctx.drawImage(bmp, texX, texY - metrics.height, swidth, sheight)
        //ctx.drawImage(g.bmp, texX, texY, swidth, sheight)
    }

    private fun getTextScale(size: Double) = size.toDouble() / fontSize.toDouble()

	fun measureWidth(text: String): Int {
		var x = 0
		for (c in text) {
			val glyph = glyphs[c.toInt()]
			if (glyph != null) x += glyph.xadvance
		}
		return x
	}

	fun getKerning(first: Char, second: Char): Kerning? = getKerning(first.toInt(), second.toInt())
	fun getKerning(first: Int, second: Int): Kerning? = kernings[Kerning.buildKey(first, second)]

	class Kerning(
		val first: Int,
		val second: Int,
		val amount: Int
	) {
        companion object {
            fun buildKey(f: Int, s: Int) = 0.insert(f, 0, 16).insert(s, 16, 16)
        }
    }


	data class Glyph(
        val fontSize: Double,
		val id: Int,
		val texture: BitmapSlice<Bitmap>,
		val xoffset: Int,
		val yoffset: Int,
		val xadvance: Int
	) {
        val bmp: Bitmap32 by lazy { texture.extract().toBMP32() }

        internal val naturalMetrics = GlyphMetrics(
            fontSize, true, -1,
            Rectangle(xoffset, yoffset, texture.width, texture.height),
            xadvance.toDouble()
        )
    }

	val dummyGlyph by lazy { Glyph(fontSize, -1, Bitmaps.transparent, 0, 0, 0) }
	val anyGlyph: Glyph by lazy { glyphs[glyphs.keys.iterator().next()] ?: dummyGlyph }
	val baseBmp: Bitmap by lazy { anyGlyph.texture.bmp }

	operator fun get(charCode: Int): Glyph = glyphs[charCode] ?: glyphs[32] ?: dummyGlyph
	operator fun get(char: Char): Glyph = this[char.toInt()]

	companion object {
        /**
         * Creates a new [BitmapFont] of [fontSize] using an existing [Font] ([SystemFont] is valid).
         * Just creates the glyphs specified in [chars].
         * Allows to set a different [fontName] than the one provided at [Font].
         */
        operator fun invoke(
                font: Font,
                fontSize: Number,
                chars: CharacterSet = CharacterSet.LATIN_ALL,
                fontName: String = font.name,
                paint: Paint = Colors.WHITE,
                mipmaps: Boolean = true,
                effect: BitmapEffect? = null,
        ): BitmapFont {
            val fontSize = fontSize.toDouble()
            val fmetrics = font.getFontMetrics(fontSize)
            val glyphMetrics = chars.codePoints.map { font.getGlyphMetrics(fontSize, it) }
            val requiredArea = glyphMetrics.map { (it.width + 4) * (fmetrics.lineHeight + 4) }.sum().toIntCeil()
            val requiredAreaSide = sqrt(requiredArea.toDouble()).toIntCeil()
            val matlas = MutableAtlas<TextToBitmapResult>(requiredAreaSide.nextPowerOfTwo, requiredAreaSide.nextPowerOfTwo)
            val border = 2
            for (codePoint in chars.codePoints) {
                val result = font.renderGlyphToBitmap(fontSize, codePoint, paint = paint, fill = true, border = 1, effect = effect)
                //val result = font.renderGlyphToBitmap(fontSize, codePoint, paint = DefaultPaint, fill = true)
                //println("codePoint[${codePoint.toChar()}]: $result")
                matlas.add(result.bmp.toBMP32().premultipliedIfRequired(), result)
            }
            val fm = matlas.entries.first().data.fmetrics
            val atlas = matlas.bitmap
            return BitmapFont(
                fontSize = fontSize,
                lineHeight = fmetrics.lineHeight,
                base = fmetrics.top,
                glyphs = matlas.entries.associate {
                    val slice = it.slice
                    val g = it.data.glyphs.first()
                    //val fm = it.data.fmetrics
                    val m = g.metrics
                    g.codePoint to Glyph(fontSize, g.codePoint, slice, -border, (border - m.height - m.top).toInt() + fm.ascent.toInt(), m.xadvance.toIntCeil())
                }.toIntMap(),
                kernings = IntMap(),
                atlas = atlas.mipmaps(mipmaps),
                name = fontName
            )
        }
	}
}

suspend fun VfsFile.readBitmapFont(imageFormat: ImageFormat = RegisteredImageFormats, mipmaps: Boolean = true): BitmapFont {
	val fntFile = this
	val content = fntFile.readString().trim()
	val textures = hashMapOf<Int, BitmapSlice<Bitmap>>()

    return when {
        content.startsWith('<') -> readBitmapFontXml(content, fntFile, textures, imageFormat, mipmaps)
        content.startsWith("info") -> readBitmapFontTxt(content, fntFile, textures, imageFormat, mipmaps)
        else -> TODO("Unsupported font type starting with ${content.substr(0, 16)}")
    }
}

private suspend fun readBitmapFontTxt(
	content: String,
	fntFile: VfsFile,
	textures: HashMap<Int, BitmapSlice<Bitmap>>,
	imageFormat: ImageFormat = RegisteredImageFormats,
    mipmaps: Boolean = true
): BitmapFont {
    val kernings = arrayListOf<BitmapFont.Kerning>()
	val glyphs = arrayListOf<BitmapFont.Glyph>()
	var lineHeight = 16.0
	var fontSize = 16.0
	var base: Double? = null
	for (rline in content.lines()) {
		val line = rline.trim()
		val map = LinkedHashMap<String, String>()
		for (part in line.split(' ')) {
			val (key, value) = part.split('=') + listOf("", "")
			map[key] = value
		}
		when {
			line.startsWith("info") -> {
				fontSize = map["size"]?.toDouble() ?: 16.0
			}
			line.startsWith("page") -> {
				val id = map["id"]?.toInt() ?: 0
				val file = map["file"]?.unquote() ?: error("page without file")
				textures[id] = fntFile.parent[file].readBitmap(imageFormat).mipmaps(mipmaps).slice()
			}
			line.startsWith("common ") -> {
				lineHeight = map["lineHeight"]?.toDoubleOrNull() ?: 16.0
				base = map["base"]?.toDoubleOrNull()
			}
			line.startsWith("char ") -> {
				//id=54 x=158 y=88 width=28 height=42 xoffset=2 yoffset=8 xadvance=28 page=0 chnl=0
				val page = map["page"]?.toIntOrNull() ?: 0
				val texture = textures[page] ?: textures.values.first()
				glyphs += KDynamic {
                    BitmapFont.Glyph(
                        fontSize = fontSize,
                        id = map["id"].int,
                        xoffset = map["xoffset"].int,
                        yoffset = map["yoffset"].int,
                        xadvance = map["xadvance"].int,
                        texture = texture.sliceWithSize(map["x"].int, map["y"].int, map["width"].int, map["height"].int)
                    )
				}
			}
			line.startsWith("kerning ") -> {
				kernings += BitmapFont.Kerning(
                    first = map["first"]?.toIntOrNull() ?: 0,
                    second = map["second"]?.toIntOrNull() ?: 0,
                    amount = map["amount"]?.toIntOrNull() ?: 0
                )
			}
		}
	}
	return BitmapFont(
        atlas = textures.values.first().bmp,
        fontSize = fontSize,
        lineHeight = lineHeight,
        base = base ?: lineHeight,
        glyphs = glyphs.map { it.id to it }.toMap().toIntMap(),
        kernings = kernings.associateByInt { _, it ->
            BitmapFont.Kerning.buildKey(it.first, it.second)
        }
    )
}

private suspend fun readBitmapFontXml(
	content: String,
	fntFile: VfsFile,
	textures: MutableMap<Int, BitmapSlice<Bitmap>>,
    imageFormat: ImageFormat = RegisteredImageFormats,
    mipmaps: Boolean = true
): BitmapFont {
	val xml = Xml(content)

	val fontSize = xml["info"].firstOrNull()?.double("size", 16.0) ?: 16.0
	val lineHeight = xml["common"].firstOrNull()?.double("lineHeight", 16.0) ?: 16.0
	val base = xml["common"].firstOrNull()?.double("base", 16.0) ?: 16.0

	for (page in xml["pages"]["page"]) {
		val id = page.int("id")
		val file = page.str("file")
		val texFile = fntFile.parent[file]
		val tex = texFile.readBitmap(imageFormat).mipmaps(mipmaps).slice()
		textures[id] = tex
	}

	val glyphs = xml["chars"]["char"].map {
		val page = it.int("page")
		val texture = textures[page] ?: textures.values.first()
        BitmapFont.Glyph(
            fontSize = fontSize,
            id = it.int("id"),
            texture = texture.sliceWithSize(it.int("x"), it.int("y"), it.int("width"), it.int("height")),
            xoffset = it.int("xoffset"),
            yoffset = it.int("yoffset"),
            xadvance = it.int("xadvance")
        )
	}

	val kernings = xml["kernings"]["kerning"].map {
        BitmapFont.Kerning(
            first = it.int("first"),
            second = it.int("second"),
            amount = it.int("amount")
        )
	}

	return BitmapFont(
        atlas = textures.values.first().bmp,
        fontSize = fontSize,
        lineHeight = lineHeight,
        base = base,
        glyphs = glyphs.map { it.id to it }.toMap().toIntMap(),
        kernings = kernings.map {
            BitmapFont.Kerning.buildKey(
                it.first,
                it.second
            ) to it
        }.toMap().toIntMap()
    )
}

fun Bitmap32.drawText(
    font: BitmapFont,
    str: String,
    x: Int = 0, y: Int = 0,
    color: RGBA = Colors.WHITE,
    size: Double = font.fontSize.toDouble(),
    alignment: TextAlignment = TextAlignment.TOP_LEFT,
) = context2d {
    this.font = font
    this.fontSize = size
    this.alignment = alignment
    this.fillStyle = createColor(color)
    this.fillText(str, x, y)
}

fun Font.toBitmapFont(
        fontSize: Number,
        chars: CharacterSet = CharacterSet.LATIN_ALL,
        fontName: String = this.name,
        paint: Paint = Colors.WHITE,
        mipmaps: Boolean = true,
        effect: BitmapEffect? = null,
) = BitmapFont(this, fontSize, chars, fontName, paint, mipmaps, effect)

suspend fun BitmapFont.writeToFile(out: VfsFile, writeBitmap: Boolean = true) {
    val font = this
    val fntFile = out
    val bmpFile = out.parent["${out.pathInfo.baseNameWithoutExtension}.png"]
    if (writeBitmap) {
        bmpFile.writeBitmap(font.baseBmp, PNG)
    }
    fntFile.writeString(buildString {
        appendLine("info face=\"${font.name}\" size=${font.fontSize.toInt()} bold=0 italic=0 charset=\"\" unicode=0 stretchH=100 smooth=1 aa=1 padding=0,0,0,0 spacing=0,0")
        appendLine("common lineHeight=${font.lineHeight.toInt()} base=${font.base.toInt()} scaleW=${font.baseBmp.width} scaleH=${font.baseBmp.height} pages=1 packed=0")
        appendLine("page id=0 file=\"${bmpFile.baseName}\"")
        val glyphs = font.glyphs.toMap()
        appendLine("chars count=${glyphs.size}")
        for ((charId, glyph) in glyphs) {
            val x = glyph.texture.left
            val y = glyph.texture.top
            val width = glyph.texture.width
            val height = glyph.texture.height
            val xoffset = glyph.xoffset
            val yoffset = glyph.yoffset
            val xadvance = glyph.xadvance
            appendLine("char id=$charId x=$x y=$y width=$width height=$height xoffset=$xoffset yoffset=$yoffset xadvance=$xadvance page=0 chnl=0")
        }
        val kernings = font.kernings.toMap()
        appendLine("kernings count=${kernings.size}")
        for ((_, kerning) in kernings) {
            appendLine("kerning first=${kerning.first} second=${kerning.second} amount=${kerning.amount}")
        }
    })
}
