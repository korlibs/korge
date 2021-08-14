/*
// Copyright(C) David W. Jeske, 2014, and released to the public domain.
//
// Dynamic BVH (Bounding Volume Hierarchy) using incremental refit and tree-rotations
//
// initial BVH build based on: Bounding Volume Hierarchies (BVH) – A brief tutorial on what they are and how to implement them
//              http://www.3dmuve.com/3dmblog/?p=182
//
// Dynamic Updates based on: "Fast, Effective BVH Updates for Animated Scenes" (Kopta, Ize, Spjut, Brunvand, David, Kensler)
//              https://github.com/jeske/SimpleScene/blob/master/SimpleScene/Util/BVH/docs/BVH_fast_effective_updates_for_animated_scenes.pdf
//
// see also:  Space Partitioning: Octree vs. BVH
//            http://thomasdiewald.com/blog/?p=1488
//
//

@file:Suppress("MemberVisibilityCanBePrivate")

package com.soywiz.korma.geom.ds

import com.soywiz.kds.*
import com.soywiz.kds.iterators.*
import com.soywiz.korma.geom.*
import kotlin.math.*

// Other BVH implementation: https://github.com/imbcmdth/jsBVH/blob/master/src/jsbvh.js

/** Binary Volume Hierarchy */
class CSBVH<GO> private constructor(
    dummy: Boolean,
    val nAda: NodeAdaptor<GO>,
    val LEAF_OBJ_MAX: Int = 1,
) {
    val leafs = LinkedHashMap<GO, CSBVH.BVHNode<GO>>()
    fun mapObjectToBVHLeaf(obj: GO, leaf: CSBVH.BVHNode<GO>) {
        leafs[obj] = leaf
    }
    fun unmapObject(obj: GO) {
        leafs.remove(obj)
    }
    fun checkMap(obj: GO) {
        if (obj !in leafs) error("missing map from shuffled child")
    }
    fun getLeaf(obj: GO): CSBVH.BVHNode<GO> = leafs[obj]!!

    interface NodeAdaptor<T> {
        fun objectpos(obj: T): IVector3
        fun radius(obj: T): Float
    }

    enum class Axis {
        X, Y, Z;

        val next: Axis get() =
            when (this) {
                X -> Y
                Y -> Z
                Z -> X
            }
    }

    lateinit var rootBVH: BVHNode<GO>

    init {
        if (LEAF_OBJ_MAX != 1) {
            println("Warning BVH dynamic updates not supported when LEAF_OBJ_MAX != 1 (LEAF_OBJ_MAX = $LEAF_OBJ_MAX)")
        }
    }

    var nodeCount = 0
    var maxDepth = 0

    val refitNodes = HashSet<BVHNode<GO>>()

    companion object {
        // WARNING! currently this must be 1 to use dynamic BVH updates
        operator fun <GO> invoke(nodeAdaptor: NodeAdaptor<GO>, objects: FastArrayList<GO> = FastArrayList(), LEAF_OBJ_MAX: Int = 1): CSBVH<GO> =
            CSBVH(true, nodeAdaptor, LEAF_OBJ_MAX).also { bvh ->
                //nodeAdaptor.BVH = bvh
                bvh.rootBVH = when {
                    objects.isNotEmpty() -> BVHNode<GO>(bvh, objects)
                    else -> BVHNode<GO>(bvh).also { it.gobjects = fastArrayListOf() }
                }
            }

        fun <T> List<T>.GetRange(index: Int, count: Int) = this.subList(index, index + count)
    }

    // public interface to traversal..
    fun traverse(hitTest: (box: AABB3D) -> Boolean): List<BVHNode<GO>> {
        val hits = FastArrayList<BVHNode<GO>>()
        val exploreNodes = Deque<BVHNode<GO>>()
        exploreNodes.add(rootBVH)
        var iterCount = 0

        while (exploreNodes.isNotEmpty()) {
            val curNode = exploreNodes.removeFirst()
            iterCount++
            if (hitTest(curNode.box)) {
                hits.add(curNode)
                curNode.left?.let { exploreNodes.add(it) }
                curNode.right?.let { exploreNodes.add(it) }
            }
        }

        //println("traverse.iterCount=$iterCount")

        return hits
    }

    fun traverse(ray: Ray3D): List<BVHNode<GO>> = traverse { box -> ray.intersectRayAABox1(box) }
    fun traverse(volume: AABB3D): List<BVHNode<GO>> = traverse { it.intersectsAABB(volume) }

    fun optimize() {
        if (LEAF_OBJ_MAX != 1) {
            throw Exception("In order to use optimize, you must set LEAF_OBJ_MAX=1")
        }

        while (refitNodes.size > 0) {
            val maxdepth = refitNodes.maxOf { it.depth }

            val sweepNodes = refitNodes.filter { n -> n.depth == maxdepth }
            refitNodes.removeAll(sweepNodes)
            sweepNodes.fastForEach { it.tryRotate(this) }
        }
    }

    fun addObject(newOb: GO) {
        val box = AABB3D.fromSphere(nAda.objectpos(newOb), nAda.radius(newOb))
        val boxSAH = BVHNode.SA(box)
        rootBVH.addObject(this,newOb, box, boxSAH)
    }

    fun removeObject(newObj: GO) {
        val leaf = this.getLeaf(newObj)
        leaf.removeObject(this,newObj)
    }

    fun countBVHNodes(): Int {
        return rootBVH.countBVHNodes()
    }

    class BVHNode<GO>(val bvh: CSBVH<GO>) {
        var box: AABB3D = AABB3D()

        //val value: GO get() = bvh.leafs

        //val value: GO get() = gobjects!!.first()

        var parent: BVHNode<GO>? = null
        var left: BVHNode<GO>? = null
        var right: BVHNode<GO>? = null

        var depth: Int = 0
        var nodeNumber: Int = bvh.nodeCount++ // for debugging

        var gobjects: FastArrayList<GO>? = FastArrayList<GO>()  // only populated in leaf nodes

        internal constructor(bvh: CSBVH<GO>, gobjectlist: FastArrayList<GO>) : this (bvh,null, gobjectlist, Axis.X,0)

        private constructor(bvh: CSBVH<GO>, lparent: BVHNode<GO>?, gobjectlist: FastArrayList<GO>, lastSplitAxis: Axis, curdepth: Int) : this(bvh) {
            this.nodeNumber = bvh.nodeCount++

            this.parent = lparent // save off the parent BVHGObj Node
            this.depth = curdepth

            if (bvh.maxDepth < curdepth) {
                bvh.maxDepth = curdepth
            }

            // Early out check due to bad data
            // If the list is empty then we have no BVHGObj, or invalid parameters are passed in
            if (gobjectlist.isEmpty()) {
                throw Exception("BVHNode constructed with invalid paramaters")
            }

            // Check if we’re at our LEAF node, and if so, save the objects and stop recursing.  Also store the min/max for the leaf node and update the parent appropriately
            if (gobjectlist.size <= bvh.LEAF_OBJ_MAX)
            {
                // once we reach the leaf node, we must set prev/next to null to signify the end
                left = null
                right = null
                // at the leaf node we store the remaining objects, so initialize a list
                gobjects = gobjectlist
                gobjects!!.fastForEach {
                    bvh.mapObjectToBVHLeaf(it, this)
                }
                computeVolume(bvh)
                splitIfNecessary(bvh)
            } else {
                // --------------------------------------------------------------------------------------------
                // if we have more than (bvh.LEAF_OBJECT_COUNT) objects, then compute the volume and split
                gobjects = gobjectlist
                computeVolume(bvh)
                splitNode(bvh)
                childRefit(bvh,propagate = false)
            }
        }


        override fun toString(): String = "BVHNode($nodeNumber)"

        private fun pickSplitAxis(): Axis {
            val box = this.box
            val axis_x = box.max.x - box.min.x
            val axis_y = box.max.y - box.min.y
            val axis_z = box.max.z - box.min.z

            // return the biggest axis
            if (axis_x > axis_y) {
                if (axis_x > axis_z) {
                    return Axis.X
                } else {
                    return Axis.Z
                }
            } else {
                if (axis_y > axis_z) {
                    return Axis.Y
                } else {
                    return Axis.Z
                }
            }

        }
        val IsLeaf: Boolean get() {
            val isLeaf = (this.gobjects != null)
            // if we're a leaf, then both left and right should be null..
            if (isLeaf &&  ( (right != null) || (left != null) ) ) {
                throw Exception("BVH Leaf has objects and left/right pointers!")
            }
            return isLeaf
        }

        private fun NextAxis(cur: Axis): Axis = cur.next

        fun refit_ObjectChanged(bvh: CSBVH<GO>, obj: GO) {
            if (gobjects == null) { throw Exception("dangling leaf!"); }
            if ( refitVolume(bvh) ) {
                // add our parent to the optimize list...
                if (parent != null) {
                    bvh.refitNodes.add(parent!!)

                    // you can force an optimize every time something moves, but it's not very efficient
                    // instead we do this per-frame after a bunch of updates.
                    // nAda.BVH.optimize();                    
                }
            }
        }

        private fun expandVolume(nAda: NodeAdaptor<GO>, objectpos: IVector3, radius: Float) {
            var expanded = false
            val box = this.box

            // test min X and max X against the current bounding volume
            if ((objectpos.x - radius) < box.min.x) {
                box.min.x = (objectpos.x - radius); expanded = true
            }
            if ((objectpos.x + radius) > box.max.x) {
                box.max.x = (objectpos.x + radius); expanded = true
            }
            // test min Y and max Y against the current bounding volume
            if ((objectpos.y - radius) < box.min.y) {
                box.min.y = (objectpos.y - radius); expanded = true
            }
            if ((objectpos.y + radius) > box.max.y) {
                box.max.y = (objectpos.y + radius); expanded = true
            }
            // test min Z and max Z against the current bounding volume
            if ((objectpos.z - radius) < box.min.z ) {
                box.min.z = (objectpos.z - radius); expanded = true
            }
            if ((objectpos.z + radius) > box.max.z ) {
                box.max.z = (objectpos.z + radius); expanded = true
            }

            if (expanded && parent != null) {
                parent!!.childExpanded(nAda, this)
            }
        }

        private fun assignVolume(objectpos: IVector3, radius: Float) {
            val box = this.box
            box.min.x = objectpos.x - radius
            box.max.x = objectpos.x + radius
            box.min.y = objectpos.y - radius
            box.max.y = objectpos.y + radius
            box.min.z = objectpos.z - radius
            box.max.z = objectpos.z + radius
        }

        internal fun computeVolume(bvh: CSBVH<GO>) {
            val gobjects = gobjects!!
            val nAda = bvh.nAda
            assignVolume( nAda.objectpos(gobjects[0]), nAda.radius(gobjects[0]))
            for (i in 1 until gobjects.size) {
                expandVolume( nAda, nAda.objectpos(gobjects[i]) , nAda.radius(gobjects[i]) )
            }
        }

        internal fun refitVolume(bvh: CSBVH<GO>): Boolean {
            if (gobjects == null || gobjects!!.size == 0) TODO()  // TODO: fix this... we should never get called in this case...

            val oldbox = box

            computeVolume(bvh)
            if (box != oldbox) {
                if (parent != null) parent!!.childRefit(bvh)
                return true
            } else {
                return false
            }
        }

        internal fun SAofPair(nodea: BVHNode<GO>, nodeb: BVHNode<GO> ): Float {
            val box = nodea.box
            box.expandToFit(nodeb.box)
            return SA(box)
        }
        internal fun SAofPair(boxa: AABB3D, boxb: AABB3D ): Float {
            val pairbox = boxa
            pairbox.expandToFit(boxb)
            return SA(pairbox)
        }

        internal fun SAofList(nAda: NodeAdaptor<GO>, list: List<GO>): Float {
            val box = AABBofOBJ(nAda, list[0])

            list.toList().GetRange(1, list.size - 1).fastForEach { obj ->
                val newbox = AABBofOBJ(nAda, obj)
                box.expandBy(newbox)
            }
            return SA(box)
        }

        // The list of all candidate rotations, from "Fast, Effective BVH Updates for Animated Scenes", Figure 1.
        internal enum class Rot {
            NONE, L_RL, L_RR, R_LL, R_LR, LL_RR, LL_RL,
        }

        internal class rotOpt(
            val SAH: Float, val rot: Rot,
        ) : Comparable<rotOpt> {  // rotation option
            override fun compareTo(other: rotOpt): Int = SAH.compareTo(other.SAH)
        }

        /// <summary>
        /// tryRotate looks at all candidate rotations, and executes the rotation with the best resulting SAH (if any)
        /// </summary>
        /// <param name="bvh"></param>
        internal fun tryRotate(bvh: CSBVH<GO>) {
            val nAda = bvh.nAda
            //val left = this.left!!
            //val right = this.right!!

            // if we are not a grandparent, then we can't rotate, so queue our parent and bail out
            if (left!!.IsLeaf && right!!.IsLeaf) {
                if (parent != null) {
                    bvh.refitNodes.add(parent!!)
                    return
                }
            }

            // for each rotation, check that there are grandchildren as necessary (aka not a leaf)
            // then compute total SAH cost of our branches after the rotation.

            val mySA = SA(left!!) + SA(right!!)

            val bestRot = eachRot.minOf { rot ->
                val right = right!!
                val left = left!!
                when (rot) {
                    Rot.NONE -> rotOpt(mySA,Rot.NONE)
                    // child to grandchild rotations
                    Rot.L_RL -> if (right.IsLeaf) rotOpt(Float.MAX_VALUE,Rot.NONE); else rotOpt(SA(right!!.left!!) + SA(AABBofPair(left,right!!.right!!)), rot)
                    Rot.L_RR -> if (right.IsLeaf) rotOpt(Float.MAX_VALUE,Rot.NONE);else rotOpt(SA(right!!.right!!) + SA(AABBofPair(left,right!!.left!!)), rot)
                    Rot.R_LL -> if (left.IsLeaf) rotOpt(Float.MAX_VALUE,Rot.NONE);else  rotOpt(SA(AABBofPair(right,left!!.right!!)) + SA(left.left!!), rot)
                    Rot.R_LR -> if (left.IsLeaf) rotOpt(Float.MAX_VALUE,Rot.NONE);else rotOpt(SA(AABBofPair(right,left!!.left!!)) + SA(left.right!!), rot)
                    // grandchild to grandchild rotations
                    Rot.LL_RR -> if (left.IsLeaf || right.IsLeaf) rotOpt(Float.MAX_VALUE,Rot.NONE); else rotOpt(SA(AABBofPair(right.right!!,left.right!!)) + SA(AABBofPair(right.left!!,left.left!!)), rot)
                    Rot.LL_RL -> if (left.IsLeaf || right.IsLeaf) rotOpt(Float.MAX_VALUE,Rot.NONE); else rotOpt(SA(AABBofPair(right.left!!,left.right!!)) + SA(AABBofPair(left.left!!,right.right!!)), rot)
                    // unknown...
                    else -> TODO("missing implementation for BVH Rotation SAH Computation .. $rot")
                }
            }

            // perform the best rotation...            
            if (bestRot.rot != Rot.NONE) {
                // if the best rotation is no-rotation... we check our parents anyhow..                
                if (parent != null) {
                    // but only do it some random percentage of the time.
                    //if ((DateTime.Now.Ticks % 100) < 2) {
                    bvh.refitNodes.add(parent!!)
                    //}
                }
            } else {

                if (parent != null) { bvh.refitNodes.add(parent!!); }

                if ( ((mySA - bestRot.SAH) / mySA ) < 0.3f) {
                    return // the benefit is not worth the cost
                }
                println("BVH swap ${bestRot.rot} from ${mySA} to ${bestRot.SAH}")

                // in order to swap we need to:
                //  1. swap the node locations
                //  2. update the depth (if child-to-grandchild)
                //  3. update the parent pointers
                //  4. refit the boundary box
                var swap: BVHNode<GO>? = null
                when (bestRot.rot) {
                    Rot.NONE -> Unit
                    // child to grandchild rotations
                    Rot.L_RL -> { swap = left;  left  = right!!.left;   left!!.parent = this;  right!!.left  = swap;  swap!!.parent = right; right!!.childRefit(bvh,propagate = false); }
                    Rot.L_RR -> { swap = left;  left  = right!!.right;  left!!.parent = this;  right!!.right = swap;  swap!!.parent = right; right!!.childRefit(bvh,propagate = false); }
                    Rot.R_LL -> { swap = right; right = left!!.left;    right!!.parent = this; left!!.left  = swap;   swap!!.parent = left;   left!!.childRefit(bvh,propagate = false); }
                    Rot.R_LR -> { swap = right; right = left!!.right;   right!!.parent = this; left!!.right = swap;   swap!!.parent = left;   left!!.childRefit(bvh,propagate = false); }

                    // grandchild to grandchild rotations
                    Rot.LL_RR -> { swap = left!!.left; left!!.left = right!!.right; right!!.right = swap; left!!.left!!.parent = left; swap!!.parent = right; left!!.childRefit(bvh,propagate = false); right!!.childRefit(bvh,propagate = false); }
                    Rot.LL_RL -> { swap = left!!.left; left!!.left = right!!.left;  right!!.left  = swap; left!!.left!!.parent = left; swap!!.parent = right; left!!.childRefit(bvh,propagate = false); right!!.childRefit(bvh,propagate = false); }

                    // unknown...
                    else -> TODO("""missing implementation for BVH Rotation .. ${bestRot.rot}""")
                }

                // fix the depths if necessary....
                when (bestRot.rot) {
                    Rot.L_RL, Rot.L_RR, Rot.R_LL, Rot.R_LR -> this.setDepth(bvh,this.depth)
                }
            }
        }

        internal class SplitAxisOpt<GO>(
            val SAH: Float,
            val axis: Axis,
            val left: FastArrayList<GO>,
            val right: FastArrayList<GO>
        ) : Comparable<SplitAxisOpt<GO>> {  // split Axis option
            override fun compareTo(other: SplitAxisOpt<GO>): Int = SAH.compareTo(other.SAH)
        }

        internal fun splitNode(bvh: CSBVH<GO>) {
            // second, decide which axis to split on, and sort..
            val nAda = bvh.nAda
            val splitlist = gobjects!!
            splitlist.fastForEach { bvh.unmapObject(it) }
            val center = (splitlist.size / 2) // find the center object

            val bestSplit = eachAxis.minOf { axis ->
                val orderedlist = splitlist.toMutableList()
                when (axis) {
                    Axis.X -> { orderedlist.sortWith { go1, go2 ->
                        nAda.objectpos(go1).x.compareTo(
                            nAda.objectpos(
                                go2
                            ).x
                        );
                    }; }
                    Axis.Y -> { orderedlist.sortWith { go1, go2 ->
                        nAda.objectpos(go1).y.compareTo(
                            nAda.objectpos(
                                go2
                            ).y
                        );
                    }; }
                    Axis.Z -> { orderedlist.sortWith { go1, go2 ->
                        nAda.objectpos(go1).z.compareTo(
                            nAda.objectpos(
                                go2
                            ).z
                        );
                    }; }
                }

                val left_s = orderedlist.GetRange(0, center).ensureFastList()
                val right_s = orderedlist.GetRange(center, splitlist.size - center).ensureFastList()

                val SAH = SAofList(nAda,left_s) * left_s.size  + SAofList(nAda,right_s) * right_s.size
                SplitAxisOpt(SAH,axis, left_s, right_s)
            }

            // perform the split
            gobjects = null
            this.left = BVHNode<GO>(bvh, this, bestSplit.left, bestSplit.axis, this.depth + 1) // Split the Hierarchy to the left
            this.right = BVHNode<GO>(bvh, this, bestSplit.right, bestSplit.axis, this.depth + 1) // Split the Hierarchy to the right
        }

        internal fun splitIfNecessary(bvh: CSBVH<GO>) {
            if (gobjects!!.size > bvh.LEAF_OBJ_MAX) {
                splitNode(bvh)
            }
        }

        internal fun addObject(bvh: CSBVH<GO>, newOb: GO, newObBox: AABB3D, newObSAH: Float) {
            addObject(bvh,this,newOb, newObBox, newObSAH)
        }

        internal fun countBVHNodes(): Int {
            if (gobjects != null) {
                return 1
            } else {
                return left!!.countBVHNodes() + right!!.countBVHNodes()
            }
        }

        internal fun removeObject(bvh: CSBVH<GO>, newOb: GO) {
            if (gobjects == null) { throw Exception("removeObject() called on nonLeaf!"); }

            bvh.unmapObject(newOb)
            gobjects!!.remove(newOb)
            if (gobjects!!.size > 0) {
                refitVolume(bvh)
            } else {
                // our leaf is empty, so collapse it if we are not the root...
                if (parent != null) {
                    gobjects = null
                    parent!!.removeLeaf(bvh, this)
                    parent = null
                }
            }
        }

        fun setDepth(bvh: CSBVH<GO>, newdepth: Int) {
            this.depth = newdepth
            if (newdepth > bvh.maxDepth) {
                bvh.maxDepth = newdepth
            }
            if (gobjects == null) {
                left!!.setDepth(bvh, newdepth+1)
                right!!.setDepth(bvh, newdepth+1)
            }
        }

        internal fun removeLeaf(bvh: CSBVH<GO>, removeLeaf: BVHNode<GO>) {
            if (left == null || right == null) { throw Exception("bad intermediate node"); }
            val keepLeaf = when (removeLeaf) {
                left -> right!!
                right -> left!!
                else -> throw Exception("removeLeaf doesn't match any leaf!")
            }

            // "become" the leaf we are keeping.
            box = keepLeaf.box
            left = keepLeaf.left
            right = keepLeaf.right
            gobjects = keepLeaf.gobjects
            // clear the leaf..
            // keepLeaf.left = null; keepLeaf.right = null; keepLeaf.gobjects = null; keepLeaf.parent = null; 

            if (gobjects == null) {
                left!!.parent = this; right!!.parent = this  // reassign child parents..
                this.setDepth(bvh, this.depth) // this reassigns depth for our children
            } else {
                // map the objects we adopted to us...
                gobjects!!.fastForEach { o -> bvh.mapObjectToBVHLeaf(o,this); }
            }

            // propagate our new volume..
            parent?.childRefit(bvh)
        }


        internal fun rootNode(): BVHNode<GO> {
            var cur = this
            while (cur.parent != null) { cur = cur.parent!! }
            return cur
        }


        internal fun findOverlappingLeaves(nAda: NodeAdaptor<GO>, origin: Vector3, radius: Float, overlapList: FastArrayList<BVHNode<GO>>) {
            if (box!!.intersectsSphere(origin,radius)) {
                if (gobjects != null) {
                    overlapList.add(this)
                } else {
                    left!!.findOverlappingLeaves(nAda,origin,radius,overlapList)
                    right!!.findOverlappingLeaves(nAda,origin,radius,overlapList)
                }
            }
        }

        internal fun findOverlappingLeaves(nAda: NodeAdaptor<GO>, AABB3D: AABB3D, overlapList: FastArrayList<BVHNode<GO>>) {
            if (box!!.intersectsAABB(AABB3D)) {
                if (gobjects != null) {
                    overlapList.add(this)
                } else {
                    left!!.findOverlappingLeaves(nAda,AABB3D,overlapList)
                    right!!.findOverlappingLeaves(nAda,AABB3D,overlapList)
                }
            }
        }

        internal fun toAABB(): AABB3D = box.clone()

        internal fun childExpanded(nAda: NodeAdaptor<GO>, child: BVHNode<GO>) {
            var expanded = false

            val box = this.box
            val childBox = child.box

            if (childBox.min.x < box.min.x) {
                box.min.x = childBox.min.x; expanded = true
            }
            if (childBox.max.x > box.max.x) {
                box.max.x = childBox.max.x; expanded = true
            }
            if (childBox.min.y < box.min.y) {
                box.min.y = childBox.min.y; expanded = true
            }
            if (childBox.max.y > box.max.y) {
                box.max.y = childBox.max.y; expanded = true
            }
            if (childBox.min.z < box.min.z) {
                box.min.z = childBox.min.z; expanded = true
            }
            if (childBox.max.z > box.max.z) {
                box.max.z = childBox.max.z; expanded = true
            }

            if (expanded && parent != null) {
                parent!!.childExpanded(nAda, this)
            }
        }

        internal fun childRefit(bvh: CSBVH<GO>, propagate: Boolean=true) {
            childRefit(bvh, this, propagate = propagate)
        }

        companion object {

            internal fun <GO> AABBofOBJ(nAda: NodeAdaptor<GO>, obj: GO): AABB3D {
                val radius = nAda.radius(obj)
                val box = AABB3D()
                box.setX(-radius, +radius)
                box.setY(-radius, +radius)
                box.setZ(-radius, +radius)
                return box
            }

            internal fun SA(box: AABB3D): Float {
                val x_size = box.max.x - box.min.x
                val y_size = box.max.y - box.min.y
                val z_size = box.max.z - box.min.z
                return 2.0f * ( (x_size * y_size) + (x_size * z_size) + (y_size * z_size) )
            }

            internal fun <GO> SA(node: BVHNode<GO>): Float = SA(node.box)
            internal fun <GO> SA(nAda: NodeAdaptor<GO>, obj: GO): Float {
                val radius = nAda.radius(obj)
                val size = radius * 2
                return 6.0f * (size * size)
            }

            internal fun <GO> AABBofPair(nodea: BVHNode<GO>,  nodeb: BVHNode<GO>): AABB3D {
                val box = nodea.box
                box.expandToFit(nodeb.box)
                return box
            }
            private val eachRot: List<Rot> get() = Rot.values().toList()

            private val eachAxis: List<Axis> get() = Axis.values().toList()

            internal fun <GO> addObject_Pushdown(bvh: CSBVH<GO>, curNode: BVHNode<GO>, newOb: GO) {
                val left = curNode.left
                val right = curNode.right

                // merge and pushdown left and right as a new node..
                val mergedSubnode = BVHNode<GO>(bvh)
                mergedSubnode.left = left
                mergedSubnode.right = right
                mergedSubnode.parent = curNode
                mergedSubnode.gobjects = null // we need to be an interior node... so null out our object list..
                left!!.parent = mergedSubnode
                right!!.parent = mergedSubnode
                mergedSubnode.childRefit(bvh, propagate = false)

                // make new subnode for obj
                val newSubnode = BVHNode<GO>(bvh)
                newSubnode.parent = curNode
                newSubnode.gobjects = fastArrayListOf(newOb)
                bvh.mapObjectToBVHLeaf(newOb, newSubnode)
                newSubnode.computeVolume(bvh)

                // make assignments..
                curNode.left = mergedSubnode
                curNode.right = newSubnode
                curNode.setDepth(bvh, curNode.depth) // propagate new depths to our children.
                curNode.childRefit(bvh)
            }
            internal fun <GO> addObject(bvh: CSBVH<GO>, curNode: BVHNode<GO>, newOb: GO, newObBox: AABB3D, newObSAH: Float) {
                var curNode = curNode
                // 1. first we traverse the node looking for the best leaf
                while (curNode.gobjects == null) {
                    // find the best way to add this object.. 3 options..
                    // 1. send to left node  (L+N,R)
                    // 2. send to right node (L,R+N)
                    // 3. merge and pushdown left-and-right node (L+R,N)

                    val left = curNode.left!!
                    val right = curNode.right!!

                    val leftSAH = SA(left)
                    val rightSAH = SA(right)
                    val sendLeftSAH = rightSAH + SA(left.box.expandedBy(newObBox))    // (L+N,R)
                    val sendRightSAH = leftSAH + SA(right.box.expandedBy(newObBox))   // (L,R+N)
                    val mergedLeftAndRightSAH = SA(AABBofPair(left,right)) + newObSAH // (L+R,N)

                    // Doing a merge-and-pushdown can be expensive, so we only do it if it's notably better
                    val MERGE_DISCOUNT = 0.3f

                    if (mergedLeftAndRightSAH < min(sendLeftSAH,sendRightSAH) * MERGE_DISCOUNT) {
                        addObject_Pushdown(bvh,curNode,newOb)
                        return
                    } else {
                        curNode = if ( sendLeftSAH < sendRightSAH ) left else right
                    }
                }

                // 2. then we add the object and map it to our leaf
                curNode.gobjects!!.add(newOb)
                bvh.mapObjectToBVHLeaf(newOb,curNode)
                curNode.refitVolume(bvh)
                // split if necessary...
                curNode.splitIfNecessary(bvh)
            }

            internal fun <GO> childRefit(bvh: CSBVH<GO>, curNode: BVHNode<GO>, propagate: Boolean = true) {
                var curNode: BVHNode<GO> = curNode
                do {
                    val oldbox = curNode.box
                    val left = curNode.left!!
                    val right = curNode.right!!

                    // start with the left box
                    val newBox = left.box

                    // expand any dimension bigger in the right node
                    if (right.box.min.x < newBox.min.x) {
                        newBox.min.x = right.box.min.x; }
                    if (right.box.min.y < newBox.min.y) {
                        newBox.min.y = right.box.min.y; }
                    if (right.box.min.z < newBox.min.z) {
                        newBox.min.z = right.box.min.z; }

                    if (right.box.max.x > newBox.max.x) {
                        newBox.max.x = right.box.max.x; }
                    if (right.box.max.y > newBox.max.y) {
                        newBox.max.y = right.box.max.y; }
                    if (right.box.max.z > newBox.max.z) {
                        newBox.max.z = right.box.max.z; }

                    // now set our box to the newly created box
                    curNode.box = newBox

                    // and walk up the tree
                    curNode = curNode.parent ?: break
                } while (propagate)
            }
        }
    }

}
*/
