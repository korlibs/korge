package com.github.quillraven.fleks

import kotlin.reflect.KClass
import kotlin.test.*

internal class ComponentTest {

    private data class ComponentTestComponent(var x: Int = 0)

    private class ComponentTestComponentListener : ComponentListener<ComponentTestComponent> {
        var numAddCalls = 0
        var numRemoveCalls = 0
        lateinit var cmpCalled: ComponentTestComponent
        var entityCalled = Entity(-1)
        var lastCall = ""

        override fun onComponentAdded(entity: Entity, component: ComponentTestComponent) {
            numAddCalls++
            cmpCalled = component
            entityCalled = entity
            lastCall = "add"
        }

        override fun onComponentRemoved(entity: Entity, component: ComponentTestComponent) {
            numRemoveCalls++
            cmpCalled = component
            entityCalled = entity
            lastCall = "remove"
        }
    }

    private val componentFactory = mutableMapOf<KClass<*>, () -> Any>()

    private inline fun <reified T : Any> initComponentFactory(noinline compFactory: () -> T) {
        val compType = T::class

        if (compType in componentFactory) {
            throw FleksComponentAlreadyAddedException(compType)
        }
        componentFactory[compType] = compFactory
    }

    init {
        initComponentFactory(::ComponentTestComponent)
    }

    @Test
    fun addEntityToMapperWithSufficientCapacity() {
        val cmpService = ComponentService(componentFactory)
        val mapper = cmpService.mapper<ComponentTestComponent>()
        val entity = Entity(0)

        val cmp = mapper.addInternal(entity) { x = 5 }

        assertTrue(entity in mapper)
        assertEquals(5, cmp.x)
    }

    @Test
    fun addEntityToMapperWithInsufficientCapacity() {
        val cmpService = ComponentService(componentFactory)
        val mapper = cmpService.mapper<ComponentTestComponent>()
        val entity = Entity(10_000)

        val cmp = mapper.addInternal(entity)

        assertTrue(entity in mapper)
        assertEquals(0, cmp.x)
    }

    @Test
    fun addAlreadyExistingEntityToMapper() {
        val cmpService = ComponentService(componentFactory)
        val mapper = cmpService.mapper<ComponentTestComponent>()
        val entity = Entity(10_000)
        val expected = mapper.addInternal(entity)

        val actual = mapper.addInternal(entity) { x = 2 }

        assertSame(expected, actual)
        assertEquals(2, actual.x)
    }

    @Test
    fun returnsFalseWhenEntityIsNotPartOfMapper() {
        val cmpService = ComponentService(componentFactory)
        val mapper = cmpService.mapper<ComponentTestComponent>()

        assertFalse(Entity(0) in mapper)
        assertFalse(Entity(10_000) in mapper)
    }

    @Test
    fun removeExistingEntityFromMapper() {
        val cmpService = ComponentService(componentFactory)
        val mapper = cmpService.mapper<ComponentTestComponent>()
        val entity = Entity(0)
        mapper.addInternal(entity)

        mapper.removeInternal(entity)

        assertFalse(entity in mapper)
    }

    @Test
    fun getComponentOfExistingEntity() {
        val cmpService = ComponentService(componentFactory)
        val mapper = cmpService.mapper<ComponentTestComponent>()
        val entity = Entity(0)
        mapper.addInternal(entity) { x = 2 }

        val cmp = mapper[entity]

        assertEquals(2, cmp.x)
    }

    @Test
    fun cannotGetComponentOfNonExistingEntity() {
        val cmpService = ComponentService(componentFactory)
        val mapper = cmpService.mapper<ComponentTestComponent>()
        val entity = Entity(0)

        assertFailsWith<FleksNoSuchEntityComponentException> { mapper[entity] }
    }

    @Test
    fun getComponentOfNonExistingEntityWithSufficientCapacity() {
        val cmpService = ComponentService(componentFactory)
        val mapper = cmpService.mapper<ComponentTestComponent>()
        val entity = Entity(0)

        val cmp = mapper.getOrNull(entity)

        assertNull(cmp)
    }

    @Test
    fun getComponentOfNonExistingEntityWithoutSufficientCapacity() {
        val cmpService = ComponentService(componentFactory)
        val mapper = cmpService.mapper<ComponentTestComponent>()
        val entity = Entity(2048)

        val cmp = mapper.getOrNull(entity)

        assertNull(cmp)
    }

    @Test
    fun getComponentOfExistingEntityViaGetOrNull() {
        val cmpService = ComponentService(componentFactory)
        val mapper = cmpService.mapper<ComponentTestComponent>()
        val entity = Entity(0)
        mapper.addInternal(entity) { x = 2 }

        val cmp = mapper.getOrNull(entity)

        assertEquals(2, cmp?.x)
    }

    @Test
    fun createNewMapper() {
        val cmpService = ComponentService(componentFactory)

        val mapper = cmpService.mapper<ComponentTestComponent>()

        assertEquals(0, mapper.id)
    }

    @Test
    fun doNotCreateTheSameMapperTwice() {
        val cmpService = ComponentService(componentFactory)
        val expected = cmpService.mapper<ComponentTestComponent>()

        val actual = cmpService.mapper<ComponentTestComponent>()

        assertSame(expected, actual)
    }

    @Test
    fun getMapperByComponentId() {
        val cmpService = ComponentService(componentFactory)
        val expected = cmpService.mapper<ComponentTestComponent>()

        val actual = cmpService.mapper(0)

        assertSame(expected, actual)
    }

    @Test
    fun addComponentListener() {
        val cmpService = ComponentService(componentFactory)
        val listener = ComponentTestComponentListener()
        val mapper = cmpService.mapper<ComponentTestComponent>()

        mapper.addComponentListenerInternal(listener)

        assertTrue(listener in mapper)
    }

    @Test
    fun removeComponentListener() {
        val cmpService = ComponentService(componentFactory)
        val listener = ComponentTestComponentListener()
        val mapper = cmpService.mapper<ComponentTestComponent>()
        mapper.addComponentListenerInternal(listener)

        mapper.removeComponentListener(listener)

        assertFalse(listener in mapper)
    }

    @Test
    fun addComponentWithComponentListener() {
        val cmpService = ComponentService(componentFactory)
        val mapper = cmpService.mapper<ComponentTestComponent>()
        val listener = ComponentTestComponentListener()
        mapper.addComponentListener(listener)
        val expectedEntity = Entity(1)

        val expectedCmp = mapper.addInternal(expectedEntity)

        assertEquals(1, listener.numAddCalls)
        assertEquals(0, listener.numRemoveCalls)
        assertEquals(expectedEntity, listener.entityCalled)
        assertEquals(expectedCmp, listener.cmpCalled)
    }

    @Test
    fun addComponentWithComponentListenerWhenComponentAlreadyPresent() {
        val cmpService = ComponentService(componentFactory)
        val mapper = cmpService.mapper<ComponentTestComponent>()
        val expectedEntity = Entity(1)
        mapper.addInternal(expectedEntity)
        val listener = ComponentTestComponentListener()
        mapper.addComponentListener(listener)

        val expectedCmp = mapper.addInternal(expectedEntity)

        assertEquals(1, listener.numAddCalls)
        assertEquals(1, listener.numRemoveCalls)
        assertEquals(expectedEntity, listener.entityCalled)
        assertEquals(expectedCmp, listener.cmpCalled)
        assertEquals("add", listener.lastCall)
    }

    @Test
    fun addComponentIfItDoesNotExistYet() {
        val cmpService = ComponentService(componentFactory)
        val mapper = cmpService.mapper<ComponentTestComponent>()
        val entity = Entity(1)

        val cmp = mapper.addOrUpdateInternal(entity) { x++ }

        assertTrue(entity in mapper)
        assertEquals(1, cmp.x)
    }

    @Test
    fun updateComponentIfItAlreadyExists() {
        val cmpService = ComponentService(componentFactory)
        val mapper = cmpService.mapper<ComponentTestComponent>()
        val entity = Entity(1)
        val expectedCmp = mapper.addOrUpdateInternal(entity) { x++ }

        val actualCmp = mapper.addOrUpdateInternal(entity) { x++ }

        assertTrue(entity in mapper)
        assertEquals(expectedCmp, actualCmp)
        assertEquals(2, actualCmp.x)
    }
}
