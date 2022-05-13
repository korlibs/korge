package com.soywiz.korma.algo

import com.soywiz.kds.BooleanArray2
import com.soywiz.kds.map2
import com.soywiz.korma.geom.IPointInt
import com.soywiz.korma.geom.PointInt
import com.soywiz.korma.geom.toPoints
import kotlin.test.Test
import kotlin.test.assertEquals

class AStarTest {
    @Test
    fun testFindReachable() {
        assertFind(
            input = """
                    S#....
                    .#.##.
                    .#.#E.
                    ...#..
                """,
            expected = """
                    0#89ab
                    1#7##c
                    2#6#ed
                    345#..
                """,
            findClosest = false
        )
    }

    @Test
    fun testFindUnreachable() {
        assertFind(
            input = """
                    S#....
                    .#.##.
                    .#.#E.
                    .#.#..
                """,
            expected = """
                    .#....
                    .#.##.
                    .#.#..
                    .#.#..
                """,
            findClosest = false
        )
    }

    @Test
    fun testFindClosestUnreachable() {
        assertFind(
            input = """
                    S#....
                    .#.##.
                    .#.#E.
                    .#.#..
                """,
            expected = """
                    0#....
                    1#.##.
                    2#.#..
                    .#.#..
                """,
            findClosest = true
        )
    }


    data class Result(val map: BooleanArray2, val start: IPointInt, val end: IPointInt)

    fun map(str: String): Result {
        var start = PointInt(0, 0)
        var end = PointInt(0, 0)
        val map = BooleanArray2(str) arr@{ c, x, y ->
            //println("$x, $y, $c")
            if (c == '.') return@arr false
            if (c == '*' || c == '#') return@arr true
            if (c == 'S') {
                start = PointInt(x, y)
                return@arr false
            }
            if (c == 'E') {
                end = PointInt(x, y)
                return@arr false
            }
            return@arr false
        }
        return Result(map, start, end)
    }

    //fun find(map: String): List<PointInt> {
    //	val result = map(map)
    //	return AStar.find(result.map, result.start.x, result.start.y, result.end.x, result.end.y)
    //}

    val xdigits = "0123456789abcdefghijklmnopqrstuvwxyz"

    fun assertFind(input: String, expected: String, findClosest: Boolean = false) {
        val input2 = map(input)
        val points = AStar.find(
            input2.map,
            input2.start.x,
            input2.start.y,
            input2.end.x,
            input2.end.y,
            findClosest = findClosest
        )
        val pointsMap = points.toPoints().withIndex().map { it.value to it.index }.toMap()
        val res = input2.map.map2 { x, y, c ->
            //pointsMap[PointInt(x, y)]?.let { xdigits[it] } ?: (if (c) '#' else '.') // @TODO: Kotlin-native: Regression Crashes BUG in runtime - https://github.com/JetBrains/kotlin-native/issues/1736
            (pointsMap[PointInt(x, y)]?.let { "" + xdigits[it] } ?: (if (c) "#" else ".")).first()
        }
        val output = res.asString { it }
        assertEquals(expected.trimIndent(), output)
    }
}
