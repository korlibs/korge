package samples.fleks.systems

import com.github.quillraven.fleks.*
import samples.fleks.components.*
import samples.fleks.entities.createMeteoriteObject

class SpawnerSystem : IteratingSystem(
    allOfComponents = arrayOf(Spawner::class),
    interval = EachFrame
) {

    private val positions = Inject.componentMapper<Position>()
    private val spawners = Inject.componentMapper<Spawner>()

    override fun onTickEntity(entity: Entity) {
        val spawner = spawners[entity]
        if (spawner.interval > 0) {
            if (spawner.nextSpawnIn <= 0) {
                spawn(entity)
                spawner.nextSpawnIn = spawner.interval
                if (spawner.timeVariation != 0) spawner.nextSpawnIn += (-spawner.timeVariation..spawner.timeVariation).random()
            } else {
                spawner.nextSpawnIn--
            }
        }
    }

    private fun spawn(entity: Entity) {
        val spawnerPosition = positions[entity]
        val spawner = spawners[entity]
        for (i in 0 until spawner.numberOfObjects) {
            world.createMeteoriteObject(spawnerPosition, spawner)
        }
    }
}
