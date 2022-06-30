package samples.fleks.components

/**
 * This component contains details on destruction of the entity like if other entities should be spawned
 * or if other systems should be fed with data (score, player health or damage, enemy damage,
 * collectable rewards, etc.)
 *
 */
data class Destruct(
    // Setting this to true triggers the DestructSystem to execute destruction of the entity
    // TODO Instead of triggering the destruction here with a property the destruction can also be triggered by adding this
    //      component to the entity which shall be destroyed. That means as long as an entity does not contain this "Destruct" component
    //      it will live. Once this component is added to an entity the destruction of the entity will start and the
    //      entity will be destroyed finally.
    var triggerDestruction: Boolean = false,
    // details about what explosion animation should be spawned, etc.
    var spawnExplosion: Boolean = false,
    var explosionParticleRange: Double = 0.0,
    var explosionParticleAcceleration: Double = 0.0,
)
