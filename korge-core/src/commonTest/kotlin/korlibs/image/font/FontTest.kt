package korlibs.image.font

import korlibs.image.atlas.*
import korlibs.image.bitmap.*
import korlibs.image.color.*
import korlibs.image.paint.*
import korlibs.image.text.*
import korlibs.io.async.*
import korlibs.io.file.std.*
import korlibs.logger.*
import korlibs.math.geom.*
import korlibs.platform.*
import kotlin.test.*

class FontTest {
    val logger = Logger("FontTest")

    /*
    @Test
    @Ignore
    fun test() = suspendTest {
        BitmapFont(SystemFont("Arial"), 100.0, chars = CharacterSet.LATIN_ALL).register(name = "Arial")
        //BitmapFont(SystemFont("Arial"), 10.0, chars = CharacterSet.LATIN_ALL).register(name = "Arial")
        //resourcesVfs["tinymce-small.ttf"].readTtfFont().register(name = "Arial")
        //BitmapFont(resourcesVfs["chunky-wally.ttf"].readTtfFont(), 100.0).register(name = "Arial") // @TODO: This doesn't work probably because bounds are not right
        resourcesVfs["chunky-wally.ttf"].readTtfFont().register(name = "Arial")
        //resourcesVfs["Comfortaa-Regular.ttf"].readTtfFont().register(name = "Arial")
        //resourcesVfs["OptimusPrinceps.ttf"].readTtfFont().register(name = "Arial")

        val svgXmlString = buildSvgXml {
            this.fillStyle = createLinearGradient(0, 0, 0, 48).add(0.0, Colors.BLUE).add(1.0, Colors.GREEN)
            //this.fillStyle = createColor(Colors.BLACK)
            this.fontName = "Arial"
            //this.font = font
            this.fontSize = 48.0
            translate(20, 20)
            rotate(15.degrees)
            fillText("12 Hello World!", 16, 48)
        }

        //val font =
        //val font = SystemFont("Arial")
        //SystemFontRegistry.register(BitmapFont(SystemFont("Arial"), 48.0, chars = CharacterSet.LATIN_ALL), "Arial")
        //BitmapFont()
        //val font = SystemFont("Arial", 24)
        //val font = SystemFont("Arial", 24)
        //val image = NativeImage(512, 512).context2d {
        //val image = Bitmap32(512, 512).context2d(antialiased = false) {
        val image = Bitmap32(512, 512, premultiplied = false).context2d(antialiased = true) {
            this.fillStyle = createLinearGradient(0, 0, 0, 48).add(0.0, Colors.BLUE).add(1.0, Colors.GREEN)
            //this.fillStyle = createColor(Colors.BLACK)
            this.fontName = "Arial"
            //this.font = font
            this.fontSize = 48.0
            translate(20, 20)
            rotate(15.degrees)
            fillText("12 Hello World!", 16, 48)
            //font.fillText(this, "HELLO", color = Colors.BLACK, x = 50.0, y = 50.0)
        }
        //image.showImageAndWait()
    }

     */

    @Test
    fun test2() = suspendTest {
        //val font = DefaultTtfFont


        //DefaultTtfFont.renderGlyphToBitmap(256.0, '´'.toInt()).bmp.showImageAndWait()
        //DefaultTtfFont.renderGlyphToBitmap(256.0, 'o'.toInt(), nativeRendering = false).bmp.showImageAndWait()
        //DefaultTtfFont.renderGlyphToBitmap(256.0, 'ó'.toInt(), nativeRendering = false).bmp.showImageAndWait()
        //return@suspendTest

        //DefaultTtfFont.renderGlyphToBitmap(256.0, 'ó'.toInt(), nativeRendering = false).bmp.showImageAndWait()
        //DefaultTtfFont.renderGlyphToBitmap(256.0, 'ó'.toInt()).bmp.showImageAndWait()
        //println(result2)
        //result2.bmp.showImageAndWait()

        //BitmapFont(DefaultTtfFont, 64.0, paint = ColorPaint(Colors.RED)).atlas.showImageAndWait()
        //BitmapFont(SystemFont("Arial"), 64.0, paint = ColorPaint(Colors.RED)).atlas.showImageAndWait()

        val img1 = Bitmap32(128, 128, premultiplied = false).context2d {
            //rect(0, 0, 50, 50)
            circle(Point(25, 50), 30.0)
            clip()
            beginPath()

            moveTo(Point(0, 100))
            lineTo(Point(30, 10))
            lineTo(Point(60, 100))
            close()
            fill()
        }

        //val font = SystemFont("Arial")
        val font = DefaultTtfFont
        //val font = BitmapFont(DefaultTtfFont, 64.0)
        //val font = BitmapFont(DefaultTtfFont, 24.0)
        //val font = BitmapFont(SystemFont("Arial"), 24.0)
        //val font = SystemFont("Arial")
        //val font = BitmapFont(DefaultTtfFont, 24.0)

        //println(buildSvgXml { drawText("Hello World!") }.toString())
        //font.atlas.showImageAndWait()
        //val paint = ColorPaint(Colors.RED)
        //val paint = ColorPaint(Colors.BLUE)
        val paint = LinearGradientPaint(0, 0, 0, -48).add(0.0, Colors.BLUE).add(1.0, Colors.GREEN)
        //NativeImage(100, 100).context2d {
        //    fillStyle = paint
        //    fillRect(0, 0, 100, 100)
        //}.showImageAndWait()

        //val result = font.renderTextToBitmap(48.0, "Helló World!", paint, nativeRendering = false, renderer = CreateStringTextRenderer { text, n, c, c1, g, advance ->
        val result = font.renderTextToBitmap(48.0, "Helló World!", paint, nativeRendering = false, renderer = CreateStringTextRenderer { reader, c, g, advance ->
        //val result = font.renderTextToBitmap(24.0, "llll", ColorPaint(Colors.RED), renderer = CreateStringTextRenderer { text, n, c, c1, g, advance ->
        //val result = font.renderTextToBitmap(24.0, "Hello World!", renderer = CreateStringTextRenderer { text, n, c, c1, g, advance ->
            //dy = -n.toDouble()
            val scale = 1f + reader.position * 0.1f
            //val scale = 1.0
            //transform.translate(0.0, scale)
            transform = Matrix().scaled(scale).rotated(25.degrees)
            put(reader, c)
            advance(advance * scale)
        })

        //img1.showImageAndWait()
        //result.bmp.showImageAndWait()
    }

    @Test
    fun testDefaultFont() {
        val font = DefaultTtfFont
        val fmetrics = font.getFontMetrics(16.0)
        val gmetrics = font.getGlyphMetrics(16.0, 'k'.code)
        assertEquals(
            """
                FontMetrics(size=16, top=14.8, ascent=14.8, baseline=0, descent=-3.1, bottom=-3.1, lineGap=0.4, unitsPerEm=16, maxWidth=21.5, lineHeight=18.4)
                GlyphMetrics(codePoint=107 ('k'), existing=true, xadvance=7, bounds=Rectangle(x=0, y=0, width=6, height=10))
            """.trimIndent(),
            "$fmetrics\n$gmetrics"
        )
    }

    @Test
    fun testReadFont() = suspendTestNoBrowser {
        val font1 = resourcesVfs["myfont.ttf"].readTtfFont()
        val font2 = resourcesVfs["myfont-bug.ttf"].readTtfFont()
        val font3 = resourcesVfs["myfont-bug2.ttf"].readTtfFont()
        val font4 = resourcesVfs["myfont-bug3.ttf"].readTtfFont()
        //font1.renderTextToBitmap(20.0, "Hello World!", border = 64, nativeRendering = false).bmp.showImageAndWait()
        //font4.renderTextToBitmap(64.0, "12 Hello World", nativeRendering = true).bmp.showImageAndWait()
    }

    @Test
    fun testReadOpenTypeFont() = suspendTestNoBrowser {
        //assertFailsWith<UnsupportedOperationException> {
            val font1 = resourcesVfs["helvetica.otf"].readTtfFont()
            logger.debug { "font1=$font1" }
        //}
    }

    @Test
    fun testTextBounds() {
        val text0 = ""
        val text1 = "Hello : jworld"
        val text2 = "Hello : jworld\ntest"
        fun metrics(text: String, align: TextAlignment): TextMetrics =
            DefaultTtfFont.getTextBounds(16.0, text, align = align).round()
        assertEquals(
            """
                [0]left:     TextMetrics[0, 0, 0, 18][0, 18]
                [0]middle:   TextMetrics[0, -9, 0, 18][0, 9]
                [0]baseline: TextMetrics[0, -15, 0, 18][0, 3]
                [0]bottom:   TextMetrics[0, -18, 0, 18][0, 0]
                [1]left:     TextMetrics[-1, 0, 79, 18][1, 18]
                [1]middle:   TextMetrics[-1, -9, 79, 18][1, 9]
                [1]baseline: TextMetrics[-1, -15, 79, 18][1, 3]
                [1]bottom:   TextMetrics[-1, -18, 79, 18][1, 0]
                [2]left:     TextMetrics[-1, 0, 79, 37][1, 18]
                [2]middle:   TextMetrics[-1, -18, 79, 37][1, 0]
                [2]baseline: TextMetrics[-1, -15, 79, 37][1, 3]
                [2]bottom:   TextMetrics[-1, -37, 79, 37][1, -19]
                Rectangle(x=3, y=0, width=342, height=73)
            """.trimIndent(),
            """
                [0]left:     ${metrics(text0, TextAlignment.TOP_LEFT)}
                [0]middle:   ${metrics(text0, TextAlignment.MIDDLE_LEFT)}
                [0]baseline: ${metrics(text0, TextAlignment.BASELINE_LEFT)}
                [0]bottom:   ${metrics(text0, TextAlignment.BOTTOM_LEFT)}
                [1]left:     ${metrics(text1, TextAlignment.TOP_LEFT)}
                [1]middle:   ${metrics(text1, TextAlignment.MIDDLE_LEFT)}
                [1]baseline: ${metrics(text1, TextAlignment.BASELINE_LEFT)}
                [1]bottom:   ${metrics(text1, TextAlignment.BOTTOM_LEFT)}
                [2]left:     ${metrics(text2, TextAlignment.TOP_LEFT)}
                [2]middle:   ${metrics(text2, TextAlignment.MIDDLE_LEFT)}
                [2]baseline: ${metrics(text2, TextAlignment.BASELINE_LEFT)}
                [2]bottom:   ${metrics(text2, TextAlignment.BOTTOM_LEFT)}
                ${DefaultTtfFont.getTextBounds(64.0, "jHello : Worljg").bounds.int}
            """.trimIndent()
        )
    }

    @Test
    fun testBitmapFonts() = suspendTest({ !Platform.isJsNodeJs }) {
        val atlas = MutableAtlasUnit(512, 512, border = 1)
        val txtFont = resourcesVfs["reality_hyper_regular_17.fnt"].readBitmapFont(atlas = atlas)
        val xmlFont = resourcesVfs["example-font.xml"].readBitmapFont(atlas = atlas)

        //atlas.allBitmaps.showImagesAndWait()
    }
}
