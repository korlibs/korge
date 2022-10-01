package com.github.quillraven.fleks

import com.github.quillraven.fleks.collection.BitArray
import com.github.quillraven.fleks.collection.IntBag
import com.github.quillraven.fleks.collection.bag

/**
 * An entity of a [world][World]. It represents a unique id.
 */
data class Entity(val id: Int)

/**
 * Interface of an [entity][Entity] listener that gets notified when the component configuration changes.
 * The [onEntityCfgChanged] function gets also called when an [entity][Entity] gets created and removed.
 */
interface EntityListener {
    /**
     * Function that gets called when an [entity's][Entity] component configuration changes.
     * This happens when a component gets added or removed or the [entity] gets added or removed from the [world][World].
     *
     * @param entity the [entity][Entity] with the updated component configuration.
     *
     * @param compMask the [BitArray] representing what type of components the entity has. Each component type has a
     * unique id. Refer to [ComponentMapper] for more details.
     */
    fun onEntityCfgChanged(entity: Entity, compMask: BitArray) = Unit

    /**
     * Function that gets called when an [entity][Entity] gets removed.
     *
     * @param entity the [entity][Entity] that gets removed.
     */
    fun onEntityRemoved(entity: Entity) = Unit
}

@DslMarker
annotation class EntityCfgMarker

/**
 * A DSL class to add components to a newly created [entity][Entity].
 */
@EntityCfgMarker
class EntityCreateCfg(
    @PublishedApi
    internal val compService: ComponentService
) {
    @PublishedApi
    internal var entity = Entity(0)

    @PublishedApi
    internal lateinit var compMask: BitArray

    /**
     * Adds and returns a component of the given type to the [entity] and
     * applies the [configuration] to the component.
     * Notifies any registered [ComponentListener].
     */
    inline fun <reified T : Any> add(configuration: T.() -> Unit = {}): T {
        val mapper = compService.mapper<T>()
        compMask.set(mapper.id)
        return mapper.addInternal(entity, configuration)
    }
}

/**
 * A DSL class to update components of an already existing [entity][Entity].
 * It contains extension functions for [ComponentMapper] which is how the component configuration of
 * existing entities is changed. This usually happens within [IteratingSystem] classes.
 */
@EntityCfgMarker
class EntityUpdateCfg {
    @PublishedApi
    internal lateinit var compMask: BitArray

    /**
     * Adds and returns a component of the given type to the [entity] and applies the [configuration] to that component.
     * If the [entity] already has a component of the given type then no new component is created and instead
     * the existing one will be updated.
     * Notifies any registered [ComponentListener].
     */
    inline fun <reified T : Any> ComponentMapper<T>.add(entity: Entity, configuration: T.() -> Unit = {}): T {
        compMask.set(this.id)
        return this.addInternal(entity, configuration)
    }

    /**
     * Adds a new component of the given type to the [entity] if it does not have it yet.
     * Otherwise, updates the already existing component.
     * Applies the [configuration] in both cases and returns the component.
     * Notifies any registered [ComponentListener] if a new component is created.
     */
    inline fun <reified T : Any> ComponentMapper<T>.addOrUpdate(entity: Entity, configuration: T.() -> Unit = {}): T {
        compMask.set(this.id)
        return this.addOrUpdateInternal(entity, configuration)
    }

    /**
     * Removes a component of the given type from the [entity].
     * Notifies any registered [ComponentListener].
     *
     * @throws [IndexOutOfBoundsException] if the id of the [entity] exceeds the mapper's capacity.
     */
    inline fun <reified T : Any> ComponentMapper<T>.remove(entity: Entity) {
        compMask.clear(this.id)
        this.removeInternal(entity)
    }
}

/**
 * A service class that is responsible for creation and removal of [entities][Entity].
 * It also stores the component configuration of each entity as a [BitArray] to have quick access to
 * what kind of components an entity has or doesn't have.
 */
class EntityService(
    initialEntityCapacity: Int,
    private val compService: ComponentService
) {
    /**
     * The id that will be given to a newly created [entity][Entity] if there are no [recycledEntities].
     */
    @PublishedApi
    internal var nextId = 0

    /**
     * Separate BitArray to remember if an [entity][Entity] was already removed.
     * This is faster than looking up the [recycledEntities].
     */
    @PublishedApi
    internal val removedEntities = BitArray(initialEntityCapacity)

    /**
     * The already removed [entities][Entity] which can be reused whenever a new entity is needed.
     */
    @PublishedApi
    internal val recycledEntities = ArrayDeque<Entity>()

    /**
     * Returns the amount of active entities.
     */
    val numEntities: Int
        get() = nextId - recycledEntities.size

    /**
     * Returns the maximum capacity of active entities.
     */
    val capacity: Int
        get() = compMasks.capacity

    /**
     * The component configuration per [entity][Entity].
     */
    @PublishedApi
    internal val compMasks = bag<BitArray>(initialEntityCapacity)

    @PublishedApi
    internal val createCfg = EntityCreateCfg(compService)

    @PublishedApi
    internal val updateCfg = EntityUpdateCfg()

    @PublishedApi
    internal val listeners = bag<EntityListener>()

    /**
     * Flag that indicates if an iteration of an [IteratingSystem] is currently in progress.
     * In such cases entities will not be removed immediately.
     * Refer to [IteratingSystem.onTick] for more details.
     */
    @PublishedApi
    internal var delayRemoval = false

    /**
     * The entities that get removed at the end of an [IteratingSystem] iteration.
     */
    private val delayedEntities = IntBag()

    /**
     * Creates and returns a new [entity][Entity] and applies the given [configuration].
     * If there are [recycledEntities] then they will be preferred over creating new entities.
     * Notifies any registered [EntityListener].
     */
    inline fun create(configuration: EntityCreateCfg.(Entity) -> Unit): Entity {
        val newEntity = if (recycledEntities.isEmpty()) {
            Entity(nextId++)
        } else {
            val recycled = recycledEntities.removeLast()
            removedEntities.clear(recycled.id)
            recycled
        }

        if (newEntity.id >= compMasks.size) {
            compMasks[newEntity.id] = BitArray(64)
        }
        val compMask = compMasks[newEntity.id]
        createCfg.run {
            this.entity = newEntity
            this.compMask = compMask
            configuration(this.entity)
        }
        listeners.forEach { it.onEntityCfgChanged(newEntity, compMask) }

        return newEntity
    }

    /**
     * Updates an [entity] with the given [configuration].
     * Notifies any registered [EntityListener].
     */
    inline fun configureEntity(entity: Entity, configuration: EntityUpdateCfg.(Entity) -> Unit) {
        val compMask = compMasks[entity.id]
        updateCfg.run {
            this.compMask = compMask
            configuration(entity)
        }
        listeners.forEach { it.onEntityCfgChanged(entity, compMask) }
    }

    /**
     * Updates an [entity] with the given [components].
     * Notifies any registered [EntityListener].
     * This function is only used by [World.loadSnapshot].
     */
    internal fun configureEntity(entity: Entity, components: List<Any>) {
        val compMask = compMasks[entity.id]
        components.forEach { cmp ->
            val mapper = compService.mapper(cmp::class)
            mapper.addInternal(entity, cmp)
            compMask.set(mapper.id)
        }
        listeners.forEach { it.onEntityCfgChanged(entity, compMask) }
    }

    /**
     * Recycles the given [entity] by adding it to the [recycledEntities]
     * and also resetting its component mask with an empty [BitArray].
     * This function is only used by [World.loadSnapshot].
     */
    internal fun recycle(entity: Entity) {
        recycledEntities.add(entity)
        removedEntities.set(entity.id)
        compMasks[entity.id] = BitArray(64)
    }

    /**
     * Removes the given [entity] and adds it to the [recycledEntities] for future use.
     *
     * If [delayRemoval] is set then the [entity] is not removed immediately and instead will be cleaned up
     * within the [cleanupDelays] function.
     *
     * Notifies any registered [EntityListener] when the [entity] gets removed.
     */
    fun remove(entity: Entity) {
        if (removedEntities[entity.id]) {
            // entity is already removed
            return
        }

        if (delayRemoval) {
            delayedEntities.add(entity.id)
        } else {
            removedEntities.set(entity.id)
            val compMask = compMasks[entity.id]
            recycledEntities.add(entity)
            compMask.forEachSetBit { compId ->
                compService.mapper(compId).removeInternal(entity)
            }
            compMask.clearAll()
            listeners.forEach { it.onEntityRemoved(entity) }
        }
    }

    /**
     * Removes all [entities][Entity] and adds them to the [recycledEntities] for future use.
     * If [clearRecycled] is true then the recycled entities are cleared and the ids for newly
     * created entities start at 0 again.
     *
     * Refer to [remove] for more details.
     */
    fun removeAll(clearRecycled: Boolean = false) {
        for (id in 0 until nextId) {
            val entity = Entity(id)
            if (removedEntities[entity.id]) {
                continue
            }
            remove(entity)
        }

        if (clearRecycled) {
            nextId = 0
            recycledEntities.clear()
            removedEntities.clearAll()
            compMasks.clear()
        }
    }

    /**
     * Returns true if and only if the [entity] is not removed and is part of the [EntityService].
     */
    operator fun contains(entity: Entity): Boolean {
        return entity.id in 0 until nextId && !removedEntities[entity.id]
    }

    /**
     * Performs the given [action] on each active [entity][Entity].
     */
    fun forEach(action: (Entity) -> Unit) {
        for (id in 0 until nextId) {
            val entity = Entity(id)
            if (removedEntities[entity.id]) {
                continue
            }
            entity.run(action)
        }
    }

    /**
     * Clears the [delayRemoval] flag and removes [entities][Entity] which are part of the [delayedEntities].
     */
    fun cleanupDelays() {
        delayRemoval = false
        if (delayedEntities.isNotEmpty) {
            delayedEntities.forEach { remove(Entity(it)) }
            delayedEntities.clear()
        }
    }

    /**
     * Adds the given [listener] to the list of [EntityListener].
     */
    fun addEntityListener(listener: EntityListener) = listeners.add(listener)

    /**
     * Removes the given [listener] from the list of [EntityListener].
     */
    fun removeEntityListener(listener: EntityListener) = listeners.removeValue(listener)

    /**
     * Returns true if and only if the given [listener] is part of the list of [EntityListener].
     */
    operator fun contains(listener: EntityListener) = listener in listeners
}
