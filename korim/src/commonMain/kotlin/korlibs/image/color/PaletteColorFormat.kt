package korlibs.image.color

class PaletteColorFormat(val palette: RgbaArray) : ColorFormat {
    override val bpp = 8
	override fun getR(v: Int): Int = palette[v].r
	override fun getG(v: Int): Int = palette[v].g
	override fun getB(v: Int): Int = palette[v].b
	override fun getA(v: Int): Int = palette[v].a
	override fun pack(r: Int, g: Int, b: Int, a: Int): Int = RGBA(r, g, b, a).value
}
