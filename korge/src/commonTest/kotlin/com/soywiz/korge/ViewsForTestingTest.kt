package com.soywiz.korge

import com.soywiz.korge.scene.Module
import com.soywiz.korge.scene.Scene
import com.soywiz.korge.tests.ViewsForTesting
import com.soywiz.korge.view.Container
import com.soywiz.korinject.AsyncInjector
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ViewsForTestingTest : ViewsForTesting() {
    open class Dependency {
        open val foo = "bar"
    }

    class DummyScene(val dependency: Dependency) : Scene() {
        var running = false
        override suspend fun Container.sceneInit() {
            running = true
        }
    }

    object DummyModule : Module() {
        override suspend fun AsyncInjector.configure() {
            mapSingleton {
                Dependency()
            }
            mapPrototype {
                DummyScene(get())
            }
        }
    }

    @Test
    fun sceneTestRunsScene() = sceneTest<DummyScene>(DummyModule) {
        assertEquals("bar", dependency.foo)
        assertTrue(running)
    }

    @Test
    fun sceneTestCanOverrideBindingsForTesting() = sceneTest<DummyScene>(DummyModule, {
        mapSingleton<Dependency> {
            object : Dependency() {
                override val foo = "test"
            }
        }
    }) {
        assertEquals("test", dependency.foo)
        assertTrue(running)
    }
}
