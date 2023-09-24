package korlibs.math.geom.ds

import korlibs.datastructure.ds.*
import korlibs.math.geom.*
import korlibs.platform.*
import kotlin.test.*

class BVHTest {
    @Test
    fun test2D() {
        val tree = BVH2D<String>()
        //for (n in 0 until 1_000_000) tree.insert(Rectangle(n * 5, 5, 10, 10), "$n")
        //for (n in 0 until 10_000_000) tree.insert(Rectangle(n * 5, 5, 10, 10), "$n")
        tree.insertOrUpdate(Rectangle(20, 15, 20, 20), "1")
        tree.insertOrUpdate(Rectangle(50, 50, 20, 20), "2")
        //tree.remove(Rectangle(20, 15, 20, 20), "1")

        val intersection = tree.intersect(Ray(Point(25, 100), Vector2D(0, -1)))
        val rectSearch = tree.search(Rectangle(0, 0, 60, 60))

        //assertEquals(1, intersection.size)
        //assertEquals(2, rectSearch.size)

        assertEquals(listOf("1"), intersection.map { it.obj.value }.sortedBy { it })
        assertEqualsFloat(listOf(65.0), intersection.map { it.intersect }.sorted())
        assertEquals(listOf("1", "2"), rectSearch.map { it.value }.sortedBy { it })

        //tree.debug()
    }

    @Test
    fun test3D() {
        val tree = BVH3D<String>()
        //for (n in 0 until 1_000_000) tree.insert(Rectangle(n * 5, 5, 10, 10), "$n")
        //for (n in 0 until 10_000_000) tree.insert(Rectangle(n * 5, 5, 10, 10), "$n")
        tree.insertOrUpdate(AABB3D(Vector3F(-1f, -1f, -1f), Vector3F(+1f, +1f, +1f)), "1")

        val results = tree.intersect(Ray3F(Vector3F.DOWN * 4f, Vector3F.UP.copy(x = -.125f)))
        assertEquals(1, results.size)
        val result = results.first()
        assertEquals(Vector3F(-0.375, -1.0, 0.0), result.point.toVector3())
        assertEquals(Vector3F.DOWN, result.normal.toVector3())
    }

    @Test
    fun testArrayOutOfBoundsBug() {
        if (Platform.isWasm) return // @TODO: This should be only for WASM

        val bvh = BVH2D<Int>()
        //bvh.insertOrUpdate()
        //println(BVHIntervals(223.99997,16.0, 191.99998,16.0))
        fun Rectangle(x: Number, y: Number, width: Number, height: Number): Rectangle = Rectangle.invoke(x.toFloat(), y.toFloat(), width.toFloat(), height.toFloat())
        bvh.insertOrUpdate(Rectangle(x=223.99997, y=191.99998, width=16, height=16), 1)
        bvh.insertOrUpdate(Rectangle(x=63, y=143.99998, width=16, height=16), 2)
        bvh.insertOrUpdate(Rectangle(x=207.99998, y=223.99997, width=16, height=16), 3)
        bvh.insertOrUpdate(Rectangle(x=87, y=159.99998, width=16, height=16), 4)
        bvh.insertOrUpdate(Rectangle(x=175.99998, y=191.99998, width=16, height=16), 5)
        bvh.insertOrUpdate(Rectangle(x=143.99998, y=223.99997, width=16, height=16), 6)
        bvh.insertOrUpdate(Rectangle(x=271.99997, y=175.99998, width=16, height=16), 7)
        bvh.insertOrUpdate(Rectangle(x=71, y=159.99998, width=16, height=16), 4)
        val result = bvh.search(bvh.envelope())
        assertEquals("1, 2, 3, 4, 5, 6, 7", result.map { it.value }.sortedBy { it }.joinToString(", "))
    }

    @Test
    fun testIntervals() {
        assertEquals(BVHIntervals(1f, 2f), BVHIntervals(1f, 2f))
        assertEquals(BVHIntervals(1f, 2f, 3f, 4f), BVHIntervals(1f, 2f, 3f, 4f))
        assertNotEquals(BVHIntervals(1f, 2f, 3f, 4f), BVHIntervals(1f, 2f, 3f, 1f))
    }

    @Test
    fun testVectors() {
        assertEquals(BVHVector(0f, 1f), BVHVector(0f, 1f))
        assertEquals(BVHVector(0f, 1f, 2f), BVHVector(0f, 1f, 2f))
        assertNotEquals(BVHVector(0f, 1f), BVHVector(0f, 0f))
        assertNotEquals(BVHVector(0f, 1f, 2f), BVHVector(0f, 1f, 3f))
    }

    @Test
    fun test1D() {
        val demo = BVH1D<String>()
        demo.insertOrUpdate(Segment1D(-10f, -5f), "hello")
        demo.insertOrUpdate(Segment1D(5f, 10f), "world")

        assertEquals(2, demo.intersect(Ray1D(-15f, +1f)).size)
        assertEquals(listOf("hello"), demo.intersect(Ray1D(0f, -1f)).map { it.obj.value })
        assertEquals(listOf("world"), demo.intersect(Ray1D(0f, +1f)).map { it.obj.value })

        assertEquals(2, demo.search(Segment1D(-7f, +7f)).size)
        assertEquals(2, demo.search(Segment1D(-11f, +11f)).size)
        assertEquals(1, demo.search(Segment1D(-11f, 0f)).size)
        assertEquals(1, demo.search(Segment1D(0f, +11f)).size)
    }

    @Test
    fun testUpRay() {
        val demo = BVH2D<String>()
        demo.insertOrUpdate(Rectangle(0f, 0f, 20f, 10f), "hello")

        assertEquals(listOf(90.0), demo.intersect(Ray(Point(10f, 100f), Vector2D(0f, -2f))).map { it.intersect })
        assertEquals(listOf(90.0), demo.intersect(Ray(Point(10f, 100f), Vector2D(0f, -1f))).map { it.intersect })
        assertEquals(listOf(100.0), demo.intersect(Ray(Point(10f, -100f), Vector2D(0f, +1f))).map { it.intersect })
        assertEquals(listOf(), demo.intersect(Ray(Point(10f, -100f), Vector2D(0f, -1f))).map { it.intersect })

        assertEquals(listOf(80.0), demo.intersect(Ray(Point(100f, 10f), Vector2D(-1f, 0f))).map { it.intersect })
        assertEquals(listOf(100.0), demo.intersect(Ray(Point(-100f, 10f), Vector2D(+1f, 0f))).map { it.intersect })
        assertEquals(listOf(), demo.intersect(Ray(Point(-100f, 10f), Vector2D(-1f, 0f))).map { it.intersect })
    }

    @Test
    fun testIntersectionPoint() {
        val demo = BVH2D<String>()
        demo.insertOrUpdate(Rectangle(0f, 0f, 10f, 10f), "hello")

        assertEquals(listOf(BVHVector(5f, 10f), BVHVector(0f, 1f)), demo.intersect(Ray(Point(5f, 100f), Vector2D(0f, -2f))).flatMap { listOf(it.point, it.normal) })
        assertEquals(listOf(BVHVector(10f, 10f), BVHVector(1f, 1f)), demo.intersect(Ray(Point(10f, 100f), Vector2D(0f, -2f))).flatMap { listOf(it.point, it.normal) })
        assertEquals(listOf(BVHVector(5f, 5f), BVHVector(0f, 0f)), demo.intersect(Ray(Point(5f, 5f), Vector2D(0f, -2f))).flatMap { listOf(it.point, it.normal) })
    }
}
