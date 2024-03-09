package korlibs.image.font

import korlibs.datastructure.*
import korlibs.datastructure.iterators.*
import korlibs.image.atlas.*
import korlibs.image.bitmap.*
import korlibs.image.bitmap.effect.*
import korlibs.image.color.*
import korlibs.image.format.*
import korlibs.image.paint.*
import korlibs.image.text.*
import korlibs.image.vector.*
import korlibs.io.dynamic.*
import korlibs.io.file.*
import korlibs.io.lang.*
import korlibs.io.serialization.json.*
import korlibs.io.serialization.xml.*
import korlibs.io.util.*
import korlibs.math.*
import korlibs.math.geom.*
import korlibs.memory.*
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.set
import kotlin.math.*

interface BitmapFont : Font {
    val fontSize: Double
    val lineHeight: Double
    val base: Double
    val distanceField: String?

    // @TODO: Only for generating
    val glyphs: IntMap<Glyph>
    val kernings: IntMap<Kerning>

    val baseBmp: Bitmap get() = anyGlyph.texture.bmp
    val anyGlyph: Glyph
    val invalidGlyph: Glyph

    val naturalFontMetrics: FontMetrics
    val naturalNonExistantGlyphMetrics: GlyphMetrics

    override fun getFontMetrics(size: Double, metrics: FontMetrics): FontMetrics =
        metrics.copyFromNewSize(naturalFontMetrics, size)

    override fun getGlyphMetrics(size: Double, codePoint: Int, metrics: GlyphMetrics, reader: WStringReader?): GlyphMetrics =
        metrics.copyFromNewSize(getOrNull(codePoint)?.naturalMetrics ?: naturalNonExistantGlyphMetrics, size, codePoint)

    fun getKerning(first: Int, second: Int): Kerning?
    fun getTextScale(size: Double): Double = size / fontSize
    fun getOrNull(codePoint: Int): Glyph?

    operator fun get(charCode: Int): Glyph = getOrNull(charCode) ?: getOrNull(32) ?: anyGlyph
    fun getKerning(first: Char, second: Char): Kerning? = getKerning(first.code, second.code)
    operator fun get(char: Char): Glyph = this[char.code]
    fun getGlyph(codePoint: Int): Glyph = this[codePoint]
    fun getGlyph(char: Char): Glyph = this[char]

    fun measureWidth(text: String): Int {
        var x = 0
        for (c in text) {
            val glyph = this[c.code]
            x += glyph.xadvance
        }
        return x
    }

    override fun renderGlyph(
        ctx: Context2d,
        size: Double,
        codePoint: Int,
        pos: Point,
        fill: Boolean?,
        metrics: GlyphMetrics,
        reader: WStringReader?,
        beforeDraw: (() -> Unit)?,
    ): Boolean {
        val scale = getTextScale(size)
        val g = glyphs[codePoint] ?: return false
        getGlyphMetrics(size, codePoint, metrics, reader).takeIf { it.existing } ?: return false
        if (metrics.width == 0.0 && metrics.height == 0.0) return false
        //println("SCALE: $scale")
        val texX = pos.x + metrics.left
        val texY = pos.y + metrics.top
        val swidth = metrics.width
        val sheight = metrics.height

        val bmp = if (ctx.fillStyle == DefaultPaint) {
            g.bmp
        } else {
            val bmpFill = Bitmap32(g.bmp.width, g.bmp.height, premultiplied = true).context2d {
                this.keepTransform {
                    this.scale(1.0 / scale)
                    this.fillStyle = ctx.fillStyle
                    fillRect(0, 0, width * scale, height * scale)
                }
            }
            bmpFill.writeChannel(BitmapChannel.ALPHA, g.bmp, BitmapChannel.ALPHA)
            bmpFill
        }
        beforeDraw?.invoke()
        ctx.drawImage(bmp, Point(texX, texY - metrics.bottom), Size(swidth, sheight))
        //ctx.drawImage(g.bmp, texX, texY, swidth, sheight)
        return true
    }

    override fun getKerning(size: Double, leftCodePoint: Int, rightCodePoint: Int): Double =
        getTextScale(size) * (getKerning(leftCodePoint, rightCodePoint)?.amount?.toDouble() ?: 0.0)

    companion object {
        inline operator fun invoke(
            fontSize: Number,
            lineHeight: Number,
            base: Number,
            glyphs: IntMap<Glyph>,
            kernings: IntMap<Kerning>,
            name: String = "BitmapFont",
            distanceField: String? = null,
        ): BitmapFont = BitmapFontImpl(
            fontSize.toDouble(),
            lineHeight.toDouble(),
            base.toDouble(),
            glyphs,
            kernings,
            name,
            distanceField)

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
            val fontSizeD = fontSize.toDouble()
            val fmetrics = font.getFontMetrics(fontSizeD)
            val glyphMetrics = chars.codePoints.map { font.getGlyphMetrics(fontSizeD, it, reader = null) }
            val requiredArea = glyphMetrics.map { (it.width + 4) * (fmetrics.lineHeight + 4) }.sum().toIntCeil()
            val requiredAreaSide = sqrt(requiredArea.toFloat()).toIntCeil()
            val matlas = MutableAtlas<TextToBitmapResult>(requiredAreaSide.nextPowerOfTwo, requiredAreaSide.nextPowerOfTwo)
            val border = 2
            for (codePoint in chars.codePoints) {
                val result = font.renderGlyphToBitmap(fontSizeD, codePoint, paint = paint, fill = true, border = 1, effect = effect)
                //val result = font.renderGlyphToBitmap(fontSize, codePoint, paint = DefaultPaint, fill = true)
                //println("codePoint[${codePoint.toChar()}]: $result")
                matlas.add(result.bmp.toBMP32().premultipliedIfRequired(), result)
            }
            val fm = matlas.entries.first().data.fmetrics
            val atlas = matlas.bitmap.mipmaps(mipmaps)
            return BitmapFont(
                fontSize = fontSize,
                lineHeight = fmetrics.lineHeight,
                base = fmetrics.top,
                glyphs = matlas.entries.associate {
                    val slice = it.slice
                    val g = it.data.glyphs.first()
                    //val fm = it.data.fmetrics
                    val m = g.metrics
                    g.codePoint to Glyph(fontSizeD, g.codePoint, slice, -border, (border - m.height - m.top).toInt() + fm.ascent.toInt(), m.xadvance.toIntCeil())
                }.toIntMap(),
                kernings = IntMap(),
                name = fontName
            )
        }
    }


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
        val texture: BmpSlice,
        val xoffset: Int,
        val yoffset: Int,
        val xadvance: Int
    ) {
        val texWidth: Int get() = texture.width
        val texHeight: Int get() = texture.height
        val bmp: Bitmap32 by lazy { texture.extract().toBMP32() }

        internal val naturalMetrics = GlyphMetrics(
            fontSize, true, -1,
            Rectangle(xoffset, yoffset, texture.width, texture.height),
            xadvance.toDouble()
        )
    }
}

@PublishedApi
internal class BitmapFontImpl constructor(
    override val fontSize: Double,
    override val lineHeight: Double,
    override val base: Double,
    override val glyphs: IntMap<BitmapFont.Glyph>,
    override val kernings: IntMap<BitmapFont.Kerning>,
    override val name: String = "BitmapFont",
    override val distanceField: String? = null,
) : BitmapFont, Extra by Extra.Mixin() {
    override fun getOrNull() = this
    override suspend fun get() = this

    val naturalDescent = lineHeight - base

    override val naturalFontMetrics: FontMetrics by lazy {
        val ascent = base
        val baseline = 0.0
        FontMetrics(
            fontSize, ascent, ascent, baseline, -naturalDescent, -naturalDescent, 0.0,
            maxWidth = run {
                var width = 0.0
                for (glyph in glyphs.values) if (glyph != null) width = max(width, glyph.texture.width.toDouble())
                width
            }
        )
    }
    override val naturalNonExistantGlyphMetrics: GlyphMetrics = GlyphMetrics(fontSize, false, 0, Rectangle(), 0.0)

	override fun getKerning(first: Int, second: Int): BitmapFont.Kerning? = kernings[BitmapFont.Kerning.buildKey(first, second)]
    override fun getOrNull(codePoint: Int): BitmapFont.Glyph? = glyphs[codePoint]

    override val anyGlyph: BitmapFont.Glyph by lazy { glyphs[glyphs.keys.iterator().next()] ?: invalidGlyph }
    override val invalidGlyph: BitmapFont.Glyph by lazy { BitmapFont.Glyph(fontSize, -1, Bitmaps.transparent, 0, 0, 0) }
}

suspend fun VfsFile.readBitmapFont(
    props: ImageDecodingProps = ImageDecodingProps.DEFAULT,
    mipmaps: Boolean = true,
    atlas: MutableAtlasUnit? = null
) : BitmapFont {
	val fntFile = this
	val content = fntFile.readString().trim()
	val textures = hashMapOf<Int, BmpSlice>()

    return when {
        content.startsWith('<') -> readBitmapFontXml(content, fntFile, textures, props, mipmaps, atlas)
        content.startsWith('{') -> readBitmapFontJson(content, fntFile, textures, props, mipmaps, atlas)
        content.startsWith("info") -> readBitmapFontTxt(content, fntFile, textures, props, mipmaps, atlas)
        else -> TODO("Unsupported font type starting with ${content.substr(0, 16)}")
    }
}

private suspend fun readBitmapFontJson(
    content: String,
    fntFile: VfsFile,
    textures: HashMap<Int, BmpSlice>,
    props: ImageDecodingProps = ImageDecodingProps.DEFAULT,
    mipmaps: Boolean = true,
    atlas: MutableAtlasUnit? = null
): BitmapFont {
    val json = Json.parseDyn(content)

    val fontSize = json["info"]["size"].toDoubleDefault(16.0)
    val lineHeight = json["common"]["lineHeight"].toDoubleDefault(16.0)
    val base = json["common"]["base"].toDoubleDefault(16.0)
    val distanceField = json["distanceField"]["fieldType"].toStringOrNull()

    json["pages"].toList().fastForEachWithIndex { id, page ->
        val file = page.toString()
        val texFile = fntFile.parent[file]
        val tex = texFile.readBitmap(props).mipmaps(mipmaps).slice()
        textures[id] = tex
    }

    val glyphs = json["chars"].toList().map {
        val page = it["page"].int
        val texture = textures[page] ?: textures.values.first()
        BitmapFont.Glyph(
            fontSize = fontSize,
            id = it["id"].int,
            texture = atlas?.add(texture.sliceWithSize(it["x"].int, it["y"].int, it["width"].int, it["height"].int) as BmpSlice, Unit)?.slice
                ?: texture.sliceWithSize(it["x"].int, it["y"].int, it["width"].int, it["height"].int),
            xoffset = it["xoffset"].int,
            yoffset = it["yoffset"].int,
            xadvance = it["xadvance"].int,
        )
    }


    val kernings = json["kernings"].toList().map {
        BitmapFont.Kerning(
            first = it["first"].int,
            second = it["second"].int,
            amount = it["amount"].int,
        )
    }

    return BitmapFont(
        fontSize = fontSize,
        lineHeight = lineHeight,
        base = base,
        glyphs = glyphs.map { it.id to it }.toMap().toIntMap(),
        kernings = kernings.map {
            BitmapFont.Kerning.buildKey(
                it.first,
                it.second
            ) to it
        }.toMap().toIntMap(),
        distanceField = distanceField
    )
}

private suspend fun readBitmapFontTxt(
	content: String,
	fntFile: VfsFile,
	textures: HashMap<Int, BmpSlice>,
	props: ImageDecodingProps = ImageDecodingProps.DEFAULT,
    mipmaps: Boolean = true,
    atlas: MutableAtlasUnit? = null
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
				fontSize = (map["size"]?.toDouble() ?: 16.0).absoluteValue
			}
			line.startsWith("page") -> {
				val id = map["id"]?.toInt() ?: 0
				val file = map["file"]?.unquote() ?: error("page without file")
				textures[id] = fntFile.parent[file].readBitmap(props).mipmaps(mipmaps).slice()
			}
			line.startsWith("common ") -> {
				lineHeight = map["lineHeight"]?.toDoubleOrNull() ?: 16.0
				base = map["base"]?.toDoubleOrNull()
			}
			line.startsWith("char ") -> {
				//id=54 x=158 y=88 width=28 height=42 xoffset=2 yoffset=8 xadvance=28 page=0 chnl=0
				val page = map["page"]?.toIntOrNull() ?: 0
				val texture = textures[page] ?: textures.values.first()
                val dmap = map.dyn
                val id = dmap["id"].int
				glyphs += BitmapFont.Glyph(
                    fontSize = fontSize,
                    id = id,
                    xoffset = dmap["xoffset"].int,
                    yoffset = dmap["yoffset"].int,
                    xadvance = dmap["xadvance"].int,
                    texture = atlas?.add(texture.sliceWithSize(dmap["x"].int, dmap["y"].int, dmap["width"].int, dmap["height"].int, "glyph-${id.toChar()}") as BmpSlice, Unit)?.slice
                        ?: texture.sliceWithSize(dmap["x"].int, dmap["y"].int, dmap["width"].int, dmap["height"].int, "glyph-${id.toChar()}")
                )
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
        fontSize = fontSize,
        lineHeight = lineHeight,
        base = base ?: lineHeight,
        glyphs = glyphs.associateBy { it.id }.toIntMap(),
        kernings = kernings.associateByInt { _, it ->
            BitmapFont.Kerning.buildKey(it.first, it.second)
        }
    )
}

private suspend fun readBitmapFontXml(
	content: String,
	fntFile: VfsFile,
	textures: MutableMap<Int, BmpSlice>,
    props: ImageDecodingProps = ImageDecodingProps.DEFAULT,
    mipmaps: Boolean = true,
    atlas: MutableAtlasUnit? = null
): BitmapFont {
	val xml = Xml(content)

	val fontSize = xml["info"].firstOrNull()?.doubleNull("size") ?: 16.0
	val lineHeight = xml["common"].firstOrNull()?.doubleNull("lineHeight") ?: 16.0
	val base = xml["common"].firstOrNull()?.doubleNull("base") ?: 16.0
    val distanceField = xml["distanceField"].firstOrNull()?.strNull("fieldType")

	for (page in xml["pages"]["page"]) {
		val id = page.int("id")
		val file = page.str("file")
		val texFile = fntFile.parent[file]
		val tex = texFile.readBitmap(props).mipmaps(mipmaps).slice()
		textures[id] = tex
	}

	val glyphs = xml["chars"]["char"].map {
		val page = it.int("page")
		val texture = textures[page] ?: textures.values.first()
        BitmapFont.Glyph(
            fontSize = fontSize,
            id = it.int("id"),
            texture = atlas?.add(texture.sliceWithSize(it.int("x"), it.int("y"), it.int("width"), it.int("height")) as BmpSlice, Unit)?.slice
                ?: texture.sliceWithSize(it.int("x"), it.int("y"), it.int("width"), it.int("height")),
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
        fontSize = fontSize,
        lineHeight = lineHeight,
        base = base,
        glyphs = glyphs.map { it.id to it }.toMap().toIntMap(),
        kernings = kernings.map {
            BitmapFont.Kerning.buildKey(
                it.first,
                it.second
            ) to it
        }.toMap().toIntMap(),
        distanceField = distanceField
    )
}

fun Bitmap32.drawText(
    font: BitmapFont,
    str: String,
    pos: Point = Point.ZERO,
    color: RGBA = Colors.WHITE,
    size: Double = font.fontSize,
    alignment: TextAlignment = TextAlignment.TOP_LEFT,
) = context2d {
    this.font = font
    this.fontSize = size
    this.alignment = alignment
    this.fillStyle = createColor(color)
    this.fillText(str, pos)
}

inline fun Font.toBitmapFont(
    fontSize: Number,
    chars: CharacterSet = CharacterSet.LATIN_ALL,
    fontName: String = this.name,
    paint: Paint = Colors.WHITE,
    mipmaps: Boolean = true,
    effect: BitmapEffect? = null,
): BitmapFont = BitmapFont(this, fontSize.toDouble(), chars, fontName, paint, mipmaps, effect)

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
