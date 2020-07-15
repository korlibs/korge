package com.soywiz.korma.geom.triangle

import com.soywiz.korma.geom.*
import com.soywiz.korma.triangle.internal.*
import com.soywiz.korma.triangle.pathfind.*
import kotlin.math.*
import kotlin.test.*

class EdgeTest {
    private var p1: IPoint = IPoint(0, 0)
    private var p2: IPoint = IPoint(-1, 0)
    private val edgeContext = EdgeContext()
    private var e1: Edge = edgeContext.createEdge(p1, p2)

    @Test
    fun testInstantiated() {
        @Suppress("USELESS_IS_CHECK")
        assertTrue(e1 is Edge)
    }

    @Test
    fun testValues() {
        // Order of points was reversed.
        assertEquals(e1.p, p2)
        assertEquals(e1.q, p1)
    }

    @Test
    fun testInEdgeList() {
        // q contains the edge_list
        assertTrue(edgeContext.getPointEdgeList(e1.q).indexOf(e1) != -1)
    }
}

class FunnelTest {
    @Test
    fun testStringPull() {
        val channel = SpatialMeshFind.Channel.Funnel()

        channel.push(IPoint(1, 0))
        channel.push(IPoint(0, 4), IPoint(4, 3))
        channel.push(IPoint(4, 7), IPoint(4, 3))
        channel.push(IPoint(16, 0), IPoint(10, 1))
        channel.push(IPoint(16, 0), IPoint(9, -5))
        channel.push(IPoint(12, -11))
        channel.stringPull()

        assertEquals("[(1, 0), (4, 3), (10, 1), (12, -11)]", channel.path.toString())
    }
}

class OrientationTest {
    @Test
    fun testOrientationCollinear() {
        assertEquals(Orientation.orient2d(IPoint(0, 0), IPoint(1, 1), IPoint(2, 2)), Orientation.COLLINEAR)
        assertFalse(
            Orientation.orient2d(
                IPoint(-1, 0),
                IPoint(0, 0),
                IPoint(+1, 0)
            ) != Orientation.COLLINEAR
        )
    }

    @Test
    fun testOrientationCW() {
        assertEquals(Orientation.orient2d(IPoint(0, 0), IPoint(1, 1), IPoint(2, 0)), Orientation.CLOCK_WISE)
    }

    @Test
    fun testOrientationCCW() {
        assertEquals(Orientation.orient2d(IPoint(0, 0), IPoint(-1, 1), IPoint(-2, 0)), Orientation.COUNTER_CLOCK_WISE)
    }
}

//class PathFindTest {
//	protected var vp:VisiblePolygon;
//	protected var spatialMesh:SpatialMesh;
//	protected var pathNodes:ArrayList<SpatialNode>;
//	protected var pathFind:PathFind;
//
//	val triangles get() = vp.triangles
//
//	init {
//		vp = VisiblePolygon();
//		vp.addRectangle(0, 0, 400, 400);
//		vp.addRectangle(200, 100, 80, 80);
//		vp.addRectangle(100, 200, 80, 80);
//
//		//vp.addRectangle(110, 210, 60, 60);
//
//		vp.addRectangle(100, 20, 80, 150);
//		vp.addRectangle(37, 140, 37, 104);
//		var vec = ArrayList<Point2d>();
//		vec.push(Point2d(10, 10));
//		vec.push(Point2d(40, 10));
//		vec.push(Point2d(60, 20));
//		vec.push(Point2d(40, 40));
//		vec.push(Point2d(10, 40));
//		vp.addPolyline(vec);
//		spatialMesh = SpatialMesh.fromTriangles(triangles);
//		pathFind = PathFind(spatialMesh);
//	}
//
//	fun debugDrawPath():Unit {
//		var mc:MovieClip = MovieClip();
//		mc.graphics.clear();
//		vp.drawShape(mc.graphics);
//		for each (var node:SpatialNode in pathNodes) {
//		VisiblePolygon.drawTriangleHighlight(node.triangle, mc.graphics);
//	}
//		/*
//		VisiblePolygon.drawTriangleHighlight(nodeStart.triangle, mc.graphics);
//		VisiblePolygon.drawTriangleHighlight(nodeEnd.triangle, mc.graphics);
//		*/
//		this.context.addChild(mc);
//		context.stage.addEventListener(MouseEvent.CLICK, onClick);
//	}
//
//	private fun onClick(e:MouseEvent):Unit {
//		//trace(TrianglesToSpatialNodesConverter.convert(triangles));
//
//		/*
//		var pointStart:Point2d = Point2d(50, 50);
//		var pointEnd:Point2d = Point2d(340, 200);
//		//var pointEnd:Point2d = Point2d(160, 260);
//		var nodeStart:SpatialNode   = spatialMesh.getNodeFromTriangle(vp.getTriangleAtPoint(pointStart));
//		var nodeEnd:SpatialNode     = spatialMesh.getNodeFromTriangle(vp.getTriangleAtPoint(pointEnd));
//
//		pathNodes = pathFind.find(nodeStart, nodeEnd);
//
//		assertTrue(pathNodes.toString(), "SpatialNode(45, 53),SpatialNode(70, 100),SpatialNode(91, 110),SpatialNode(82, 184),SpatialNode(91, 204),SpatialNode(126, 180),SpatialNode(153, 190),SpatialNode(186, 183),SpatialNode(186, 220),SpatialNode(220, 213),SpatialNode(286, 286)");
//
//		var portals:NewFunnel = PathFindChannel.channelToPortals(pointStart, pointEnd, pathNodes)
//
//		debugDrawPath(); var mc:MovieClip = MovieClip(); Drawing.drawLines(mc, portals.path); addChild(mc);
//
//		assertEquals("Point2d(50, 50),Point2d(100, 170),Point2d(180, 200),Point2d(300, 300)", portals.path);
//		*/
//
//		var pointStart:Point2d = Point2d(50, 50);
//		var pointEnd:Point2d = Point2d(300, 300);
//
//		var nodeStart:SpatialNode   = spatialMesh.getNodeFromTriangle(vp.getTriangleAtPoint(pointStart));
//		var nodeEnd:SpatialNode     = spatialMesh.getNodeFromTriangle(vp.getTriangleAtPoint(pointEnd));
//
//		pathNodes = pathFind.find(nodeStart, nodeEnd);
//
//		var portals:NewFunnel = PathFindChannel.channelToPortals(pointStart, pointEnd, pathNodes)
//
//		debugDrawPath(); var mc:MovieClip = MovieClip(); Drawing.drawLines(mc, portals.path); addChild(mc);
//	}
//
//	fun testDemo():Unit {
//
//		//trace(TrianglesToSpatialNodesConverter.convert(triangles));
//
//		var pointStart:Point2d = Point2d(50, 50);
//		var pointEnd:Point2d = Point2d(300, 300);
//
//		var nodeStart:SpatialNode   = spatialMesh.getNodeFromTriangle(vp.getTriangleAtPoint(pointStart));
//		var nodeEnd:SpatialNode     = spatialMesh.getNodeFromTriangle(vp.getTriangleAtPoint(pointEnd));
//
//		pathNodes = pathFind.find(nodeStart, nodeEnd);
//
//		assertTrue(pathNodes.toString(), "SpatialNode(45, 53),SpatialNode(70, 100),SpatialNode(91, 110),SpatialNode(82, 184),SpatialNode(91, 204),SpatialNode(126, 180),SpatialNode(153, 190),SpatialNode(186, 183),SpatialNode(186, 220),SpatialNode(220, 213),SpatialNode(286, 286)");
//
//		var portals:NewFunnel = PathFindChannel.channelToPortals(pointStart, pointEnd, pathNodes)
//
//		//debugDrawPath(); var mc:MovieClip = MovieClip(); Drawing.drawLines(mc, portals.path); addChild(mc);
//
//		assertEquals("Point2d(50, 50),Point2d(100, 170),Point2d(180, 200),Point2d(300, 300)", portals.path);
//	}
//}

class PointTest {
    companion object {
        private const val P1X: Double = 1.0
        private const val P1Y: Double = 3.0
        private const val P2X: Double = 7.0
        private const val P2Y: Double = 2.0
        private const val P3X: Double = 5.0
        private const val P3Y: Double = 5.0
        private const val SCALAR: Double = 3.0
        private const val DELTA_FLOAT: Double = 0.0001
    }

    private var p1: Point
    private var p2: Point
    private var p3: Point

    init {
        p1 = Point(P1X, P1Y)
        p2 = Point(P2X, P2Y)
        p3 = Point(P3X, P3Y)
    }

    @Test
    fun testInstantiated() {
        @Suppress("USELESS_IS_CHECK")
        assertTrue(p1 is IPoint)
    }

    @Test
    fun testValues() {
        assertEqualsNumber(p1.x, P1X, DELTA_FLOAT)
        assertEqualsNumber(p1.y, P1Y, DELTA_FLOAT)
    }

    @Test
    fun testSum() {
        p1.add(p2)
        assertEqualsNumber(p1.x, P1X + P2X, DELTA_FLOAT)
        assertEqualsNumber(p1.y, P1Y + P2Y, DELTA_FLOAT)
    }

    @Test
    fun testSub() {
        p1.sub(p2)
        assertEqualsNumber(p1.x, P1X - P2X, DELTA_FLOAT)
        assertEqualsNumber(p1.y, P1Y - P2Y, DELTA_FLOAT)
    }

    @Test
    fun testNeg() {
        p1.neg()
        assertEqualsNumber(p1.x, -P1X, DELTA_FLOAT)
        assertEqualsNumber(p1.y, -P1Y, DELTA_FLOAT)
    }

    @Test
    fun testMul() {
        p1.mul(SCALAR)
        assertEqualsNumber(p1.x, P1X * SCALAR, DELTA_FLOAT)
        assertEqualsNumber(p1.y, P1Y * SCALAR, DELTA_FLOAT)
    }

    @Test
    fun testLength() {
        assertEqualsNumber(p1.length, sqrt(P1X * P1X + P1Y * P1Y), DELTA_FLOAT)
    }

    @Test
    fun testNormalize() {
        assertNotEquals(p3.length, 1.0)
        p3.normalize(); assertEqualsNumber(p3.length, 1.0, DELTA_FLOAT)
        p1.normalize(); assertEqualsNumber(p1.length, 1.0, DELTA_FLOAT)
        p2.normalize(); assertEqualsNumber(p2.length, 1.0, DELTA_FLOAT)
    }

    @Test
    fun testEquals() {
        assertTrue(p1 == p1)
        assertTrue(p1 == IPoint(P1X, P1Y))
        assertFalse(p1 == p2)
    }

    @Test
    fun testToString() {
        assertEquals(IPoint(P2X, P2Y), p2)
    }
}

@Suppress("unused")
class SpatialMeshTest {
    val p1 = IPoint(1, 0)
    val p2 = IPoint(2, 3)
    val p3 = IPoint(3, 1)
    val p4 = IPoint(4, 3)
    val p5 = IPoint(2, -1)
    val p6 = IPoint(4, 4)
    val p7 = IPoint(0, 2)

    val t1 = Triangle(p1, p2, p3, true)
    val t2 = Triangle(p2, p3, p4, true)
    val t3 = Triangle(p1, p3, p5, true)
    val t4 = Triangle(p2, p6, p4, true)
    val t5 = Triangle(p7, p2, p1, true)

    val triangles = listOf(t1, t2, t3, t4, t5)

    @Test
    fun testTest() {
        val spatialMesh = SpatialMesh(triangles)
        assertEquals("SpatialMesh(SpatialNode(2, 1),SpatialNode(3, 2),SpatialNode(2, 0),SpatialNode(3, 3),SpatialNode(1, 1))", spatialMesh.toString())
    }
}

class SweepContextTest {
    private var initialPoints = arrayListOf(IPoint(0, 0), IPoint(0, 100), IPoint(100, 100), IPoint(100, 0))
    private var holePoints = arrayListOf(IPoint(10, 10), IPoint(10, 90), IPoint(90, 90), IPoint(90, 10))
    private var sweepContext = SweepContext(initialPoints).apply { addHole(holePoints) }

    @Test
    fun testInitTriangulation() {
        assertEquals(
            "[(0, 0), (0, 100), (100, 100), (100, 0), (10, 10), (10, 90), (90, 90), (90, 10)]",
            this.sweepContext.points.toString()
        )
        this.sweepContext.initTriangulation()
        assertEquals(
            "[(0, 0), (100, 0), (10, 10), (90, 10), (10, 90), (90, 90), (0, 100), (100, 100)]",
            this.sweepContext.points.toString()
        )
    }
}

class SweepTest {
    private var initialPoints = arrayListOf(IPoint(0, 0), IPoint(100, 0), IPoint(100, 100), IPoint(0, 100))
    private var holePoints = arrayListOf(
        IPoint(10, 10),
        IPoint(10, 90),
        IPoint(90, 90),
        IPoint(90, 10)
    )
    private var sweepContext: SweepContext = SweepContext(initialPoints)
    private var sweep: Sweep = Sweep(this.sweepContext)

    @Test
    fun testBoxTriangulate() {
        //Edge.traceList(this.sweepContext.edge_list);
        this.sweep.triangulate()
        assertEquals(2, this.sweepContext.triangles.size)
        assertEquals(
            "[Triangle((0, 100), (100, 0), (100, 100)), Triangle((0, 100), (0, 0), (100, 0))]",
            this.sweepContext.triangles.toString()
        )
    }

    @Test
    fun testBoxWithHoleTriangulate() {
        this.sweepContext.addHole(holePoints)
        this.sweep.triangulate()
        assertEquals(8, this.sweepContext.triangles.size)
        assertEquals(
            "[Triangle((0, 100), (10, 90), (100, 100)), Triangle((10, 90), (90, 90), (100, 100)), Triangle((90, 90), (100, 0), (100, 100)), Triangle((90, 90), (90, 10), (100, 0)), Triangle((90, 10), (10, 10), (100, 0)), Triangle((10, 10), (0, 0), (100, 0)), Triangle((0, 0), (10, 10), (10, 90)), Triangle((0, 100), (0, 0), (10, 90))]",
            this.sweepContext.triangles.toString()
        )
    }
}

class TriangleTest {
    /**
    *    p5
    *     |\
    *     | \
    *     |   \ p4
    *   p3|____\____.p6
    *    / \ t2|   /
    *   / t1\  |  /
    *  /_____\.|/
    * p1      p2
    *
    */
    private val p1 = IPoint(-1, 0)
    private val p2 = IPoint(+1, 0)
    private val p3 = IPoint(0, +1)
    private val p4 = IPoint(+1, +1)
    private val p5 = IPoint(0, +2)
    private val p6 = IPoint(+2, +1)
    private val pInside = IPoint(0.3, 0.3)
    private val pOutside = IPoint(+1, +1)
    private val ec = EdgeContext()
    private val t1 = PolyTriangle(p1, p2, p3, true) // CCW
    private val t2 = PolyTriangle(p3, p4, p2, true) // CW
    private val t3 = PolyTriangle(p4, p5, p3, true) // CCW
    private val t4 = PolyTriangle(p2, p4, p6, true) // CW

    @Test
    fun testArea() {
        val triangle = Triangle(IPoint(0, 0), IPoint(0, -10), IPoint(+10, 0))
        assertEqualsNumber(50.0, triangle.area, 0.0001)
    }

    @Test
    fun testInstantiated() {
        @Suppress("USELESS_IS_CHECK")
        assertTrue(t1 is PolyTriangle)
    }

    @Test
    fun testContainsPoint() {
        assertTrue(t1.containsPoint(p1))
        assertTrue(t1.containsPoint(p2))
        assertTrue(t1.containsPoint(p3))
        assertFalse(t1.containsPoint(pInside))
        assertFalse(t1.containsPoint(pOutside))
    }

    @Test
    fun testContainsEdge() {
        assertTrue(t1.containsEdge(ec.createEdge(p1, p2)))
        assertTrue(t1.containsEdge(ec.createEdge(p2, p3)))
        assertTrue(t1.containsEdge(ec.createEdge(p3, p1)))
        assertFalse(t1.containsEdge(ec.createEdge(pInside, pOutside)))
        assertFalse(t1.containsEdge(ec.createEdge(p1, pOutside)))
        assertFalse(t1.containsEdge(ec.createEdge(pInside, p3)))
    }

    @Test
    fun testMarkNeighbor() {
        t1.markNeighborTriangle(t2)
        assertTrue(t1.neighbors[0] == t2 || t1.neighbors[1] == t2 || t1.neighbors[2] == t2)
        assertTrue(t2.neighbors[0] == t1 || t2.neighbors[1] == t1 || t2.neighbors[2] == t1)

        t1.markNeighborTriangle(t3)
        assertTrue(t1.neighbors[0] != t3 && t1.neighbors[1] != t3 && t1.neighbors[2] != t3)
    }

    @Test
    fun testGetPointIndexOffset() {
        // CCW
        assertEquals(0, t1.getPointIndexOffset(p1))
        assertEquals(1, t1.getPointIndexOffset(p2))
        assertEquals(2, t1.getPointIndexOffset(p3))

        assertEquals(2, t1.getPointIndexOffset(p1, -1))
        assertEquals(0, t1.getPointIndexOffset(p2, -1))
        assertEquals(1, t1.getPointIndexOffset(p3, -1))

        assertEquals(1, t1.getPointIndexOffset(p1, +1))
        assertEquals(2, t1.getPointIndexOffset(p2, +1))
        assertEquals(0, t1.getPointIndexOffset(p3, +1))
    }

    @Test
    fun testPointCW() {
        assertTrue(t1.pointCW(p1) == p3)
        assertTrue(t1.pointCCW(p1) == p2)
        assertTrue(t1.pointCW(p2) == p1)

        assertTrue(t2.pointCW(p3) == p4)
    }

    @Test
    fun testNeighborCW() {
        assertEquals(null, t2.neighborCW(p3))

        t2.markNeighborTriangle(t1)
        t2.markNeighborTriangle(t3)
        t2.markNeighborTriangle(t4)

        assertEquals(t3, t2.neighborCW(p3))
        assertEquals(t1, t2.neighborCCW(p3))

        assertEquals(t3, t2.neighborCCW(p4))
        assertEquals(t4, t2.neighborCW(p4))

        assertEquals(t4, t2.neighborCCW(p2))
        assertEquals(t1, t2.neighborCW(p2))
    }

    @Test
    fun testConstrainedEdge() {
        t1.markConstrainedEdgeByPoints(p1, p3)
        t1.markConstrainedEdgeByPoints(p1, p2)
        assertTrue(t1.getConstrainedEdgeCW(p1))
        assertFalse(t1.getConstrainedEdgeCW(p3))
        assertTrue(t1.getConstrainedEdgeCCW(p1))
        t1.markConstrainedEdgeByIndex(t1.edgeIndex(p3, p2))
        assertTrue(t1.getConstrainedEdgeCW(p3))

        // Constraints not propagated to other triangles.
        assertFalse(t2.getConstrainedEdgeCCW(p3))
    }

    @Test
    fun testSetEdgeSide() {
        t2.markNeighborTriangle(t1)
        t2.markNeighborTriangle(t3)
        t2.markNeighborTriangle(t4)

        //t2.setEdgeSide(p3, p2);
        t2.isEdgeSide(p3, p2)

        assertTrue(t2.getConstrainedEdgeCCW(p3))
        assertTrue(t1.getConstrainedEdgeCW(p3))
    }

    /*
    public fun testRotateTrianglePair():Unit {
        Triangle.rotateTrianglePair(t1, p3, t2, p2);
    }
    */
}

class UtilsTest {
    val p1 = IPoint(0, +1)
    val p2 = IPoint(-1, 0)
    val p3 = IPoint(+1, 0)
    val p4 = IPoint(0.0, -0.01)
    val p5 = IPoint(0, -1)

    @Test
    fun testInsideCircleTrue() {
        assertTrue(Triangle.insideIncircle(p1, p2, p3, p4))
    }

    @Test
    fun testInsideCircleFalse() {
        assertFalse(Triangle.insideIncircle(p1, p2, p3, p5))
    }
}
