package com.soywiz.korma.algo

import com.soywiz.kds.*
import com.soywiz.korma.geom.*
import com.soywiz.korma.math.*

class AStar(val width: Int, val height: Int, val isBlocking: (x: Int, y: Int) -> Boolean) {
    companion object {
        operator fun invoke(board: Array2<Boolean>) = AStar(board.width, board.height) { x, y -> board[x, y] }
        fun find(
            board: Array2<Boolean>, x0: Int, y0: Int, x1: Int, y1: Int, findClosest: Boolean = false,
            diagonals: Boolean = false
        ): IPointIntArrayList = AStar(board.width, board.height) { x, y -> board[x, y] }.find(x0, y0, x1, y1, findClosest, diagonals)
    }

    fun find(x0: Int, y0: Int, x1: Int, y1: Int, findClosest: Boolean = false, diagonals: Boolean = false): IPointIntArrayList {
        val out = PointIntArrayList()
        find(x0, y0, x1, y1, findClosest, diagonals) { x, y -> out.add(x, y) }
        out.reverse()
        return out
    }

    fun find(x0: Int, y0: Int, x1: Int, y1: Int, findClosest: Boolean = false, diagonals: Boolean = false, emit: (Int, Int) -> Unit) {
        // Reset
        queue.clear()
        for (n in weights.indices) weights[n] = Int.MAX_VALUE
        for (n in prev.indices) prev[n] = NULL.index

        val first = getNode(x0, y0)
        val dest = getNode(x1, y1)
        var closest = first
        var closestDist = Point.distance(x0, y0, x1, y1)
        if (!first.value) {
            queue.add(first.index)
            first.weight = 0
        }

        while (queue.isNotEmpty()) {
            val last = AStarNode(queue.removeHead())
            val dist = Point.distance(last.posX, last.posY, dest.posX, dest.posY)
            if (dist < closestDist) {
                closestDist = dist
                closest = last
            }
            val nweight = last.weight + 1
            last.neighborhoods(diagonals) { n ->
                if (nweight < n.weight) {
                    n.prev = last
                    queue.add(n.index)
                    n.weight = nweight
                }
            }
        }

        if (findClosest || closest == dest) {
            var current: AStarNode = closest
            while (current != NULL) {
                emit(current.posX, current.posY)
                current = current.prev
            }
        }
    }

    private val NULL = AStarNode(-1)

    private val posX = IntArray(width * height) { it % width }
    private val posY = IntArray(width * height) { it / width }
    private val weights = IntArray(width * height) { Int.MAX_VALUE }
    private val prev = IntArray(width * height) { NULL.index }
    private val queue = IntPriorityQueue { a, b -> AStarNode(a).weight - AStarNode(b).weight }

    private fun inside(x: Int, y: Int): Boolean = (x in 0 until width) && (y in 0 until height)
    private fun getNode(x: Int, y: Int): AStarNode = AStarNode(y * width + x)

    private val AStarNode.posX: Int get() = this@AStar.posX[index]
    private val AStarNode.posY: Int get() = this@AStar.posY[index]
    private val AStarNode.value: Boolean get() = isBlocking(posX, posY)
    private var AStarNode.weight: Int
        set(value) = run { this@AStar.weights[index] = value }
        get() = this@AStar.weights[index]
    private var AStarNode.prev: AStarNode
        set(value) = run { this@AStar.prev[index] = value.index }
        get() = AStarNode(this@AStar.prev[index])

    private inline fun AStarNode.neighborhoods(diagonals: Boolean, emit: (AStarNode) -> Unit) {
        for (dy in -1 .. +1) {
            for (dx in -1 .. +1) {
                if (dx == 0 && dy == 0) continue
                if (!diagonals && dx != 0 && dy != 0) continue
                val x = posX + dx
                val y = posY + dy
                if (inside(x, y) && !getNode(x, y).value) {
                    emit(getNode(x, y))
                }
            }
        }
    }
}

private inline class AStarNode(val index: Int)
