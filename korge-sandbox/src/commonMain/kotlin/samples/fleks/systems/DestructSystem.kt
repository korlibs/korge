package samples.fleks.systems

import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.Inject
import com.github.quillraven.fleks.IteratingSystem
import samples.fleks.components.*
import samples.fleks.entities.createExplosionArtefact

/**
 * This system controls the "destruction" of an entity (game object).
 *
 */
class DestructSystem : IteratingSystem(
    allOfComponents = arrayOf(Destruct::class)
) {

    private val positions = Inject.componentMapper<Position>()
    private val destructs = Inject.componentMapper<Destruct>()

    override fun onTickEntity(entity: Entity) {
        val destruct = destructs[entity]
        if (destruct.triggerDestruction) {
            val position = positions[entity]
            // The spawning of explosion objects is hardcoded here to 40 objects - TODO that should be put into some component config later
            for (i in 0 until 40) {
                world.createExplosionArtefact(position, destruct)
            }
            // now destroy entity
            world.remove(entity)
        }
    }
}
