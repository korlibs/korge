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
import org.jbox2d.callbacks.TreeCallback
import org.jbox2d.callbacks.TreeRayCastCallback
import org.jbox2d.collision.AABB
import org.jbox2d.collision.RayCastInput
import org.jbox2d.common.BufferUtils
import org.jbox2d.common.Color3f
import org.jbox2d.common.MathUtils
import org.jbox2d.common.Settings
import org.jbox2d.common.Vec2
import org.jbox2d.internal.*
import kotlin.reflect.*

class DynamicTreeFlatNodes : BroadPhaseStrategy {


    var m_root: Int = NULL_NODE
    lateinit var m_aabb: Array<AABB>
    lateinit var m_userData: Array<Any?>
    lateinit protected var m_parent: IntArray
    lateinit protected var m_child1: IntArray
    lateinit protected var m_child2: IntArray
    lateinit protected var m_height: IntArray

    private var m_nodeCount: Int = 0
    private var m_nodeCapacity: Int = 16

    private var m_freeList: Int = 0

    private val drawVecs = Array<Vec2>(4) { Vec2() }

    private var nodeStack = IntArray(20)
    private var nodeStackIndex: Int = 0

    private val r = Vec2()
    private val aabb = AABB()
    private val subInput = RayCastInput()

    override val height: Int
        get() = if (m_root == NULL_NODE) {
            0
        } else m_height[m_root]

    override val maxBalance: Int
        get() {
            var maxBalance = 0
            for (i in 0 until m_nodeCapacity) {
                if (m_height[i] <= 1) {
                    continue
                }

                assert(m_child1[i] != NULL_NODE)

                val child1 = m_child1[i]
                val child2 = m_child2[i]
                val balance = MathUtils.abs(m_height[child2] - m_height[child1])
                maxBalance = MathUtils.max(maxBalance, balance)
            }

            return maxBalance
        }

    override// Free node in pool
    val areaRatio: Float
        get() {
            if (m_root == NULL_NODE) {
                return 0.0f
            }

            val root = m_root
            val rootArea = m_aabb[root].perimeter

            var totalArea = 0.0f
            for (i in 0 until m_nodeCapacity) {
                if (m_height[i] < 0) {
                    continue
                }

                totalArea += m_aabb[i].perimeter
            }

            return totalArea / rootArea
        }

    private val combinedAABB = AABB()

    private val color = Color3f()
    private val textVec = Vec2()

    init {
        expandBuffers(0, m_nodeCapacity)
    }

    private fun expandBuffers(oldSize: Int, newSize: Int) {
        m_aabb = BufferUtils.reallocateBuffer({ AABB() }, m_aabb, oldSize, newSize)
        m_userData = BufferUtils.reallocateBuffer<Any>({ Any() }, m_userData as Array<Any>, oldSize, newSize) as Array<Any?>
        m_parent = BufferUtils.reallocateBuffer(m_parent, oldSize, newSize)
        m_child1 = BufferUtils.reallocateBuffer(m_child1, oldSize, newSize)
        m_child2 = BufferUtils.reallocateBuffer(m_child2, oldSize, newSize)
        m_height = BufferUtils.reallocateBuffer(m_height, oldSize, newSize)

        // Build a linked list for the free list.
        for (i in oldSize until newSize) {
            m_aabb[i] = AABB()
            m_parent[i] = if (i == newSize - 1) NULL_NODE else i + 1
            m_height[i] = -1
            m_child1[i] = -1
            m_child2[i] = -1
        }
        m_freeList = oldSize
    }

    override fun createProxy(aabb: AABB, userData: Any): Int {
        val node = allocateNode()
        // Fatten the aabb
        val nodeAABB = m_aabb[node]
        nodeAABB.lowerBound.x = aabb.lowerBound.x - Settings.aabbExtension
        nodeAABB.lowerBound.y = aabb.lowerBound.y - Settings.aabbExtension
        nodeAABB.upperBound.x = aabb.upperBound.x + Settings.aabbExtension
        nodeAABB.upperBound.y = aabb.upperBound.y + Settings.aabbExtension
        m_userData[node] = userData

        insertLeaf(node)

        return node
    }

    override fun destroyProxy(proxyId: Int) {
        assert(0 <= proxyId && proxyId < m_nodeCapacity)
        assert(m_child1[proxyId] == NULL_NODE)

        removeLeaf(proxyId)
        freeNode(proxyId)
    }

    override fun moveProxy(proxyId: Int, aabb: AABB, displacement: Vec2): Boolean {
        assert(0 <= proxyId && proxyId < m_nodeCapacity)
        val node = proxyId
        assert(m_child1[node] == NULL_NODE)

        val nodeAABB = m_aabb[node]
        // if (nodeAABB.contains(aabb)) {
        if (nodeAABB.lowerBound.x <= aabb.lowerBound.x && nodeAABB.lowerBound.y <= aabb.lowerBound.y
                && aabb.upperBound.x <= nodeAABB.upperBound.x && aabb.upperBound.y <= nodeAABB.upperBound.y) {
            return false
        }

        removeLeaf(node)

        // Extend AABB
        val lowerBound = nodeAABB.lowerBound
        val upperBound = nodeAABB.upperBound
        lowerBound.x = aabb.lowerBound.x - Settings.aabbExtension
        lowerBound.y = aabb.lowerBound.y - Settings.aabbExtension
        upperBound.x = aabb.upperBound.x + Settings.aabbExtension
        upperBound.y = aabb.upperBound.y + Settings.aabbExtension

        // Predict AABB displacement.
        val dx = displacement.x * Settings.aabbMultiplier
        val dy = displacement.y * Settings.aabbMultiplier
        if (dx < 0.0f) {
            lowerBound.x += dx
        } else {
            upperBound.x += dx
        }

        if (dy < 0.0f) {
            lowerBound.y += dy
        } else {
            upperBound.y += dy
        }

        insertLeaf(proxyId)
        return true
    }

    override fun getUserData(proxyId: Int): Any? {
        assert(0 <= proxyId && proxyId < m_nodeCount)
        return m_userData[proxyId]
    }

    override fun getFatAABB(proxyId: Int): AABB {
        assert(0 <= proxyId && proxyId < m_nodeCount)
        return m_aabb[proxyId]
    }

    override fun query(callback: TreeCallback, aabb: AABB) {
        nodeStackIndex = 0
        nodeStack[nodeStackIndex++] = m_root

        while (nodeStackIndex > 0) {
            val node = nodeStack[--nodeStackIndex]
            if (node == NULL_NODE) {
                continue
            }

            if (AABB.testOverlap(m_aabb[node], aabb)) {
                val child1 = m_child1[node]
                if (child1 == NULL_NODE) {
                    val proceed = callback.treeCallback(node)
                    if (!proceed) {
                        return
                    }
                } else {
                    if (nodeStack.size - nodeStackIndex - 2 <= 0) {
                        nodeStack = BufferUtils.reallocateBuffer(nodeStack, nodeStack.size, nodeStack.size * 2)
                    }
                    nodeStack[nodeStackIndex++] = child1
                    nodeStack[nodeStackIndex++] = m_child2[node]
                }
            }
        }
    }

    override fun raycast(callback: TreeRayCastCallback, input: RayCastInput) {
        val p1 = input.p1
        val p2 = input.p2
        val p1x = p1.x
        val p2x = p2.x
        val p1y = p1.y
        val p2y = p2.y
        val vx: Float
        val vy: Float
        val rx: Float
        val ry: Float
        val absVx: Float
        val absVy: Float
        var cx: Float
        var cy: Float
        var hx: Float
        var hy: Float
        var tempx: Float
        var tempy: Float
        r.x = p2x - p1x
        r.y = p2y - p1y
        assert(r.x * r.x + r.y * r.y > 0f)
        r.normalize()
        rx = r.x
        ry = r.y

        // v is perpendicular to the segment.
        vx = -1f * ry
        vy = 1f * rx
        absVx = MathUtils.abs(vx)
        absVy = MathUtils.abs(vy)

        // Separating axis for segment (Gino, p80).
        // |dot(v, p1 - c)| > dot(|v|, h)

        var maxFraction = input.maxFraction

        // Build a bounding box for the segment.
        val segAABB = aabb
        // Vec2 t = p1 + maxFraction * (p2 - p1);
        // before inline
        // temp.set(p2).subLocal(p1).mulLocal(maxFraction).addLocal(p1);
        // Vec2.minToOut(p1, temp, segAABB.lowerBound);
        // Vec2.maxToOut(p1, temp, segAABB.upperBound);
        tempx = (p2x - p1x) * maxFraction + p1x
        tempy = (p2y - p1y) * maxFraction + p1y
        segAABB.lowerBound.x = if (p1x < tempx) p1x else tempx
        segAABB.lowerBound.y = if (p1y < tempy) p1y else tempy
        segAABB.upperBound.x = if (p1x > tempx) p1x else tempx
        segAABB.upperBound.y = if (p1y > tempy) p1y else tempy
        // end inline

        nodeStackIndex = 0
        nodeStack[nodeStackIndex++] = m_root
        while (nodeStackIndex > 0) {
            nodeStack[--nodeStackIndex] = m_root
            val node = nodeStack[--nodeStackIndex]
            if (node == NULL_NODE) {
                continue
            }

            val nodeAABB = m_aabb[node]
            if (!AABB.testOverlap(nodeAABB, segAABB)) {
                continue
            }

            // Separating axis for segment (Gino, p80).
            // |dot(v, p1 - c)| > dot(|v|, h)
            // node.aabb.getCenterToOut(c);
            // node.aabb.getExtentsToOut(h);
            cx = (nodeAABB.lowerBound.x + nodeAABB.upperBound.x) * .5f
            cy = (nodeAABB.lowerBound.y + nodeAABB.upperBound.y) * .5f
            hx = (nodeAABB.upperBound.x - nodeAABB.lowerBound.x) * .5f
            hy = (nodeAABB.upperBound.y - nodeAABB.lowerBound.y) * .5f
            tempx = p1x - cx
            tempy = p1y - cy
            val separation = MathUtils.abs(vx * tempx + vy * tempy) - (absVx * hx + absVy * hy)
            if (separation > 0.0f) {
                continue
            }

            val child1 = m_child1[node]
            if (child1 == NULL_NODE) {
                subInput.p1.x = p1x
                subInput.p1.y = p1y
                subInput.p2.x = p2x
                subInput.p2.y = p2y
                subInput.maxFraction = maxFraction

                val value = callback.raycastCallback(subInput, node)

                if (value == 0.0f) {
                    // The client has terminated the ray cast.
                    return
                }

                if (value > 0.0f) {
                    // Update segment bounding box.
                    maxFraction = value
                    // temp.set(p2).subLocal(p1).mulLocal(maxFraction).addLocal(p1);
                    // Vec2.minToOut(p1, temp, segAABB.lowerBound);
                    // Vec2.maxToOut(p1, temp, segAABB.upperBound);
                    tempx = (p2x - p1x) * maxFraction + p1x
                    tempy = (p2y - p1y) * maxFraction + p1y
                    segAABB.lowerBound.x = if (p1x < tempx) p1x else tempx
                    segAABB.lowerBound.y = if (p1y < tempy) p1y else tempy
                    segAABB.upperBound.x = if (p1x > tempx) p1x else tempx
                    segAABB.upperBound.y = if (p1y > tempy) p1y else tempy
                }
            } else {
                nodeStack[nodeStackIndex++] = child1
                nodeStack[nodeStackIndex++] = m_child2[node]
            }
        }
    }

    override fun computeHeight(): Int {
        return computeHeight(m_root)
    }

    private fun computeHeight(node: Int): Int {
        assert(0 <= node && node < m_nodeCapacity)

        if (m_child1[node] == NULL_NODE) {
            return 0
        }
        val height1 = computeHeight(m_child1[node])
        val height2 = computeHeight(m_child2[node])
        return 1 + MathUtils.max(height1, height2)
    }

    /**
     * Validate this tree. For testing.
     */
    fun validate() {
        validateStructure(m_root)
        validateMetrics(m_root)

        var freeCount = 0
        var freeNode = m_freeList
        while (freeNode != NULL_NODE) {
            assert(0 <= freeNode && freeNode < m_nodeCapacity)
            freeNode = m_parent[freeNode]
            ++freeCount
        }

        assert(height == computeHeight())
        assert(m_nodeCount + freeCount == m_nodeCapacity)
    }

    // /**
    // * Build an optimal tree. Very expensive. For testing.
    // */
    // public void rebuildBottomUp() {
    // int[] nodes = new int[m_nodeCount];
    // int count = 0;
    //
    // // Build array of leaves. Free the rest.
    // for (int i = 0; i < m_nodeCapacity; ++i) {
    // if (m_nodes[i].height < 0) {
    // // free node in pool
    // continue;
    // }
    //
    // DynamicTreeNode node = m_nodes[i];
    // if (node.isLeaf()) {
    // node.parent = null;
    // nodes[count] = i;
    // ++count;
    // } else {
    // freeNode(node);
    // }
    // }
    //
    // AABB b = new AABB();
    // while (count > 1) {
    // float minCost = Float.MAX_VALUE;
    // int iMin = -1, jMin = -1;
    // for (int i = 0; i < count; ++i) {
    // AABB aabbi = m_nodes[nodes[i]].aabb;
    //
    // for (int j = i + 1; j < count; ++j) {
    // AABB aabbj = m_nodes[nodes[j]].aabb;
    // b.combine(aabbi, aabbj);
    // float cost = b.getPerimeter();
    // if (cost < minCost) {
    // iMin = i;
    // jMin = j;
    // minCost = cost;
    // }
    // }
    // }
    //
    // int index1 = nodes[iMin];
    // int index2 = nodes[jMin];
    // DynamicTreeNode child1 = m_nodes[index1];
    // DynamicTreeNode child2 = m_nodes[index2];
    //
    // DynamicTreeNode parent = allocateNode();
    // parent.child1 = child1;
    // parent.child2 = child2;
    // parent.height = 1 + MathUtils.max(child1.height, child2.height);
    // parent.aabb.combine(child1.aabb, child2.aabb);
    // parent.parent = null;
    //
    // child1.parent = parent;
    // child2.parent = parent;
    //
    // nodes[jMin] = nodes[count - 1];
    // nodes[iMin] = parent.id;
    // --count;
    // }
    //
    // m_root = m_nodes[nodes[0]];
    //
    // validate();
    // }

    private fun allocateNode(): Int {
        if (m_freeList == NULL_NODE) {
            assert(m_nodeCount == m_nodeCapacity)
            m_nodeCapacity *= 2
            expandBuffers(m_nodeCount, m_nodeCapacity)
        }
        assert(m_freeList != NULL_NODE)
        val node = m_freeList
        m_freeList = m_parent[node]
        m_parent[node] = NULL_NODE
        m_child1[node] = NULL_NODE
        m_height[node] = 0
        ++m_nodeCount
        return node
    }

    /**
     * returns a node to the pool
     */
    private fun freeNode(node: Int) {
        assert(node != NULL_NODE)
        assert(0 < m_nodeCount)
        m_parent[node] = if (m_freeList != NULL_NODE) m_freeList else NULL_NODE
        m_height[node] = -1
        m_freeList = node
        m_nodeCount--
    }

    private fun insertLeaf(leaf: Int) {
        if (m_root == NULL_NODE) {
            m_root = leaf
            m_parent[m_root] = NULL_NODE
            return
        }

        // find the best sibling
        val leafAABB = m_aabb[leaf]
        var index = m_root
        while (m_child1[index] != NULL_NODE) {
            val node = index
            val child1 = m_child1[node]
            val child2 = m_child2[node]
            val nodeAABB = m_aabb[node]
            val area = nodeAABB.perimeter

            combinedAABB.combine(nodeAABB, leafAABB)
            val combinedArea = combinedAABB.perimeter

            // Cost of creating a new parent for this node and the new leaf
            val cost = 2.0f * combinedArea

            // Minimum cost of pushing the leaf further down the tree
            val inheritanceCost = 2.0f * (combinedArea - area)

            // Cost of descending into child1
            val cost1: Float
            val child1AABB = m_aabb[child1]
            if (m_child1[child1] == NULL_NODE) {
                combinedAABB.combine(leafAABB, child1AABB)
                cost1 = combinedAABB.perimeter + inheritanceCost
            } else {
                combinedAABB.combine(leafAABB, child1AABB)
                val oldArea = child1AABB.perimeter
                val newArea = combinedAABB.perimeter
                cost1 = newArea - oldArea + inheritanceCost
            }

            // Cost of descending into child2
            val cost2: Float
            val child2AABB = m_aabb[child2]
            if (m_child1[child2] == NULL_NODE) {
                combinedAABB.combine(leafAABB, child2AABB)
                cost2 = combinedAABB.perimeter + inheritanceCost
            } else {
                combinedAABB.combine(leafAABB, child2AABB)
                val oldArea = child2AABB.perimeter
                val newArea = combinedAABB.perimeter
                cost2 = newArea - oldArea + inheritanceCost
            }

            // Descend according to the minimum cost.
            if (cost < cost1 && cost < cost2) {
                break
            }

            // Descend
            if (cost1 < cost2) {
                index = child1
            } else {
                index = child2
            }
        }

        val sibling = index
        val oldParent = m_parent[sibling]
        val newParent = allocateNode()
        m_parent[newParent] = oldParent
        m_userData[newParent] = null
        m_aabb[newParent].combine(leafAABB, m_aabb[sibling])
        m_height[newParent] = m_height[sibling] + 1

        if (oldParent != NULL_NODE) {
            // The sibling was not the root.
            if (m_child1[oldParent] == sibling) {
                m_child1[oldParent] = newParent
            } else {
                m_child2[oldParent] = newParent
            }

            m_child1[newParent] = sibling
            m_child2[newParent] = leaf
            m_parent[sibling] = newParent
            m_parent[leaf] = newParent
        } else {
            // The sibling was the root.
            m_child1[newParent] = sibling
            m_child2[newParent] = leaf
            m_parent[sibling] = newParent
            m_parent[leaf] = newParent
            m_root = newParent
        }

        // Walk back up the tree fixing heights and AABBs
        index = m_parent[leaf]
        while (index != NULL_NODE) {
            index = balance(index)

            val child1 = m_child1[index]
            val child2 = m_child2[index]

            assert(child1 != NULL_NODE)
            assert(child2 != NULL_NODE)

            m_height[index] = 1 + MathUtils.max(m_height[child1], m_height[child2])
            m_aabb[index].combine(m_aabb[child1], m_aabb[child2])

            index = m_parent[index]
        }
        // validate();
    }

    private fun removeLeaf(leaf: Int) {
        if (leaf == m_root) {
            m_root = NULL_NODE
            return
        }

        val parent = m_parent[leaf]
        val grandParent = m_parent[parent]
        val parentChild1 = m_child1[parent]
        val parentChild2 = m_child2[parent]
        val sibling: Int
        if (parentChild1 == leaf) {
            sibling = parentChild2
        } else {
            sibling = parentChild1
        }

        if (grandParent != NULL_NODE) {
            // Destroy parent and connect sibling to grandParent.
            if (m_child1[grandParent] == parent) {
                m_child1[grandParent] = sibling
            } else {
                m_child2[grandParent] = sibling
            }
            m_parent[sibling] = grandParent
            freeNode(parent)

            // Adjust ancestor bounds.
            var index = grandParent
            while (index != NULL_NODE) {
                index = balance(index)

                val child1 = m_child1[index]
                val child2 = m_child2[index]

                m_aabb[index].combine(m_aabb[child1], m_aabb[child2])
                m_height[index] = 1 + MathUtils.max(m_height[child1], m_height[child2])

                index = m_parent[index]
            }
        } else {
            m_root = sibling
            m_parent[sibling] = NULL_NODE
            freeNode(parent)
        }

        // validate();
    }

    // Perform a left or right rotation if node A is imbalanced.
    // Returns the new root index.
    private fun balance(iA: Int): Int {
        assert(iA != NULL_NODE)

        val A = iA
        if (m_child1[A] == NULL_NODE || m_height[A] < 2) {
            return iA
        }

        val iB = m_child1[A]
        val iC = m_child2[A]
        assert(0 <= iB && iB < m_nodeCapacity)
        assert(0 <= iC && iC < m_nodeCapacity)

        val B = iB
        val C = iC

        val balance = m_height[C] - m_height[B]

        // Rotate C up
        if (balance > 1) {
            val iF = m_child1[C]
            val iG = m_child2[C]
            val F = iF
            val G = iG
            // assert (F != null);
            // assert (G != null);
            assert(0 <= iF && iF < m_nodeCapacity)
            assert(0 <= iG && iG < m_nodeCapacity)

            // Swap A and C
            m_child1[C] = iA
            m_parent[C] = m_parent[A]
            val cParent = m_parent[C]
            m_parent[A] = iC

            // A's old parent should point to C
            if (cParent != NULL_NODE) {
                if (m_child1[cParent] == iA) {
                    m_child1[cParent] = iC
                } else {
                    assert(m_child2[cParent] == iA)
                    m_child2[cParent] = iC
                }
            } else {
                m_root = iC
            }

            // Rotate
            if (m_height[F] > m_height[G]) {
                m_child2[C] = iF
                m_child2[A] = iG
                m_parent[G] = iA
                m_aabb[A].combine(m_aabb[B], m_aabb[G])
                m_aabb[C].combine(m_aabb[A], m_aabb[F])

                m_height[A] = 1 + MathUtils.max(m_height[B], m_height[G])
                m_height[C] = 1 + MathUtils.max(m_height[A], m_height[F])
            } else {
                m_child2[C] = iG
                m_child2[A] = iF
                m_parent[F] = iA
                m_aabb[A].combine(m_aabb[B], m_aabb[F])
                m_aabb[C].combine(m_aabb[A], m_aabb[G])

                m_height[A] = 1 + MathUtils.max(m_height[B], m_height[F])
                m_height[C] = 1 + MathUtils.max(m_height[A], m_height[G])
            }

            return iC
        }

        // Rotate B up
        if (balance < -1) {
            val iD = m_child1[B]
            val iE = m_child2[B]
            val D = iD
            val E = iE
            assert(0 <= iD && iD < m_nodeCapacity)
            assert(0 <= iE && iE < m_nodeCapacity)

            // Swap A and B
            m_child1[B] = iA
            m_parent[B] = m_parent[A]
            val Bparent = m_parent[B]
            m_parent[A] = iB

            // A's old parent should point to B
            if (Bparent != NULL_NODE) {
                if (m_child1[Bparent] == iA) {
                    m_child1[Bparent] = iB
                } else {
                    assert(m_child2[Bparent] == iA)
                    m_child2[Bparent] = iB
                }
            } else {
                m_root = iB
            }

            // Rotate
            if (m_height[D] > m_height[E]) {
                m_child2[B] = iD
                m_child1[A] = iE
                m_parent[E] = iA
                m_aabb[A].combine(m_aabb[C], m_aabb[E])
                m_aabb[B].combine(m_aabb[A], m_aabb[D])

                m_height[A] = 1 + MathUtils.max(m_height[C], m_height[E])
                m_height[B] = 1 + MathUtils.max(m_height[A], m_height[D])
            } else {
                m_child2[B] = iE
                m_child1[A] = iD
                m_parent[D] = iA
                m_aabb[A].combine(m_aabb[C], m_aabb[D])
                m_aabb[B].combine(m_aabb[A], m_aabb[E])

                m_height[A] = 1 + MathUtils.max(m_height[C], m_height[D])
                m_height[B] = 1 + MathUtils.max(m_height[A], m_height[E])
            }

            return iB
        }

        return iA
    }

    private fun validateStructure(node: Int) {
        if (node == NULL_NODE) {
            return
        }

        if (node == m_root) {
            assert(m_parent[node] == NULL_NODE)
        }

        val child1 = m_child1[node]
        val child2 = m_child2[node]

        if (child1 == NULL_NODE) {
            assert(child1 == NULL_NODE)
            assert(child2 == NULL_NODE)
            assert(m_height[node] == 0)
            return
        }

        assert(child1 != NULL_NODE && 0 <= child1 && child1 < m_nodeCapacity)
        assert(child2 != NULL_NODE && 0 <= child2 && child2 < m_nodeCapacity)

        assert(m_parent[child1] == node)
        assert(m_parent[child2] == node)

        validateStructure(child1)
        validateStructure(child2)
    }

    private fun validateMetrics(node: Int) {
        if (node == NULL_NODE) {
            return
        }

        val child1 = m_child1[node]
        val child2 = m_child2[node]

        if (child1 == NULL_NODE) {
            assert(child1 == NULL_NODE)
            assert(child2 == NULL_NODE)
            assert(m_height[node] == 0)
            return
        }

        assert(child1 != NULL_NODE && 0 <= child1 && child1 < m_nodeCapacity)
        assert(child2 != child1 && 0 <= child2 && child2 < m_nodeCapacity)

        val height1 = m_height[child1]
        val height2 = m_height[child2]
        val height: Int
        height = 1 + MathUtils.max(height1, height2)
        assert(m_height[node] == height)

        val aabb = AABB()
        aabb.combine(m_aabb[child1], m_aabb[child2])

        assert(aabb.lowerBound == m_aabb[node].lowerBound)
        assert(aabb.upperBound == m_aabb[node].upperBound)

        validateMetrics(child1)
        validateMetrics(child2)
    }

    override fun drawTree(argDraw: DebugDraw) {
        if (m_root == NULL_NODE) {
            return
        }
        val height = computeHeight()
        drawTree(argDraw, m_root, 0, height)
    }

    fun drawTree(argDraw: DebugDraw, node: Int, spot: Int, height: Int) {
        val a = m_aabb[node]
        a.getVertices(drawVecs)

        color.set(1f, (height - spot) * 1f / height, (height - spot) * 1f / height)
        argDraw.drawPolygon(drawVecs, 4, color)

        argDraw.viewportTranform!!.getWorldToScreen(a.upperBound, textVec)
        argDraw.drawString(textVec.x, textVec.y, node.toString() + "-" + (spot + 1) + "/" + height, color)

        val c1 = m_child1[node]
        val c2 = m_child2[node]
        if (c1 != NULL_NODE) {
            drawTree(argDraw, c1, spot + 1, height)
        }
        if (c2 != NULL_NODE) {
            drawTree(argDraw, c2, spot + 1, height)
        }
    }

    companion object {

        val MAX_STACK_SIZE = 64

        val NULL_NODE = -1

        val INITIAL_BUFFER_LENGTH = 16
    }
}
