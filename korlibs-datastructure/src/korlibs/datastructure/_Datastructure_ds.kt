@file:Suppress("LocalVariableName", "FunctionName", "PackageDirectoryMismatch")

package korlibs.datastructure.ds

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

import korlibs.datastructure.Deque
import korlibs.datastructure.FastArrayList
import korlibs.datastructure.fastArrayListOf
import korlibs.math.isAlmostEquals
import korlibs.datastructure.internal.niceStr
import korlibs.datastructure.mapDouble
import kotlin.collections.set
import kotlin.math.abs
import kotlin.math.absoluteValue
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/**
 * A Bounding Volume Hierarchy implementation for arbitrary dimensions.
 * NTree - A simple n-tree structure for great results.
 * @constructor
 */
class BVH<T>(
    val dimensions: Int,
    width: Int = dimensions * 3,
    val allowUpdateObjects: Boolean = true
) : Iterable<BVH.Node<T>> {
    // Variables to control tree
    // Number of "interval pairs" per node
    // Maximum width of any node before a split
    private val maxWidth = width

    // Minimum width of any node before a merge
    private val minWidth = floor(maxWidth.toDouble() / this.dimensions).toInt()

    data class Node<T>(
        var d: BVHRect,
        var id: String? = null,
        var nodes: FastArrayList<Node<T>>? = null,
        var value: T? = null,
    )

    class RemoveSubtreeRetObject<T>(
        var d: BVHRect,
        var target: T? = null,
        var nodes: FastArrayList<Node<T>>? = null
    )

    data class IntersectResult<T>(
        val ray: BVHRay,
        val intersect: Double,
        val obj: Node<T>,
    ) {
        val point: BVHVector by lazy {
            BVHVector(DoubleArray(ray.dimensions) { dim ->
                ray.pos(dim) + ray.dir(dim) * intersect
            })
        }

        val normal: BVHVector by lazy {
            val bounds = obj.d
            BVHVector(DoubleArray(ray.dimensions) { dim ->
                val bmin = bounds.min(dim)
                val bmax = bounds.max(dim)
                val p = point[dim]
                if (bmin.isAlmostEquals(p) || p < bmin) -1.0 else if (bmax.isAlmostEquals(p) || p > bmax) +1.0 else 0.0
            })
        }
    }

    open class Comparators {
        companion object : Comparators()

        fun overlap_intervals(a: BVHRect, b: BVHRect): Boolean {
            if (a.length != b.length) error("Not matching dimensions")
            for (i in 0 until a.length) {
                if (!(a.min(i) < b.max(i) && a.max(i) > b.min(i))) return false
            }
            return true
        }

        fun contains_intervals(a: BVHRect, b: BVHRect): Boolean {
            if (a.length != b.length) error("Not matching dimensions")
            for (i in 0 until a.length) {
                if (!(a.max(i) <= b.max(i) && a.min(i) >= b.min(i))) return false
            }
            return true
        }
    }

    private fun _make_Empty(): BVHRect = BVHRect(BVHIntervals(this.dimensions))
    private fun _make_Intervals(other: BVHRect, out: BVHRect = BVHRect(this.dimensions)) =
        out.copyFrom(other)

    // Start with an empty root-tree
    var root = Node<T>(
        d = _make_Empty(),
        id = "root",
        nodes = fastArrayListOf()
    )

    fun isEmpty(): Boolean {
        val nodes = root.nodes ?: return true
        return nodes.isEmpty()
    }

    /* expands intervals A to include intervals B, intervals B is untouched
	 * [ rectangle a ] = expand_rectangle(rectangle a, rectangle b)
	 * @static function
	 */
    private fun _expand_intervals(a: BVHRect, b: BVHRect): BVHRect {
        for (i in 0 until this.dimensions) {
            val a_a = a.min(i)
            val b_a = b.min(i)
            val a_b = a.size(i)
            val b_b = b.size(i)
            val n = min(a_a, b_a)
            a.size(i, max(a_a + a_b, b_a + b_b) - n)
            a.min(i, n)
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
        intervals: BVHRect?
    ): BVHRect {
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
    private fun _jons_ratio(intervals: BVHRect, count: Int): Double {
        // Area of new enlarged rectangle
        val dims = intervals.length
        var sum = intervals.intervals.bSum()
        val mul = intervals.intervals.bMult()

        sum /= dims
        val lgeo = mul / sum.pow(dims)

        // return the ratio of the perimeter to the area - the closer to 1 we are,
        // the more "square" or "cubic" a volume is. conversly, when approaching zero the
        // more elongated a rectangle is
        return (mul * count / lgeo)
    }

    private fun BVHRect.checkDimensions() = checkDimensions(this@BVH.dimensions)
    private fun BVHRay.checkDimensions() = checkDimensions(this@BVH.dimensions)
    private fun BVHIntervals.checkDimensions() = checkDimensions(this@BVH.dimensions)

    /* find the best specific node(s) for object to be deleted from
     * [ leaf node parent ] = _remove_subtree(rectangle, object, root)
     * @private
     */
    private fun _remove_subtree(
        intervals: BVHRect?,
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
                if (tree.nodes!!.size >= i + 1) {
                    tree.nodes!!.removeAt(i + 1) // Remove unsplit node
                }
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
        intervals: BVHRect,
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
                if (l.d.min(d) > nodes[highest_low[d]].d.min(d)) {
                    highest_low[d] = i
                } else if (l.d.min(d) + l.d.size(d) < nodes[lowest_high[d]].d.min(d) + nodes[lowest_high[d]].d.size(d)) {
                    lowest_high[d] = i
                }
            }
        }

        var d = 0
        var last_difference = 0.0
        //for (i = 0; i < _Dimensions; i++) {
        for (i in 0 until this.dimensions) {
            val difference =
                abs((nodes[lowest_high[i]].d.min(i) + nodes[lowest_high[i]].d.size(i)) - nodes[highest_low[i]].d.min(i))
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

    fun envelope(): BVHRect {
        // Return a copy
        return _make_Intervals(this.root.d)
    }

    // Intersect with overall tree bounding-box
    // Returns a segment contained within the pointing box
    private fun _intersect_Intervals(
        ray: BVHRay,
        intervals: BVHRect?
    ): BVHRect? {
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
            parameters[0][i] = ints.min(i)
            parameters[1][i] = ints.min(i) + ints.size(i)

            val j = 1.0 / ray.dir(i)
            inv_direction[i] = j
            sign[i] = if (j <= 0) 1 else 0
        }

        var omin = (parameters[sign[0]][0] - ray.pos(0)) * inv_direction[0]
        var omax = (parameters[1 - sign[0]][0] - ray.pos(0)) * inv_direction[0]

        for (i in 1 until this.dimensions) {
            val tmin = (parameters[sign[i]][i] - ray.pos(i)) * inv_direction[i]
            val tmax = (parameters[1 - sign[i]][i] - ray.pos(i)) * inv_direction[i]

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
            rs.min(i, ray.pos(i) + ray.dir(i) * omin)
            rs.size(i, ray.pos(i) + ray.dir(i) * omax)
        }

        return (rs)
    }

    /* non-recursive internal search function
	 * [ nodes | objects ] = _search_subtree(intervals, [return node data], [array to fill], root to begin search at)
	 * @private
	 */
    private fun _intersect_subtree(
        ray: BVHRay,
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
                        //val dimSizes = (0 until ray.dimensions).map { ray.size(it) }
                        //println("dimSizes=$dimSizes")
                        val dim = (0 until ray.dimensions).maxBy { ray.dir(it).absoluteValue }
                        val raySize = ray.dir(dim)
                        val imin = intersect_points.min(dim)
                        val rmin = ray.pos(dim)
                        val tminNum = imin - rmin
                        val tmin = tminNum / raySize

                        //println("dim=$dim, tminNum=$tminNum [$imin, $rmin], tminDen=$raySize : $tmin")

                        return_array.add(IntersectResult(
                            ray = ray,
                            intersect = tmin,
                            obj = ltree,
                        ))
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
        intervals: BVHRect,
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
            //nodes.fastForEachReverse { ltree ->
            for (i in nodes.size - 1 downTo 0) { val ltree = nodes[i]
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
        intervals: BVHRect,
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

    @Deprecated("Use BVHRay signature")
    fun intersectRay(ray: BVHIntervals, intervals: BVHRect?): BVHRect? = _intersect_Intervals(BVHRay(ray), intervals)
    fun intersectRay(ray: BVHRay, intervals: BVHRect?): BVHRect? = _intersect_Intervals(ray, intervals)

    /* non-recursive intersect function
	 * [ nodes | objects ] = NTree.intersect( options )
	 * @public
	 */
    fun intersect(
        ray: BVHIntervals,
        return_array: FastArrayList<IntersectResult<T>> = fastArrayListOf(),
    ) = _intersect_subtree(ray = BVHRay(ray), return_array = return_array, root = this.root)

    fun intersect(
        ray: BVHRay,
        return_array: FastArrayList<IntersectResult<T>> = fastArrayListOf(),
    ) = _intersect_subtree(ray = ray, return_array = return_array, root = this.root)


    /* non-recursive search function
	 * [ nodes | objects ] = NTree.search(intervals, [return node data], [array to fill])
	 * @public
	 */
    @Deprecated("USe BVHRect signature")
    fun search(
        intervals: BVHIntervals,
        return_array: FastArrayList<Node<T>> = fastArrayListOf(),
        comparators: Comparators = Comparators,
    ): FastArrayList<Node<T>> = search(BVHRect(intervals), return_array, comparators)

    fun search(
        intervals: BVHRect,
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

    override fun iterator(): Iterator<Node<T>> = iterator<Node<T>> {
        val deque = Deque<Node<T>>()
        deque.addLast(root)
        while (deque.isNotEmpty()) {
            val node = deque.removeFirst()
            yield(node)
            node.nodes?.let { deque.addAll(it) }
        }
    }

    fun findAll(): List<Node<T>> {
        return this.toList()
    }

    fun findAllValues(): List<T> {
        return findAll().mapNotNull { it.value }
    }

    @Deprecated("Use BVHRect signature")
    fun searchValues(
        intervals: BVHIntervals,
        comparators: Comparators = Comparators,
    ): List<T> = searchValues(BVHRect(intervals), comparators)

    fun searchValues(
        rect: BVHRect,
        comparators: Comparators = Comparators,
    ): List<T> {
        return _search_subtree(
            intervals = rect,
            root = this.root,
            comparators = comparators
        ).mapNotNull { it.value }
    }

    @Deprecated("USe BVHRect signature")
    fun insertOrUpdate(
        intervals: BVHIntervals,
        obj: T,
    ) = insertOrUpdate(BVHRect(intervals), obj)

    /* non-recursive insert function
	 * [] = NTree.insert(intervals, object to insert)
	 */
    fun insertOrUpdate(
        rect: BVHRect,
        obj: T,
    ) {
        rect.checkDimensions()
        if (allowUpdateObjects) {
            val oldIntervals = objectToIntervalMap[obj]
            if (oldIntervals != null) {
                if (rect == oldIntervals) {
                    return
                }
                if (obj in objectToIntervalMap) remove(obj)
            }
        }
        _insert_subtree(root = this.root, node = Node(d = rect, value = obj))
        if (allowUpdateObjects) objectToIntervalMap[obj] = rect
    }

    fun remove(obj: T) {
        if (!allowUpdateObjects) error("allowUpdateObjects not enabled")
        val intervals = objectToIntervalMap[obj]
        if (intervals != null) remove(intervals, obj)
    }

    fun getObjectBounds(obj: T): BVHIntervals? = objectToIntervalMap[obj]?.intervals
    fun getObjectBoundsRect(obj: T): BVHRect? = objectToIntervalMap[obj]

    //private val objectToIntervalMap = FastIdentityMap<T, BVHIntervals>()
    private val objectToIntervalMap = HashMap<T, BVHRect>()

    @Deprecated("Use BVHRect signature")
    fun remove(
        intervals: BVHIntervals,
        obj: T? = null,
        comparators: Comparators = Comparators,
    ): FastArrayList<Node<T>> = remove(BVHRect(intervals), obj, comparators)

    /* non-recursive function that deletes a specific
	 * [ number ] = NTree.remove(intervals, obj)
	 */
    fun remove(
        rect: BVHRect,
        obj: T? = null,
        comparators: Comparators = Comparators,
    ): FastArrayList<Node<T>> {
        rect.checkDimensions()
        return if (obj == null) { // Do area-wide delete
            val ret_array = fastArrayListOf<Node<T>>()
            do {
                val numberdeleted = ret_array.size
                ret_array.addAll(
                    _remove_subtree(
                        root = this.root,
                        intervals = rect,
                        obj = null,
                        comparators = comparators
                    )
                )
            } while (numberdeleted != ret_array.size)
            ret_array
        } else { // Delete a specific item
            _remove_subtree(root = this.root, intervals = rect, obj = obj, comparators = comparators)
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

data class BVHVector(val data: DoubleArray) {
    companion object {
        operator fun invoke(vararg data: Float): BVHVector = BVHVector(data.mapDouble { it.toDouble() })
        operator fun invoke(vararg data: Double): BVHVector = BVHVector(data)
        operator fun invoke(vararg data: Int): BVHVector = BVHVector(data.mapDouble { it.toDouble() })
    }
    val dimensions: Int get() = data.size
    fun checkDimensions(dims: Int) {
        if (dims != this.dimensions) error("Expected $dims dimensions, but found $dimensions")
    }

    operator fun get(dim: Int): Double = data[dim]
    @Deprecated("Mutable")
    operator fun set(dim: Int, value: Double) {
        data[dim] = value
    }

    override fun equals(other: Any?): Boolean = other is BVHVector && data.contentEquals(other.data)

    override fun toString(): String = "BVHVector(${data.joinToString(", ") { it.niceStr }})"
}

private fun checkDimensions(actual: Int, expected: Int) {
    if (actual != expected) error("element $actual doesn't match dimensions $expected")

}

inline class BVHRay(val intervals: BVHIntervals) {

    fun checkDimensions(dimensions: Int) {
        checkDimensions(this.dimensions, dimensions)
    }

    val data get() = intervals.data
    fun copyFrom(other: BVHRay): BVHRay { intervals.copyFrom(other.intervals); return this }
    fun clone() = BVHRay(BVHIntervals(data.copyOf()))
    val length: Int get() = dimensions
    val dimensions: Int get() = data.size / 2

    fun pos(dim: Int): Double = data[dim * 2 + 0]
    fun dir(dim: Int): Double = data[dim * 2 + 1]

    val pos: BVHVector get() = BVHVector(DoubleArray(dimensions) { pos(it) })
    val dir: BVHVector get() = BVHVector(DoubleArray(dimensions) { dir(it) })

    override fun toString(): String = "BVHRay(pos=$pos, dir=$dir)"
}

inline class BVHRect(val intervals: BVHIntervals) {
    constructor(dimensions: Int) : this(BVHIntervals(dimensions))

    fun checkDimensions(dimensions: Int) {
        checkDimensions(this.dimensions, dimensions)
    }

    val data get() = intervals.data
    fun copyFrom(other: BVHRect): BVHRect { intervals.copyFrom(other.intervals); return this }
    fun clone() = BVHRect(BVHIntervals(data.copyOf()))
    val length: Int get() = dimensions
    val dimensions: Int get() = data.size / 2

    fun min(dim: Int): Double = data[dim * 2 + 0]
    fun size(dim: Int): Double = data[dim * 2 + 1]
    fun max(dim: Int): Double = min(dim) + size(dim)

    fun min(dim: Int, value: Double) { data[dim * 2 + 0] = value }
    fun size(dim: Int, value: Double) { data[dim * 2 + 1] = value }

    val min: BVHVector get() = BVHVector(DoubleArray(dimensions) { min(it) })
    val size: BVHVector get() = BVHVector(DoubleArray(dimensions) { size(it) })
    val max: BVHVector get() = BVHVector(DoubleArray(dimensions) { max(it) })

    override fun toString(): String = "BVHRect(min=$min, max=$max)"
}


/**
 * In the format:
 *
 * [x, width]
 * [x, width, y, height]
 * [x, width, y, height, z, depth]
 *
 * In the case of rays:
 *
 * [x, xDir]
 * [x, xDir, y, yDir]
 * [x, xDir, y, yDir, z, zDir]
 */
//@Suppress("INLINE_CLASS_DEPRECATED")
data class BVHIntervals(val data: DoubleArray) {
    constructor(dimensions: Int) : this(DoubleArray(dimensions * 2))

    private val cachedHashCode = data.contentHashCode()

    override fun hashCode(): Int = cachedHashCode
    override fun equals(other: Any?): Boolean = other is BVHIntervals && this.data.contentEquals(other.data)

    companion object {
        operator fun invoke(vararg values: Double): BVHIntervals = BVHIntervals(values)
        operator fun invoke(vararg values: Float): BVHIntervals = BVHIntervals(values.mapDouble { it.toDouble() })
        operator fun invoke(vararg values: Int): BVHIntervals = BVHIntervals(DoubleArray(values.size) { values[it].toDouble() })
    }

    fun checkDimensions(dimensions: Int) {
        checkDimensions(this.dimensions, dimensions)
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

    val length: Int get() = data.size / 2
    val dimensions: Int get() = length

    fun a(index: Int): Double = min(index)
    fun b(index: Int): Double = size(index)

    @Deprecated("Mutable")
    fun a(index: Int, value: Double) {
        data[index * 2 + 0] = value
    }
    @Deprecated("Mutable")
    fun b(index: Int, value: Double) {
        data[index * 2 + 1] = value
    }

    fun min(dim: Int): Double = data[dim * 2 + 0]
    fun size(dim: Int): Double = data[dim * 2 + 1]
    fun max(dim: Int): Double = min(dim) + size(dim)

    fun aPlusB(index: Int) = min(index) + size(index)

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
            append(min(n))
            append(',')
            append(size(n))
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

package korlibs.math.geom.ds

import korlibs.datastructure.*
import korlibs.datastructure.iterators.*
import korlibs.math.geom.*
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
