package org.jbox2d.callbacks

import org.jbox2d.dynamics.World
import org.jbox2d.particle.ParticleGroup

interface ParticleDestructionListener {
    /**
     * Called when any particle group is about to be destroyed.
     */
    fun sayGoodbye(group: ParticleGroup)

    /**
     * Called when a particle is about to be destroyed. The index can be used in conjunction with
     * [World.getParticleUserDataBuffer] to determine which particle has been destroyed.
     *
     * @param index
     */
    fun sayGoodbye(index: Int)
}
