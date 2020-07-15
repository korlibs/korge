package org.jbox2d.particle

import org.jbox2d.common.*
import org.jbox2d.userdata.*

class ParticleDef : Box2dTypedUserData by Box2dTypedUserData.Mixin() {
    /**
     * Specifies the type of particle. A particle may be more than one type. Multiple types are
     * chained by logical sums, for example: pd.flags = ParticleType.b2_elasticParticle |
     * ParticleType.b2_viscousParticle.
     */

    internal var flags: Int = 0

    /** The world position of the particle.  */

    val position = Vec2()

    /** The linear velocity of the particle in world co-ordinates.  */

    val velocity = Vec2()

    /** The color of the particle.  */

    var color: ParticleColor? = null

    /** Use this to store application-specific body data.  */

    var userData: Any? = null
}
