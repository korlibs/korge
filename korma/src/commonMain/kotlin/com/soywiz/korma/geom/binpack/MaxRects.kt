package com.soywiz.korma.geom.binpack

import com.soywiz.kds.fastArrayListOf
import com.soywiz.korma.geom.MRectangle

class MaxRects(
    maxWidth: Double,
    maxHeight: Double
) : BinPacker.Algo {
    var freeRectangles = fastArrayListOf<MRectangle>(MRectangle(0.0, 0.0, maxWidth, maxHeight))

    override fun add(width: Double, height: Double): MRectangle? = quickInsert(width, height)

    fun quickInsert(width: Double, height: Double): MRectangle? {
        if (width <= 0.0 && height <= 0.0) return MRectangle(0, 0, 0, 0)
        val newNode = quickFindPositionForNewNodeBestAreaFit(width, height)

        if (newNode.height == 0.0) return null

        var numRectanglesToProcess = freeRectangles.size
        var i = 0
        while (i < numRectanglesToProcess) {
            if (splitFreeNode(freeRectangles[i], newNode)) {
                freeRectangles.removeAt(i)
                --numRectanglesToProcess
                --i
            }
            i++
        }

        pruneFreeList()
        return newNode
    }

    private fun quickFindPositionForNewNodeBestAreaFit(width: Double, height: Double): MRectangle {
        var score = Double.MAX_VALUE
        var areaFit: Double
        val bestNode = MRectangle()

        for (r in freeRectangles) {
            // Try to place the rectangle in upright (non-flipped) orientation.
            if (r.width >= width && r.height >= height) {
                areaFit = r.width * r.height - width * height
                if (areaFit < score) {
                    bestNode.x = r.x
                    bestNode.y = r.y
                    bestNode.width = width
                    bestNode.height = height
                    score = areaFit
                }
            }
        }

        return bestNode
    }

    private fun splitFreeNode(freeNode: MRectangle, usedNode: MRectangle): Boolean {
        var newNode: MRectangle
        // Test with SAT if the rectangles even intersect.
        if (usedNode.left >= freeNode.right || usedNode.right <= freeNode.x || usedNode.top >= freeNode.bottom || usedNode.bottom <= freeNode.top) {
            return false
        }
        if (usedNode.x < freeNode.right && usedNode.right > freeNode.x) {
            // New node at the top side of the used node.
            if (usedNode.y > freeNode.y && usedNode.y < freeNode.bottom) {
                newNode = freeNode.clone()
                newNode.height = usedNode.y - newNode.y
                freeRectangles.add(newNode)
            }
            // New node at the bottom side of the used node.
            if (usedNode.bottom < freeNode.bottom) {
                newNode = freeNode.clone()
                newNode.top = usedNode.bottom
                newNode.height = freeNode.bottom - usedNode.bottom
                freeRectangles.add(newNode)
            }
        }
        if (usedNode.y < freeNode.bottom && usedNode.bottom > freeNode.y) {
            // New node on the left side of the used node.
            if (usedNode.x > freeNode.x && usedNode.x < freeNode.right) {
                newNode = freeNode.clone()
                newNode.width = usedNode.x - newNode.x
                freeRectangles.add(newNode)
            }
            // New node on the right side of the used node.
            if (usedNode.right < freeNode.right) {
                newNode = freeNode.clone()
                newNode.x = usedNode.right
                newNode.width = freeNode.right - usedNode.right
                freeRectangles.add(newNode)
            }
        }
        return true
    }

    private fun pruneFreeList() {
        // Go through each pair and remove any rectangle that is redundant.
        var len = freeRectangles.size
        var i = 0
        while (i < len) {
            var j = i + 1
            val tmpRect = freeRectangles[i]
            while (j < len) {
                val tmpRect2 = freeRectangles[j]
                if (MRectangle.isContainedIn(tmpRect, tmpRect2)) {
                    freeRectangles.removeAt(i)
                    --i
                    --len
                    break
                }
                if (MRectangle.isContainedIn(tmpRect2, tmpRect)) {
                    freeRectangles.removeAt(j)
                    --len
                    --j
                }
                j++
            }
            i++
        }
    }
}
