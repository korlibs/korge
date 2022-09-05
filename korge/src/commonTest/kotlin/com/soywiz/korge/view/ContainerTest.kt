package com.soywiz.korge.view

import com.soywiz.korev.MouseEvent
import com.soywiz.korev.dispatch
import com.soywiz.korev.dispatchSimple
import com.soywiz.korev.dispatchWithResult
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
    fun testEventListener() {
        val log = arrayListOf<String>()
        fun lg() = log.joinToString(",")
        val container1 = Container().also { it.name = "container1" }
        val container2 = Container().also { it.name = "container2" }
        assertEquals(0, container1.getEventListenerCount(MouseEvent.Type.DOWN))
        assertEquals(0, container2.getEventListenerCount(MouseEvent.Type.DOWN))

        val listener = container2.addEventListener(MouseEvent.Type.DOWN) { log += "event" }
        run {
            assertEquals(0, container1.getEventListenerCount(MouseEvent.Type.DOWN))
            assertEquals(1, container2.getEventListenerCount(MouseEvent.Type.DOWN))
            assertEquals("", lg())
            val result = container1.dispatchWithResult(MouseEvent(MouseEvent.Type.DOWN))
            assertEquals("", lg())
            assertEquals(0, result.iterationCount)
        }

        run {
            container1.addChild(container2)
            assertEquals(1, container1.getEventListenerCount(MouseEvent.Type.DOWN))
            assertEquals(1, container2.getEventListenerCount(MouseEvent.Type.DOWN))
            assertEquals("", lg())
            val result = container1.dispatchWithResult(MouseEvent(MouseEvent.Type.DOWN))
            assertEquals("event", lg())
            assertEquals(2, result.iterationCount)
        }

        run {
            container1.removeChild(container2)
            assertEquals(0, container1.getEventListenerCount(MouseEvent.Type.DOWN))
            assertEquals(1, container2.getEventListenerCount(MouseEvent.Type.DOWN))
        }

        run {
            log.clear()
            container1.addChild(container2)
            assertEquals(1, container1.getEventListenerCount(MouseEvent.Type.DOWN))
            assertEquals(1, container2.getEventListenerCount(MouseEvent.Type.DOWN))
            container1.dispatchSimple(MouseEvent(MouseEvent.Type.DOWN))
            assertEquals("event", lg())
        }

        run {
            log.clear()
            listener.close()
            assertEquals(0, container1.getEventListenerCount(MouseEvent.Type.DOWN))
            assertEquals(0, container2.getEventListenerCount(MouseEvent.Type.DOWN))
            assertEquals("", lg())
        }
    }

    @Test
    fun testEventListenerMinCall() {
        fun Container.createContainers(count: Int): List<Container> {
            return (0 until count).map { this.container().name("container$it") }
        }
        val root = Container()
        val containers = root.createContainers(10).map {
            it.also {
                it.createContainers(5)
            }
        }
        var log = ""
        containers[3][1].addEventListener(MouseEvent.Type.DOWN) { log += "a" }
        containers[5][3].addEventListener(MouseEvent.Type.DOWN) { log += "b" }
        containers[6].addEventListener(MouseEvent.Type.DOWN) { log += "c" }

        val result = root.dispatchWithResult(MouseEvent(MouseEvent.Type.DOWN))
        assertEquals("abc", log)
        assertEquals(6, result.iterationCount)
    }
}
