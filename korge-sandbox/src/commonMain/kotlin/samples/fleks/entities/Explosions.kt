package samples.fleks.entities

import com.github.quillraven.fleks.World
import samples.fleks.components.*
import samples.fleks.utils.random

fun World.createExplosionArtefact(position: Position, destruct: Destruct) {
    entity {
        add<Position> {  // Position of explosion object
            // set initial position of explosion object to collision position
            x = position.x
            y = position.y - (destruct.explosionParticleRange * 0.5)
            if (destruct.explosionParticleRange != 0.0) {
                x += (-destruct.explosionParticleRange..destruct.explosionParticleRange).random()
                y += (-destruct.explosionParticleRange..destruct.explosionParticleRange).random()
            }
            // make sure that all spawned objects are above 200 - this is hardcoded for now since we only have some basic collision detection at y > 200
            // otherwise the explosion artefacts will be destroyed immediately and appear at position 0x0 for one frame
            if (y > 200.0) { y = 199.0 }
            xAcceleration = position.xAcceleration + random(destruct.explosionParticleAcceleration)
            yAcceleration = -position.yAcceleration + random(destruct.explosionParticleAcceleration)
        }
        add<Sprite> {
            imageData = "meteorite"  // "" - Disable sprite graphic for spawned object
            animation = "FireTrail"  // "FireTrail" - "TestNum"
            isPlaying = true
        }
        add<Rigidbody> {
            mass = 2.0
        }
        add<Impulse> {}
    }
}
