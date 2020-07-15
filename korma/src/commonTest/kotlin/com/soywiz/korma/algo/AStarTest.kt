package com.soywiz.korma.algo

import com.soywiz.kds.*
import com.soywiz.korma.geom.*
import kotlin.test.*

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


    data class Result(val map: Array2<Boolean>, val start: IPointInt, val end: IPointInt)

    fun map(str: String): Result {
        var start = PointInt(0, 0)
        var end = PointInt(0, 0)
        val map = Array2(str) { c, x, y ->
            //println("$x, $y, $c")
            if (c == '.') return@Array2 false
            if (c == '*' || c == '#') return@Array2 true
            if (c == 'S') {
                start = PointInt(x, y)
                return@Array2 false
            }
            if (c == 'E') {
                end = PointInt(x, y)
                return@Array2 false
            }
            return@Array2 false
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
        val output = res.toString { it }
        assertEquals(expected.trimIndent(), output)
    }
}
