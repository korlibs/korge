/*******************************************************************************
 * Copyright (c) 2013, Daniel Murphy
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 * * Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 * * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package org.jbox2d.collision.broadphase

import org.jbox2d.callbacks.DebugDraw
import org.jbox2d.callbacks.PairCallback
import org.jbox2d.callbacks.TreeCallback
import org.jbox2d.callbacks.TreeRayCastCallback
import org.jbox2d.collision.AABB
import org.jbox2d.collision.RayCastInput
import org.jbox2d.common.Vec2
import org.jbox2d.internal.*

/**
 * The broad-phase is used for computing pairs and performing volume queries and ray casts. This
 * broad-phase does not persist pairs. Instead, this reports potentially new pairs. It is up to the
 * client to consume the new pairs and to track subsequent overlap.
 *
 * @author Daniel Murphy
 */
class DefaultBroadPhaseBuffer(private val m_tree: BroadPhaseStrategy) : TreeCallback, BroadPhase {

    override var proxyCount: Int = 0
        private set(value: Int) {
            field = value
        }

    private var m_moveBuffer: IntArray? = null
    private var m_moveCapacity: Int = 0
    private var m_moveCount: Int = 0

    private var m_pairBuffer: LongArray? = null
    private var m_pairCapacity: Int = 0
    private var m_pairCount: Int = 0

    private var m_queryProxyId: Int = 0

    override val treeHeight: Int
        get() = m_tree.height

    override val treeBalance: Int
        get() = m_tree.maxBalance

    override val treeQuality: Float
        get() = m_tree.areaRatio

    init {
        proxyCount = 0

        m_pairCapacity = 16
        m_pairCount = 0
        m_pairBuffer = LongArray(m_pairCapacity)

        m_moveCapacity = 16
        m_moveCount = 0
        m_moveBuffer = IntArray(m_moveCapacity)
        m_queryProxyId = BroadPhase.NULL_PROXY
    }

    override fun createProxy(aabb: AABB, userData: Any): Int {
        val proxyId = m_tree.createProxy(aabb, userData)
        ++proxyCount
        bufferMove(proxyId)
        return proxyId
    }

    override fun destroyProxy(proxyId: Int) {
        unbufferMove(proxyId)
        --proxyCount
        m_tree.destroyProxy(proxyId)
    }

    override fun moveProxy(proxyId: Int, aabb: AABB, displacement: Vec2) {
        val buffer = m_tree.moveProxy(proxyId, aabb, displacement)
        if (buffer) {
            bufferMove(proxyId)
        }
    }

    override fun touchProxy(proxyId: Int) {
        bufferMove(proxyId)
    }

    override fun getUserData(proxyId: Int): Any? {
        return m_tree.getUserData(proxyId)
    }

    override fun getFatAABB(proxyId: Int): AABB {
        return m_tree.getFatAABB(proxyId)
    }

    override fun testOverlap(proxyIdA: Int, proxyIdB: Int): Boolean {
        // return AABB.testOverlap(proxyA.aabb, proxyB.aabb);
        // return m_tree.overlap(proxyIdA, proxyIdB);
        val a = m_tree.getFatAABB(proxyIdA)
        val b = m_tree.getFatAABB(proxyIdB)
        if (b.lowerBound.x - a.upperBound.x > 0.0f || b.lowerBound.y - a.upperBound.y > 0.0f) {
            return false
        }

        return if (a.lowerBound.x - b.upperBound.x > 0.0f || a.lowerBound.y - b.upperBound.y > 0.0f) {
            false
        } else true

    }

    override fun drawTree(argDraw: DebugDraw) {
        m_tree.drawTree(argDraw)
    }

    override fun updatePairs(callback: PairCallback) {
        // Reset pair buffer
        m_pairCount = 0

        // Perform tree queries for all moving proxies.
        for (i in 0 until m_moveCount) {
            m_queryProxyId = m_moveBuffer!![i]
            if (m_queryProxyId == BroadPhase.NULL_PROXY) {
                continue
            }

            // We have to query the tree with the fat AABB so that
            // we don't fail to create a pair that may touch later.
            val fatAABB = m_tree.getFatAABB(m_queryProxyId)

            // Query tree, create pairs and add them pair buffer.
            // log.debug("quering aabb: "+m_queryProxy.aabb);
            m_tree.query(this, fatAABB)
        }
        // log.debug("Number of pairs found: "+m_pairCount);

        // Reset move buffer
        m_moveCount = 0

        // Sort the pair buffer to expose duplicates.
        Arrays_sort(m_pairBuffer!!, 0, m_pairCount)

        // Send the pairs back to the client.
        var i = 0
        while (i < m_pairCount) {
            val primaryPair = m_pairBuffer!![i]
            val userDataA = m_tree.getUserData((primaryPair shr 32).toInt())
            val userDataB = m_tree.getUserData(primaryPair.toInt())

            // log.debug("returning pair: "+userDataA+", "+userDataB);
            callback.addPair(userDataA, userDataB)
            ++i

            // Skip any duplicate pairs.
            while (i < m_pairCount) {
                val pair = m_pairBuffer!![i]
                if (pair != primaryPair) {
                    break
                }
                ++i
            }
        }
    }

    override fun query(callback: TreeCallback, aabb: AABB) {
        m_tree.query(callback, aabb)
    }

    override fun raycast(callback: TreeRayCastCallback, input: RayCastInput) {
        m_tree.raycast(callback, input)
    }

    protected fun bufferMove(proxyId: Int) {
        if (m_moveCount == m_moveCapacity) {
            val old = m_moveBuffer
            m_moveCapacity *= 2
            m_moveBuffer = IntArray(m_moveCapacity)
            arraycopy(old!!, 0, m_moveBuffer!!, 0, old.size)
        }

        m_moveBuffer!![m_moveCount] = proxyId
        ++m_moveCount
    }

    protected fun unbufferMove(proxyId: Int) {
        for (i in 0 until m_moveCount) {
            if (m_moveBuffer!![i] == proxyId) {
                m_moveBuffer!![i] = BroadPhase.NULL_PROXY
            }
        }
    }

    /**
     * This is called from DynamicTree::query when we are gathering pairs.
     */
    override fun treeCallback(proxyId: Int): Boolean {
        // A proxy cannot form a pair with itself.
        if (proxyId == m_queryProxyId) {
            return true
        }

        // Grow the pair buffer as needed.
        if (m_pairCount == m_pairCapacity) {
            val oldBuffer = m_pairBuffer
            m_pairCapacity *= 2
            m_pairBuffer = LongArray(m_pairCapacity)
            arraycopy(oldBuffer!!, 0, m_pairBuffer!!, 0, oldBuffer.size)
            for (i in oldBuffer.size until m_pairCapacity) {
                m_pairBuffer!![i] = 0
            }
        }

        if (proxyId < m_queryProxyId) {
            m_pairBuffer!![m_pairCount] = (proxyId.toLong() shl 32) or m_queryProxyId.toLong()
        } else {
            m_pairBuffer!![m_pairCount] = (m_queryProxyId.toLong() shl 32) or proxyId.toLong()
        }

        ++m_pairCount
        return true
    }
}
