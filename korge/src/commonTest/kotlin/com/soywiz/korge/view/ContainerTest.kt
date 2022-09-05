package com.soywiz.korge.view

import com.soywiz.klock.milliseconds
import com.soywiz.korge.component.UpdateComponent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

internal class ContainerTest {
    @Test
    fun retainAllWorks() {
        val container = Container()
        val collection = container.children

        val view1 = SolidRect(1, 100)
        val view2 = SolidRect(2, 100)
        val view3 = SolidRect(3, 100)
        val view4 = SolidRect(4, 100)

        collection.add(view1)
        collection.add(view2)
        collection.add(view3)
        collection.add(view4)

        assertTrue {
            collection.containsAll(listOf(
                view1, view2, view3, view4
            ))
        }

        assertTrue {
            collection.retainAll(listOf(
                view1, view3
            ))
        }

        assertEquals(collection.size, 2)

        assertTrue {
            collection.containsAll(listOf(
                view1, view3
            ))
        }
    }

    @Test
    fun removeAllWorks() {
        val container = Container()
        val collection = container.children

        val view1 = SolidRect(1, 100)
        val view2 = SolidRect(2, 100)
        val view3 = SolidRect(3, 100)
        val view4 = SolidRect(4, 100)

        collection.add(view1)
        collection.add(view2)
        collection.add(view3)
        collection.add(view4)

        assertTrue {
            collection.containsAll(listOf(
                view1, view2, view3, view4
            ))
        }

        assertTrue {
            collection.removeAll(listOf(
                view1, view3
            ))
        }

        assertEquals(collection.size, 2)

        assertTrue {
            collection.containsAll(listOf(
                view2, view4
            ))
        }
    }

    @Test
    fun testRemove() {
        val c = Container().apply {
            solidRect(1, 1).name("a")
            solidRect(1, 1).name("b")
            solidRect(1, 1).name("c")
            solidRect(1, 1).name("d")
            solidRect(1, 1).name("e")
        }
        c.removeChildAt(1, 2)
        assertEquals(listOf("a", "d", "e"), c.children.map { it.name })
    }

    @Test
    fun testSwap() {
        val c = Container().apply {
            solidRect(1, 1).name("a")
            solidRect(1, 1).name("b")
            solidRect(1, 1).name("c")
            solidRect(1, 1).name("d")
            solidRect(1, 1).name("e")
        }
        c.swapChildrenAt(0, 2, 2)
        assertEquals(listOf("c", "d", "a", "b", "e"), c.children.map { it.name })
    }

    @Test
    fun testMove() {
        val c = Container().apply {
            solidRect(1, 1).name("a")
            solidRect(1, 1).name("b")
            solidRect(1, 1).name("c")
            solidRect(1, 1).name("d")
            solidRect(1, 1).name("e")
        }
        c.moveChildrenAt(1, 3, 1)
        assertEquals(listOf("a", "c", "b", "d", "e"), c.children.map { it.name })
    }

    @Test
    fun testMutableIterator() {
        val c = Container().apply {
            solidRect(1, 1).name("a")
            solidRect(1, 1).name("b")
            solidRect(1, 1).name("c")
            solidRect(1, 1).name("d")
            solidRect(1, 1).name("e")
        }
        fun validateIndices() {
            for (n in 0 until c.numChildren) assertEquals(n, c.children[n].index)
        }
        val iterator = c.children.iterator()
        while (iterator.hasNext()) {
            val view = iterator.next()
            if (view.name == "c") iterator.remove()
        }
        assertEquals(listOf("a", "b", "d", "e"), c.children.map { it.name })
        validateIndices()
    }

    @Test
    fun testComponents() {
        val container1 = Container().name("container1")
        val container2 = Container().name("container2")
        var log = ""
        assertEquals(0, container1.getComponentCountInDescendants(UpdateComponent))
        assertEquals(0, container2.getComponentCountInDescendants(UpdateComponent))
        container2.addUpdater(first = false) { log += "a" }
        assertEquals(0, container1.getComponentCountInDescendants(UpdateComponent))
        assertEquals(1, container2.getComponentCountInDescendants(UpdateComponent))
        container1.addChild(container2)
        assertEquals(1, container1.getComponentCountInDescendants(UpdateComponent))
        assertEquals(1, container2.getComponentCountInDescendants(UpdateComponent))
        assertEquals("", log)
        container1.forEachComponentOfTypeRecursive(UpdateComponent) {
            it.update(0.milliseconds)
        }
        assertEquals("a", log)
    }
}
