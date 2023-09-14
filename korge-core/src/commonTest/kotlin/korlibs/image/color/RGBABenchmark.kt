package korlibs.image.color

import kotlin.jvm.JvmStatic

object RGBABenchmark {
    @JvmStatic
    fun main(args: Array<String>) {
        //printBenchmark("benchmarkPremultiplyAccurate") { var m = 0; for (n in 0 until 8192 * 8192) m += RGBA.premultiplyAccurate(colors[n and 1]) }
        //printBenchmark("benchmarkPremultiplyFast") { var m = 0; for (n in 0 until 8192 * 8192) m += RGBA.premultiplyFast(colors[n and 1]) }
        //printBenchmark("benchmarkDepremultiplyAccurate") { var m = 0; for (n in 0 until 8192 * 8192) m += RGBA.depremultiplyAccurate(colors[n and 1]) }
        //printBenchmark("benchmarkDepremultiplyFast") { var m = 0; for (n in 0 until 8192 * 8192) m += RGBA.depremultiplyFast(colors[n and 1]) }
        //printBenchmark("benchmarkDepremultiplyFastest") { var m = 0; for (n in 0 until 8192 * 8192) m += RGBA.depremultiplyFastest(colors[n and 1]) }
        //printBenchmark("benchmark3") { var m = 0; val c = RGBA.pack(0xFF, 0xFF, 0xFF, 0x7F);for (n in 0 until 10000000000) m += RGBA.premultiplyFast2(c) }
    }
}
