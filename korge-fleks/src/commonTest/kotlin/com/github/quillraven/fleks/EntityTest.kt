package com.github.quillraven.fleks

import com.github.quillraven.fleks.collection.BitArray
import kotlin.reflect.KClass
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private class EntityTestListener : EntityListener {
    var numCalls = 0
    var entityReceived = Entity(-1)
    lateinit var cmpMaskReceived: BitArray

    override fun onEntityCfgChanged(entity: Entity, compMask: BitArray) {
        ++numCalls
        entityReceived = entity
        cmpMaskReceived = compMask
    }

    override fun onEntityRemoved(entity: Entity) {
        ++numCalls
        entityReceived = entity
    }
}

private data class EntityTestComponent(var x: Float = 0f)

internal class EntityTest {
    private val componentFactory = mutableMapOf<KClass<*>, () -> Any>()

    private inline fun <reified T : Any> initComponentFactory(noinline compFactory: () -> T) {
        val compType = T::class

        if (compType in componentFactory) {
            throw FleksComponentAlreadyAddedException(compType)
        }
        componentFactory[compType] = compFactory
    }

    init {
        initComponentFactory(::EntityTestComponent)
    }

    @Test
    fun createEmptyServiceFor32Entities() {
        val cmpService = ComponentService(componentFactory)

        val entityService = EntityService(32, cmpService)

        assertEquals(0, entityService.numEntities)
        assertEquals(32, entityService.capacity)
    }

    @Test
    fun createEntityWithoutConfigurationAndSufficientCapacity() {
        val cmpService = ComponentService(componentFactory)
        val entityService = EntityService(32, cmpService)

        val entity = entityService.create {}

        assertEquals(0, entity.id)
        assertEquals(1, entityService.numEntities)
    }

    @Test
    fun createEntityWithoutConfigurationAndInsufficientCapacity() {
        val cmpService = ComponentService(componentFactory)
        val entityService = EntityService(0, cmpService)

        val entity = entityService.create {}

        assertEquals(0, entity.id)
        assertEquals(1, entityService.numEntities)
        assertEquals(1, entityService.capacity)
    }

    @Test
    fun createEntityWithConfigurationAndCustomListener() {
        val cmpService = ComponentService(componentFactory)
        val entityService = EntityService(32, cmpService)
        val listener = EntityTestListener()
        entityService.addEntityListener(listener)
        var processedEntity = Entity(-1)

        val expectedEntity = entityService.create { entity ->
            add<EntityTestComponent>()
            processedEntity = entity
        }
        val mapper = cmpService.mapper<EntityTestComponent>()

        assertEquals(1, listener.numCalls)
        assertEquals(expectedEntity, listener.entityReceived)
        assertTrue(listener.cmpMaskReceived[0])
        assertEquals(0f, mapper[listener.entityReceived].x)
        assertEquals(expectedEntity, processedEntity)
    }

    @Test
    fun removeComponentFromEntityWithCustomListener() {
        val cmpService = ComponentService(componentFactory)
        val entityService = EntityService(32, cmpService)
        val listener = EntityTestListener()
        val expectedEntity = entityService.create { add<EntityTestComponent>() }
        val mapper = cmpService.mapper<EntityTestComponent>()
        entityService.addEntityListener(listener)

        entityService.configureEntity(expectedEntity) { mapper.remove(expectedEntity) }

        assertEquals(1, listener.numCalls)
        assertEquals(expectedEntity, listener.entityReceived)
        assertFalse(listener.cmpMaskReceived[0])
        assertFalse(expectedEntity in mapper)
    }

    @Test
    fun addComponentToEntityWithCustomListener() {
        val cmpService = ComponentService(componentFactory)
        val entityService = EntityService(32, cmpService)
        val listener = EntityTestListener()
        val expectedEntity = entityService.create { }
        val mapper = cmpService.mapper<EntityTestComponent>()
        entityService.addEntityListener(listener)

        entityService.configureEntity(expectedEntity) { mapper.add(expectedEntity) }

        assertEquals(1, listener.numCalls)
        assertEquals(expectedEntity, listener.entityReceived)
        assertTrue(listener.cmpMaskReceived[0])
        assertTrue(expectedEntity in mapper)
    }

    @Test
    fun updateComponentOfEntityIfItAlreadyExistsWithCustomListener() {
        val cmpService = ComponentService(componentFactory)
        val entityService = EntityService(32, cmpService)
        val listener = EntityTestListener()
        val expectedEntity = entityService.create { }
        val mapper = cmpService.mapper<EntityTestComponent>()
        entityService.addEntityListener(listener)

        entityService.configureEntity(expectedEntity) {
            mapper.add(expectedEntity) { ++x }
            mapper.addOrUpdate(expectedEntity) { x++ }
        }

        assertTrue(expectedEntity in mapper)
        assertEquals(2f, mapper[expectedEntity].x)
        assertEquals(1, listener.numCalls)
    }

    @Test
    fun removeEntityWithComponentImmediatelyWithCustomListener() {
        val cmpService = ComponentService(componentFactory)
        val entityService = EntityService(32, cmpService)
        val listener = EntityTestListener()
        val expectedEntity = entityService.create { add<EntityTestComponent>() }
        val mapper = cmpService.mapper<EntityTestComponent>()
        entityService.addEntityListener(listener)

        entityService.remove(expectedEntity)

        assertEquals(1, listener.numCalls)
        assertEquals(expectedEntity, listener.entityReceived)
        assertFalse(expectedEntity in mapper)
    }

    @Test
    fun removeAllEntities() {
        val entityService = EntityService(32, ComponentService(componentFactory))
        entityService.create {}
        entityService.create {}

        entityService.removeAll()

        assertEquals(2, entityService.recycledEntities.size)
        assertEquals(0, entityService.numEntities)
    }

    @Test
    fun removeAllEntitiesWithAlreadyRecycledEntities() {
        val entityService = EntityService(32, ComponentService(componentFactory))
        val recycled = entityService.create {}
        entityService.create {}
        entityService.remove(recycled)

        entityService.removeAll()

        assertEquals(2, entityService.recycledEntities.size)
        assertEquals(0, entityService.numEntities)
    }

    @Test
    fun removeAllEntitiesWhenRemovalIsDelayed() {
        val entityService = EntityService(32, ComponentService(componentFactory))
        entityService.create {}
        entityService.create {}
        entityService.delayRemoval = true
        val listener = EntityTestListener()
        entityService.addEntityListener(listener)

        entityService.removeAll()

        assertEquals(0, listener.numCalls)
        assertTrue(entityService.delayRemoval)
    }

    @Test
    fun createRecycledEntity() {
        val cmpService = ComponentService(componentFactory)
        val entityService = EntityService(32, cmpService)
        val expectedEntity = entityService.create { }
        entityService.remove(expectedEntity)

        val actualEntity = entityService.create { }

        assertEquals(expectedEntity, actualEntity)
    }

    @Test
    fun delayEntityRemoval() {
        val cmpService = ComponentService(componentFactory)
        val entityService = EntityService(32, cmpService)
        val entity = entityService.create { }
        val listener = EntityTestListener()
        entityService.addEntityListener(listener)
        entityService.delayRemoval = true

        entityService.remove(entity)

        assertEquals(0, listener.numCalls)
    }

    @Test
    fun removeDelayedEntity() {
        val cmpService = ComponentService(componentFactory)
        val entityService = EntityService(32, cmpService)
        val entity = entityService.create { }
        val listener = EntityTestListener()
        entityService.addEntityListener(listener)
        entityService.delayRemoval = true
        entityService.remove(entity)

        // call two times to make sure that removals are only processed once
        entityService.cleanupDelays()
        entityService.cleanupDelays()

        assertFalse(entityService.delayRemoval)
        assertEquals(1, listener.numCalls)
    }

    @Test
    fun removeExistingListener() {
        val cmpService = ComponentService(componentFactory)
        val entityService = EntityService(32, cmpService)
        val listener = EntityTestListener()
        entityService.addEntityListener(listener)

        entityService.removeEntityListener(listener)

        assertFalse(listener in entityService)
    }

    @Test
    fun removeEntityTwice() {
        val cmpService = ComponentService(componentFactory)
        val entityService = EntityService(32, cmpService)
        val entity = entityService.create { }
        val listener = EntityTestListener()
        entityService.addEntityListener(listener)

        entityService.remove(entity)
        entityService.remove(entity)

        assertEquals(1, entityService.recycledEntities.size)
        assertEquals(1, listener.numCalls)
    }

    @Test
    fun testContainsEntity() {
        val service = EntityService(32, ComponentService(componentFactory))
        val e1 = service.create { }
        val e2 = service.create { }
        service.remove(e2)
        val e3 = Entity(2)

        assertTrue(e1 in service)
        assertFalse(e2 in service)
        assertFalse(e3 in service)
    }
}
