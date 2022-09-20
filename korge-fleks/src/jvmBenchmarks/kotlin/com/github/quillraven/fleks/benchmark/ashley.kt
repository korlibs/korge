package com.github.quillraven.fleks.benchmark

import com.badlogic.ashley.core.*
import com.badlogic.ashley.systems.IteratingSystem
import org.openjdk.jmh.annotations.*
import java.util.concurrent.TimeUnit

data class AshleyPosition(
    var x: Float = 0f,
    var y: Float = 0f
) : Component {
    companion object {
        val MAPPER: ComponentMapper<AshleyPosition> = ComponentMapper.getFor(AshleyPosition::class.java)
    }
}

data class AshleyLife(
    var life: Float = 0f
) : Component

data class AshleySprite(
    var path: String = "",
    var animationTime: Float = 0f
) : Component {
    companion object {
        val MAPPER: ComponentMapper<AshleySprite> = ComponentMapper.getFor(AshleySprite::class.java)
    }
}

class AshleySystemSimple : IteratingSystem(Family.all(AshleyPosition::class.java).get()) {
    override fun processEntity(entity: Entity?, deltaTime: Float) {
        AshleyPosition.MAPPER.get(entity).x++
    }
}

class AshleySystemComplex1 : IteratingSystem(
    Family
        .all(AshleyPosition::class.java)
        .exclude(AshleyLife::class.java)
        .one(AshleySprite::class.java)
        .get()
) {
    private var processCalls = 0

    override fun processEntity(entity: Entity?, deltaTime: Float) {
        if (processCalls % 2 == 0) {
            AshleyPosition.MAPPER.get(entity).x++
            entity?.add(engine.createComponent(AshleyLife::class.java))
        } else {
            entity?.remove(AshleyPosition::class.java)
        }
        AshleySprite.MAPPER.get(entity).animationTime++
        ++processCalls
    }
}

class AshleySystemComplex2 : IteratingSystem(
    Family
        .one(AshleyPosition::class.java, AshleyLife::class.java, AshleySprite::class.java)
        .get()
) {
    override fun processEntity(entity: Entity?, deltaTime: Float) {
        entity?.remove(AshleyLife::class.java)
        entity?.add(engine.createComponent(AshleyPosition::class.java))
    }
}

@State(Scope.Benchmark)
open class AshleyStateAddRemove {
    lateinit var engine: Engine

    @Setup(value = Level.Iteration)
    fun setup() {
        engine = Engine()
        engine.addSystem(AshleySystemSimple())
    }
}

@State(Scope.Benchmark)
open class AshleyStateSimple {
    lateinit var engine: Engine

    @Setup(value = Level.Iteration)
    fun setup() {
        engine = Engine()
        engine.addSystem(AshleySystemSimple())
        repeat(NUM_ENTITIES) {
            val cmp = engine.createComponent(AshleyPosition::class.java)
            val entity = engine.createEntity()
            entity.add(cmp)
            engine.addEntity(entity)
        }
    }
}

@State(Scope.Benchmark)
open class AshleyStateComplex {
    lateinit var engine: Engine

    @Setup(value = Level.Iteration)
    fun setup() {
        engine = Engine()
        engine.addSystem(AshleySystemComplex1())
        engine.addSystem(AshleySystemComplex2())
        repeat(NUM_ENTITIES) {
            val entity = engine.createEntity()
            entity.add(engine.createComponent(AshleyPosition::class.java))
            entity.add(engine.createComponent(AshleySprite::class.java))
            engine.addEntity(entity)
        }
    }
}

@Fork(value = WARMUPS)
@Warmup(iterations = WARMUPS)
@Measurement(iterations = ITERATIONS, time = TIME, timeUnit = TimeUnit.SECONDS)
open class AshleyBenchmark {
    @Benchmark
    fun addRemove(state: AshleyStateAddRemove) {
        repeat(NUM_ENTITIES) {
            val cmp = state.engine.createComponent(AshleyPosition::class.java)
            val entity = state.engine.createEntity()
            entity.add(cmp)
            state.engine.addEntity(entity)
        }
        state.engine.removeAllEntities()
    }

    @Benchmark
    fun simple(state: AshleyStateSimple) {
        repeat(WORLD_UPDATES) { state.engine.update(1f) }
    }

    @Benchmark
    fun complex(state: AshleyStateComplex) {
        repeat(WORLD_UPDATES) { state.engine.update(1f) }
    }
}
