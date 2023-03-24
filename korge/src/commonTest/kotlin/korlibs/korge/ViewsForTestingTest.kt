package korlibs.korge

import korlibs.korge.scene.Module
import korlibs.korge.scene.Scene
import korlibs.korge.tests.ViewsForTesting
import korlibs.korge.view.SContainer
import korlibs.inject.AsyncInjector
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ViewsForTestingTest : ViewsForTesting() {
    open class Dependency {
        open val foo = "bar"
    }

    class DummyScene(val dependency: Dependency) : Scene() {
        var running = false
        override suspend fun SContainer.sceneInit() {
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
