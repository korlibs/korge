package com.github.quillraven.fleks

import com.github.quillraven.fleks.collection.Bag
import com.github.quillraven.fleks.collection.bag
import kotlin.math.max

/**
 * Interface of a component listener that gets notified when a component of a specific type
 * gets added or removed from an [entity][Entity].
 */
interface ComponentListener<T> {
    fun onComponentAdded(entity: Entity, component: T)
    fun onComponentRemoved(entity: Entity, component: T)
}

/**
 * A class that is responsible to store components of a specific type for all [entities][Entity] in a [world][World].
 * Each component is assigned a unique [id] for fast access and to avoid lookups via a class which is slow.
 * Hint: A component at index [id] in the [components] array belongs to [Entity] with the same [id].
 *
 * Refer to [ComponentService] for more details.
 */
class ComponentMapper<T>(
    @PublishedApi
    internal val id: Int = 0,
    @PublishedApi
    @Suppress("UNCHECKED_CAST")
    internal var components: Array<T?> = Array<Any?>(64) { null } as Array<T?>,
    @PublishedApi
    internal val factory: () -> T
) {
    @PublishedApi
    internal val listeners = bag<ComponentListener<T>>(2)

    /**
     * Creates and returns a new component of the specific type for the given [entity] and applies the [configuration].
     * If the [entity] already has a component of that type then no new instance will be created.
     * Notifies any registered [ComponentListener].
     */
    @PublishedApi
    internal inline fun addInternal(entity: Entity, configuration: T.() -> Unit = {}): T {
        if (entity.id >= components.size) {
            components = components.copyOf(max(components.size * 2, entity.id + 1))
        }
        val comp = components[entity.id]
        return if (comp == null) {
            val newComp = factory.invoke().apply(configuration)
            components[entity.id] = newComp
            listeners.forEach { it.onComponentAdded(entity, newComp) }
            newComp
        } else {
            // component already added -> reuse it and do not create a new instance.
            // Call onComponentRemoved first in case users do something special in onComponentAdded.
            // Otherwise, onComponentAdded will be executed twice on a single component without executing onComponentRemoved
            // which is not correct.
            listeners.forEach { it.onComponentRemoved(entity, comp) }
            val existingComp = comp.apply(configuration)
            listeners.forEach { it.onComponentAdded(entity, existingComp) }
            existingComp
        }
    }

    /**
     * Creates a new component if the [entity] does not have it yet. Otherwise, updates the existing component.
     * Applies the [configuration] in both cases and returns the component.
     * Notifies any registered [ComponentListener] if a new component is created.
     */
    @PublishedApi
    internal inline fun addOrUpdateInternal(entity: Entity, configuration: T.() -> Unit = {}): T {
        return if (entity in this) {
            this[entity].apply(configuration)
        } else {
            addInternal(entity, configuration)
        }
    }

    /**
     * Removes a component of the specific type from the given [entity].
     * Notifies any registered [ComponentListener].
     *
     * @throws [ArrayIndexOutOfBoundsException] if the id of the [entity] exceeds the components' capacity.
     */
    @PublishedApi
    internal fun removeInternal(entity: Entity) {
        components[entity.id]?.let { comp ->
            listeners.forEach { it.onComponentRemoved(entity, comp) }
        }
        components[entity.id] = null
    }

    /**
     * Returns a component of the specific type of the given [entity].
     *
     * @throws [FleksNoSuchEntityComponentException] if the [entity] does not have such a component.
     */
    operator fun get(entity: Entity): T {
        return components[entity.id] ?: throw FleksNoSuchEntityComponentException(entity, factory.toString())
    }

    /**
     * Returns true if and only if the given [entity] has a component of the specific type.
     */
    operator fun contains(entity: Entity): Boolean = components.size > entity.id && components[entity.id] != null

    /**
     * Adds the given [listener] to the list of [ComponentListener].
     */
    fun addComponentListener(listener: ComponentListener<T>) = listeners.add(listener)

    /**
     * Adds the given [listener] to the list of [ComponentListener]. This function is only used internally
     * to add listeners through the [WorldConfiguration].
     */
    @Suppress("UNCHECKED_CAST")
    internal fun addComponentListenerInternal(listener: ComponentListener<*>) =
        addComponentListener(listener as ComponentListener<T>)

    /**
     * Removes the given [listener] from the list of [ComponentListener].
     */
    fun removeComponentListener(listener: ComponentListener<T>) = listeners.removeValue(listener)

    /**
     * Returns true if and only if the given [listener] is part of the list of [ComponentListener].
     */
    operator fun contains(listener: ComponentListener<T>) = listener in listeners

    override fun toString(): String {
        return "ComponentMapper(id=$id, component=${factory})"
    }
}

/**
 * A service class that is responsible for managing [ComponentMapper] instances.
 * It creates a [ComponentMapper] for every unique component type and assigns a unique id for each mapper.
 */
class ComponentService(
    componentFactory: Map<String, () -> Any>
) {
    /**
     * Returns map of [ComponentMapper] that stores mappers by its component type.
     * It is used by the [SystemService] during system creation and by the [EntityService] for entity creation.
     */
    @PublishedApi
    internal val mappers: Map<String, ComponentMapper<*>>

    /**
     * Returns [Bag] of [ComponentMapper]. The id of the mapper is the index of the bag.
     * It is used by the [EntityService] to fasten up the cleanup process of delayed entity removals.
     */
    private val mappersBag = bag<ComponentMapper<*>>()

    init {
        // Create component mappers with help of constructor functions from component factory
        mappers = componentFactory.mapValues {
            val compMapper = ComponentMapper(id = mappersBag.size, factory = it.value)
            mappersBag.add(compMapper)
            compMapper
        }
    }

    /**
     * Returns a [ComponentMapper] for the given [type].
     *
     * @throws [FleksNoSuchComponentException] if the component of the given [type] does not exist in the
     * world configuration.
     */
    fun mapper(type: String): ComponentMapper<*> {
        return mappers[type] ?: throw FleksNoSuchComponentException(type)
    }

    /**
     * Returns a [ComponentMapper] for the specific type.
     *
     * @throws [FleksNoSuchComponentException] if the component of the given [type] does not exist in the
     * world configuration.
     */
    @Suppress("UNCHECKED_CAST")
    inline fun <reified T : Any> mapper(): ComponentMapper<T> {
        val type = T::class.simpleName ?: throw FleksInjectableTypeHasNoName(T::class)
        return mapper(type) as ComponentMapper<T>
    }

    /**
     * Returns an already existing [ComponentMapper] for the given [compId].
     */
    fun mapper(compId: Int) = mappersBag[compId]
}
