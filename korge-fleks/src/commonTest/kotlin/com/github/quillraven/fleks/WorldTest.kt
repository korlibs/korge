package com.github.quillraven.fleks

import kotlin.test.*

private data class WorldTestComponent(var x: Float = 0f)

private class WorldTestIntervalSystem : IntervalSystem() {
    var numCalls = 0
    var disposed = false

    override fun onTick() {
        ++numCalls
    }

    override fun onDispose() {
        disposed = true
    }
}

private class WorldTestIteratingSystem : IteratingSystem(
    allOfComponents = arrayOf(WorldTestComponent::class)
) {
    var numCalls = 0

    val testInject: String = Inject.dependency()
    val mapper: ComponentMapper<WorldTestComponent> = Inject.componentMapper()

    override fun onTick() {
        ++numCalls
        super.onTick()
    }

    override fun onTickEntity(entity: Entity) = Unit
}

private class WorldTestNamedDependencySystem : IntervalSystem() {
    val _name: String = Inject.dependency("name")
    val level: String = Inject.dependency("level")

    val name: String = _name

    override fun onTick() = Unit
}

private class WorldTestComponentListener : ComponentListener<WorldTestComponent> {
    override fun onComponentAdded(entity: Entity, component: WorldTestComponent) = Unit
    override fun onComponentRemoved(entity: Entity, component: WorldTestComponent) = Unit
}

internal class WorldTest {
    @Test
    fun createEmptyWorldTor32Rntities() {
        val w = World { entityCapacity = 32 }

        assertEquals(0, w.numEntities)
        assertEquals(32, w.capacity)
    }

    @Test
    fun createEmptyWorldWithIntervalSystem() {
        val w = World { system(::WorldTestIntervalSystem) }

        assertNotNull(w.system<WorldTestIntervalSystem>())
    }

    @Test
    fun createEmptyWorldWithOneInjectableIteratingSystem() {
        val w = World {
            system(::WorldTestIteratingSystem)
            component(::WorldTestComponent)

            inject("42")
        }

        assertNotNull(w.system<WorldTestIteratingSystem>())
        assertEquals("42", w.system<WorldTestIteratingSystem>().testInject)
    }

    @Test
    fun createEmptyWorldWith2NamedInjectablesSystem() {
        val expectedName = "myName"
        val expectedLevel = "myLevel"
        val w = World {
            system(::WorldTestNamedDependencySystem)
            component(::WorldTestComponent)

            inject("name", expectedName)
            inject("level", "myLevel")
        }

        assertNotNull(w.system<WorldTestNamedDependencySystem>())
        assertEquals(expectedName, w.system<WorldTestNamedDependencySystem>().name)
        assertEquals(expectedLevel, w.system<WorldTestNamedDependencySystem>().level)
    }

    @Test
    fun cannotAddTheSameSystemTwice() {
        assertFailsWith<FleksSystemAlreadyAddedException> {
            World {
                system(::WorldTestIntervalSystem)
                system(::WorldTestIntervalSystem)
            }
        }
    }

    @Test
    fun cannotAccessSystemThatWasNotAdded() {
        val w = World {}

        assertFailsWith<FleksNoSuchSystemException> { w.system<WorldTestIntervalSystem>() }
    }

    @Test
    fun cannotCreateSystemWhenInjectablesAreMissing() {
        assertFailsWith<FleksSystemDependencyInjectException> {
            World { system(::WorldTestIteratingSystem) }
        }
    }

    @Test
    fun cannotInjectTheSameTypeTwice() {
        assertFailsWith<FleksInjectableAlreadyAddedException> {
            World {
                inject("42")
                inject("42")
            }
        }
    }

    @Test
    fun createNewEntity() {
        val w = World {
            system(::WorldTestIteratingSystem)
            component(::WorldTestComponent)
            inject("42")
        }

        val e = w.entity {
            add<WorldTestComponent> { x = 5f }
        }

        assertEquals(1, w.numEntities)
        assertEquals(0, e.id)
        assertEquals(5f, w.system<WorldTestIteratingSystem>().mapper[e].x)
    }

    @Test
    fun removeExistingEntity() {
        val w = World {}
        val e = w.entity()

        w.remove(e)

        assertEquals(0, w.numEntities)
    }

    @Test
    fun updateWorldWithDeltaTimeOf1() {
        val w = World {
            system(::WorldTestIntervalSystem)
            system(::WorldTestIteratingSystem)
            component(::WorldTestComponent)

            inject("42")
        }
        w.system<WorldTestIteratingSystem>().enabled = false

        w.update(1f)

        assertEquals(1f, w.deltaTime)
        assertEquals(1, w.system<WorldTestIntervalSystem>().numCalls)
        assertEquals(0, w.system<WorldTestIteratingSystem>().numCalls)
    }

    @Test
    fun removeAllEntities() {
        val w = World {}
        w.entity()
        w.entity()

        w.removeAll()

        assertEquals(0, w.numEntities)
    }

    @Test
    fun disposeWorld() {
        val w = World {
            system(::WorldTestIntervalSystem)
        }
        w.entity()
        w.entity()

        w.dispose()

        assertTrue(w.system<WorldTestIntervalSystem>().disposed)
        assertEquals(0, w.numEntities)
    }

    @Test
    fun createWorldWithComponentListener() {
        val w = World {
            component(::WorldTestComponent, ::WorldTestComponentListener)
        }

        assertEquals(1, w.componentService.mapper<WorldTestComponent>().listeners.size)
    }

    @Test
    fun cannotAddSameComponentTtwice() {
        assertFailsWith<FleksComponentAlreadyAddedException> {
            World {
                component(::WorldTestComponent)
                component(::WorldTestComponent)
            }
        }
    }

    @Test
    fun getMapper() {
        val w = World {
            component(::WorldTestComponent)
        }

        val mapper = w.mapper<WorldTestComponent>()

        assertEquals(0, mapper.id)
    }

    @Test
    fun throwExceptionWhenThereAreUnusedInjectables() {
        assertFailsWith<FleksUnusedInjectablesException> {
            World {
                inject("42")
            }
        }
    }

    @Test
    fun iterateOverAllActiveEntities() {
        val w = World {}
        val e1 = w.entity()
        val e2 = w.entity()
        val e3 = w.entity()
        w.remove(e2)
        val actualEntities = mutableListOf<Entity>()

        w.forEach { actualEntities.add(it) }

        assertContentEquals(listOf(e1, e3), actualEntities)
    }
}
