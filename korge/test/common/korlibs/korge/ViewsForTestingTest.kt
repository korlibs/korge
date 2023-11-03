package korlibs.korge

import korlibs.inject.*
import korlibs.korge.scene.*
import korlibs.korge.tests.*
import korlibs.korge.view.*
import kotlin.test.*

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

    fun Injector.mapCommon() {
        mapSingleton { Dependency() }
        mapPrototype { DummyScene(get()) }
    }

    @Test
    fun sceneTestRunsScene() = sceneTest<DummyScene>(configureInjector = { mapCommon() }) {
        assertEquals("bar", dependency.foo)
        assertTrue(running)
    }

    @Test
    fun sceneTestCanOverrideBindingsForTesting() = sceneTest<DummyScene>(configureInjector = {
        mapCommon()
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
