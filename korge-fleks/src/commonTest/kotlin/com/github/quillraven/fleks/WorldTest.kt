package com.github.quillraven.fleks

import com.github.quillraven.fleks.collection.compareEntity
import kotlin.test.*

private data class WorldTestComponent(var x: Float = 0f)

private class WorldTestComponent2

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
    var numCallsEntity = 0

    val testInject: String = Inject.dependency()
    val mapper: ComponentMapper<WorldTestComponent> = Inject.componentMapper()

    override fun onTick() {
        ++numCalls
        super.onTick()
    }

    override fun onTickEntity(entity: Entity) {
        ++numCallsEntity
    }
}

private class WorldTestInitSystem : IteratingSystem(
    allOfComponents = arrayOf(WorldTestComponent::class)
) {
    init {
        world.entity { add<WorldTestComponent>() }
    }

    override fun onTickEntity(entity: Entity) = Unit
}

private class WorldTestInitSystemExtraFamily : IteratingSystem(
    allOfComponents = arrayOf(WorldTestComponent::class)
) {
    val extraFamily = world.family(
        anyOf = arrayOf(WorldTestComponent2::class),
        noneOf = arrayOf(WorldTestComponent::class)
    )

    init {
        world.entity { add<WorldTestComponent2>() }
    }

    override fun onTickEntity(entity: Entity) = Unit
}

private class WorldTestNamedDependencySystem : IntervalSystem() {
    val injName: String = Inject.dependency("name")
    val level: String = Inject.dependency("level")

    val name: String = injName

    override fun onTick() = Unit
}

private class WorldTestComponentListener : ComponentListener<WorldTestComponent> {
    val world: World = Inject.dependency()
    var numAdd = 0
    var numRemove = 0
    override fun onComponentAdded(entity: Entity, component: WorldTestComponent) {
        ++numAdd
    }

    override fun onComponentRemoved(entity: Entity, component: WorldTestComponent) {
        ++numRemove
    }
}

private class WorldTestFamilyListener : FamilyListener(
    allOfComponents = arrayOf(WorldTestComponent::class)
) {
    val world: World = Inject.dependency()
    var numAdd = 0
    var numRemove = 0

    override fun onEntityAdded(entity: Entity) {
        ++numAdd
    }

    override fun onEntityRemoved(entity: Entity) {
        ++numRemove
    }
}

private class WorldTestFamilyListenerMissingCfg : FamilyListener()

internal class WorldTest {
    @Test
    fun createEmptyWorldFor32Entities() {
        val w = world { entityCapacity = 32 }

        assertEquals(0, w.numEntities)
        assertEquals(32, w.capacity)
    }

    @Test
    fun createEmptyWorldWith1NoArgsIntervalSystem() {
        val w = world {
            systems {
                add(::WorldTestIntervalSystem)
            }
        }

        assertNotNull(w.system<WorldTestIntervalSystem>())
    }

    @Test
    fun getWorldSystems() {
        val w = world {
            systems {
                add(::WorldTestIntervalSystem)
            }
        }

        assertEquals(w.systemService.systems, w.systems)
    }

    @Test
    fun createEmptyWorldWith1InjectableArgsIteratingSystem() {
        val w = world {
            injectables {
                add("42")
            }

            systems {
                add(::WorldTestIteratingSystem)
            }

            components {
                add(::WorldTestComponent)
            }

        }

        assertNotNull(w.system<WorldTestIteratingSystem>())
        assertEquals("42", w.system<WorldTestIteratingSystem>().testInject)
    }

    @Test
    fun createEmptyWorldWith2NamedInjectablesSystem() {
        val expectedName = "myName"
        val expectedLevel = "myLevel"
        val w = world {
            injectables {
                add("name", expectedName)
                add("level", "myLevel")
            }

            systems {
                add(::WorldTestNamedDependencySystem)
            }

            components {
                add(::WorldTestComponent)
            }
        }

        assertNotNull(w.system<WorldTestNamedDependencySystem>())
        assertEquals(expectedName, w.system<WorldTestNamedDependencySystem>().name)
        assertEquals(expectedLevel, w.system<WorldTestNamedDependencySystem>().level)
    }

    @Test
    fun cannotAddTheSameSystemTwice() {
        assertFailsWith<FleksSystemAlreadyAddedException> {
            world {
                systems {
                    add(::WorldTestIntervalSystem)
                    add(::WorldTestIntervalSystem)
                }
            }
        }
    }

    @Test
    fun cannotAccessSystemThatWasNotAdded() {
        val w = world {}

        assertFailsWith<FleksNoSuchSystemException> { w.system<WorldTestIntervalSystem>() }
    }

    @Test
    fun cannotCreateSystemWhenInjectablesAreMissing() {
        assertFailsWith<FleksSystemDependencyInjectException> {
            world {
                components {
                    add(::WorldTestComponent)
                }

                systems {
                    add(::WorldTestIteratingSystem)
                }
            }
        }
    }

    @Test
    fun cannotInjectTheSameTypeTwice() {
        assertFailsWith<FleksInjectableAlreadyAddedException> {
            world {
                injectables {
                    add("42")
                    add("42")
                }
            }
        }
    }

    @Test
    fun createNewEntity() {
        val w = world {
            systems {
                add(::WorldTestIteratingSystem)
            }

            components {
                add(::WorldTestComponent)
            }

            injectables {
                add("42")
            }
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
        val w = world {}
        val e = w.entity()

        w.remove(e)

        assertEquals(0, w.numEntities)
    }

    @Test
    fun updateWorldWithDeltaTimeOf1() {
        val w = world {
            systems {
                add(::WorldTestIntervalSystem)
                add(::WorldTestIteratingSystem)
            }

            components {
                add(::WorldTestComponent)
            }

            injectables {
                add("42")
            }
        }
        w.system<WorldTestIteratingSystem>().enabled = false

        w.update(1f)

        assertEquals(1f, w.deltaTime)
        assertEquals(1, w.system<WorldTestIntervalSystem>().numCalls)
        assertEquals(0, w.system<WorldTestIteratingSystem>().numCalls)
    }

    @Test
    fun removeAllEntities() {
        val w = world {}
        w.entity()
        w.entity()

        w.removeAll()

        assertEquals(0, w.numEntities)
    }

    @Test
    fun disposeWorld() {
        val w = world {
            systems {
                add(::WorldTestIntervalSystem)
            }
        }
        w.entity()
        w.entity()

        w.dispose()

        assertTrue(w.system<WorldTestIntervalSystem>().disposed)
        assertEquals(0, w.numEntities)
    }

    @Test
    fun getMapper() {
        val w = world {
            components {
                add(::WorldTestComponent)
            }
        }

        val mapper = w.mapper<WorldTestComponent>()

        assertEquals(0, mapper.id)
    }

    @Test
    fun throwExceptionWhenThereAreUnusedInjectables() {
        assertFailsWith<FleksUnusedInjectablesException> {
            world {
                injectables {
                    add("42")
                }
            }
        }
    }

    @Test
    fun iterateOverAllActiveEntities() {
        val w = world {}
        val e1 = w.entity()
        val e2 = w.entity()
        val e3 = w.entity()
        w.remove(e2)
        val actualEntities = mutableListOf<Entity>()

        w.forEach { actualEntities.add(it) }

        assertContentEquals(listOf(e1, e3), actualEntities)
    }

    @Test
    fun createTwoWorldsWithDifferentDependencies() {
        val w1 = world {
            systems {
                add(::WorldTestNamedDependencySystem)
            }

            injectables {
                add("name", "name1")
                add("level", "level1")
            }

        }
        val w2 = world {
            systems {
                add(::WorldTestNamedDependencySystem)
            }

            injectables {
                add("name", "name2")
                add("level", "level2")
            }

        }
        val s1 = w1.system<WorldTestNamedDependencySystem>()
        val s2 = w2.system<WorldTestNamedDependencySystem>()

        assertEquals("name1", s1.injName)
        assertEquals("level1", s1.level)
        assertEquals("name2", s2.injName)
        assertEquals("level2", s2.level)
    }

    @Test
    fun configureEntityAfterCreation() {
        val w = world {
            injectables {
                add("test")
            }

            components {
                add(::WorldTestComponent)
            }

            systems {
                add(::WorldTestIteratingSystem)
            }
        }
        val e = w.entity()
        val mapper: ComponentMapper<WorldTestComponent> = w.mapper()

        w.configureEntity(e) { mapper.add(it) }
        w.update(0f)

        assertEquals(1, w.system<WorldTestIteratingSystem>().numCallsEntity)
    }

    @Test
    fun getFamilyAfterWorldCreation() {
        // WorldTestInitSystem creates an entity in its init block
        // -> family must be dirty and has a size of 1
        val w = world {
            components {
                add(::WorldTestComponent)
            }

            systems {
                add(::WorldTestInitSystem)
            }
        }

        val wFamily = w.family(allOf = arrayOf(WorldTestComponent::class))

        assertTrue(wFamily.isDirty)
        assertEquals(1, wFamily.numEntities)
    }

    @Test
    fun getFamilyWithinSystemConstructor() {
        // WorldTestInitSystemExtraFamily creates an entity in its init block and
        // also a family with a different configuration that the system itself
        // -> system family is empty and extra family contains 1 entity
        val w = world {
            components {
                add(::WorldTestComponent2)
                add(::WorldTestComponent)
            }

            systems {
                add(::WorldTestInitSystemExtraFamily)
            }
        }
        val s = w.system<WorldTestInitSystemExtraFamily>()

        assertEquals(1, s.extraFamily.numEntities)
        assertEquals(0, s.family.numEntities)
    }

    @Test
    fun iterateOverFamily() {
        val w = world {
            components {
                add(::WorldTestComponent)
            }
        }
        val e1 = w.entity { add<WorldTestComponent>() }
        val e2 = w.entity { add<WorldTestComponent>() }
        val f = w.family(allOf = arrayOf(WorldTestComponent::class))
        val actualEntities = mutableListOf<Entity>()

        f.forEach { actualEntities.add(it) }

        assertTrue(actualEntities.containsAll(arrayListOf(e1, e2)))
    }

    @Test
    fun sortedIterationOverFamily() {
        val w = world {
            components {
                add(::WorldTestComponent)
            }
        }
        val e1 = w.entity { add<WorldTestComponent> { x = 15f } }
        val e2 = w.entity { add<WorldTestComponent> { x = 10f } }
        val f = w.family(allOf = arrayOf(WorldTestComponent::class))
        val actualEntities = mutableListOf<Entity>()
        val mapper = w.mapper<WorldTestComponent>()

        f.sort(compareEntity { entity1, entity2 -> mapper[entity1].x.compareTo(mapper[entity2].x) })
        f.forEach { actualEntities.add(it) }

        assertEquals(arrayListOf(e2, e1), actualEntities)
    }

    @Test
    fun cannotCreateFamilyWithoutAnyConfiguration() {
        val w = world {}

        assertFailsWith<FleksFamilyException> { w.family() }
        assertFailsWith<FleksFamilyException> { w.family(arrayOf(), arrayOf(), arrayOf()) }
    }

    @Test
    fun createWorldWithComponentListener() {
        val w = world {
            components {
                add(::WorldTestComponent, ::WorldTestComponentListener)
            }
        }
        val actualListeners = w.componentService.mapper<WorldTestComponent>().listeners

        assertEquals(1, actualListeners.size)
        assertEquals(w, (actualListeners[0] as WorldTestComponentListener).world)
    }

    @Test
    fun cannotAddSameComponentTwice() {
        assertFailsWith<FleksComponentAlreadyAddedException> {
            world {
                components {
                    add(::WorldTestComponent)
                    add(::WorldTestComponent)
                }
            }
        }
    }

    @Test
    fun notifyComponentListenerDuringSystemCreation() {
        val w = world {
            systems {
                add(::WorldTestInitSystem)
            }

            components {
                add(::WorldTestComponent, ::WorldTestComponentListener)
            }
        }
        val listener = w.mapper<WorldTestComponent>().listeners[0] as WorldTestComponentListener

        assertEquals(1, listener.numAdd)
        assertEquals(0, listener.numRemove)
    }

    @Test
    fun createWorldWithFamilyListener() {
        val w = world {
            components {
                add(::WorldTestComponent)
            }

            families {
                add(::WorldTestFamilyListener)
            }
        }
        val actualListeners = w.family(allOf = arrayOf(WorldTestComponent::class)).listeners

        assertEquals(1, actualListeners.size)
        assertEquals(w, (actualListeners[0] as WorldTestFamilyListener).world)
    }

    @Test
    fun cannotAddSameFamilyListenerTwice() {
        assertFailsWith<FleksFamilyListenerAlreadyAddedException> {
            world {
                families {
                    add(::WorldTestFamilyListener)
                    add(::WorldTestFamilyListener)
                }
            }
        }
    }

    @Test
    fun cannotCreateFamilyListenerWithoutComponentConfiguration() {
        assertFailsWith<FleksFamilyListenerCreationException> {
            world {
                families {
                    add(::WorldTestFamilyListenerMissingCfg)
                }
            }
        }
    }

    @Test
    fun notifyFamilyListenerDuringSystemCreation() {
        val w = world {
            components {
                add(::WorldTestComponent)
            }

            systems {
                add(::WorldTestInitSystem)
            }

            families {
                add(::WorldTestFamilyListener)
            }
        }
        val listener = w.family(allOf = arrayOf(WorldTestComponent::class)).listeners[0] as WorldTestFamilyListener

        assertEquals(1, listener.numAdd)
        assertEquals(0, listener.numRemove)
        // verify that listener and system are not creating the same family twice
        assertEquals(1, w.allFamilies.size)
    }

    @Test
    fun testFamilyFirstAndEmptyFunctions() {
        val w = world {
            components {
                add(::WorldTestComponent)
            }
        }

        val f = w.family(allOf = arrayOf(WorldTestComponent::class))
        assertTrue(f.isEmpty)
        assertFalse(f.isNotEmpty)
        assertFailsWith<NoSuchElementException> { f.first() }
        assertNull(f.firstOrNull())

        val e = w.entity { add<WorldTestComponent>() }
        assertFalse(f.isEmpty)
        assertTrue(f.isNotEmpty)
        assertEquals(e, f.first())
        assertEquals(e, f.firstOrNull())
    }

    @Test
    fun testFamilyFirstDuringIterationWithModifications() {
        val w = world {
            components {
                add(::WorldTestComponent)
            }
        }
        val f = w.family(allOf = arrayOf(WorldTestComponent::class))
        // create entity with id 0 that is not part of family because 0 is the default value for IntBag
        // and could potentially lead to a false verification in this test case
        w.entity { }
        val e1 = w.entity { add<WorldTestComponent>() }
        val e2 = w.entity { add<WorldTestComponent>() }
        val e3 = w.entity { add<WorldTestComponent>() }
        val expectedEntities = listOf(e3, e2, e1)

        val actualEntities = mutableListOf<Entity>()
        f.forEach { entity ->
            if (actualEntities.isEmpty()) {
                // remove second entity on first iteration
                // this will not flag the family as 'dirty' because removal is delayed
                w.remove(e2)
                // that's why we add an entity to flag the family
                w.entity { add<WorldTestComponent>() }
            }
            // a call to 'first' updates the entities bag of a family internally
            // but should not mess up current iteration
            f.first()
            actualEntities.add(entity)
        }

        assertContentEquals(expectedEntities, actualEntities)
        assertEquals(3, f.numEntities)
    }

    @Test
    fun testEntityRemovalWithNoneOfFamily() {
        // entity that gets removed has no components and is therefore
        // part of any family that only has a noneOf configuration.
        // However, such entities still need to be removed of those families.
        val w = world {
            components {
                add(::WorldTestComponent)
            }
        }
        val family = w.family(noneOf = arrayOf(WorldTestComponent::class))
        val e = w.entity { }

        family.updateActiveEntities()
        assertTrue(e.id in family.entitiesBag)

        w.remove(e)
        family.updateActiveEntities()
        assertFalse(e.id in family.entitiesBag)
    }

    @Test
    fun testSnapshot() {
        val w = world {
            components {
                add(::WorldTestComponent)
                add(::WorldTestComponent2)
            }
        }
        lateinit var comp1: Any
        val e1 = w.entity { comp1 = add<WorldTestComponent>() }
        val e2 = w.entity { }
        lateinit var comp31: Any
        lateinit var comp32: Any
        val e3 = w.entity {
            comp31 = add<WorldTestComponent>()
            comp32 = add<WorldTestComponent2>()
        }
        val expected = mapOf(
            e1 to listOf(comp1),
            e2 to emptyList(),
            e3 to listOf(comp31, comp32)
        )

        val actual = w.snapshot()

        assertEquals(expected.size, actual.size)
        expected.forEach { (entity, expectedComps) ->
            val actualComps = actual[entity]
            assertNotNull(actualComps)
            assertEquals(expectedComps.size, actualComps.size)
            assertTrue(expectedComps.containsAll(actualComps) && actualComps.containsAll(expectedComps))
        }
    }

    @Test
    fun testSnapshotOf() {
        val w = world {
            components {
                add(::WorldTestComponent)
            }
        }
        lateinit var comp1: WorldTestComponent
        val e1 = w.entity { comp1 = add() }
        val e2 = w.entity { }
        val expected1 = listOf<Any>(comp1)
        val expected2 = emptyList<Any>()

        assertEquals(expected1, w.snapshotOf(e1))
        assertEquals(expected2, w.snapshotOf(e2))
        assertEquals(expected2, w.snapshotOf(Entity(42)))
    }

    @Test
    fun testLoadEmptySnapshot() {
        val w = world { }
        // loading snapshot will remove any existing entity
        w.entity { }

        w.loadSnapshot(emptyMap())

        assertEquals(0, w.numEntities)
    }

    @Test
    fun testLoadSnapshotWhileFamilyIterationInProcess() {
        val w = world {
            components {
                add(::WorldTestComponent)
            }
        }
        val f = w.family(allOf = arrayOf(WorldTestComponent::class))
        w.entity { add<WorldTestComponent>() }

        f.forEach {
            assertFailsWith<FleksSnapshotException> { w.loadSnapshot(emptyMap()) }
        }
    }

    @Test
    fun testLoadSnapshotWithOneEntity() {
        val w = world {
            components {
                add(::WorldTestComponent)
            }
        }
        val entity = Entity(0)
        val comps = listOf(WorldTestComponent())
        val snapshot = mapOf(entity to comps)

        w.loadSnapshot(snapshot)
        val actual = w.snapshotOf(entity)

        assertEquals(1, w.numEntities)
        assertEquals(actual, comps)
    }

    @Test
    fun testLoadSnapshotWithThreeEntities() {
        val w = world {
            injectables {
                add("42")
            }

            components {
                add(::WorldTestComponent, ::WorldTestComponentListener)
                add(::WorldTestComponent2)
            }

            systems {
                add(::WorldTestIteratingSystem)
            }
        }
        val compListener = w.componentService.mapper<WorldTestComponent>().listeners[0] as WorldTestComponentListener
        val snapshot = mapOf(
            Entity(3) to listOf(WorldTestComponent(), WorldTestComponent2()),
            Entity(5) to listOf(WorldTestComponent()),
            Entity(7) to listOf()
        )

        w.loadSnapshot(snapshot)
        val actual = w.snapshot()
        w.update(1f)

        // 3 entities should be loaded
        assertEquals(3, w.numEntities)
        // actual snapshot after loading the test snapshot is loaded should match
        assertEquals(snapshot.size, actual.size)
        snapshot.forEach { (entity, expectedComps) ->
            val actualComps = actual[entity]
            assertNotNull(actualComps)
            assertEquals(expectedComps.size, actualComps.size)
            assertTrue(expectedComps.containsAll(actualComps) && actualComps.containsAll(expectedComps))
        }
        // 2 out of 3 loaded entities should be part of the IteratingSystem family
        assertEquals(2, w.system<WorldTestIteratingSystem>().numCallsEntity)
        // 2 out of 3 loaded entities should notify the WorldTestComponentListener
        assertEquals(2, compListener.numAdd)
        assertEquals(0, compListener.numRemove)
        assertEquals(3, actual.size)
    }

    @Test
    fun testCreateEntityAfterSnapshotLoaded() {
        val w = world { }
        val snapshot = mapOf(
            Entity(1) to listOf<Any>()
        )

        w.loadSnapshot(snapshot)

        // first created entity should be recycled Entity 0
        assertEquals(Entity(0), w.entity())
        // next created entity should be new Entity 2
        assertEquals(Entity(2), w.entity())
    }
}
