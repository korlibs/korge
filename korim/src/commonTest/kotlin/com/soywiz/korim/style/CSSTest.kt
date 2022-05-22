package com.soywiz.korim.style

import com.soywiz.korio.util.reader
import com.soywiz.korma.geom.Matrix
import kotlin.test.Test
import kotlin.test.assertEquals

class CSSTest {
    @Test
    fun test() {
        val css = CSS.parseCSS("""
            @keyframes a { 
                0% { color: red; opacity: 0; transform: translate(65.22px, 113.274326px) } 
                100% { /* hello */ color: green /**/; opacity: 1; transform: translate(0, 0) } 
            }            
        """.trimIndent())
        val animation = css.animationsById["a"]!!
        println(animation.getAt(0.0))
        println(animation.getAt(0.0).getColor("color"))
        println(animation.getAt(0.1).getColor("color"))
        println(animation.getAt(0.5).getColor("color"))
        println(animation.getAt(1.0).getColor("color"))
        println(animation.getAt(0.5).getRatio("opacity"))
        println(animation.getAt(0.5).getTransform("transform"))
        println(animation.getAt(0.5).k0["transform"]?.transform)
        println(animation.getAt(0.5).k1["transform"]?.transform)
        //println(css.animationsById.values.first().fullKeyFrames)
    }

    @Test
    fun test2() {
        assertEquals(Matrix(a=-1.0, b=0.0, c=0.0, d=-1.0, tx=132.47, ty=97.81), CSS.parseTransform("matrix(-1 0 0-1 132.47 97.81)"))
    }
}
