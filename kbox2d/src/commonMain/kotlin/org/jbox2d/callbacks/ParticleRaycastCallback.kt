package org.jbox2d.callbacks

import org.jbox2d.common.Vec2

interface ParticleRaycastCallback {
    /**
     * Called for each particle found in the query. See
     * [RayCastCallback.reportFixture] for
     * argument info.
     *
     * @param index
     * @param point
     * @param normal
     * @param fraction
     * @return
     */
    fun reportParticle(index: Int, point: Vec2, normal: Vec2, fraction: Float): Float

}
