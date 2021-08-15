package com.soywiz.korim.bitmap.trace

import com.soywiz.korim.bitmap.*
import com.soywiz.korim.color.*
import com.soywiz.korma.geom.*
import com.soywiz.korma.geom.vector.*
import kotlin.test.*

class BitmapTracerTest {
    @Test
    fun testSmokeTrace() {
        val bmp = Bitmap32(300, 200).context2d {
            fill(Colors.WHITE) {
                rect(Rectangle.fromBounds(2, 2, 18, 18))
                rectHole(Rectangle.fromBounds(6, 6, 9, 12))
                rectHole(Rectangle.fromBounds(10, 5, 15, 12))
                rect(Rectangle.fromBounds(50, 2, 68, 18))
                circle(100, 100, 40)
                circle(100, 100, 20)
                roundRect(200, 50, 50, 50, 5, 5)
                //circle(130, 100, 20) // @TODO: Bug
            }
        }
        assertEquals(
            "M18,2 L18,5 L2,5 L2,2 Z M10,5 L10,6 L2,6 L2,5 Z M6,6 L6,12 L2,12 L2,6 Z M10,6 L10,12 L9,12 L9,6 Z M18,5 L18,12 L15,12 L15,5 Z M18,12 L18,17 L2,17 L2,12 Z M68,2 L68,17 L50,17 L50,2 Z M106,60 L114,62 L122,66 L124,68 L126,69 L132,75 L132,76 L134,78 L134,79 L135,80 L65,80 L66,79 L66,78 L68,76 L68,75 L75,68 L77,67 L78,66 L86,62 L89,61 L94,60 Z M247,50 L249,51 L249,52 L250,53 L250,97 L248,99 L202,99 L200,97 L200,52 L201,51 L203,50 Z M96,80 L90,82 L89,83 L87,84 L84,87 L84,88 L82,90 L82,91 L81,92 L81,94 L80,95 L80,104 L81,105 L81,107 L82,108 L82,109 L84,111 L84,112 L88,116 L92,118 L95,119 L95,120 L65,120 L65,119 L64,118 L64,117 L63,116 L63,114 L62,113 L62,111 L61,110 L61,107 L60,106 L60,93 L61,92 L61,89 L62,88 L62,86 L63,85 L63,84 L64,83 L64,82 L65,81 L65,80 Z M135,80 L135,81 L136,82 L136,83 L137,84 L137,85 L138,86 L138,88 L139,89 L139,93 L140,94 L140,105 L139,106 L139,110 L138,111 L138,113 L137,114 L137,115 L136,116 L136,118 L135,119 L135,120 L105,120 L105,119 L108,118 L110,117 L111,116 L113,115 L116,112 L116,111 L118,109 L118,108 L119,107 L119,104 L120,103 L120,96 L119,95 L119,93 L118,92 L118,91 L117,90 L117,89 L115,87 L115,86 L114,85 L112,84 L111,83 L107,81 L103,80 Z M134,120 L134,121 L132,123 L132,124 L125,131 L123,132 L122,133 L120,134 L119,135 L117,136 L111,138 L107,139 L93,139 L89,138 L83,136 L79,134 L77,132 L75,131 L68,124 L68,123 L66,121 L66,120 Z",
            bmp.trace().toSvgString()
        )
    }
}
