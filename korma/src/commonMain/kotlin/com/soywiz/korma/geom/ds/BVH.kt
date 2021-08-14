/******************************************************************************
jsBVH.js - General-Purpose Non-Recursive Bounding-Volume Hierarchy Library
Version 0.2.1, April 3rd 2010

Copyright (c) 2010 Jon-Carlos Rivera

Permission is hereby granted, free of charge, to any person obtaining
a copy of this software and associated documentation files (the
"Software"), to deal in the Software without restriction, including
without limitation the rights to use, copy, modify, merge, publish,
distribute, sublicense, and/or sell copies of the Software, and to
permit persons to whom the Software is furnished to do so, subject to
the following conditions:

The above copyright notice and this permission notice shall be
included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

Jon-Carlos Rivera - imbcmdth@hotmail.com
 ******************************************************************************/
@file:Suppress("LocalVariableName", "FunctionName")

package com.soywiz.korma.geom.ds

import com.soywiz.kds.*
import com.soywiz.kds.iterators.*
import kotlin.math.*

/**
 * A Bounding Volume Hierarchy implementation for arbitrary dimensions.
 * NTree - A simple n-tree structure for great results.
 * @constructor
 */
class BVH<T>(
    val dimensions: Int = 2,
    width: Int = dimensions * 3,
    val allowUpdateObjects: Boolean = true
) {
    // Variables to control tree
    // Number of "interval pairs" per node
    // Maximum width of any node before a split
    private val maxWidth = width

    // Minimum width of any node before a merge
    private val minWidth = floor(maxWidth.toDouble() / this.dimensions).toInt()

    data class Node<T>(
        var d: BVHIntervals,
        var id: String? = null,
        var nodes: FastArrayList<Node<T>>? = null,
        var value: T? = null,
    )

    class RemoveSubtreeRetObject<T>(
        var d: BVHIntervals,
        var target: T? = null,
        var nodes: FastArrayList<Node<T>>? = null
    )

    data class IntersectResult<T>(val intersect: Double, val obj: Node<T>)

    open class Comparators {
        companion object : Comparators()

        fun overlap_intervals(a: BVHIntervals, b: BVHIntervals): Boolean {
            var ret_val = true
            if (a.length != b.length) error("Not matching dimensions")
            for (i in 0 until a.length) {
                ret_val = ret_val && (a.a(i) < (b.a(i) + b.b(i)) && (a.a(i) + a.b(i)) > b.a(i))
            }
            return ret_val
        }

        fun contains_intervals(a: BVHIntervals, b: BVHIntervals): Boolean {
            var ret_val = true
            if (a.length != b.length) error("Not matching dimensions")
            for (i in 0 until a.length) {
                ret_val = ret_val && ((a.a(i) + a.b(i)) <= (b.a(i) + b.b(i)) && a.a(i) >= b.a(i))
            }
            return ret_val
        }
    }

    private fun _make_Empty(): BVHIntervals = BVHIntervals(this.dimensions)
    private fun _make_Intervals(other: BVHIntervals, out: BVHIntervals = BVHIntervals(this.dimensions)) =
        out.copyFrom(other)

    // Start with an empty root-tree
    var root = Node<T>(
        d = _make_Empty(),
        id = "root",
        nodes = fastArrayListOf()
    )

    /* expands intervals A to include intervals B, intervals B is untouched
	 * [ rectangle a ] = expand_rectangle(rectangle a, rectangle b)
	 * @static function
	 */
    private fun _expand_intervals(a: BVHIntervals, b: BVHIntervals): BVHIntervals {
        for (i in 0 until this.dimensions) {
            val a_a = a.a(i)
            val b_a = b.a(i)
            val a_b = a.b(i)
            val b_b = b.b(i)
            val n = min(a_a, b_a)
            a.b(i, max(a_a + a_b, b_a + b_b) - n)
            a.a(i, n)
        }
        return a
    }

    /* generates a minimally bounding intervals for all intervals in
	 * array "nodes". If intervals is set, it is modified into the MBV. Otherwise,
	 * a new set of intervals is generated and returned.
	 * [ rectangle a ] = make_MBR(rectangle array nodes, rectangle rect)
	 * @static function
	 */
    private fun _make_MBV(
        nodes: List<Node<T>>,
        intervals: BVHIntervals?
    ): BVHIntervals {
        //throw "_make_MBV: nodes must contain at least one object to bound!";
        if (nodes.isEmpty()) return _make_Empty()

        val ints = intervals?.copyFrom(nodes[0].d) ?: nodes[0].d.clone()

        for (i in nodes.size - 1 downTo 1) {
            _expand_intervals(ints, nodes[i].d)
        }

        return (ints)
    }

    // This is my special addition to the world of r-trees
    // every other (simple) method I found produced crap trees
    // this skews insertions to prefering squarer and emptier nodes
    private fun _jons_ratio(intervals: BVHIntervals, count: Int): Double {
        // Area of new enlarged rectangle
        val dims = intervals.length
        var sum = intervals.bSum()
        val mul = intervals.bMult()

        sum /= dims
        val lgeo = mul / sum.pow(dims)

        // return the ratio of the perimeter to the area - the closer to 1 we are,
        // the more "square" or "cubic" a volume is. conversly, when approaching zero the
        // more elongated a rectangle is
        return (mul * count / lgeo)
    }

    private fun BVHIntervals.checkDimensions() = checkDimensions(this@BVH.dimensions)

    /* find the best specific node(s) for object to be deleted from
     * [ leaf node parent ] = _remove_subtree(rectangle, object, root)
     * @private
     */
    private fun _remove_subtree(
        intervals: BVHIntervals?,
        obj: T?,
        root: Node<T>,
        comparators: Comparators = Comparators,
    ): FastArrayList<Node<T>> {
        val hit_stack = fastArrayListOf<Node<T>>() // Contains the elements that overlap
        val count_stack = fastArrayListOf<Int>() // Contains the elements that overlap
        var ret_array = fastArrayListOf<Node<T>>()
        var current_depth = 1

        if (intervals == null || !comparators.overlap_intervals(intervals, root.d)) return ret_array

        val ret_obj = RemoveSubtreeRetObject(
            d = intervals.clone(),
            target = obj
        )

        count_stack.add(root.nodes!!.size)
        hit_stack.add(root)

        do {
            var tree = hit_stack.removeLast()
            var i = count_stack.removeLast() - 1

            if (ret_obj.target != null) { // We are searching for a target
                while (i >= 0) {
                    val ltree = tree.nodes!![i]
                    if (comparators.overlap_intervals(ret_obj.d, ltree.d)) {
                        if ((ret_obj.target != null && ltree.value != null && ltree.value === ret_obj.target) || (ret_obj.target == null && (ltree.value != null || comparators.contains_intervals(
                                ltree.d,
                                ret_obj.d
                            )))
                        ) { // A Match !!
                            // Yup we found a match...
                            // we can cancel search and start walking up the list
                            if (ltree.nodes != null) { // If we are deleting a node not a leaf...
                                ret_array = _search_subtree(
                                    intervals = ltree.d,
                                    root = ltree,
                                    comparators = comparators,
                                )
                                tree.nodes?.removeAt(i)
                            } else {
                                ret_array = fastArrayListOf(tree.nodes!!.removeAt(i))
                            }
                            // Resize MBR down...
                            _make_MBV(tree.nodes!!, tree.d)
                            ret_obj.target = null
                            if (tree.nodes!!.size < minWidth) { // Underflow
                                ret_obj.nodes = _search_subtree(
                                    intervals = tree.d,
                                    root = tree,
                                    comparators = comparators
                                )
                            }
                            break
                        }
                        /*	else if("load" in ltree) { // A load
				  	    }*/
                        else if (ltree.nodes != null) { // Not a Leaf
                            current_depth += 1
                            count_stack.add(i)
                            hit_stack.add(tree)
                            tree = ltree
                            i = ltree.nodes!!.size
                        }
                    }
                    i -= 1
                }
            } else if (ret_obj.nodes != null) { // We are unsplitting
                tree.nodes!!.removeAt(i + 1) // Remove unsplit node
                // ret_obj.nodes contains a list of elements removed from the tree so far
                if (tree.nodes!!.isNotEmpty()) {
                    _make_MBV(tree.nodes!!, tree.d)
                }
                for (t in 0 until ret_obj.nodes!!.size) {
                    _insert_subtree(
                        root = tree,
                        node = ret_obj.nodes!![t]
                    )
                }
                ret_obj.nodes!!.clear()
                if (hit_stack.isEmpty() && tree.nodes!!.size <= 1) { // Underflow..on root!
                    ret_obj.nodes = _search_subtree(
                        intervals = tree.d,
                        return_array = ret_obj.nodes!!,
                        root = tree,
                        comparators = comparators
                    )
                    tree.nodes!!.clear()
                    hit_stack.add(tree)
                    count_stack.add(1)
                } else if (hit_stack.isNotEmpty() && tree.nodes!!.size < minWidth) { // Underflow..AGAIN!
                    ret_obj.nodes = _search_subtree(
                        intervals = tree.d,
                        return_array = ret_obj.nodes!!,
                        root = tree,
                        comparators = comparators
                    )
                    tree.nodes!!.clear()
                } else {
                    ret_obj.nodes = null // Just start resizing
                }
            } else { // we are just resizing
                _make_MBV(tree.nodes!!, tree.d)
            }
            current_depth -= 1
        } while (hit_stack.isNotEmpty())

        return (ret_array)
    }

    /* choose the best damn node for rectangle to be inserted into
	 * [ leaf node parent ] = _choose_leaf_subtree(rectangle, root to start search at)
	 * @private
	 */
    private fun _choose_leaf_subtree(
        intervals: BVHIntervals,
        root: Node<T>,
    ): FastArrayList<Node<T>> {
        var best_choice_index = -1
        val best_choice_stack = fastArrayListOf<Node<T>>()
        var best_choice_area = 0.0

        best_choice_stack.add(root)
        var nodes = root.nodes!!

        do {
            if (best_choice_index != -1) {
                best_choice_stack.add(nodes[best_choice_index])
                nodes = nodes[best_choice_index].nodes!!
                best_choice_index = -1
            }

            // for (var i = nodes.length - 1; i >= 0; i--) {
            for (i in nodes.size - 1 downTo 0) {
                val ltree = nodes[i]
                if (ltree.value != null) {
                    // Bail out of everything and start inserting
                    best_choice_index = -1
                    break
                }
                // Area of new enlarged rectangle
                val old_lratio = _jons_ratio(ltree.d, ltree.nodes!!.size + 1)

                val copy_of_intervals = ltree.d.clone()
                _expand_intervals(copy_of_intervals, intervals)

                // Area of new enlarged rectangle
                val lratio = _jons_ratio(copy_of_intervals, ltree.nodes!!.size + 2)

                if (best_choice_index < 0 || abs(lratio - old_lratio) < best_choice_area) {
                    best_choice_area = abs(lratio - old_lratio)
                    best_choice_index = i
                }
            }
        } while (best_choice_index != -1)

        return (best_choice_stack)
    }

    /* split a set of nodes into two roughly equally-filled nodes
	 * [ an array of two new arrays of nodes ] = linear_split(array of nodes)
	 * @private
	 */
    private fun _linear_split(nodes: FastArrayList<Node<T>>): FastArrayList<Node<T>> {
        val n = _pick_linear(nodes)
        while (nodes.isNotEmpty()) {
            _pick_next(nodes, n[0], n[1])
        }
        return (n)
    }

    /* insert the best source rectangle into the best fitting parent node: a or b
	 * [] = pick_next(array of source nodes, target node array a, target node array b)
	 * @private
	 */
    private fun _pick_next(nodes: FastArrayList<Node<T>>, a: Node<T>, b: Node<T>) {
        // Area of new enlarged rectangle
        val area_a = _jons_ratio(a.d, a.nodes!!.size + 1)
        val area_b = _jons_ratio(b.d, b.nodes!!.size + 1)
        var high_area_delta: Double? = null
        var high_area_node: Int? = null
        lateinit var lowest_growth_group: Node<T>

        //for (var i = nodes.length - 1; i >= 0; i--) {
        for (i in nodes.size - 1 downTo 0) {
            val l = nodes[i]

            var copy_of_intervals = _make_Intervals(a.d)
            _expand_intervals(copy_of_intervals, l.d)
            val change_new_area_a = abs(_jons_ratio(copy_of_intervals, a.nodes!!.size + 2) - area_a)

            copy_of_intervals = _make_Intervals(b.d)
            _expand_intervals(copy_of_intervals, l.d)
            val change_new_area_b = abs(_jons_ratio(copy_of_intervals, b.nodes!!.size + 2) - area_b)

            if (high_area_node == null || high_area_delta == null || abs(change_new_area_b - change_new_area_a) < high_area_delta) {
                high_area_node = i
                high_area_delta = abs(change_new_area_b - change_new_area_a)
                lowest_growth_group = if (change_new_area_b < change_new_area_a) b else a
            }
        }
        val temp_node = nodes.removeAt(high_area_node!!)
        if (a.nodes!!.size + nodes.size + 1 <= minWidth) {
            a.nodes!!.add(temp_node)
            _expand_intervals(a.d, temp_node.d)
        } else if (b.nodes!!.size + nodes.size + 1 <= minWidth) {
            b.nodes!!.add(temp_node)
            _expand_intervals(b.d, temp_node.d)
        } else {
            lowest_growth_group.nodes!!.add(temp_node)
            _expand_intervals(lowest_growth_group.d, temp_node.d)
        }
    }

    /* pick the "best" two starter nodes to use as seeds using the "linear" criteria
	 * [ an array of two new arrays of nodes ] = pick_linear(array of source nodes)
	 * @private
	 */
    private fun _pick_linear(nodes: FastArrayList<Node<T>>): FastArrayList<Node<T>> {
        val lowest_high = Array(this.dimensions) { nodes.size - 1 }
        val highest_low = Array(this.dimensions) { 0 }

        // for (i = nodes.length - 2; i >= 0; i--) {
        for (i in nodes.size - 2 downTo 0) {
            val l = nodes[i]
            //for (d = 0; d < _Dimensions; d++) {
            for (d in 0 until this.dimensions) {
                if (l.d.a(d) > nodes[highest_low[d]].d.a(d)) {
                    highest_low[d] = i
                } else if (l.d.a(d) + l.d.b(d) < nodes[lowest_high[d]].d.a(d) + nodes[lowest_high[d]].d.b(d)) {
                    lowest_high[d] = i
                }
            }
        }

        var d = 0
        var last_difference = 0.0
        //for (i = 0; i < _Dimensions; i++) {
        for (i in 0 until this.dimensions) {
            val difference =
                abs((nodes[lowest_high[i]].d.a(i) + nodes[lowest_high[i]].d.b(i)) - nodes[highest_low[i]].d.a(i))
            if (difference > last_difference) {
                d = i
                last_difference = difference
            }
        }

        val t1: Node<T>
        val t2: Node<T>

        if (lowest_high[d] > highest_low[d]) {
            t1 = nodes.removeAt(lowest_high[d])
            t2 = nodes.removeAt(highest_low[d])
        } else {
            t2 = nodes.removeAt(highest_low[d])
            t1 = nodes.removeAt(lowest_high[d])
        }

        return fastArrayListOf(
            Node(d = _make_Intervals(t1.d), nodes = fastArrayListOf(t1)),
            Node(d = _make_Intervals(t2.d), nodes = fastArrayListOf(t2))
        )
    }

    //fun _attach_data(node: TNode, more_tree: TNode): TNode {
    //    node.nodes = more_tree.nodes
    //    node.x = more_tree.x
    //    node.y = more_tree.y
    //    node.w = more_tree.w
    //    node.h = more_tree.h
    //    return (node)
    //}

    /* non-recursive internal insert function
	 * [] = _insert_subtree(rectangle, object to insert, root to begin insertion at)
	 * @private
	 */
    private fun _insert_subtree(
        root: Node<T>,
        node: Node<T>,
    ) {
        var bc: Node<T>? = null // Best Current node
        // Initial insertion is special because we resize the Tree and we don't
        // care about any overflow (seriously, how can the first object overflow?)
        if (root.nodes!!.isEmpty()) {
            _make_Intervals(node.d, root.d)
            root.nodes!!.add(node)
            return
        }

        // Find the best fitting leaf node
        // choose_leaf returns an array of all tree levels (including root)
        // that were traversed while trying to find the leaf
        val tree_stack = _choose_leaf_subtree(
            intervals = node.d,
            root = root,
        )
        var ret_obj_array: FastArrayList<Node<T>>? = null
        var ret_obj: Node<T>? = node //{x:rect.x,y:rect.y,w:rect.w,h:rect.h, leaf:obj};
        // Walk back up the tree resizing and inserting as needed
        do {
            //handle the case of an empty node (from a split)
            if (bc?.nodes != null && bc.nodes!!.isEmpty()) {
                val pbc = bc // Past bc
                bc = tree_stack.removeLast()
                //for (var t = 0; t < bc.nodes.length; t++) {
                for (t in 0 until bc.nodes!!.size) {
                    if (bc.nodes!![t] === pbc || bc.nodes!![t].nodes!!.isEmpty()) {
                        bc.nodes!!.removeAt(t)
                        break
                    }
                }
            } else {
                bc = tree_stack.removeLast()
            }

            // If there is data attached to this ret_obj
            if (ret_obj_array != null || ret_obj!!.value != null || ret_obj.nodes != null) {
                // Do Insert
                if (ret_obj_array != null) {
                    //for (var ai = 0; ai < ret_obj.length; ai++) {
                    for (ai in 0 until ret_obj_array.size) {
                        _expand_intervals(bc.d, ret_obj_array[ai].d)
                    }
                    bc.nodes!!.addAll(ret_obj_array)
                } else {
                    _expand_intervals(bc.d, ret_obj!!.d)
                    bc.nodes!!.add(ret_obj) // Do Insert
                }

                if (bc.nodes!!.size <= maxWidth) { // Start Resizeing Up the Tree
                    ret_obj_array = null
                    ret_obj = Node(d = _make_Intervals(bc.d))
                } else { // Otherwise Split this Node
                    // linear_split() returns an array containing two new nodes
                    // formed from the split of the previous node's overflow
                    val a = _linear_split(bc.nodes!!)
                    ret_obj_array = a
                    ret_obj = null //[1];
                    if (tree_stack.isEmpty()) { // If are splitting the root..
                        bc.nodes!!.add(a[0])
                        tree_stack.add(bc) // Reconsider the root element
                        ret_obj_array = null
                        ret_obj = a[1]
                    }
                }
            } else { // Otherwise Do Resize
                //Just keep applying the new bounding rectangle to the parents..
                _expand_intervals(bc.d, ret_obj.d)
                ret_obj_array = null
                ret_obj = Node(
                    d = _make_Intervals(bc.d)
                )
            }
        } while (tree_stack.isNotEmpty())
    }

    fun envelope(): BVHIntervals {
        // Return a copy
        return _make_Intervals(this.root.d)
    }

    // Intersect with overall tree bounding-box
    // Returns a segment contained within the pointing box
    private fun _intersect_Intervals(
        ray: BVHIntervals,
        intervals: BVHIntervals?
    ): BVHIntervals? {
        var ints = intervals
        if (ints == null) {
            ints = this.root.d // By default, use the scene bounding box
        }
        val parameters = Array(2) { DoubleArray(this.dimensions) }
        // inv_direction and sign can be pre-computed per ray
        val inv_direction = DoubleArray(this.dimensions)
        val sign = IntArray(this.dimensions)

        // Initialize values
        for (i in 0 until this.dimensions) {
            parameters[0][i] = ints.a(i)
            parameters[1][i] = ints.a(i) + ints.b(i)

            val j = 1.0 / ray.b(i)
            inv_direction[i] = j
            sign[i] = if (j <= 0) 1 else 0
        }

        var omin = (parameters[sign[0]][0] - ray.a(0)) * inv_direction[0]
        var omax = (parameters[1 - sign[0]][0] - ray.a(0)) * inv_direction[0]

        for (i in 1 until this.dimensions) {
            val tmin = (parameters[sign[i]][i] - ray.a(i)) * inv_direction[i]
            val tmax = (parameters[1 - sign[i]][i] - ray.a(i)) * inv_direction[i]

            if ((omin > tmax) || (tmin > omax)) {
                return null
            }
            if (tmin > omin) {
                omin = tmin
            }
            if (tmax < omax) {
                omax = tmax
            }
        }

        if (omin >= Double.POSITIVE_INFINITY || omax <= Double.NEGATIVE_INFINITY) {
            return null
        }
        if (omin < 0 && omax < 0) return null
        if (omin < 0) omin = 0.0
        val rs = _make_Empty()

        for (i in 0 until this.dimensions) {
            rs.a(i, ray.a(i) + ray.b(i) * omin)
            rs.b(i, ray.a(i) + ray.b(i) * omax)
        }

        return (rs)
    }

    /* non-recursive internal search function
	 * [ nodes | objects ] = _search_subtree(intervals, [return node data], [array to fill], root to begin search at)
	 * @private
	 */
    private fun _intersect_subtree(
        ray: BVHIntervals,
        return_array: FastArrayList<IntersectResult<T>> = fastArrayListOf(),
        root: Node<T> = this.root,
    ): FastArrayList<IntersectResult<T>> {
        val hit_stack = fastArrayListOf<List<Node<T>>>() // Contains the elements that overlap

        if (_intersect_Intervals(ray, root.d) == null) return (return_array)

        hit_stack.add(root.nodes!!)

        do {
            val nodes = hit_stack.removeLast()

            // for (var i = nodes.length - 1; i >= 0; i--) {
            for (i in nodes.size - 1 downTo 0) {
                val ltree = nodes[i]
                val intersect_points = _intersect_Intervals(ray, ltree.d)
                if (intersect_points != null) {
                    if (ltree.nodes != null) { // Not a Leaf
                        hit_stack.add(ltree.nodes!!)
                    } else if (ltree.value != null) { // A Leaf !!
                        val tmin = (intersect_points.a(0) - ray.a(0)) / ray.b(0)
                        return_array.add(IntersectResult(intersect = tmin, obj = ltree))
                    }
                }
            }
        } while (hit_stack.isNotEmpty())

        return (return_array)
    }

    /* non-recursive internal search function
	 * [ nodes | objects ] = _search_subtree(intervals, [return node data], [array to fill], root to begin search at)
	 * @private
	 */
    private fun _search_subtree(
        intervals: BVHIntervals,
        comparators: Comparators,
        return_array: FastArrayList<Node<T>> = fastArrayListOf(),
        root: Node<T> = this.root,
    ): FastArrayList<Node<T>> {
        intervals.checkDimensions()
        val hit_stack = fastArrayListOf<List<Node<T>>>() // Contains the elements that overlap

        if (!comparators.overlap_intervals(intervals, root.d)) return (return_array)

        hit_stack.add(root.nodes!!)

        do {
            val nodes = hit_stack.removeLast()

            //for (var i = nodes.length - 1; i >= 0; i--) {
            for (i in nodes.size - 1 downTo 0) {
                val ltree = nodes[i]
                if (comparators.overlap_intervals(intervals, ltree.d)) {
                    if (ltree.nodes != null) { // Not a Leaf
                        hit_stack.add(ltree.nodes!!)
                    } else if (ltree.value != null) { // A Leaf !!
                        return_array.add(ltree)
                    }
                }
            }
        } while (hit_stack.isNotEmpty())

        return (return_array)
    }

    /* non-recursive internal yield_to function
	 * [ nodes | objects ] = _yield( options )
	 * @private
	 */
    @Suppress("unused")
    fun yieldTo(
        intervals: BVHIntervals,
        yield_leaf: (node: Node<T>) -> Unit,
        yield_node: (node: Node<T>) -> Unit,
        root: Node<T> = this.root,
        comparators: Comparators = Comparators,
    ) {
        val hit_stack = fastArrayListOf<List<Node<T>>>() // Contains the nodes that overlap

        if (!comparators.overlap_intervals(intervals, root.d)) return

        hit_stack.add(root.nodes!!)

        do {
            val nodes = hit_stack.removeLast()

            for (i in nodes.size - 1 downTo 0) {
                val ltree = nodes[i]
                if (comparators.overlap_intervals(intervals, ltree.d)) {
                    if (ltree.nodes != null) { // Not a Leaf
                        yield_node(ltree)
                        hit_stack.add(ltree.nodes!!)
                    } else if (ltree.value != null) { // A Leaf !!
                        yield_leaf(ltree)
                    }
                }
            }
        } while (hit_stack.isNotEmpty())
    }


    fun intersectRay(ray: BVHIntervals, intervals: BVHIntervals?) = _intersect_Intervals(ray, intervals)

    /* non-recursive intersect function
	 * [ nodes | objects ] = NTree.intersect( options )
	 * @public
	 */
    fun intersect(
        ray: BVHIntervals,
        return_array: FastArrayList<IntersectResult<T>> = fastArrayListOf(),
    ) = _intersect_subtree(ray = ray, return_array = return_array, root = this.root)


    /* non-recursive search function
	 * [ nodes | objects ] = NTree.search(intervals, [return node data], [array to fill])
	 * @public
	 */
    fun search(
        intervals: BVHIntervals,
        return_array: FastArrayList<Node<T>> = fastArrayListOf(),
        comparators: Comparators = Comparators,
    ): FastArrayList<Node<T>> {
        return _search_subtree(
            intervals = intervals,
            return_array = return_array,
            root = this.root,
            comparators = comparators
        )
    }

    /* non-recursive insert function
	 * [] = NTree.insert(intervals, object to insert)
	 */
    fun insertOrUpdate(
        intervals: BVHIntervals,
        obj: T,
    ) {
        intervals.checkDimensions()
        if (allowUpdateObjects) if (obj in objectToIntervalMap) remove(obj)
        _insert_subtree(root = this.root, node = Node(d = intervals, value = obj))
        if (allowUpdateObjects) objectToIntervalMap[obj] = intervals
    }

    fun remove(obj: T) {
        if (!allowUpdateObjects) error("allowUpdateObjects not enabled")
        val intervals = objectToIntervalMap[obj]
        if (intervals != null) remove(intervals, obj)
    }

    fun getObjectBounds(obj: T): BVHIntervals? = objectToIntervalMap[obj]

    //private val objectToIntervalMap = FastIdentityMap<T, BVHIntervals>()
    private val objectToIntervalMap = HashMap<T, BVHIntervals>()

    /* non-recursive function that deletes a specific
	 * [ number ] = NTree.remove(intervals, obj)
	 */
    fun remove(
        intervals: BVHIntervals,
        obj: T? = null,
        comparators: Comparators = Comparators,
    ): FastArrayList<Node<T>> {
        intervals.checkDimensions()
        return if (obj == null) { // Do area-wide delete
            val ret_array = fastArrayListOf<Node<T>>()
            do {
                val numberdeleted = ret_array.size
                ret_array.addAll(
                    _remove_subtree(
                        root = this.root,
                        intervals = intervals,
                        obj = null,
                        comparators = comparators
                    )
                )
            } while (numberdeleted != ret_array.size)
            ret_array
        } else { // Delete a specific item
            _remove_subtree(root = this.root, intervals = intervals, obj = obj, comparators = comparators)
        }.also {
            if (allowUpdateObjects) {
                it.fastForEach { node ->
                    objectToIntervalMap.remove(node.value)
                }
            }
        }
    }

    fun debug(node: Node<T> = this.root, indentation: String = "") {
        println("$indentation${node.d}:${node.value}")
        if (node.nodes != null) {
            val newIndentation = "$indentation  "
            for (n in node.nodes!!) {
                debug(n, newIndentation)
            }
        }
    }
}

@Suppress("INLINE_CLASS_DEPRECATED")
inline class BVHIntervals(val data: DoubleArray) {
    constructor(dimensions: Int) : this(DoubleArray(dimensions * 2))

    companion object {
        operator fun invoke(vararg values: Double) = BVHIntervals(values)
    }

    fun checkDimensions(dimensions: Int) {
        if (dimensions != length) error("element $length doesn't match dimensions $dimensions")
    }

    fun setTo(vararg values: Double) {
        values.copyInto(data)
    }

    fun setTo(a0: Double, b0: Double, a1: Double, b1: Double) {
        data[0] = a0
        data[1] = b0
        data[2] = a1
        data[3] = b1
    }

    fun setTo(a0: Double, b0: Double, a1: Double, b1: Double, a2: Double, b2: Double) {
        data[0] = a0
        data[1] = b0
        data[2] = a1
        data[3] = b1
        data[4] = a2
        data[5] = b2
    }

    val length get() = data.size / 2
    fun a(index: Int): Double = data[index * 2 + 0]
    fun a(index: Int, value: Double) { data[index * 2 + 0] = value }

    fun b(index: Int): Double = data[index * 2 + 1]
    fun b(index: Int, value: Double) {
        data[index * 2 + 1] = value
    }

    fun aPlusB(index: Int) = a(index) + b(index)

    fun bSum(): Double {
        var result = 0.0
        val data = this.data
        for (n in 0 until length) result += data[n * 2 + 1]
        return result
    }
    fun bMult(): Double {
        var result = 1.0
        val data = this.data
        for (n in 0 until length) result *= data[n * 2 + 1]
        return result
    }

    fun copyFrom(other: BVHIntervals): BVHIntervals {
        other.data.copyInto(data)
        return this
    }

    fun clone() = BVHIntervals(data.copyOf())

    override fun toString(): String = buildString {
        append('[')
        for (n in 0 until this@BVHIntervals.length) {
            if (n != 0) append(", ")
            append('x' + n)
            append('=')
            append('(')
            append(a(n))
            append(',')
            append(b(n))
            append(')')
        }
        append(']')
    }
}

//@Suppress("INLINE_CLASS_DEPRECATED")
//class BVHIntervals(val a: DoubleArray, val b: DoubleArray) {
//    init {
//        check(a.size == b.size)
//    }
//
//    constructor(dimensions: Int) : this(DoubleArray(dimensions), DoubleArray(dimensions))
//
//    companion object {
//        operator fun invoke(vararg values: Double) = BVHIntervals(
//            DoubleArray(values.size / 2) { values[it * 2 + 0] },
//            DoubleArray(values.size / 2) { values[it * 2 + 1] }
//        )
//    }
//
//    fun checkDimensions(dimensions: Int) {
//        if (dimensions != length) error("element $length doesn't match dimensions $dimensions")
//    }
//
//    val length get() = a.size
//    fun a(index: Int): Double = a[index]
//    fun a(index: Int, value: Double) { a[index] = value }
//
//    fun b(index: Int): Double = b[index]
//    fun b(index: Int, value: Double) {
//        b[index] = value
//    }
//
//    fun copyFrom(other: BVHIntervals): BVHIntervals {
//        other.a.copyInto(a)
//        other.b.copyInto(b)
//        return this
//    }
//
//    fun clone() = BVHIntervals(a.copyOf(), b.copyOf())
//
//    override fun toString(): String = buildString {
//        append('[')
//        for (n in 0 until this@BVHIntervals.length) {
//            if (n != 0) append(", ")
//            append('x' + n)
//            append('=')
//            append('(')
//            append(a(n))
//            append(',')
//            append(b(n))
//            append(')')
//        }
//        append(']')
//    }
//}
