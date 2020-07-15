package org.jbox2d.collision.broadphase

import org.jbox2d.callbacks.DebugDraw
import org.jbox2d.callbacks.TreeCallback
import org.jbox2d.callbacks.TreeRayCastCallback
import org.jbox2d.collision.AABB
import org.jbox2d.collision.RayCastInput
import org.jbox2d.common.Vec2

interface BroadPhaseStrategy {

    /**
     * Compute the height of the binary tree in O(N) time. Should not be called often.
     *
     * @return
     */
    val height: Int

    /**
     * Get the maximum balance of an node in the tree. The balance is the difference in height of the
     * two children of a node.
     *
     * @return
     */
    val maxBalance: Int

    /**
     * Get the ratio of the sum of the node areas to the root area.
     *
     * @return
     */
    val areaRatio: Float

    /**
     * Create a proxy. Provide a tight fitting AABB and a userData pointer.
     *
     * @param aabb
     * @param userData
     * @return
     */
    fun createProxy(aabb: AABB, userData: Any): Int

    /**
     * Destroy a proxy
     *
     * @param proxyId
     */
    fun destroyProxy(proxyId: Int)

    /**
     * Move a proxy with a swepted AABB. If the proxy has moved outside of its fattened AABB, then the
     * proxy is removed from the tree and re-inserted. Otherwise the function returns immediately.
     *
     * @return true if the proxy was re-inserted.
     */
    fun moveProxy(proxyId: Int, aabb: AABB, displacement: Vec2): Boolean

    fun getUserData(proxyId: Int): Any?

    fun getFatAABB(proxyId: Int): AABB

    /**
     * Query an AABB for overlapping proxies. The callback class is called for each proxy that
     * overlaps the supplied AABB.
     *
     * @param callback
     * @param araabbgAABB
     */
    fun query(callback: TreeCallback, aabb: AABB)

    /**
     * Ray-cast against the proxies in the tree. This relies on the callback to perform a exact
     * ray-cast in the case were the proxy contains a shape. The callback also performs the any
     * collision filtering. This has performance roughly equal to k * log(n), where k is the number of
     * collisions and n is the number of proxies in the tree.
     *
     * @param input the ray-cast input data. The ray extends from p1 to p1 + maxFraction * (p2 - p1).
     * @param callback a callback class that is called for each proxy that is hit by the ray.
     */
    fun raycast(callback: TreeRayCastCallback, input: RayCastInput)

    /**
     * Compute the height of the tree.
     */
    fun computeHeight(): Int

    fun drawTree(draw: DebugDraw)
}