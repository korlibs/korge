package com.github.quillraven.fleks

import com.github.quillraven.fleks.collection.BitArray
import kotlin.native.concurrent.ThreadLocal
import kotlin.reflect.KClass

/**
 * Wrapper class for injectables of the [WorldConfiguration].
 * It is used in the [SystemService] to find out any unused injectables.
 */
data class Injectable(val injObj: Any, var used: Boolean = false)

@DslMarker
annotation class ComponentCfgMarker

/**
 * A DSL class to configure components and [ComponentListener] of a [WorldConfiguration].
 */
@ComponentCfgMarker
class ComponentConfiguration {
    @PublishedApi
    internal val compListenerFactory = mutableMapOf<KClass<*>, () -> ComponentListener<*>>()

    @PublishedApi
    internal val componentFactory = mutableMapOf<KClass<*>, () -> Any>()

    /**
     * Adds the specified component and its [ComponentListener] to the [world][World]. If a component listener
     * is not needed than it can be omitted.
     *
     * @param compFactory the constructor method for creating the component.
     * @param listenerFactory the constructor method for creating the component listener.
     * @throws [FleksComponentAlreadyAddedException] if the component was already added before.
     */
    inline fun <reified T : Any> add(
        noinline compFactory: () -> T,
        noinline listenerFactory: (() -> ComponentListener<T>)? = null
    ) {
        val compType = T::class

        if (compType in componentFactory) {
            throw FleksComponentAlreadyAddedException(compType)
        }
        componentFactory[compType] = compFactory
        if (listenerFactory != null) {
            // No need to check compType again in compListenerFactory - it is already guarded with check in componentFactory
            compListenerFactory[compType] = listenerFactory
        }
    }
}

@DslMarker
annotation class SystemCfgMarker

/**
 * A DSL class to configure [IntervalSystem] of a [WorldConfiguration].
 */
@SystemCfgMarker
class SystemConfiguration {
    @PublishedApi
    internal val systemFactory = mutableMapOf<KClass<*>, () -> IntervalSystem>()

    /**
     * Adds the specified [IntervalSystem] to the [world][World].
     * The order in which systems are added is the order in which they will be executed when calling [World.update].
     *
     * @param factory A function which creates an object of type [T].
     * @throws [FleksSystemAlreadyAddedException] if the system was already added before.
     */
    inline fun <reified T : IntervalSystem> add(noinline factory: () -> T) {
        val systemType = T::class
        if (systemType in systemFactory) {
            throw FleksSystemAlreadyAddedException(systemType)
        }
        systemFactory[systemType] = factory
    }
}

@DslMarker
annotation class InjectableCfgMarker

/**
 * A DSL class to configure [Injectable] of a [WorldConfiguration].
 */
@InjectableCfgMarker
class InjectableConfiguration {
    @PublishedApi
    internal val injectables = mutableMapOf<String, Injectable>()

    /**
     * Adds the specified [dependency] under the given [name] which can then be injected to any [IntervalSystem], [ComponentListener] or [FamilyListener].
     *
     * @throws [FleksInjectableAlreadyAddedException] if the dependency was already added before.
     */
    fun <T : Any> add(name: String, dependency: T) {
        if (name in injectables) {
            throw FleksInjectableAlreadyAddedException(name)
        }

        injectables[name] = Injectable(dependency)
    }

    /**
     * Adds the specified dependency which can then be injected to any [IntervalSystem], [ComponentListener] or [FamilyListener].
     * Refer to [add]: the name is the simpleName of the class of the [dependency].
     *
     * @throws [FleksInjectableAlreadyAddedException] if the dependency was already added before.
     * @throws [FleksInjectableTypeHasNoName] if the simpleName of the [dependency] is null.
     */
    inline fun <reified T : Any> add(dependency: T) {
        val key = T::class.simpleName ?: throw FleksInjectableTypeHasNoName(T::class)
        add(key, dependency)
    }
}

@DslMarker
annotation class FamilyCfgMarker

/**
 * A DSL class to configure [FamilyListener] of a [WorldConfiguration].
 */
@FamilyCfgMarker
class FamilyConfiguration {
    @PublishedApi
    internal val famListenerFactory = mutableMapOf<KClass<out FamilyListener>, () -> FamilyListener>()

    /**
     * Adds the specified [FamilyListener] to the [world][World].
     *
     * @param listenerFactory the constructor method for creating the FamilyListener.
     * @throws [FleksFamilyListenerAlreadyAddedException] if the listener was already added before.
     */
    inline fun <reified T : FamilyListener> add(
        noinline listenerFactory: (() -> T)
    ) {
        val listenerType = T::class
        if (listenerType in famListenerFactory) {
            throw FleksFamilyListenerAlreadyAddedException(listenerType)
        }
        famListenerFactory[listenerType] = listenerFactory
    }
}

@DslMarker
annotation class WorldCfgMarker

/**
 * A configuration for an entity [world][World] to define the initial maximum entity capacity,
 * the systems of the [world][World] and the systems' dependencies to be injected.
 * Additionally, you can define [ComponentListener] to define custom logic when a specific component is
 * added or removed from an [entity][Entity].
 */
@WorldCfgMarker
class WorldConfiguration {
    /**
     * Initial maximum entity capacity.
     * Will be used internally when a [world][World] is created to set the initial
     * size of some collections and to avoid slow resizing calls.
     */
    var entityCapacity = 512

    internal val compCfg = ComponentConfiguration()

    internal val systemCfg = SystemConfiguration()

    internal val injectableCfg = InjectableConfiguration()

    internal val familyCfg = FamilyConfiguration()

    fun components(cfg: ComponentConfiguration.() -> Unit) = compCfg.run(cfg)

    fun systems(cfg: SystemConfiguration.() -> Unit) = systemCfg.run(cfg)

    fun injectables(cfg: InjectableConfiguration.() -> Unit) = injectableCfg.run(cfg)

    fun families(cfg: FamilyConfiguration.() -> Unit) = familyCfg.run(cfg)
}

/**
 * Creates a new [world][World] with the given [cfg][WorldConfiguration].
 *
 * @param cfg the [configuration][WorldConfiguration] of the world containing the initial maximum entity capacity,
 * the [systems][IntervalSystem], injectables, components, [ComponentListener] and [FamilyListener].
 */
fun world(cfg: WorldConfiguration.() -> Unit): World {
    val worldCfg = WorldConfiguration().apply(cfg)
    return World(
        worldCfg.entityCapacity,
        worldCfg.injectableCfg.injectables,
        worldCfg.compCfg.componentFactory,
        worldCfg.compCfg.compListenerFactory,
        worldCfg.familyCfg.famListenerFactory,
        worldCfg.systemCfg.systemFactory
    )
}

/**
 * A world to handle [entities][Entity] and [systems][IntervalSystem].
 *
 * @param entityCapacity the initial maximum capacity of entities.
 * @param injectables the injectables for any [system][IntervalSystem], [ComponentListener] or [FamilyListener].
 * @param componentFactory the factories to create components.
 * @param compListenerFactory the factories to create [ComponentListener].
 * @param famListenerFactory the factories to create [FamilyListener].
 * @param systemFactory the factories to create [systems][IntervalSystem].
 */
class World internal constructor(
    entityCapacity: Int,
    injectables: MutableMap<String, Injectable>,
    componentFactory: MutableMap<KClass<*>, () -> Any>,
    compListenerFactory: MutableMap<KClass<*>, () -> ComponentListener<*>>,
    famListenerFactory: MutableMap<KClass<out FamilyListener>, () -> FamilyListener>,
    systemFactory: MutableMap<KClass<*>, () -> IntervalSystem>
) {
    /**
     * Returns the time that is passed to [update][World.update].
     * It represents the time in seconds between two frames.
     */
    var deltaTime = 0f
        private set

    @PublishedApi
    internal val systemService: SystemService

    @PublishedApi
    internal val componentService: ComponentService

    @PublishedApi
    internal val entityService: EntityService

    /**
     * List of all [families][Family] of the world that are created either via
     * an [IteratingSystem] or via the world's [family] function to
     * avoid creating duplicates.
     */
    internal val allFamilies = mutableListOf<Family>()

    /**
     * Returns the amount of active entities.
     */
    val numEntities: Int
        get() = entityService.numEntities

    /**
     * Returns the maximum capacity of active entities.
     */
    val capacity: Int
        get() = entityService.capacity

    /**
     * Returns the world's systems.
     */
    val systems: Array<IntervalSystem>
        get() = systemService.systems

    init {
        componentService = ComponentService(componentFactory)
        entityService = EntityService(entityCapacity, componentService)
        // add the world as a used dependency in case any system or ComponentListener needs it
        injectables["World"] = Injectable(this, true)

        // set a Fleks internal global reference to the current world that
        // gets created. This is used to correctly initialize the world
        // reference of any created system and to correctly register
        // any created FamilyListener below.
        CURRENT_WORLD = this

        Inject.injectObjects = injectables
        Inject.mapperObjects = componentService.mappers

        // create and register ComponentListener
        // it is important to do this BEFORE creating systems because if a system's init block
        // is creating entities then ComponentListener already need to be registered to get notified
        compListenerFactory.forEach {
            val compType = it.key
            val listener = it.value.invoke()
            val mapper = componentService.mapper(compType)
            mapper.addComponentListenerInternal(listener)
        }

        // create and register FamilyListener
        // like ComponentListener this must happen before systems are created
        famListenerFactory.forEach {
            val (listenerType, factory) = it
            try {
                val listener = factory.invoke()
                FamilyListener.CURRENT_FAMILY.addFamilyListener(listener)
            } catch (e: Exception) {
                if (e is FleksFamilyException) {
                    throw FleksFamilyListenerCreationException(
                        listenerType,
                        "FamilyListener must define at least one of AllOf, NoneOf or AnyOf"
                    )
                }
                throw e
            }
        }

        // create systems
        systemService = SystemService(systemFactory)

        // verify that there are no unused injectables
        val unusedInjectables = injectables.filterValues { !it.used }.map { it.value.injObj::class }
        if (unusedInjectables.isNotEmpty()) {
            throw FleksUnusedInjectablesException(unusedInjectables)
        }

        // clear dependencies at the end because they are no longer necessary,
        // and we don't want to keep a reference to them
        Inject.injectObjects = EMPTY_INJECTIONS
        Inject.mapperObjects = EMPTY_MAPPERS
    }

    /**
     * Adds a new [entity][Entity] to the world using the given [configuration][EntityCreateCfg].
     */
    inline fun entity(configuration: EntityCreateCfg.(Entity) -> Unit = {}): Entity {
        return entityService.create(configuration)
    }

    /**
     * Updates an [entity] using the given [configuration] to add and remove components.
     */
    inline fun configureEntity(entity: Entity, configuration: EntityUpdateCfg.(Entity) -> Unit) {
        entityService.configureEntity(entity, configuration)
    }

    /**
     * Removes the given [entity] from the world. The [entity] will be recycled and reused for
     * future calls to [World.entity].
     */
    fun remove(entity: Entity) {
        entityService.remove(entity)
    }

    /**
     * Removes all [entities][Entity] from the world. The entities will be recycled and reused for
     * future calls to [World.entity].
     * If [clearRecycled] is true then the recycled entities are cleared and the ids for newly
     * created entities start at 0 again.
     */
    fun removeAll(clearRecycled: Boolean = false) {
        entityService.removeAll(clearRecycled)
    }

    /**
     * Performs the given [action] on each active [entity][Entity].
     */
    fun forEach(action: (Entity) -> Unit) {
        entityService.forEach(action)
    }

    /**
     * Returns the specified [system][IntervalSystem] of the world.
     *
     * @throws [FleksNoSuchSystemException] if there is no such [system][IntervalSystem].
     */
    inline fun <reified T : IntervalSystem> system(): T {
        return systemService.system()
    }

    /**
     * Returns a [ComponentMapper] for the given type. If the mapper does not exist then it will be created.
     *
     * @throws [FleksNoSuchComponentException] if the component of the given type does not exist in the
     * world configuration.
     */
    inline fun <reified T : Any> mapper() = componentService.mapper<T>()

    /**
     * Creates a new [Family] for the given [allOf], [noneOf] and [anyOf] component configuration.
     *
     * This function internally either creates or reuses an already existing [family][Family].
     * In case a new [family][Family] gets created it will be initialized with any already existing [entity][Entity]
     * that matches its configuration.
     * Therefore, this might have a performance impact on the first call if there are a lot of entities in the world.
     *
     * As a best practice families should be created as early as possible, ideally during world creation.
     * Also, store the result of this function instead of calling this function multiple times with the same arguments.
     *
     * @throws [FleksFamilyException] if [allOf], [noneOf] and [anyOf] are null or empty.
     */
    fun family(
        allOf: Array<KClass<*>>? = null,
        noneOf: Array<KClass<*>>? = null,
        anyOf: Array<KClass<*>>? = null,
    ): Family {
        val allOfCmps = if (!allOf.isNullOrEmpty()) {
            allOf.map { componentService.mapper(it) }
        } else {
            null
        }

        val noneOfCmps = if (!noneOf.isNullOrEmpty()) {
            noneOf.map { componentService.mapper(it) }
        } else {
            null
        }

        val anyOfCmps = if (!anyOf.isNullOrEmpty()) {
            anyOf.map { componentService.mapper(it) }
        } else {
            null
        }

        return familyOfMappers(allOfCmps, noneOfCmps, anyOfCmps)
    }

    /**
     * Creates or returns an already created [family][Family] for the given
     * [allOf], [noneOf] and [anyOf] component configuration.
     *
     * Also, adds a newly created [family][Family] as [EntityListener] and
     * initializes it by notifying it with any already existing [entity][Entity]
     * that matches its configuration.
     *
     * @throws [FleksFamilyException] if [allOf], [noneOf] and [anyOf] are null or empty.
     */
    private fun familyOfMappers(
        allOf: List<ComponentMapper<*>>?,
        noneOf: List<ComponentMapper<*>>?,
        anyOf: List<ComponentMapper<*>>?,
    ): Family {
        if (allOf.isNullOrEmpty() && noneOf.isNullOrEmpty() && anyOf.isNullOrEmpty()) {
            throw FleksFamilyException(allOf, noneOf, anyOf)
        }

        val allBs = if (allOf == null) null else BitArray().apply { allOf.forEach { this.set(it.id) } }
        val noneBs = if (noneOf == null) null else BitArray().apply { noneOf.forEach { this.set(it.id) } }
        val anyBs = if (anyOf == null) null else BitArray().apply { anyOf.forEach { this.set(it.id) } }

        var family = allFamilies.find { it.allOf == allBs && it.noneOf == noneBs && it.anyOf == anyBs }
        if (family == null) {
            family = Family(allBs, noneBs, anyBs, entityService).apply {
                entityService.addEntityListener(this)
                allFamilies.add(this)
                // initialize a newly created family by notifying it for any already existing entity
                entityService.forEach { this.onEntityCfgChanged(it, entityService.compMasks[it.id]) }
            }
        }
        return family
    }

    /**
     * Returns a map that contains all [entities][Entity] and their components of this world.
     * The keys of the map are the entities.
     * The values are a list of components that a specific entity has. If the entity
     * does not have any components then the value is an empty list.
     */
    fun snapshot(): Map<Entity, List<Any>> {
        val entityComps = mutableMapOf<Entity, List<Any>>()

        entityService.forEach { entity ->
            val components = mutableListOf<Any>()
            val compMask = entityService.compMasks[entity.id]
            compMask.forEachSetBit { cmpId ->
                components += componentService.mapper(cmpId)[entity] as Any
            }
            entityComps[entity] = components
        }

        return entityComps
    }

    /**
     * Returns a list that contains all components of the given [entity] of this world.
     * If the entity does not have any components then an empty list is returned.
     */
    fun snapshotOf(entity: Entity): List<Any> {
        val comps = mutableListOf<Any>()

        if (entity in entityService) {
            entityService.compMasks[entity.id].forEachSetBit { cmpId ->
                comps += componentService.mapper(cmpId)[entity] as Any
            }
        }

        return comps
    }

    /**
     * Loads the given [snapshot] of the world. This will first clear any existing
     * entity of the world. After that it will load all provided entities and components.
     * This will also notify [ComponentListener] and [FamilyListener].
     *
     * @throws FleksSnapshotException if a family iteration is currently in process.
     *
     * @throws [FleksNoSuchComponentException] if any of the components does not exist in the
     * world configuration.
     */
    fun loadSnapshot(snapshot: Map<Entity, List<Any>>) {
        if (entityService.delayRemoval) {
            throw FleksSnapshotException("Snapshots cannot be loaded while a family iteration is in process")
        }

        // remove any existing entity and clean up recycled ids
        removeAll(true)
        if (snapshot.isEmpty()) {
            // snapshot is empty -> nothing to load
            return
        }

        // Set next entity id to the maximum provided id + 1.
        // All ids before that will be either created or added to the recycled
        // ids to guarantee that the provided snapshot entity ids match the newly created ones.
        with(entityService) {
            val maxId = snapshot.keys.maxOf { it.id }
            this.nextId = maxId + 1
            repeat(maxId + 1) {
                val entity = Entity(it)
                this.recycle(entity)
                val components = snapshot[entity]
                if (components != null) {
                    // components for entity are provided -> create it
                    // note that the id for the entity will be the recycled id from above
                    this.configureEntity(this.create { }, components)
                }
            }
        }
    }

    /**
     * Updates all [enabled][IntervalSystem.enabled] [systems][IntervalSystem] of the world
     * using the given [deltaTime].
     */
    fun update(deltaTime: Float) {
        this.deltaTime = deltaTime
        systemService.update()
    }

    /**
     * Removes all [entities][Entity] of the world and calls the [onDispose][IntervalSystem.onDispose] function of each system.
     */
    fun dispose() {
        entityService.removeAll()
        systemService.dispose()
    }


    @ThreadLocal
    companion object {
        private val EMPTY_INJECTIONS: Map<String, Injectable> = emptyMap()
        private val EMPTY_MAPPERS: Map<KClass<*>, ComponentMapper<*>> = emptyMap()
        internal lateinit var CURRENT_WORLD: World
    }
}
