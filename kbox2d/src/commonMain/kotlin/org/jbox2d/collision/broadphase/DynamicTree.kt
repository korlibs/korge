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
import org.jbox2d.common.Color3f
import org.jbox2d.common.MathUtils
import org.jbox2d.common.Settings
import org.jbox2d.common.Vec2
import org.jbox2d.internal.*

/**
 * A dynamic tree arranges data in a binary tree to accelerate queries such as volume queries and
 * ray casts. Leafs are proxies with an AABB. In the tree we expand the proxy AABB by _fatAABBFactor
 * so that the proxy AABB is bigger than the client object. This allows the client object to move by
 * small amounts without triggering a tree update.
 *
 * @author daniel
 */
class DynamicTree : BroadPhaseStrategy {

    private var m_root: DynamicTreeNode? = null
    private var m_nodes: Array<DynamicTreeNode> = Array(16) { DynamicTreeNode(it) }
    private var m_nodeCount: Int = 0
    private var m_nodeCapacity: Int = 16

    private var m_freeList: Int = 0

    private val drawVecs = Array<Vec2>(4) { Vec2() }
    private var nodeStack = arrayOfNulls<DynamicTreeNode>(20)
    private var nodeStackIndex = 0

    private val r = Vec2()
    private val aabb = AABB()
    private val subInput = RayCastInput()

    override val height: Int
        get() = if (m_root == null) {
            0
        } else m_root!!.height

    override val maxBalance: Int
        get() {
            var maxBalance = 0
            for (i in 0 until m_nodeCapacity) {
                val node = m_nodes!![i]
                if (node.height <= 1) {
                    continue
                }

                assert(node.child1 == null == false)

                val child1 = node.child1
                val child2 = node.child2
                val balance = MathUtils.abs(child2!!.height - child1!!.height)
                maxBalance = MathUtils.max(maxBalance, balance)
            }

            return maxBalance
        }

    override// Free node in pool
    val areaRatio: Float
        get() {
            if (m_root == null) {
                return 0.0f
            }

            val root = m_root
            val rootArea = root!!.aabb.perimeter

            var totalArea = 0.0f
            for (i in 0 until m_nodeCapacity) {
                val node = m_nodes!![i]
                if (node.height < 0) {
                    continue
                }

                totalArea += node.aabb.perimeter
            }

            return totalArea / rootArea
        }

    private val combinedAABB = AABB()

    private val color = Color3f()
    private val textVec = Vec2()

    init {
        // Build a linked list for the free list.
        for (i in m_nodeCapacity - 1 downTo 0) {
            m_nodes!![i].parent = if (i == m_nodeCapacity - 1) null else m_nodes!![i + 1]
            m_nodes!![i].height = -1
        }
    }

    override fun createProxy(aabb: AABB, userData: Any): Int {
        assert(aabb.isValid)
        val node = allocateNode()
        val proxyId = node.id
        // Fatten the aabb
        val nodeAABB = node.aabb
        nodeAABB.lowerBound.x = aabb.lowerBound.x - Settings.aabbExtension
        nodeAABB.lowerBound.y = aabb.lowerBound.y - Settings.aabbExtension
        nodeAABB.upperBound.x = aabb.upperBound.x + Settings.aabbExtension
        nodeAABB.upperBound.y = aabb.upperBound.y + Settings.aabbExtension
        node.userData = userData

        insertLeaf(proxyId)

        return proxyId
    }

    override fun destroyProxy(proxyId: Int) {
        assert(0 <= proxyId && proxyId < m_nodeCapacity)
        val node = m_nodes!![proxyId]
        assert(node.child1 == null)

        removeLeaf(node)
        freeNode(node)
    }

    override fun moveProxy(proxyId: Int, aabb: AABB, displacement: Vec2): Boolean {
        assert(aabb.isValid)
        assert(0 <= proxyId && proxyId < m_nodeCapacity)
        val node = m_nodes!![proxyId]
        assert(node.child1 == null)

        val nodeAABB = node.aabb
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
        assert(0 <= proxyId && proxyId < m_nodeCapacity)
        return m_nodes!![proxyId].userData
    }

    override fun getFatAABB(proxyId: Int): AABB {
        assert(0 <= proxyId && proxyId < m_nodeCapacity)
        return m_nodes!![proxyId].aabb
    }

    override fun query(callback: TreeCallback, aabb: AABB) {
        assert(aabb.isValid)
        nodeStackIndex = 0
        nodeStack[nodeStackIndex++] = m_root

        while (nodeStackIndex > 0) {
            val node = nodeStack[--nodeStackIndex] ?: continue

            if (AABB.testOverlap(node.aabb, aabb)) {
                if (node.child1 == null) {
                    val proceed = callback.treeCallback(node.id)
                    if (!proceed) {
                        return
                    }
                } else {
                    if (nodeStack.size - nodeStackIndex - 2 <= 0) {
                        val newBuffer = arrayOfNulls<DynamicTreeNode>(nodeStack.size * 2)
                        arraycopy(nodeStack, 0, newBuffer, 0, nodeStack.size)
                        nodeStack = newBuffer
                    }
                    nodeStack[nodeStackIndex++] = node.child1
                    nodeStack[nodeStackIndex++] = node.child2
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
            val node = nodeStack[--nodeStackIndex] ?: continue

            val nodeAABB = node.aabb
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

            if (node.child1 == null) {
                subInput.p1.x = p1x
                subInput.p1.y = p1y
                subInput.p2.x = p2x
                subInput.p2.y = p2y
                subInput.maxFraction = maxFraction

                val value = callback.raycastCallback(subInput, node.id)

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
                if (nodeStack.size - nodeStackIndex - 2 <= 0) {
                    val newBuffer = arrayOfNulls<DynamicTreeNode>(nodeStack.size * 2)
                    arraycopy(nodeStack, 0, newBuffer, 0, nodeStack.size)
                    nodeStack = newBuffer
                }
                nodeStack[nodeStackIndex++] = node.child1
                nodeStack[nodeStackIndex++] = node.child2
            }
        }
    }

    override fun computeHeight(): Int {
        return computeHeight(m_root!!)
    }

    private fun computeHeight(node: DynamicTreeNode): Int {
        assert(0 <= node.id && node.id < m_nodeCapacity)

        if (node.child1 == null) {
            return 0
        }
        val height1 = computeHeight(node.child1!!)
        val height2 = computeHeight(node.child2!!)
        return 1 + MathUtils.max(height1, height2)
    }

    /**
     * Validate this tree. For testing.
     */
    fun validate() {
        validateStructure(m_root)
        validateMetrics(m_root)

        var freeCount = 0
        var freeNode: DynamicTreeNode? = if (m_freeList != NULL_NODE) m_nodes!![m_freeList] else null
        while (freeNode != null) {
            assert(0 <= freeNode.id && freeNode.id < m_nodeCapacity)
            assert(freeNode === m_nodes!![freeNode.id])
            freeNode = freeNode.parent
            ++freeCount
        }

        assert(height == computeHeight())

        assert(m_nodeCount + freeCount == m_nodeCapacity)
    }

    /**
     * Build an optimal tree. Very expensive. For testing.
     */
    fun rebuildBottomUp() {
        val nodes = IntArray(m_nodeCount)
        var count = 0

        // Build array of leaves. Free the rest.
        for (i in 0 until m_nodeCapacity) {
            if (m_nodes!![i].height < 0) {
                // free node in pool
                continue
            }

            val node = m_nodes!![i]
            if (node.child1 == null) {
                node.parent = null
                nodes[count] = i
                ++count
            } else {
                freeNode(node)
            }
        }

        val b = AABB()
        while (count > 1) {
            var minCost = Float.MAX_VALUE
            var iMin = -1
            var jMin = -1
            for (i in 0 until count) {
                val aabbi = m_nodes!![nodes[i]].aabb

                for (j in i + 1 until count) {
                    val aabbj = m_nodes!![nodes[j]].aabb
                    b.combine(aabbi, aabbj)
                    val cost = b.perimeter
                    if (cost < minCost) {
                        iMin = i
                        jMin = j
                        minCost = cost
                    }
                }
            }

            val index1 = nodes[iMin]
            val index2 = nodes[jMin]
            val child1 = m_nodes!![index1]
            val child2 = m_nodes!![index2]

            val parent = allocateNode()
            parent.child1 = child1
            parent.child2 = child2
            parent.height = 1 + MathUtils.max(child1.height, child2.height)
            parent.aabb.combine(child1.aabb, child2.aabb)
            parent.parent = null

            child1.parent = parent
            child2.parent = parent

            nodes[jMin] = nodes[count - 1]
            nodes[iMin] = parent.id
            --count
        }

        m_root = m_nodes!![nodes[0]]

        validate()
    }

    private fun allocateNode(): DynamicTreeNode {
        if (m_freeList == NULL_NODE) {
            assert(m_nodeCount == m_nodeCapacity)

            val old = m_nodes
            m_nodeCapacity *= 2
            m_nodes = arrayOfNulls<DynamicTreeNode>(m_nodeCapacity) as Array<DynamicTreeNode>
            arraycopy(old!!, 0, m_nodes!!, 0, old.size)

            // Build a linked list for the free list.
            for (i in m_nodeCapacity - 1 downTo m_nodeCount) {
                m_nodes!![i] = DynamicTreeNode(i)
                m_nodes!![i].parent = if (i == m_nodeCapacity - 1) null else m_nodes!![i + 1]
                m_nodes!![i].height = -1
            }
            m_freeList = m_nodeCount
        }
        val nodeId = m_freeList
        val treeNode = m_nodes!![nodeId]
        m_freeList = if (treeNode.parent != null) treeNode.parent!!.id else NULL_NODE

        treeNode.parent = null
        treeNode.child1 = null
        treeNode.child2 = null
        treeNode.height = 0
        treeNode.userData = null
        ++m_nodeCount
        return treeNode
    }

    /**
     * returns a node to the pool
     */
    private fun freeNode(node: DynamicTreeNode) {
        assert(node != null)
        assert(0 < m_nodeCount)
        node.parent = if (m_freeList != NULL_NODE) m_nodes!![m_freeList] else null
        node.height = -1
        m_freeList = node.id
        m_nodeCount--
    }

    private fun insertLeaf(leaf_index: Int) {
        val leaf = m_nodes!![leaf_index]
        if (m_root == null) {
            m_root = leaf
            m_root!!.parent = null
            return
        }

        // find the best sibling
        val leafAABB = leaf.aabb
        var index = m_root
        while (index!!.child1 != null) {
            val node = index
            val child1 = node.child1
            val child2 = node.child2

            val area = node.aabb.perimeter

            combinedAABB.combine(node.aabb, leafAABB)
            val combinedArea = combinedAABB.perimeter

            // Cost of creating a new parent for this node and the new leaf
            val cost = 2.0f * combinedArea

            // Minimum cost of pushing the leaf further down the tree
            val inheritanceCost = 2.0f * (combinedArea - area)

            // Cost of descending into child1
            val cost1: Float
            if (child1!!.child1 == null) {
                combinedAABB.combine(leafAABB, child1.aabb)
                cost1 = combinedAABB.perimeter + inheritanceCost
            } else {
                combinedAABB.combine(leafAABB, child1.aabb)
                val oldArea = child1.aabb.perimeter
                val newArea = combinedAABB.perimeter
                cost1 = newArea - oldArea + inheritanceCost
            }

            // Cost of descending into child2
            val cost2: Float
            if (child2!!.child1 == null) {
                combinedAABB.combine(leafAABB, child2.aabb)
                cost2 = combinedAABB.perimeter + inheritanceCost
            } else {
                combinedAABB.combine(leafAABB, child2.aabb)
                val oldArea = child2.aabb.perimeter
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
        val oldParent = m_nodes!![sibling.id].parent
        val newParent = allocateNode()
        newParent.parent = oldParent
        newParent.userData = null
        newParent.aabb.combine(leafAABB, sibling.aabb)
        newParent.height = sibling.height + 1

        if (oldParent != null) {
            // The sibling was not the root.
            if (oldParent.child1 === sibling) {
                oldParent.child1 = newParent
            } else {
                oldParent.child2 = newParent
            }

            newParent.child1 = sibling
            newParent.child2 = leaf
            sibling.parent = newParent
            leaf.parent = newParent
        } else {
            // The sibling was the root.
            newParent.child1 = sibling
            newParent.child2 = leaf
            sibling.parent = newParent
            leaf.parent = newParent
            m_root = newParent
        }

        // Walk back up the tree fixing heights and AABBs
        index = leaf.parent
        while (index != null) {
            index = balance(index)

            val child1 = index.child1
            val child2 = index.child2

            assert(child1 != null)
            assert(child2 != null)

            index.height = 1 + MathUtils.max(child1!!.height, child2!!.height)
            index.aabb.combine(child1.aabb, child2.aabb)

            index = index.parent
        }
        // validate();
    }

    private fun removeLeaf(leaf: DynamicTreeNode) {
        if (leaf === m_root) {
            m_root = null
            return
        }

        val parent = leaf.parent
        val grandParent = parent?.parent
        val sibling: DynamicTreeNode
        if (parent!!.child1 === leaf) {
            sibling = parent!!.child2!!
        } else {
            sibling = parent!!.child1!!
        }

        if (grandParent != null) {
            // Destroy parent and connect sibling to grandParent.
            if (grandParent.child1 === parent) {
                grandParent.child1 = sibling
            } else {
                grandParent.child2 = sibling
            }
            sibling.parent = grandParent
            freeNode(parent)

            // Adjust ancestor bounds.
            var index = grandParent
            while (index != null) {
                index = balance(index)

                val child1 = index.child1
                val child2 = index.child2

                index.aabb.combine(child1!!.aabb, child2!!.aabb)
                index.height = 1 + MathUtils.max(child1.height, child2.height)

                index = index.parent
            }
        } else {
            m_root = sibling
            sibling.parent = null
            freeNode(parent)
        }

        // validate();
    }

    // Perform a left or right rotation if node A is imbalanced.
    // Returns the new root index.
    private fun balance(iA: DynamicTreeNode?): DynamicTreeNode {
        assert(iA != null)

        val A = iA
        if (A!!.child1 == null || A.height < 2) {
            return iA
        }

        val iB = A.child1
        val iC = A.child2
        assert(0 <= iB!!.id && iB.id < m_nodeCapacity)
        assert(0 <= iC!!.id && iC.id < m_nodeCapacity)

        val B = iB
        val C = iC

        val balance = C.height - B.height

        // Rotate C up
        if (balance > 1) {
            val iF = C.child1
            val iG = C.child2
            val F = iF
            val G = iG
            assert(F != null)
            assert(G != null)
            assert(0 <= iF!!.id && iF.id < m_nodeCapacity)
            assert(0 <= iG!!.id && iG.id < m_nodeCapacity)

            // Swap A and C
            C.child1 = iA
            C.parent = A.parent
            A.parent = iC

            // A's old parent should point to C
            if (C.parent != null) {
                if (C.parent!!.child1 === iA) {
                    C.parent!!.child1 = iC
                } else {
                    assert(C.parent!!.child2 === iA)
                    C.parent!!.child2 = iC
                }
            } else {
                m_root = iC
            }

            // Rotate
            if (F!!.height > G!!.height) {
                C.child2 = iF
                A.child2 = iG
                G.parent = iA
                A.aabb.combine(B.aabb, G.aabb)
                C.aabb.combine(A.aabb, F.aabb)

                A.height = 1 + MathUtils.max(B.height, G.height)
                C.height = 1 + MathUtils.max(A.height, F.height)
            } else {
                C.child2 = iG
                A.child2 = iF
                F.parent = iA
                A.aabb.combine(B.aabb, F.aabb)
                C.aabb.combine(A.aabb, G.aabb)

                A.height = 1 + MathUtils.max(B.height, F.height)
                C.height = 1 + MathUtils.max(A.height, G.height)
            }

            return iC
        }

        // Rotate B up
        if (balance < -1) {
            val iD = B.child1
            val iE = B.child2
            val D = iD
            val E = iE
            assert(0 <= iD!!.id && iD.id < m_nodeCapacity)
            assert(0 <= iE!!.id && iE.id < m_nodeCapacity)

            // Swap A and B
            B.child1 = iA
            B.parent = A.parent
            A.parent = iB

            // A's old parent should point to B
            if (B.parent != null) {
                if (B.parent!!.child1 === iA) {
                    B.parent!!.child1 = iB
                } else {
                    assert(B.parent!!.child2 === iA)
                    B.parent!!.child2 = iB
                }
            } else {
                m_root = iB
            }

            // Rotate
            if (D!!.height > E!!.height) {
                B.child2 = iD
                A.child1 = iE
                E.parent = iA
                A.aabb.combine(C.aabb, E.aabb)
                B.aabb.combine(A.aabb, D.aabb)

                A.height = 1 + MathUtils.max(C.height, E.height)
                B.height = 1 + MathUtils.max(A.height, D.height)
            } else {
                B.child2 = iE
                A.child1 = iD
                D.parent = iA
                A.aabb.combine(C.aabb, D.aabb)
                B.aabb.combine(A.aabb, E.aabb)

                A.height = 1 + MathUtils.max(C.height, D.height)
                B.height = 1 + MathUtils.max(A.height, E.height)
            }

            return iB
        }

        return iA
    }

    private fun validateStructure(node: DynamicTreeNode?) {
        if (node == null) {
            return
        }
        assert(node === m_nodes!![node.id])

        if (node === m_root) {
            assert(node.parent == null)
        }

        val child1 = node.child1
        val child2 = node.child2

        if (node.child1 == null) {
            assert(child1 == null)
            assert(child2 == null)
            assert(node.height == 0)
            return
        }

        assert(child1 != null && 0 <= child1.id && child1.id < m_nodeCapacity)
        assert(child2 != null && 0 <= child2.id && child2.id < m_nodeCapacity)

        assert(child1!!.parent === node)
        assert(child2!!.parent === node)

        validateStructure(child1)
        validateStructure(child2)
    }

    private fun validateMetrics(node: DynamicTreeNode?) {
        if (node == null) {
            return
        }

        val child1 = node.child1
        val child2 = node.child2

        if (node.child1 == null) {
            assert(child1 == null)
            assert(child2 == null)
            assert(node.height == 0)
            return
        }

        assert(child1 != null && 0 <= child1.id && child1.id < m_nodeCapacity)
        assert(child2 != null && 0 <= child2.id && child2.id < m_nodeCapacity)

        val height1 = child1!!.height
        val height2 = child2!!.height
        val height: Int
        height = 1 + MathUtils.max(height1, height2)
        assert(node.height == height)

        val aabb = AABB()
        aabb.combine(child1.aabb, child2.aabb)

        assert(aabb.lowerBound == node.aabb.lowerBound)
        assert(aabb.upperBound == node.aabb.upperBound)

        validateMetrics(child1)
        validateMetrics(child2)
    }

    override fun drawTree(argDraw: DebugDraw) {
        if (m_root == null) {
            return
        }
        val height = computeHeight()
        drawTree(argDraw, m_root!!, 0, height)
    }

    fun drawTree(argDraw: DebugDraw, node: DynamicTreeNode, spot: Int, height: Int) {
        node.aabb.getVertices(drawVecs!!)

        color.set(1f, (height - spot) * 1f / height, (height - spot) * 1f / height)
        argDraw.drawPolygon(drawVecs!! as Array<Vec2>, 4, color)

        argDraw.viewportTranform!!.getWorldToScreen(node.aabb.upperBound, textVec)
        argDraw.drawString(textVec.x, textVec.y, node.id.toString() + "-" + (spot + 1) + "/" + height, color)

        if (node.child1 != null) {
            drawTree(argDraw, node.child1!!, spot + 1, height)
        }
        if (node.child2 != null) {
            drawTree(argDraw, node.child2!!, spot + 1, height)
        }
    }

    companion object {

        val MAX_STACK_SIZE = 64

        val NULL_NODE = -1
    }
}
