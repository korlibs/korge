package com.github.quillraven.fleks

import com.github.quillraven.fleks.collection.BitArray
import com.github.quillraven.fleks.collection.EntityComparator
import com.github.quillraven.fleks.collection.IntBag
import com.github.quillraven.fleks.collection.bag
import kotlin.native.concurrent.ThreadLocal
import kotlin.reflect.KClass

/**
 * Abstract class of a [family][Family] listener that gets notified when an
 * [entity][Entity] gets added to, or removed from a family.
 *
 * @param allOfComponents is specifying the family to which this system belongs.
 * @param noneOfComponents is specifying the family to which this system belongs.
 * @param anyOfComponents is specifying the family to which this system belongs.
 */
abstract class FamilyListener(
    allOfComponents: Array<KClass<*>>? = null,
    noneOfComponents: Array<KClass<*>>? = null,
    anyOfComponents: Array<KClass<*>>? = null,
) {
    init {
        CURRENT_FAMILY = World.CURRENT_WORLD.family(allOfComponents, noneOfComponents, anyOfComponents)
    }

    /**
     * Function that gets called when an [entity][Entity] gets added to a [family][Family].
     */
    open fun onEntityAdded(entity: Entity) = Unit

    /**
     * Function that gets called when an [entity][Entity] gets removed from a [family][Family].
     */
    open fun onEntityRemoved(entity: Entity) = Unit

    @ThreadLocal
    companion object {
        internal lateinit var CURRENT_FAMILY: Family
    }
}


/**
 * A family of [entities][Entity]. It stores [entities][Entity] that have a specific configuration of components.
 * A configuration is defined via the three [IteratingSystem] properties "allOf", "noneOf" and "anyOf".
 * Each component is assigned to a unique index. That index is set in the [allOf], [noneOf] or [anyOf][] [BitArray].
 *
 * A family is an [EntityListener] and gets notified when an [entity][Entity] is added to the world or the
 * entity's component configuration changes.
 *
 * Every [IteratingSystem] is linked to exactly one family. Families are created by the [SystemService] automatically
 * when a [world][World] gets created.
 *
 * @param allOf all the components that an [entity][Entity] must have. Default value is null.
 * @param noneOf all the components that an [entity][Entity] must not have. Default value is null.
 * @param anyOf the components that an [entity][Entity] must have at least one. Default value is null.
 */
data class Family(
    internal val allOf: BitArray? = null,
    internal val noneOf: BitArray? = null,
    internal val anyOf: BitArray? = null,
    @PublishedApi
    internal val entityService: EntityService,
) : EntityListener {
    /**
     * Return the [entities] in form of an [IntBag] for better iteration performance.
     */
    @PublishedApi
    internal val entitiesBag = IntBag()

    /**
     * Returns the [entities][Entity] that belong to this family.
     */
    private val entities = BitArray(1)

    /**
     * Returns the number of [entities][Entity] that belong to this family.
     * This can be an expensive call if the amount of entities is very high because it
     * iterates through the entire underlying [BitArray].
     */
    val numEntities: Int
        get() = entities.numBits()

    /**
     * Returns true if and only if this [Family] does not contain any entity.
     */
    val isEmpty: Boolean
        get() = entities.isEmpty

    /**
     * Returns true if and only if this [Family] contains at least one entity.
     */
    val isNotEmpty: Boolean
        get() = entities.isNotEmpty

    /**
     * Flag to indicate if there are changes in the [entities]. If it is true then the [entitiesBag] should get
     * updated via a call to [updateActiveEntities].
     *
     * Refer to [IteratingSystem.onTick] for an example implementation.
     */
    @PublishedApi
    internal var isDirty = false
        private set

    @PublishedApi
    internal val listeners = bag<FamilyListener>()

    /**
     * Returns true if the specified [compMask] matches the family's component configuration.
     *
     * @param compMask the component configuration of an [entity][Entity].
     */
    operator fun contains(compMask: BitArray): Boolean {
        return (allOf == null || compMask.contains(allOf))
            && (noneOf == null || !compMask.intersects(noneOf))
            && (anyOf == null || compMask.intersects(anyOf))
    }

    /**
     * Updates the [entitiesBag] and clears the [isDirty] flag if needed.
     */
    @PublishedApi
    internal fun updateActiveEntities() {
        if (isDirty) {
            isDirty = false
            entities.toIntBag(entitiesBag)
        }
    }

    /**
     * Updates this family if needed and runs the given [action] for all [entities][Entity].
     *
     * **Important note**: There is a potential risk when iterating over entities and one of those entities
     * gets removed. Removing the entity immediately and cleaning up its components could
     * cause problems because if you access a component which is mandatory for the family, you will get
     * a FleksNoSuchComponentException. To avoid that you could check if an entity really has the component
     * before accessing it but that is redundant in context of a family.
     *
     * To avoid these kinds of issues, entity removals are delayed until the end of the iteration. This also means
     * that a removed entity of this family will still be part of the [action] for the current iteration.
     */
    inline fun forEach(action: Family.(Entity) -> Unit) {
        updateActiveEntities()
        if (!entityService.delayRemoval) {
            entityService.delayRemoval = true
            entitiesBag.forEach { this.action(Entity(it)) }
            entityService.cleanupDelays()
        } else {
            entitiesBag.forEach { this.action(Entity(it)) }
        }
    }

    /**
     * Updates this family if needed and returns its first [Entity].
     * @throws [NoSuchElementException] if the family has no entities.
     */
    fun first(): Entity {
        if (!entityService.delayRemoval || entitiesBag.isEmpty) {
            // no iteration in process -> update entities if necessary
            updateActiveEntities()
        }

        return Entity(entitiesBag.first)
    }

    /**
     * Updates this family if needed and returns its first [Entity] or null if the family has no entities.
     */
    fun firstOrNull(): Entity? {
        if (!entityService.delayRemoval || entitiesBag.isEmpty) {
            // no iteration in process -> update entities if necessary
            updateActiveEntities()
        }

        val id = entitiesBag.firstOrNull ?: return null
        return Entity(id)
    }

    /**
     * Updates an [entity] using the given [configuration] to add and remove components.
     */
    inline fun configureEntity(entity: Entity, configuration: EntityUpdateCfg.(Entity) -> Unit) {
        entityService.configureEntity(entity, configuration)
    }

    /**
     * Sorts the [entities][Entity] of this family by the given [comparator].
     */
    fun sort(comparator: EntityComparator) {
        updateActiveEntities()
        entitiesBag.sort(comparator)
    }

    /**
     * Checks if the [entity] is part of the family by analyzing the entity's components.
     * The [compMask] is a [BitArray] that indicates which components the [entity] currently has.
     *
     * The [entity] gets either added to the [entities] or removed and [isDirty] is set when needed.
     */
    override fun onEntityCfgChanged(entity: Entity, compMask: BitArray) {
        val entityInFamily = compMask in this
        if (entityInFamily && !entities[entity.id]) {
            // new entity gets added
            isDirty = true
            entities.set(entity.id)
            listeners.forEach { it.onEntityAdded(entity) }
        } else if (!entityInFamily && entities[entity.id]) {
            // existing entity gets removed
            isDirty = true
            entities.clear(entity.id)
            listeners.forEach { it.onEntityRemoved(entity) }
        }
    }

    /**
     * Removes the [entity] of the family and sets the [isDirty] flag if and only
     * if the [entity] is already in the family.
     */
    override fun onEntityRemoved(entity: Entity) {
        if (entities[entity.id]) {
            // existing entity gets removed
            isDirty = true
            entities.clear(entity.id)
            listeners.forEach { it.onEntityRemoved(entity) }
        }
    }

    /**
     * Adds the given [listener] to the list of [FamilyListener].
     */
    fun addFamilyListener(listener: FamilyListener) = listeners.add(listener)

    /**
     * Removes the given [listener] from the list of [FamilyListener].
     */
    fun removeFamilyListener(listener: FamilyListener) = listeners.removeValue(listener)

    /**
     * Returns true if and only if the given [listener] is part of the list of [FamilyListener].
     */
    operator fun contains(listener: FamilyListener) = listener in listeners
}
