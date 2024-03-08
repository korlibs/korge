package korlibs.image.bitmap.sdf

import korlibs.datastructure.*
import korlibs.memory.*
import korlibs.image.bitmap.*
import korlibs.image.color.*
import korlibs.math.*

// Based on Tiny-SDF: https://github.com/mapbox/tiny-sdf/
object NewSDF {
    private const val INF = 1e10f

    data class Result(val bmp: Bitmap32, val buffer: Int, val radius: Int, val cutoff: Double)

    fun process(
        bmp: Bitmap32,
        maxSide: Int = kotlin.math.max(bmp.width, bmp.height),
        buffer: Int = maxSide divCeil 8,
        radius: Int = maxSide divCeil 3,
        cutoff: Double = 0.5,
    ): Result {
        //val len = glyphWidth * glyphHeight

        val size = maxSide + buffer * 4
        val out = Bitmap32(bmp.width + buffer * 2, bmp.height + buffer * 2)
        val gridOuter = FloatArray2(out.width, out.height, INF)
        val gridInner = FloatArray2(out.width, out.height, 0f)
        val f = FloatArray(size)
        val z = FloatArray(size + 1)
        val v = UShortArrayInt(size)

        for (y in 0 until bmp.height) {
            for (x in 0 until bmp.width) {
                val a = bmp[x, y].af // alpha value

                val x0 = x + buffer
                val y0 = y + buffer
                when (a) {
                    0f -> { // empty pixels
                        gridOuter[x0, y0] = INF
                        gridInner[x0, y0] = 0f
                    }
                    1f -> { // fully drawn pixels
                        gridOuter[x0, y0] = 0f
                        gridInner[x0, y0] = INF
                    }
                    else -> { // aliased pixels
                        val d = 0.5f - a
                        gridOuter[x0, y0] = if (d > 0f) d * d else 0f
                        gridInner[x0, y0] = if (d < 0f) d * d else 0f
                    }
                }
            }
        }

        edt(gridOuter.data, 0, 0, out.width, out.height, gridOuter.width, f, v, z)
        edt(gridInner.data, buffer, buffer, bmp.width, bmp.height, gridInner.width, f, v, z)

        for (y in 0 until out.height) {
            for (x in 0 until out.width) {
                val gout = gridOuter[x, y]
                val gin = gridInner[x, y]
                val d = kotlin.math.sqrt(gout) - kotlin.math.sqrt(gin)
                val a = (255 - 255 * (d / radius + cutoff)).toIntRound()
                out[x, y] = RGBA(255, 255, 255, a.clamp(0, 255))
            }
        }
        return Result(out, buffer, radius, cutoff)
    }

    // 2D Euclidean squared distance transform by Felzenszwalb & Huttenlocher https://cs.brown.edu/~pff/papers/dt-final.pdf
    fun edt(data: FloatArray, x0: Int, y0: Int, width: Int, height: Int, gridSize: Int, f: FloatArray, v: UShortArrayInt, z: FloatArray) {
        for (x in x0 until x0 + width) edt1d(data, y0 * gridSize + x, gridSize, height, f, v, z)
        for (y in y0 until y0 + height) edt1d(data, y * gridSize + x0, 1, width, f, v, z)
    }

    // 1D squared distance transform
    fun edt1d(grid: FloatArray, offset: Int, stride: Int, length: Int, f: FloatArray, v: UShortArrayInt, z: FloatArray) {
        v[0] = 0
        z[0] = -INF
        z[1] = INF
        f[0] = grid[offset]

        var k = 0
        var s = 0f
        for (q in 1 until length) {
            f[q] = grid[offset + q * stride]
            val q2 = q * q
            do {
                val r = v[k]
                s = (f[q] - f[r] + q2 - r * r) / (q - r) / 2f
            } while (s <= z[k] && --k > -1)

            k++
            v[k] = q
            z[k] = s
            z[k + 1] = INF
        }

        k = 0
        for (q in 0 until length) {
            while (z[k + 1] < q) k++
            val r = v[k]
            val qr = q - r
            grid[offset + q * stride] = f[r] + qr * qr
        }
    }

}
