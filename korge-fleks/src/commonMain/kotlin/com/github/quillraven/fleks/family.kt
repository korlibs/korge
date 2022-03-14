package com.github.quillraven.fleks

import com.github.quillraven.fleks.collection.BitArray
import com.github.quillraven.fleks.collection.EntityComparator
import com.github.quillraven.fleks.collection.IntBag
import kotlin.reflect.KClass

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
    internal val anyOf: BitArray? = null
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
     * Flag to indicate if there are changes in the [entities]. If it is true then the [entitiesBag] should get
     * updated via a call to [updateActiveEntities].
     *
     * Refer to [IteratingSystem.onTick] for an example implementation.
     */
    var isDirty = false
        private set

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
     * Updates the [entitiesBag] and clears the [isDirty] flag.
     * This should be called when [isDirty] is true.
     */
    fun updateActiveEntities() {
        isDirty = false
        entities.toIntBag(entitiesBag)
    }

    /**
     * Iterates over the [entities][Entity] of this family and runs the given [action].
     */
    inline fun forEach(action: (Entity) -> Unit) {
        entitiesBag.forEach { action(Entity(it)) }
    }

    /**
     * Sorts the [entities][Entity] of this family by the given [comparator].
     */
    fun sort(comparator: EntityComparator) {
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
        } else if (!entityInFamily && entities[entity.id]) {
            // existing entity gets removed
            isDirty = true
            entities.clear(entity.id)
        }
    }
}
