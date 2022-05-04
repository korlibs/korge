package com.soywiz.korge.view.util

import com.soywiz.korge.view.SolidRect
import com.soywiz.korma.math.isAlmostEquals
import kotlin.test.Test
import kotlin.test.assertTrue

internal class UtilTest {
    @Test
    fun distributeEvenlyHorizontally_singleViewProvidedResultsInNoChange() {
        val rect1 = SolidRect(100.0, 100.0)

        assertTrue {
            rect1.x.isAlmostEquals(0.0)
        }

        distributeEvenlyHorizontally(listOf(rect1), 300.0)

        assertTrue {
            rect1.x.isAlmostEquals(0.0)
        }
    }

    @Test
    fun distributeEvenlyHorizontally_multipleViewsDistributesCorrectly1() {
        val rect1 = SolidRect(100.0, 100.0)
        val rect2 = SolidRect(100.0, 100.0)

        assertTrue {
            rect1.x.isAlmostEquals(0.0)
        }
        assertTrue {
            rect2.x.isAlmostEquals(0.0)
        }

        distributeEvenlyHorizontally(listOf(rect1, rect2), 300.0)

        assertTrue {
            rect1.x.isAlmostEquals(0.0)
        }
        assertTrue {
            rect2.x.isAlmostEquals(200.0)
        }
    }

    @Test
    fun distributeEvenlyHorizontally_multipleViewsDistributesCorrectly2() {
        val rect1 = SolidRect(100.0, 100.0)
        val rect2 = SolidRect(200.0, 200.0)
        val rect3 = SolidRect(100.0, 100.0)

        assertTrue {
            rect1.x.isAlmostEquals(0.0)
        }
        assertTrue {
            rect2.x.isAlmostEquals(0.0)
        }
        assertTrue {
            rect3.x.isAlmostEquals(0.0)
        }

        distributeEvenlyHorizontally(listOf(rect1, rect2, rect3), 1000.0)

        // Expected
        // 100 (rect 1)
        // 300 (padding)
        // 200 (rect 2)
        // 300 (padding)
        // 100 (rect 3)

        assertTrue {
            rect1.x.isAlmostEquals(0.0)
        }
        assertTrue {
            rect2.x.isAlmostEquals(400.0)
        }
        assertTrue {
            rect3.x.isAlmostEquals(900.0)
        }
    }

    @Test
    fun distributeEvenlyHorizontally_multipleViewsDistributesCorrectly3() {
        val rect1 = SolidRect(100.0, 100.0)
        val rect2 = SolidRect(200.0, 100.0)
        val rect3 = SolidRect(100.0, 100.0)

        assertTrue {
            rect1.x.isAlmostEquals(0.0)
        }
        assertTrue {
            rect2.x.isAlmostEquals(0.0)
        }
        assertTrue {
            rect3.x.isAlmostEquals(0.0)
        }

        distributeEvenlyHorizontally(listOf(rect1, rect3, rect2), 1000.0)
        // Expected
        // 100 (rect 1)
        // 300 (padding)
        // 100 (rect 3)
        // 300 (padding)
        // 200 (rect 2)

        assertTrue {
            rect1.x.isAlmostEquals(0.0)
        }
        assertTrue {
            rect3.x.isAlmostEquals(400.0)
        }
        assertTrue {
            rect2.x.isAlmostEquals(800.0)
        }
    }

    @Test
    fun distributeEvenlyHorizontally_boundingWidthCalculatedUsingRightMostView() {
        val rect1 = SolidRect(100.0, 100.0)
        val rect2 = SolidRect(200.0, 200.0)
        val rect3 = SolidRect(100.0, 100.0).apply {
            x = 900.0
        }

        assertTrue {
            rect1.x.isAlmostEquals(0.0)
        }
        assertTrue {
            rect2.x.isAlmostEquals(0.0)
        }
        assertTrue {
            rect3.x.isAlmostEquals(900.0)
        }

        distributeEvenlyHorizontally(listOf(rect1, rect2, rect3), 1000.0)

        // Expected
        // 100 (rect 1)
        // 300 (padding)
        // 200 (rect 2)
        // 300 (padding)
        // 100 (rect 3)

        assertTrue {
            rect1.x.isAlmostEquals(0.0)
        }
        assertTrue {
            rect2.x.isAlmostEquals(400.0)
        }
        assertTrue {
            rect3.x.isAlmostEquals(900.0)
        }
    }

    @Test
    fun distributeEvenlyHorizontally_offsetBasedOnFirstView() {
        val rect1 = SolidRect(100.0, 100.0).apply {
            x = 100.0
        }
        val rect2 = SolidRect(100.0, 100.0)

        assertTrue {
            rect1.x.isAlmostEquals(100.0)
        }
        assertTrue {
            rect2.x.isAlmostEquals(0.0)
        }

        distributeEvenlyHorizontally(listOf(rect1, rect2), 300.0)

        // Expected widths:
        // 100 offset
        // 100 (rect 1)
        // 100 (padding)
        // 100 (rect 2)

        assertTrue {
            rect1.x.isAlmostEquals(100.0)
        }
        assertTrue {
            rect2.x.isAlmostEquals(300.0)
        }
    }

    @Test
    fun distributeEvenlyVertically_singleViewProvidedResultsInNoChange() {
        val rect1 = SolidRect(100.0, 100.0)

        assertTrue {
            rect1.x.isAlmostEquals(0.0)
        }

        distributeEvenlyVertically(listOf(rect1), 300.0)

        assertTrue {
            rect1.y.isAlmostEquals(0.0)
        }
    }

    @Test
    fun distributeEvenlyVertically_multipleViewsDistributesCorrectly1() {
        val rect1 = SolidRect(100.0, 100.0)
        val rect2 = SolidRect(100.0, 100.0)

        assertTrue {
            rect1.y.isAlmostEquals(0.0)
        }
        assertTrue {
            rect2.y.isAlmostEquals(0.0)
        }

        distributeEvenlyVertically(listOf(rect1, rect2), 300.0)

        assertTrue {
            rect1.y.isAlmostEquals(0.0)
        }
        assertTrue {
            rect2.y.isAlmostEquals(200.0)
        }
    }

    @Test
    fun distributeEvenlyVertically_multipleViewsDistributesCorrectly2() {
        val rect1 = SolidRect(100.0, 100.0)
        val rect2 = SolidRect(200.0, 200.0)
        val rect3 = SolidRect(100.0, 100.0)

        assertTrue {
            rect1.y.isAlmostEquals(0.0)
        }
        assertTrue {
            rect2.y.isAlmostEquals(0.0)
        }
        assertTrue {
            rect3.y.isAlmostEquals(0.0)
        }

        distributeEvenlyVertically(listOf(rect1, rect2, rect3), 1000.0)

        // Expected
        // 100 (rect 1)
        // 300 (padding)
        // 200 (rect 2)
        // 300 (padding)
        // 100 (rect 3)

        assertTrue {
            rect1.y.isAlmostEquals(0.0)
        }
        assertTrue {
            rect2.y.isAlmostEquals(400.0)
        }
        assertTrue {
            rect3.y.isAlmostEquals(900.0)
        }
    }

    @Test
    fun distributeEvenlyVertically_multipleViewsDistributesCorrectly3() {
        val rect1 = SolidRect(100.0, 100.0)
        val rect2 = SolidRect(200.0, 200.0)
        val rect3 = SolidRect(100.0, 100.0)

        assertTrue {
            rect1.y.isAlmostEquals(0.0)
        }
        assertTrue {
            rect2.y.isAlmostEquals(0.0)
        }
        assertTrue {
            rect3.y.isAlmostEquals(0.0)
        }

        distributeEvenlyVertically(listOf(rect1, rect3, rect2), 1000.0)
        // Expected
        // 100 (rect 1)
        // 300 (padding)
        // 100 (rect 3)
        // 300 (padding)
        // 200 (rect 2)

        assertTrue {
            rect1.y.isAlmostEquals(0.0)
        }
        assertTrue {
            rect3.y.isAlmostEquals(400.0)
        }
        assertTrue {
            rect2.y.isAlmostEquals(800.0)
        }
    }

    @Test
    fun distributeEvenlyVertically_boundingHeightCalculatedUsingBottomMostView() {
        val rect1 = SolidRect(100.0, 100.0)
        val rect2 = SolidRect(200.0, 200.0)
        val rect3 = SolidRect(100.0, 100.0).apply {
            y = 900.0
        }

        assertTrue {
            rect1.y.isAlmostEquals(0.0)
        }
        assertTrue {
            rect2.y.isAlmostEquals(0.0)
        }
        assertTrue {
            rect3.y.isAlmostEquals(900.0)
        }

        distributeEvenlyVertically(listOf(rect1, rect2, rect3))

        // Expected
        // 100 (rect 1)
        // 300 (padding)
        // 200 (rect 2)
        // 300 (padding)
        // 100 (rect 3)

        assertTrue {
            rect1.y.isAlmostEquals(0.0)
        }
        assertTrue {
            rect2.y.isAlmostEquals(400.0)
        }
        assertTrue {
            rect3.y.isAlmostEquals(900.0)
        }
    }

    @Test
    fun distributeEvenlyVertically_offsetBasedOnFirstView() {
        val rect1 = SolidRect(100.0, 100.0).apply {
            y = 100.0
        }
        val rect2 = SolidRect(100.0, 100.0)

        assertTrue {
            rect1.y.isAlmostEquals(100.0)
        }
        assertTrue {
            rect2.y.isAlmostEquals(0.0)
        }

        distributeEvenlyVertically(listOf(rect1, rect2), 300.0)

        // Expected widths:
        // 100 offset
        // 100 (rect 1)
        // 100 (padding)
        // 100 (rect 2)

        assertTrue {
            rect1.y.isAlmostEquals(100.0)
        }
        assertTrue {
            rect2.y.isAlmostEquals(300.0)
        }
    }
}
