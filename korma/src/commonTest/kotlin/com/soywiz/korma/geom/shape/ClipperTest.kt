package com.soywiz.korma.geom.shape

import com.soywiz.korma.geom.*
import com.soywiz.korma.geom.shape.ops.internal.*
import com.soywiz.korma.geom.vector.*
import kotlin.test.*

class ClipperTest {
    @Test
    fun name() {
        val clipper = DefaultClipper()
        val path1 = Path(IPoint(0, 0), IPoint(10, 0), IPoint(10, 10), IPoint(0, 10))
        val path2 = Path(IPoint(5 + 0, 0), IPoint(5 + 10, 0), IPoint(5 + 10, 10), IPoint(5 + 0, 10))
        val paths = Paths()

        clipper.addPath(path1, Clipper.PolyType.CLIP, true)
        clipper.addPath(path2, Clipper.PolyType.SUBJECT, true)
        clipper.execute(Clipper.ClipType.INTERSECTION, paths)

        assertEquals("[[(10, 10), (5, 10), (5, 0), (10, 0)]]", paths.toString())
        assertEquals("Rectangle(x=5, y=0, width=5, height=10)", paths.bounds.toString())
    }

    //@Test
    //fun name2() {
    //    val clipper = DefaultClipper()
    //    val path = buildPath {
    //        rect(0, 0, 100, 100)
    //        rect(20, 20, 120, 60)
    //    }
    //    val out = Paths()
    //    clipper.addPaths(path.toClipperPaths(), Clipper.PolyType.SUBJECT, true)
    //    clipper.execute(Clipper.ClipType.UNION, out)
    //    assertEquals("...", out.toVectorPath().toString())
    //}
}
