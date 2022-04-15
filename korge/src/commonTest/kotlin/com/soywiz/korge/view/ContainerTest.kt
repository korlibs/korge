package com.soywiz.korge.view

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

internal class ContainerTest {
    @Test
    fun retainAllWorks() {
        val container = Container()

        val view1 = SolidRect(1, 100)
        val view2 = SolidRect(2, 100)
        val view3 = SolidRect(3, 100)
        val view4 = SolidRect(4, 100)

        container.add(view1)
        container.add(view2)
        container.add(view3)
        container.add(view4)

        assertTrue {
            container.containsAll(listOf(
                view1, view2, view3, view4
            ))
        }

        assertTrue {
            container.retainAll(listOf(
                view1, view3
            ))
        }

        assertEquals(container.size, 2)

        assertTrue {
            container.containsAll(listOf(
                view1, view3
            ))
        }
    }

    @Test
    fun removeAllWorks() {
        val container = Container()

        val view1 = SolidRect(1, 100)
        val view2 = SolidRect(2, 100)
        val view3 = SolidRect(3, 100)
        val view4 = SolidRect(4, 100)

        container.add(view1)
        container.add(view2)
        container.add(view3)
        container.add(view4)

        assertTrue {
            container.containsAll(listOf(
                view1, view2, view3, view4
            ))
        }

        assertTrue {
            container.removeAll(listOf(
                view1, view3
            ))
        }

        assertEquals(container.size, 2)

        assertTrue {
            container.containsAll(listOf(
                view2, view4
            ))
        }
    }
}
