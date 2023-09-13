package korlibs.image.color

class RGBAd(
    var r: Double,
    var g: Double,
    var b: Double,
    var a: Double
) {
    constructor(c: RGBAd) : this(c.r, c.g, c.b, c.a)
    constructor(c: Int) : this(RGBA.getRd(c), RGBA.getGd(c), RGBA.getBd(c), RGBA.getAd(c))
    constructor() : this(0.0, 0.0, 0.0, 0.0)

    fun set(r: Double, g: Double, b: Double, a: Double) {
        this.r = r
        this.g = g
        this.b = b
        this.a = a
    }

    fun add(r: Double, g: Double, b: Double, a: Double) {
        this.r += r
        this.g += g
        this.b += b
        this.a += a
    }

    fun set(c: RGBAd) = set(c.r, c.g, c.b, c.a)

    fun toRGBA(): RGBA = RGBA(
        (r * 255).toInt() and 0xFF,
        (g * 255).toInt() and 0xFF,
        (b * 255).toInt() and 0xFF,
        (a * 255).toInt() and 0xFF
    )
}
