package com.soywiz.korev

import com.soywiz.kds.FastArrayList
import kotlin.test.Test
import kotlin.test.assertEquals

class EventListenerTest {
    class MyContainer : BaseEventListener() {
        var name: String? = null
        override var baseParent: BaseEventListener? = null

        private val children = FastArrayList<MyContainer>()

        operator fun get(index: Int): MyContainer = children[index]

        fun addChild(child: MyContainer) {
            children.add(child)
            child.baseParent = this
            __updateChildListenerCount(child, add = true)
        }
        fun removeChild(child: MyContainer) {
            children.remove(child)
            child.baseParent = null
            __updateChildListenerCount(child, add = false)
        }

        override fun <T : TEvent<T>> dispatchChildren(type: EventType<T>, event: T, result: EventResult?) {
            for (child in children) {
                child.dispatch(type, event, result)
            }
        }
    }

    fun MyContainer.container(): MyContainer = MyContainer().also { addChild(it) }

    @Test
    fun testEventListener() {
        val log = arrayListOf<String>()
        fun lg() = log.joinToString(",")
        val container1 = MyContainer().also { it.name = "container1" }
        val container2 = MyContainer().also { it.name = "container2" }
        assertEquals(0, container1.onEventCount(MouseEvent.Type.DOWN))
        assertEquals(0, container2.onEventCount(MouseEvent.Type.DOWN))

        val listener = container2.onEvent(MouseEvent.Type.DOWN) { log += "event" }
        run {
            assertEquals(0, container1.onEventCount(MouseEvent.Type.DOWN))
            assertEquals(1, container2.onEventCount(MouseEvent.Type.DOWN))
            assertEquals("", lg())
            val result = container1.dispatchWithResult(MouseEvent(MouseEvent.Type.DOWN))
            assertEquals("", lg())
            assertEquals(0, result.iterationCount)
        }

        run {
            container1.addChild(container2)
            assertEquals(1, container1.onEventCount(MouseEvent.Type.DOWN))
            assertEquals(1, container2.onEventCount(MouseEvent.Type.DOWN))
            assertEquals("", lg())
            val result = container1.dispatchWithResult(MouseEvent(MouseEvent.Type.DOWN))
            assertEquals("event", lg())
            assertEquals(2, result.iterationCount)
        }

        run {
            container1.removeChild(container2)
            assertEquals(0, container1.onEventCount(MouseEvent.Type.DOWN))
            assertEquals(1, container2.onEventCount(MouseEvent.Type.DOWN))
        }

        run {
            log.clear()
            container1.addChild(container2)
            assertEquals(1, container1.onEventCount(MouseEvent.Type.DOWN))
            assertEquals(1, container2.onEventCount(MouseEvent.Type.DOWN))
            container1.dispatchSimple(MouseEvent(MouseEvent.Type.DOWN))
            assertEquals("event", lg())
        }

        run {
            log.clear()
            listener.close()
            assertEquals(0, container1.onEventCount(MouseEvent.Type.DOWN))
            assertEquals(0, container2.onEventCount(MouseEvent.Type.DOWN))
            assertEquals("", lg())
        }
    }

    @Test
    fun testEventListenerMinCall() {
        fun MyContainer.createContainers(count: Int): List<MyContainer> {
            return (0 until count).map { this.container().also { it.name = "container$it" } }
        }
        val root = MyContainer()
        val containers = root.createContainers(10).map {
            it.also {
                it.createContainers(5)
            }
        }
        var log = ""
        containers[3][1].onEvent(MouseEvent.Type.DOWN) { log += "a" }
        containers[5][3].onEvent(MouseEvent.Type.DOWN) { log += "b" }
        containers[6].onEvent(MouseEvent.Type.DOWN) { log += "c" }

        val result = root.dispatchWithResult(MouseEvent(MouseEvent.Type.DOWN))
        assertEquals("abc", log)
        assertEquals(6, result.iterationCount)
    }
}
