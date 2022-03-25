package com.github.quillraven.fleks

import kotlin.reflect.KClass

/**
 * Wrapper class for injectables of the [WorldConfiguration].
 * It is used in the [SystemService] to find out any unused injectables.
 */
data class Injectable(val injObj: Any, var used: Boolean = false)

/**
 * A configuration for an entity [world][World] to define the initial maximum entity capacity,
 * the systems of the [world][World] and the systems' dependencies to be injected.
 * Additionally, you can define [ComponentListener] to define custom logic when a specific component is
 * added or removed from an [entity][Entity].
 */
class WorldConfiguration {
    /**
     * Initial maximum entity capacity.
     * Will be used internally when a [world][World] is created to set the initial
     * size of some collections and to avoid slow resizing calls.
     */
    var entityCapacity = 512

    @PublishedApi
    internal val systemFactory = mutableMapOf<KClass<*>, () -> IntervalSystem>()

    @PublishedApi
    internal val injectables = mutableMapOf<String, Injectable>()

    @PublishedApi
    internal val compListenerFactory = mutableMapOf<String, () -> ComponentListener<*>>()

    @PublishedApi
    internal val componentFactory = mutableMapOf<String, () -> Any>()

    /**
     * Adds the specified [IntervalSystem] to the [world][World].
     * The order in which systems are added is the order in which they will be executed when calling [World.update].
     *
     * @param factory A function which creates an object of type [T].
     * @throws [FleksSystemAlreadyAddedException] if the system was already added before.
     */
    inline fun <reified T : IntervalSystem> system(noinline factory: () -> T) {
        val systemType = T::class
        if (systemType in systemFactory) {
            throw FleksSystemAlreadyAddedException(systemType)
        }
        systemFactory[systemType] = factory
    }

    /**
     * Adds the specified [dependency] under the given [type] which can then be injected to any [IntervalSystem] or [ComponentListener].
     *
     * @param type is the name of the dependency which is used to access it in systems and listeners. This is especially useful if two or more
     *             dependency objects of the same type shall be injected.
     * @param dependency object which shall be injected to systems and listeners of the Fleks ECS.
     * @param used this will set the injected dependency to [used] internally. Default is false. If set to true then Fleks will not
     *             complain if the dependency is not used by any system or listener on their creation time.
     * @throws [FleksInjectableAlreadyAddedException] if the dependency was already added before.
     */
    fun <T : Any> inject(type: String, dependency: T, used: Boolean = false) {
        if (type in injectables) {
            throw FleksInjectableAlreadyAddedException(type)
        }

        injectables[type] = Injectable(dependency, used)
    }

    /**
     * Adds the specified dependency which can then be injected to any [IntervalSystem] or [ComponentListener].
     * Refer to [inject]: the type is the simpleName of the class of the [dependency].
     *
     * @param dependency object which shall be injected to systems and listeners of the Fleks ECS.
     * @param used this will set the injected dependency to [used] internally. Default is false. If set to true then Fleks will not
     *             complain if the dependency is not used by any system or listener on their creation time.
     * @throws [FleksInjectableAlreadyAddedException] if the dependency was already added before.
     * @throws [FleksInjectableTypeHasNoName] if the dependency type has no T::class.simpleName.
     */
    inline fun <reified T : Any> inject(dependency: T, used: Boolean = false) {
        val type = T::class.simpleName ?: throw FleksInjectableTypeHasNoName(T::class)
        inject(type, dependency, used)
    }

    /**
     * Adds the specified [Component] and its [ComponentListener] to the [world][World]. If a component listener
     * is not needed than it can be omitted.
     *
     * @param compFactory the constructor method for creating the component.
     * @param listenerFactory the constructor method for creating the component listener.
     * @throws [FleksComponentAlreadyAddedException] if the component was already added before.
     * @throws [FleksInjectableTypeHasNoName] if the dependency type has no T::class.simpleName.
     */
    inline fun <reified T : Any> component(noinline compFactory: () -> T, noinline listenerFactory: (() -> ComponentListener<T>)? = null) {
        val compType = T::class.simpleName ?: throw FleksInjectableTypeHasNoName(T::class)

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

/**
 * A world to handle [entities][Entity] and [systems][IntervalSystem].
 *
 * @param cfg the [configuration][WorldConfiguration] of the world containing the initial maximum entity capacity
 * and the [systems][IntervalSystem] to be processed.
 */
class World(
    cfg: WorldConfiguration.() -> Unit
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
     * Returns the amount of active entities.
     */
    val numEntities: Int
        get() = entityService.numEntities

    /**
     * Returns the maximum capacity of active entities.
     */
    val capacity: Int
        get() = entityService.capacity

    init {
        val worldCfg = WorldConfiguration().apply(cfg)
        componentService = ComponentService(worldCfg.componentFactory)
        entityService = EntityService(worldCfg.entityCapacity, componentService)
        val injectables = worldCfg.injectables

        // Add world to inject object so that component listeners can get it form injectables, too
        // Set "used" to true to make this injectable not mandatory
        injectables["World"] = Injectable(this, true)

        systemService = SystemService(this, worldCfg.systemFactory, injectables)

        // create and register ComponentListener
        worldCfg.compListenerFactory.forEach {
            val compType = it.key
            val listener = it.value.invoke()
            val mapper = componentService.mapper(compType)
            mapper.addComponentListenerInternal(listener)
        }

        // verify that there are no unused injectables
        val unusedInjectables = injectables.filterValues { !it.used }.map { it.value.injObj::class }
        if (unusedInjectables.isNotEmpty()) {
            throw FleksUnusedInjectablesException(unusedInjectables)
        }
    }

    /**
     * Adds a new [entity][Entity] to the world using the given [configuration][EntityCreateCfg].
     */
    inline fun entity(configuration: EntityCreateCfg.(Entity) -> Unit = {}): Entity {
        return entityService.create(configuration)
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
     */
    fun removeAll() {
        entityService.removeAll()
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
     * @throws [FleksNoSuchComponentException] if the component of the given [type] does not exist in the
     * world configuration.
     * @throws [FleksInjectableTypeHasNoName] if the dependency type has no T::class.simpleName.
     */
    @Suppress("UNCHECKED_CAST")
    inline fun <reified T : Any> mapper(): ComponentMapper<T> {
        val type = T::class.simpleName ?: throw FleksInjectableTypeHasNoName(T::class)
        return componentService.mapper(type) as ComponentMapper<T>
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
}
