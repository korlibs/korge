package org.jbox2d.particle

import org.jbox2d.common.Vec2
import org.jbox2d.dynamics.Body

class ParticleBodyContact {
    /** Index of the particle making contact.  */

    var index: Int = 0
    /** The body making contact.  */

    var body: Body? = null
    /** Weight of the contact. A value between 0.0f and 1.0f.  */

    internal var weight: Float = 0.toFloat()
    /** The normalized direction from the particle to the body.  */

    val normal = Vec2()
    /** The effective mass used in calculating force.  */

    internal var mass: Float = 0.toFloat()
}
