package com.github.quillraven.fleks.benchmark

import com.github.quillraven.fleks.*
import org.openjdk.jmh.annotations.*
import java.util.concurrent.TimeUnit

data class FleksPosition(var x: Float = 0f, var y: Float = 0f)

data class FleksLife(var life: Float = 0f)

data class FleksSprite(var path: String = "", var animationTime: Float = 0f)

class FleksSystemSimple : IteratingSystem(
    allOf = AllOf(arrayOf(FleksPosition::class))
    ) {

    private val positions: ComponentMapper<FleksPosition> = Inject.componentMapper()

    override fun onTickEntity(entity: Entity) {
        positions[entity].x++
    }
}

class FleksSystemComplex1 : IteratingSystem(
    allOf = AllOf(arrayOf(FleksPosition::class)),
    noneOf = NoneOf(arrayOf(FleksLife::class)),
    anyOf = AnyOf(arrayOf(FleksSprite::class))
) {

    private val positions: ComponentMapper<FleksPosition> = Inject.componentMapper()
    private val lifes: ComponentMapper<FleksLife> = Inject.componentMapper()
    private val sprites: ComponentMapper<FleksSprite> = Inject.componentMapper()

    private var actionCalls = 0

    override fun onTickEntity(entity: Entity) {
        if (actionCalls % 2 == 0) {
            positions[entity].x++
            configureEntity(entity) { lifes.add(it) }
        } else {
            configureEntity(entity) { positions.remove(it) }
        }
        sprites[entity].animationTime++
        ++actionCalls
    }
}

class FleksSystemComplex2 : IteratingSystem(
    anyOf = AnyOf(arrayOf(FleksPosition::class, FleksLife::class, FleksSprite::class))
) {

    private val positions: ComponentMapper<FleksPosition> = Inject.componentMapper()
    private val lifes: ComponentMapper<FleksLife> = Inject.componentMapper()

    override fun onTickEntity(entity: Entity) {
        configureEntity(entity) {
            lifes.remove(it)
            positions.add(it)
        }
    }
}

@State(Scope.Benchmark)
open class FleksStateAddRemove {
    lateinit var world: World

    @Setup(value = Level.Iteration)
    fun setup() {
        world = World {
            entityCapacity = NUM_ENTITIES

            component(::FleksPosition)
        }
    }
}

@State(Scope.Benchmark)
open class FleksStateSimple {
    lateinit var world: World

    @Setup(value = Level.Iteration)
    fun setup() {
        world = World {
            entityCapacity = NUM_ENTITIES
            system(::FleksSystemSimple)

            component(::FleksPosition)
        }

        repeat(NUM_ENTITIES) {
            world.entity { add<FleksPosition>() }
        }
    }
}

@State(Scope.Benchmark)
open class FleksStateComplex {
    lateinit var world: World

    @Setup(value = Level.Iteration)
    fun setup() {
        world = World {
            entityCapacity = NUM_ENTITIES
            system(::FleksSystemComplex1)
            system(::FleksSystemComplex2)

            component(::FleksPosition)
            component(::FleksLife)
            component(::FleksSprite)
        }

        repeat(NUM_ENTITIES) {
            world.entity {
                add<FleksPosition>()
                add<FleksSprite>()
            }
        }
    }
}

@Fork(1)
@Warmup(iterations = WARMUPS)
@Measurement(iterations = ITERATIONS, time = TIME, timeUnit = TimeUnit.SECONDS)
open class FleksBenchmark {
    @Benchmark
    fun addRemove(state: FleksStateAddRemove) {
        repeat(NUM_ENTITIES) {
            state.world.entity { add<FleksPosition>() }
        }
        repeat(NUM_ENTITIES) {
            state.world.remove(Entity(it))
        }
    }

    @Benchmark
    fun simple(state: FleksStateSimple) {
        repeat(WORLD_UPDATES) {
            state.world.update(1f)
        }
    }

    @Benchmark
    fun complex(state: FleksStateComplex) {
        repeat(WORLD_UPDATES) {
            state.world.update(1f)
        }
    }
}
