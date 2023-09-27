package korlibs.image.color

// 4x5 Color Transform
data class ColorMatrix(
    val rr: Float, val rb: Float, val rg: Float, val ra: Float, val r1: Float,
    val gr: Float, val gb: Float, val gg: Float, val ga: Float, val g1: Float,
    val br: Float, val bb: Float, val bg: Float, val ba: Float, val b1: Float,
    val ar: Float, val ab: Float, val ag: Float, val aa: Float, val a1: Float
) {
    companion object {
        val IDENTITY by lazy { ColorMatrix() }

        inline operator fun invoke() = ColorMatrix(
            1f, 0f, 0f, 0f, 0f,
            0f, 1f, 0f, 0f, 0f,
            0f, 0f, 1f, 0f, 0f,
            0f, 0f, 0f, 1f, 0f
        )

        inline operator fun invoke(
            rr: Number, rb: Number, rg: Number, ra: Number, r1: Number,
            gr: Number, gb: Number, gg: Number, ga: Number, g1: Number,
            br: Number, bb: Number, bg: Number, ba: Number, b1: Number,
            ar: Number, ab: Number, ag: Number, aa: Number, a1: Number
        ): ColorMatrix = ColorMatrix(
            rr.toFloat(), rb.toFloat(), rg.toFloat(), ra.toFloat(), r1.toFloat(),
            gr.toFloat(), gb.toFloat(), gg.toFloat(), ga.toFloat(), g1.toFloat(),
            br.toFloat(), bb.toFloat(), bg.toFloat(), ba.toFloat(), b1.toFloat(),
            ar.toFloat(), ab.toFloat(), ag.toFloat(), aa.toFloat(), a1.toFloat()
        )

        //fun brightness(value: Double): ColorMatrix = TODO()
        //fun contrast(value: Double): ColorMatrix = TODO()
        //fun saturation(value: Double): ColorMatrix = TODO()
        //fun tint(color: RGBA, amount: Double = 1.0): ColorMatrix = TODO()
        //fun rotateHue(angle: Angle): ColorMatrix {
        //    val cos = angle.cosine
        //    val sin = angle.sine
        //    return ColorMatrix(
        //        ((LUMA_R + (cos * (1 - LUMA_R))) + (sin * -(LUMA_R))), ((LUMA_G + (cos * -(LUMA_G))) + (sin * -(LUMA_G))), ((LUMA_B + (cos * -(LUMA_B))) + (sin * (1 - LUMA_B))), 0, 0,
        //        ((LUMA_R + (cos * -(LUMA_R))) + (sin * 0.143f)), ((LUMA_G + (cos * (1 - LUMA_G))) + (sin * 0.14f)), ((LUMA_B + (cos * -(LUMA_B))) + (sin * -0.283f)), 0, 0,
        //        ((LUMA_R + (cos * -(LUMA_R))) + (sin * -((1 - LUMA_R)))), ((LUMA_G + (cos * -(LUMA_G))) + (sin * LUMA_G)), ((LUMA_B + (cos * (1 - LUMA_B))) + (sin * LUMA_B)), 0, 0,
        //        0, 0, 0, 1, 0
        //    )
        //}
        //private val LUMA_R = 0.212671f
        //private val LUMA_G = 0.71516f
        //private val LUMA_B = 0.072169f
        //private val LUMA_R2 = 0.3086f
        //private val LUMA_G2 = 0.6094f
        //private val LUMA_B2 = 0.0820f

        fun concat(v0: ColorMatrix, v1: ColorMatrix): ColorMatrix = ColorMatrix(
            (v0.rr * v1.rr), (v0.rg * v1.rg), (v0.rb * v1.rb), (v0.ra * v1.ra), (v0.r1 + v1.r1),
            (v0.gr * v1.gr), (v0.gg * v1.gg), (v0.gb * v1.gb), (v0.ga * v1.ga), (v0.g1 + v1.g1),
            (v0.br * v1.br), (v0.bg * v1.bg), (v0.bb * v1.bb), (v0.ba * v1.ba), (v0.b1 + v1.b1),
            (v0.ar * v1.ar), (v0.ag * v1.ag), (v0.ab * v1.ab), (v0.aa * v1.aa), (v0.a1 + v1.a1)
        )
    }

    operator fun plus(that: ColorMatrix): ColorMatrix = concat(this, that)

    inline fun copyR(rr: Number, rb: Number, rg: Number, ra: Number, r1: Number) = ColorMatrix(
        rr, rb, rg, ra, r1,
        gr, gb, gg, ga, g1,
        br, bb, bg, ba, b1,
        ar, ab, ag, aa, a1
    )

    inline fun copyG(gr: Number, gb: Number, gg: Number, ga: Number, g1: Number) = ColorMatrix(
        rr, rb, rg, ra, r1,
        gr, gb, gg, ga, g1,
        br, bb, bg, ba, b1,
        ar, ab, ag, aa, a1
    )

    inline fun copyB(br: Number, bb: Number, bg: Number, ba: Number, b1: Number) = ColorMatrix(
        rr, rb, rg, ra, r1,
        gr, gb, gg, ga, g1,
        br, bb, bg, ba, b1,
        ar, ab, ag, aa, a1
    )

    inline fun copyA(ar: Number, ab: Number, ag: Number, aa: Number, a1: Number) = ColorMatrix(
        rr, rb, rg, ra, r1,
        gr, gb, gg, ga, g1,
        br, bb, bg, ba, b1,
        ar, ab, ag, aa, a1
    )

    //fun setToBrightness(value: Double): ColorMatrix = this.apply { TODO() }
    //fun setToContrast(value: Double): ColorMatrix = this.apply { TODO() }
    //fun setToHue(angle: Double): ColorMatrix = this.apply { TODO() }
    //fun setToSaturation(value: Double): ColorMatrix = this.apply { TODO() }

    fun applyR(r: Float, g: Float, b: Float, a: Float): Float = (rr * r) + (rg * g) + (rb * b) + (ra * a) + r1
    fun applyG(r: Float, g: Float, b: Float, a: Float): Float = (gr * r) + (gg * g) + (gb * b) + (ga * a) + g1
    fun applyB(r: Float, g: Float, b: Float, a: Float): Float = (br * r) + (bg * g) + (bb * b) + (ba * a) + b1
    fun applyA(r: Float, g: Float, b: Float, a: Float): Float = (ar * r) + (ag * g) + (ab * b) + (aa * a) + a1

    fun apply(dst: RGBAf, src: RGBAf = dst) {
        val r = src.r
        val g = src.g
        val b = src.b
        val a = src.a
        dst.setTo(
            applyR(r, g, b, a),
            applyG(r, g, b, a),
            applyB(r, g, b, a),
            applyA(r, g, b, a)
        )
    }

    // @TODO: Do this using SIMD
    fun applyInline(array: RgbaArray, pos: Int = 0, count: Int = array.size) {
        for (n in pos until pos + count) {
            array[n] = transform(array[n])
        }
    }

    fun transform(src: RGBA): RGBA {
        val r = src.rf
        val g = src.gf
        val b = src.bf
        val a = src.af
        return RGBA.float(
            applyR(r, g, b, a),
            applyG(r, g, b, a),
            applyB(r, g, b, a),
            applyA(r, g, b, a)
        )
    }
}

fun RGBA.transform(matrix: ColorMatrix) = matrix.transform(this)
