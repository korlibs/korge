package com.github.quillraven.fleks

import com.github.quillraven.fleks.collection.EntityComparator
import kotlin.reflect.KClass
import kotlin.test.*

private class SystemTestIntervalSystemEachFrame : IntervalSystem(
    interval = EachFrame
) {
    var numDisposes = 0
    var numCalls = 0

    override fun onTick() {
        ++numCalls
    }

    override fun onDispose() {
        numDisposes++
    }
}

private class SystemTestIntervalSystemFixed : IntervalSystem(
    interval = Fixed(0.25f)
) {
    var numCalls = 0
    var lastAlpha = 0f

    override fun onTick() {
        ++numCalls
    }

    override fun onAlpha(alpha: Float) {
        lastAlpha = alpha
    }
}

private data class SystemTestComponent(var x: Float = 0f)

private class SystemTestIteratingSystemMapper : IteratingSystem(
    allOfComponents = arrayOf(SystemTestComponent::class),
    interval = Fixed(0.25f)
) {
    var numEntityCalls = 0
    var numAlphaCalls = 0
    var lastAlpha = 0f
    var entityToConfigure: Entity? = null

    val mapper = Inject.componentMapper<SystemTestComponent>()

    override fun onTickEntity(entity: Entity) {
        entityToConfigure?.let { e ->
            configureEntity(e) {
                mapper.remove(it)
            }
        }
        ++numEntityCalls
    }

    override fun onAlphaEntity(entity: Entity, alpha: Float) {
        lastAlpha = alpha
        ++numAlphaCalls
    }
}

private class SystemTestEntityCreation : IteratingSystem(
    anyOfComponents = arrayOf(SystemTestComponent::class)
) {
    var numTicks = 0

    init {
        world.entity { add<SystemTestComponent>() }
    }

    override fun onTickEntity(entity: Entity) {
        ++numTicks
    }
}

private class SystemTestIteratingSystemSortAutomatic : IteratingSystem(
    allOfComponents = arrayOf(SystemTestComponent::class),
    comparator = object : EntityComparator {
        private val mapper: ComponentMapper<SystemTestComponent> = Inject.componentMapper()
        override fun compare(entityA: Entity, entityB: Entity): Int {
            return mapper[entityB].x.compareTo(mapper[entityA].x)
        }
    },
) {
    var numEntityCalls = 0
    var lastEntityProcess = Entity(-1)
    var entityToRemove: Entity? = null

    override fun onTickEntity(entity: Entity) {
        entityToRemove?.let {
            world.remove(it)
            entityToRemove = null
        }

        lastEntityProcess = entity
        ++numEntityCalls
    }
}

private class SystemTestFixedSystemRemoval : IteratingSystem(
    allOfComponents = arrayOf(SystemTestComponent::class),
    interval = Fixed(1f)
) {
    var numEntityCalls = 0
    var lastEntityProcess = Entity(-1)
    var entityToRemove: Entity? = null

    private val mapper = Inject.componentMapper<SystemTestComponent>()

    override fun onTickEntity(entity: Entity) {
        entityToRemove?.let {
            world.remove(it)
            entityToRemove = null
        }
    }

    override fun onAlphaEntity(entity: Entity, alpha: Float) {
        // the next line would cause an exception if we don't update the family properly in alpha
        // because component removal is instantly
        mapper[entity].x++
        lastEntityProcess = entity
        ++numEntityCalls
    }
}

private class SystemTestIteratingSystemSortManual : IteratingSystem(
    allOfComponents = arrayOf(SystemTestComponent::class),
    comparator = object : EntityComparator {
        private val mapper: ComponentMapper<SystemTestComponent> = Inject.componentMapper()
        override fun compare(entityA: Entity, entityB: Entity): Int {
            return mapper[entityB].x.compareTo(mapper[entityA].x)
        }
    },
    sortingType = Manual
) {
    var numEntityCalls = 0
    var lastEntityProcess = Entity(-1)

    override fun onTickEntity(entity: Entity) {
        lastEntityProcess = entity
        ++numEntityCalls
    }
}

private class SystemTestIteratingSystemInjectable : IteratingSystem(
    noneOfComponents = arrayOf(SystemTestComponent::class),
    anyOfComponents = arrayOf(SystemTestComponent::class)
) {
    val injectable: String = Inject.dependency()

    override fun onTickEntity(entity: Entity) = Unit
}

private class SystemTestIteratingSystemQualifiedInjectable : IteratingSystem(
    noneOfComponents = arrayOf(SystemTestComponent::class),
    anyOfComponents = arrayOf(SystemTestComponent::class)
) {
    val injectable: String = Inject.dependency()
    val injectable2: String = Inject.dependency("q1")

    override fun onTickEntity(entity: Entity) = Unit
}

internal class SystemTest {
    private fun systemService(
        systemFactory: MutableMap<KClass<*>, () -> IntervalSystem> = mutableMapOf(),
        injectables: MutableMap<String, Injectable> = mutableMapOf(),
        world: World = world {
            components {
                add(::SystemTestComponent)
            }
        }
    ): SystemService {
        Inject.injectObjects = injectables
        Inject.mapperObjects = world.componentService.mappers
        return SystemService(systemFactory)
    }

    @Test
    fun systemWithIntervalEachFrameGetsCalledEveryTime() {
        World.CURRENT_WORLD = world { }
        val system = SystemTestIntervalSystemEachFrame()

        system.onUpdate()
        system.onUpdate()

        assertEquals(2, system.numCalls)
    }

    @Test
    fun systemWithIntervalEachFrameReturnsWorldDeltaTime() {
        World.CURRENT_WORLD = world { }
        val system = SystemTestIntervalSystemEachFrame()
        system.world.update(42f)

        assertEquals(42f, system.deltaTime)
    }

    @Test
    fun systemWithFixedIntervalOf025fGetsCalledFourTimesWhenDeltaTimeIs11f() {
        World.CURRENT_WORLD = world { }
        val system = SystemTestIntervalSystemFixed()
        system.world.update(1.1f)

        system.onUpdate()

        assertEquals(4, system.numCalls)
        assertEquals(0.1f / 0.25f, system.lastAlpha, 0.0001f)
    }

    @Test
    fun systemWithFixedIntervalReturnsStepRateAsDeltaTime() {
        val system = SystemTestIntervalSystemFixed()

        assertEquals(0.25f, system.deltaTime, 0.0001f)
    }

    @Test
    fun createIntervalSystemWithNoArgs() {
        val expectedWorld = world {
            components {
                add(::SystemTestComponent)
            }
        }

        val service = systemService(
            mutableMapOf(SystemTestIntervalSystemEachFrame::class to ::SystemTestIntervalSystemEachFrame),
            world = expectedWorld
        )

        assertEquals(1, service.systems.size)
        assertNotNull(service.system<SystemTestIntervalSystemEachFrame>())
        assertSame(expectedWorld, service.system<SystemTestIntervalSystemEachFrame>().world)
    }

    @Test
    fun createIteratingSystemWithComponentMapperArg() {
        val expectedWorld = world {
            components {
                add(::SystemTestComponent)
            }
        }

        val service = systemService(
            mutableMapOf(SystemTestIteratingSystemMapper::class to ::SystemTestIteratingSystemMapper),
            world = expectedWorld
        )

        val actualSystem = service.system<SystemTestIteratingSystemMapper>()
        assertEquals(1, service.systems.size)
        assertSame(expectedWorld, actualSystem.world)
        assertEquals(SystemTestComponent::class.simpleName, "SystemTestComponent")
    }

    @Test
    fun createIteratingSystemWithAnInjectableArg() {
        val expectedWorld = world {
            components {
                add(::SystemTestComponent)
            }
        }

        val service = systemService(
            mutableMapOf(SystemTestIteratingSystemInjectable::class to ::SystemTestIteratingSystemInjectable),
            mutableMapOf(String::class.simpleName!! to Injectable("42")),
            expectedWorld
        )

        val actualSystem = service.system<SystemTestIteratingSystemInjectable>()
        assertEquals(1, service.systems.size)
        assertSame(expectedWorld, actualSystem.world)
        assertEquals("42", actualSystem.injectable)
    }

    @Test
    fun createIteratingSystemWithQualifiedArgs() {
        val expectedWorld = world {
            components {
                add(::SystemTestComponent)
            }
        }

        val service = systemService(
            mutableMapOf(SystemTestIteratingSystemQualifiedInjectable::class to ::SystemTestIteratingSystemQualifiedInjectable),
            mutableMapOf(String::class.simpleName!! to Injectable("42"), "q1" to Injectable("43")),
            expectedWorld
        )

        val actualSystem = service.system<SystemTestIteratingSystemQualifiedInjectable>()
        assertEquals(1, service.systems.size)
        assertSame(expectedWorld, actualSystem.world)
        assertEquals("42", actualSystem.injectable)
        assertEquals("43", actualSystem.injectable2)
    }

    @Test
    fun cannotCreateIteratingSystemWithMissingInjectables() {
        assertFailsWith<FleksSystemDependencyInjectException> {
            systemService(
                mutableMapOf(
                    SystemTestIteratingSystemInjectable::class to ::SystemTestIteratingSystemInjectable
                )
            )
        }
    }

    @Test
    fun iteratingSystemCallsOnTickAndOnAlphaForEachEntityOfTheSystem() {
        val world = world {
            components {
                add(::SystemTestComponent)
            }
        }
        val service = systemService(
            mutableMapOf(SystemTestIteratingSystemMapper::class to ::SystemTestIteratingSystemMapper),
            world = world
        )
        world.entity { add<SystemTestComponent>() }
        world.entity { add<SystemTestComponent>() }
        world.update(0.3f)

        service.update()

        val system = service.system<SystemTestIteratingSystemMapper>()
        assertEquals(2, system.numEntityCalls)
        assertEquals(2, system.numAlphaCalls)
        assertEquals(0.05f / 0.25f, system.lastAlpha, 0.0001f)
    }

    @Test
    fun configureEntityDuringIteration() {
        val world = world {
            components {
                add(::SystemTestComponent)
            }
        }
        val service = systemService(
            mutableMapOf(SystemTestIteratingSystemMapper::class to ::SystemTestIteratingSystemMapper),
            world = world
        )
        world.update(0.3f)
        val entity = world.entity { add<SystemTestComponent>() }
        val system = service.system<SystemTestIteratingSystemMapper>()
        system.entityToConfigure = entity

        service.update()

        assertFalse { entity in system.mapper }
    }

    @Test
    fun sortEntitiesAutomatically() {
        val world = world {
            components {
                add(::SystemTestComponent)
            }
        }
        val service = systemService(
            mutableMapOf(SystemTestIteratingSystemSortAutomatic::class to ::SystemTestIteratingSystemSortAutomatic),
            world = world
        )
        world.entity { add<SystemTestComponent> { x = 15f } }
        world.entity { add<SystemTestComponent> { x = 10f } }
        val expectedEntity = world.entity { add<SystemTestComponent> { x = 5f } }

        service.update()

        assertEquals(expectedEntity, service.system<SystemTestIteratingSystemSortAutomatic>().lastEntityProcess)
    }

    @Test
    fun sortEntitiesProgrammatically() {
        val world = world {
            components {
                add(::SystemTestComponent)
            }
        }
        val service = systemService(
            mutableMapOf(SystemTestIteratingSystemSortManual::class to ::SystemTestIteratingSystemSortManual),
            world = world
        )
        world.entity { add<SystemTestComponent> { x = 15f } }
        world.entity { add<SystemTestComponent> { x = 10f } }
        val expectedEntity = world.entity { add<SystemTestComponent> { x = 5f } }
        val system = service.system<SystemTestIteratingSystemSortManual>()

        system.doSort = true
        service.update()

        assertEquals(expectedEntity, system.lastEntityProcess)
        assertFalse(system.doSort)
    }

    @Test
    fun cannotGetNonExistingSystem() {
        val world = world {
            components {
                add(::SystemTestComponent)
            }
        }
        val service = systemService(
            mutableMapOf(SystemTestIteratingSystemSortAutomatic::class to ::SystemTestIteratingSystemSortAutomatic),
            world = world
        )

        assertFailsWith<FleksNoSuchSystemException> {
            service.system<SystemTestIntervalSystemEachFrame>()
        }
    }

    @Test
    fun updateOnlyCallsEnabledSystems() {
        val service =
            systemService(mutableMapOf(SystemTestIntervalSystemEachFrame::class to ::SystemTestIntervalSystemEachFrame))
        val system = service.system<SystemTestIntervalSystemEachFrame>()
        system.enabled = false

        service.update()

        assertEquals(0, system.numCalls)
    }

    @Test
    fun removingAnEntityDuringUpdateIsDelayed() {
        val world = world {
            components {
                add(::SystemTestComponent)
            }
        }
        val service = systemService(
            mutableMapOf(SystemTestIteratingSystemSortAutomatic::class to ::SystemTestIteratingSystemSortAutomatic),
            world = world
        )
        world.entity { add<SystemTestComponent> { x = 15f } }
        val entityToRemove = world.entity { add<SystemTestComponent> { x = 10f } }
        world.entity { add<SystemTestComponent> { x = 5f } }
        val system = service.system<SystemTestIteratingSystemSortAutomatic>()
        system.entityToRemove = entityToRemove

        // call it twice - first call still iterates over all three entities
        // while the second call will only iterate over the remaining two entities
        service.update()
        service.update()

        assertEquals(5, system.numEntityCalls)
    }

    @Test
    fun removingAnEntityDuringAlphaIsDelayed() {
        val world = world {
            components {
                add(::SystemTestComponent)
            }
        }
        val service = systemService(
            mutableMapOf(SystemTestFixedSystemRemoval::class to ::SystemTestFixedSystemRemoval),
            world = world
        )
        // set delta time to 1f for the fixed interval
        world.update(1f)
        world.entity { add<SystemTestComponent> { x = 15f } }
        val entityToRemove = world.entity { add<SystemTestComponent> { x = 10f } }
        world.entity { add<SystemTestComponent> { x = 5f } }
        val system = service.system<SystemTestFixedSystemRemoval>()
        system.entityToRemove = entityToRemove

        // call it twice - first call still iterates over all three entities
        // while the second call will only iterate over the remaining two entities
        service.update()
        service.update()

        assertEquals(4, system.numEntityCalls)
    }

    @Test
    fun disposeService() {
        val service =
            systemService(mutableMapOf(SystemTestIntervalSystemEachFrame::class to ::SystemTestIntervalSystemEachFrame))

        service.dispose()

        assertEquals(1, service.system<SystemTestIntervalSystemEachFrame>().numDisposes)
    }

    @Test
    fun createEntityDuringSystemInit() {
        // this test verifies that entities that are created in a system's init block
        // are correctly added to families
        val world = world {
            components {
                add(::SystemTestComponent)
            }
        }

        val service =
            systemService(mutableMapOf(SystemTestEntityCreation::class to ::SystemTestEntityCreation), world = world)
        service.update()

        val system = service.system<SystemTestEntityCreation>()
        assertEquals(1, system.numTicks)
    }
}
