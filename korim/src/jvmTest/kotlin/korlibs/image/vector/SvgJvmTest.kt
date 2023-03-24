package korlibs.image.vector

import korlibs.image.bitmap.*
import korlibs.image.format.*
import korlibs.image.vector.format.*
import korlibs.image.vector.format.SVG
import korlibs.io.async.*
import korlibs.io.file.std.*
import kotlin.test.*

class SvgJvmTest {
    @Test
    fun test() = suspendTest {
        val svg = SVG(resourcesVfs["tiger.svg"].readString())
        val bmp = svg.render()
        //bmp.writeTo("/tmp/demo.png".uniVfs, PNG)
        //svg.renderToImage(512, 512).showImageAndWait()
        //svg.render().showImageAndWait()
        //svg.render(native = false).showImageAndWait()
    }

    @Test
    fun test2() = suspendTest {
        val svg = resourcesVfs["svglogo.svg"].readSVG()
        val bmp = svg.render()
        //bmp.writeTo("/tmp/demo.png".uniVfs, PNG)
    }

    @Test
    fun testTokenizePath() {
        val tokens = SvgPath.tokenizePath("m-122.3,84.285s0.1,1.894-0.73,1.875c-0.82-0.019-17.27-48.094-37.8-45.851,0,0,17.78-7.353,38.53,43.976z")
        //println(tokens)
    }

    @Test
    fun testSvg2() = suspendTest {
        val svgString = """
            <svg version="1.1" xmlns="http://www.w3.org/2000/svg" width="230" height="1024" viewBox="0 0 230 1024">
            <title></title>
            <g id="icomoon-ignore">
            </g>
            <path d="M161.118 242.688q0 18.432-13.312 31.232t-30.72 12.8q-19.456 0-33.792-12.288t-14.336-30.72q-1.024-20.48 11.776-34.816t31.232-14.336q20.48 0 34.816 13.824t14.336 34.304zM76.126 829.44v-450.56h69.632v450.56h-69.632z"></path>
            </svg>
        """.trimIndent()

        val image = SVG(svgString).render()
        //image.showImageAndWait()
    }

    // https://developer.mozilla.org/en-US/docs/Web/SVG/Tutorial/Paths
    @Test
    fun testSvgArcs() = suspendTest {
        val image1 = SVG("""
            <svg xmlns="http://www.w3.org/2000/svg" width="320" height="320">
              <path d="M 10 315
                       L 110 215
                       A 36 60 0 0 1 150.71 170.29
                       L 172.55 152.45
                       A 30 50 -45 0 1 215.1 109.9
                       L 315 10" stroke="black" fill="green" stroke-width="2" fill-opacity="0.5"/>
              <circle cx="150.71" cy="170.29" r="2" fill="red"/>
              <circle cx="110" cy="215" r="2" fill="red"/>
              <ellipse cx="144.931" cy="229.512" rx="36" ry="60" fill="transparent" stroke="blue"/>
              <ellipse cx="115.779" cy="155.778" rx="36" ry="60" fill="transparent" stroke="blue"/>
            </svg>
        """.trimIndent()).render()
        val image2 = SVG("""
            <svg width="320" height="320" xmlns="http://www.w3.org/2000/svg">
              <path d="M 10 315
                       L 110 215
                       A 30 50 0 0 1 162.55 162.45
                       L 172.55 152.45
                       A 30 50 -45 0 1 215.1 109.9
                       L 315 10" stroke="black" fill="green" stroke-width="2" fill-opacity="0.5"/>
            </svg>
        """.trimIndent()).render()
        val image3 = SVG("""
            <svg width="325" height="325" xmlns="http://www.w3.org/2000/svg">
              <path d="M 80 80
                       A 45 45, 0, 0, 0, 125 125
                       L 125 80 Z" fill="green"/>
              <path d="M 230 80
                       A 45 45, 0, 1, 0, 275 125
                       L 275 80 Z" fill="red"/>
              <path d="M 80 230
                       A 45 45, 0, 0, 1, 125 275
                       L 125 230 Z" fill="purple"/>
              <path d="M 230 230
                       A 45 45, 0, 1, 1, 275 275
                       L 275 230 Z" fill="blue"/>
            </svg>
        """.trimIndent()).render()

        //image1.showImageAndWait()
        //image2.showImageAndWait()
        //image3.showImageAndWait()
    }
}
