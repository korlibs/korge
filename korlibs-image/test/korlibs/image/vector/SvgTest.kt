package korlibs.image.vector

import korlibs.image.vector.format.*
import korlibs.io.async.suspendTest
import kotlin.test.Test
import kotlin.test.assertEquals

class SvgTest {
    @Test
    fun name() = suspendTest {
        val logo = SVG(SAMPLE_LOGO)
        //val img = logo.render().toBmp32()
        val img = logo.renderNoNative()

        //img.writeTo(localVfs("c:/temp/logo.png"), formats = PNG)
    }

    @Test
    fun testTokenizePath() {
        val tokens = SvgPath.tokenizePath("m -100.123,100.456 c -1.1234,3.3e-4 -1.111,0.123").map { it.anyValue }
        assertEquals(listOf('m', -100.123, 100.456, 'c', -1.1234, 3.3E-4, -1.111, 0.123), tokens)
    }

    @Test
    fun testTokenizePath2() {
        val tokens = SvgPath.tokenizePath("M117.35,110.75v-34.42c.34-.45.68-.89,1.05-1.31v37c-.4-.38-.71-.83-1.05-1.27Z").map { it.anyValue }
        assertEquals(listOf('M', 117.35, 110.75, 'v', -34.42, 'c', 0.34, -0.45, 0.68, -0.89, 1.05, -1.31, 'v', 37.0, 'c', -0.4, -0.38, -0.71, -0.83, -1.05, -1.27, 'Z'), tokens)
    }

    @Test
    fun testTokenizePath3() {
        val tokens = SvgPath.tokenizePath("M.1-.1.1").map { it.anyValue }
        assertEquals(arrayListOf('M', 0.1, -0.1, 0.1), tokens)
    }

    @Test
    fun testTokenizePath4() {
        val tokens = SvgPath.tokenizePath("m-220 0h440zm220 0-160 200zm0 0 160 200z").map { it.anyValue }
        assertEquals(arrayListOf('m', -220.0, 0.0, 'h', 440.0, 'z', 'm', 220.0, 0.0, -160.0, 200.0, 'z', 'm', 0.0, 0.0, 160.0, 200.0, 'z'), tokens)
    }

    @Test
    fun testRelativeWithCloseCommands() {
        val vectorPath = SvgPath.parse("m-220 0h440zm220 0-160 200zm0 0 160 200z")
        assertEquals("M0,0 M-220,0 L220,0 Z M0,0 L-160,200 Z M0,0 L160,200 Z", vectorPath.toSvgString())
    }

    @Test
    fun testShapeCoords() {
        val svg = SVG("""
            <?xml version="1.0" encoding="UTF-8" standalone="no"?>
            <svg id="svg2" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 900 900" version="1.1">
                <g transform="matrix(2,0,0,2,0,0)">
                    <rect x="0" y="0" width="100" height="100" fill="#fff" />
                </g>
            </svg>
        """.trimIndent())
        val svgShape = svg.toShape().toSvgInstance().toShape()
        val svgShape2 = svgShape.toSvgInstance().toShape()
        assertEquals(svgShape, svgShape2)
    }

	val SAMPLE_LOGO = """
<svg id="7fe010bd-4468-4253-af77-c0b7be09145b" data-name="Capa 1" xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink" width="462" height="462" viewBox="0 0 462 462">
  <defs>
    <linearGradient id="943f5ba0-08c6-4338-98b9-f347b4e8a88b" data-name="Degradado sin nombre 6" x1="16.79" y1="91.98" x2="231.92" y2="91.98" gradientTransform="translate(-10.42 21.95)" gradientUnits="userSpaceOnUse">
      <stop offset="0" stop-color="#0071bc"/>
      <stop offset="1" stop-color="#1b1464"/>
    </linearGradient>
    <linearGradient id="1dc29aca-506d-4532-94fb-b659ac65b67f" data-name="Degradado sin nombre 13" x1="19.32" y1="418.94" x2="234.45" y2="418.94" gradientTransform="translate(-12.95 -70.87)" gradientUnits="userSpaceOnUse">
      <stop offset="0" stop-color="#f7931e"/>
      <stop offset="1" stop-color="#f15a24"/>
    </linearGradient>
    <linearGradient id="04b2fd55-c3cf-4419-87c5-84e3e25bbd9b" x1="240.5" y1="113.93" x2="455.63" y2="113.93" gradientTransform="matrix(1, 0, 0, 1, 0, 0)" xlink:href="#1dc29aca-506d-4532-94fb-b659ac65b67f"/>
    <linearGradient id="cd92637f-6abd-45fb-92c8-f0cf8f946c8d" x1="240.5" y1="348.07" x2="455.63" y2="348.07" gradientTransform="matrix(1, 0, 0, 1, 0, 0)" xlink:href="#943f5ba0-08c6-4338-98b9-f347b4e8a88b"/>
  </defs>
  <title>icon</title>
  <g>
    <polygon points="107.17 221.5 6.37 120.91 121.16 6.37 221.5 107.03 221.5 221.5 107.17 221.5" fill="url(#943f5ba0-08c6-4338-98b9-f347b4e8a88b)"/>
    <path d="M145.15,36.73,241,132.89V241H133L36.74,144.91,145.15,36.73m0-12.73L24,144.91,129.07,249.76V250H250V129.17L145.16,24Z" transform="translate(-24 -24)" fill="#1b1464"/>
  </g>
  <g>
    <polygon points="6.37 340.85 107.03 240.5 221.5 240.5 221.5 354.83 120.91 455.63 6.37 340.85" fill="url(#1dc29aca-506d-4532-94fb-b659ac65b67f)"/>
    <path d="M241,269V377l-96.09,96.29L36.73,364.85,132.89,269H241m9-9H129.17L24,364.84,144.91,486,249.76,380.93H250V260Z" transform="translate(-24 -24)" fill="#f15a24"/>
  </g>
  <g>
    <polygon points="240.5 107.17 341.09 6.37 455.63 121.15 354.73 221.31 240.5 221.49 240.5 107.17" fill="url(#04b2fd55-c3cf-4419-87c5-84e3e25bbd9b)"/>
    <path d="M365.09,36.74,473.26,145.13l-96.39,95.69L269,241V133l96.09-96.29m0-12.74L260.24,129.07H260V250l120.58-.19L486,145.16,365.09,24Z" transform="translate(-24 -24)" fill="#f15a24"/>
  </g>
  <g>
    <polygon points="240.5 354.97 240.5 240.5 354.83 240.5 455.63 341.09 340.85 455.63 240.5 354.97" fill="url(#cd92637f-6abd-45fb-92c8-f0cf8f946c8d)"/>
    <path d="M377,269l96.29,96.09L364.85,473.27,269,377.11V269H377m4-9H260V380.83L364.84,486,486,365.09,380.93,260.24V260Z" transform="translate(-24 -24)" fill="#1b1464"/>
  </g>
</svg>
"""
}
