package com.soywiz.korma.geom.ds

import com.soywiz.korma.geom.*
import kotlin.test.*

class BVHTest {
    @Test
    fun test() {
        val tree = BVH2D<String>()
        //for (n in 0 until 1_000_000) tree.insert(Rectangle(n * 5, 5, 10, 10), "$n")
        //for (n in 0 until 10_000_000) tree.insert(Rectangle(n * 5, 5, 10, 10), "$n")
        tree.insertOrUpdate(Rectangle(20, 15, 20, 20), "1")
        tree.insertOrUpdate(Rectangle(50, 50, 20, 20), "2")
        //tree.remove(Rectangle(20, 15, 20, 20), "1")

        println(tree.intersect(Ray(Point(25, 100), Vector2D(0, -1))))
        //tree.debug()
        println(tree.search(Rectangle(0.0, 0.0, 60.0, 60.0)))
    }
}
