package korlibs.datastructure

typealias PriorityQueue<TGen> = TGenPriorityQueue<TGen>
typealias IntComparator = Comparator<Int>
typealias DoubleComparator = Comparator<Double>
typealias FloatComparator = Comparator<Float>

private inline class PriorityQueueNode(val index: Int) {}

// @NOTE: AUTOGENERATED: ONLY MODIFY FROM  GENERIC TEMPLATE to END OF GENERIC TEMPLATE
// Then use ./gradlew generate to regenerate the rest of the file.

// GENERIC TEMPLATE //////////////////////////////////////////

@Suppress("UNCHECKED_CAST", "RemoveExplicitTypeArguments")
class TGenPriorityQueue<TGen>
@PublishedApi internal constructor(private var data: Array<TGen>, val comparator: Comparator<TGen>) :
    MutableCollection<TGen> {
    companion object {
        operator fun <TGen> invoke(
            initialCapacity: Int,
            comparator: Comparator<TGen>,
            reversed: Boolean = false
        ): TGenPriorityQueue<TGen> =
            TGenPriorityQueue<TGen>(
                arrayOfNulls<Any>(initialCapacity) as Array<TGen>,
                if (reversed) comparator.reversed() else comparator
            )

        operator fun <TGen> invoke(comparator: Comparator<TGen>, reversed: Boolean = false): TGenPriorityQueue<TGen> =
            TGenPriorityQueue<TGen>(
                arrayOfNulls<Any>(16) as Array<TGen>,
                if (reversed) comparator.reversed() else comparator
            )

        operator fun <TGen> invoke(
            reversed: Boolean = false,
            comparator: (left: TGen, right: TGen) -> Int
        ): TGenPriorityQueue<TGen> =
            TGenPriorityQueue<TGen>(Comparator(comparator), reversed)

        operator fun <TGen : Comparable<TGen>> invoke(reversed: Boolean = false): TGenPriorityQueue<TGen> =
            TGenPriorityQueue<TGen>(comparator(), reversed)
    }

    private var PriorityQueueNode.value
        get() = data[this.index]
        set(value) {
            data[this.index] = value
        }
    private val PriorityQueueNode.isRoot: Boolean get() = this.index == 0
    private val PriorityQueueNode.parent: PriorityQueueNode get() = PriorityQueueNode((this.index - 1) / 2)
    private val PriorityQueueNode.left: PriorityQueueNode get() = PriorityQueueNode(2 * this.index + 1)
    private val PriorityQueueNode.right: PriorityQueueNode get() = PriorityQueueNode(2 * this.index + 2)

    private fun gt(a: TGen, b: TGen) = comparator.compare(a, b) > 0
    private fun lt(a: TGen, b: TGen) = comparator.compare(a, b) < 0

    private val capacity get() = data.size
    override var size = 0; private set
    val head: TGen
        get() {
            if (size <= 0) throw IndexOutOfBoundsException()
            return data[0]
        }

    override fun add(element: TGen): Boolean {
        size++
        ensure(size)
        var i = PriorityQueueNode(size - 1)
        i.value = element
        while (!i.isRoot && gt(i.parent.value, i.value)) {
            swap(i, i.parent)
            i = i.parent
        }
        return true
    }

    fun removeHead(): TGen {
        if (size <= 0) throw IndexOutOfBoundsException()
        if (size == 1) {
            size--
            return PriorityQueueNode(0).value
        }
        val root = PriorityQueueNode(0).value
        PriorityQueueNode(0).value = PriorityQueueNode(size - 1).value
        size--
        minHeapify(0)
        return root
    }

    fun indexOf(element: TGen): Int {
        for (n in 0 until size) {
            if (this.data[n] == element) return n
        }
        return -1
    }

    fun updateObject(element: TGen) {
        val index = indexOf(element)
        if (index >= 0) updateAt(index)
    }

    fun updateAt(index: Int) {
        val value = PriorityQueueNode(index).value
        removeAt(index)
        add(value)
    }

    override fun remove(element: TGen): Boolean {
        val index = indexOf(element)
        if (index >= 0) removeAt(index)
        return index >= 0
    }

    fun removeAt(index: Int) {
        var i = PriorityQueueNode(index)
        while (i.index != 0) {
            swap(i, i.parent)
            i = i.parent
        }
        removeHead()
    }

    private fun ensure(index: Int) {
        if (index >= capacity) {
            data = data.copyOf(2 + capacity * 2) as Array<TGen>
        }
    }

    private fun minHeapify(index: Int) {
        var i = PriorityQueueNode(index)
        while (true) {
            val left = i.left
            val right = i.right
            var smallest = i
            if (left.index < size && lt(left.value, i.value)) smallest = left
            if (right.index < size && lt(right.value, smallest.value)) smallest = right
            if (smallest != i) {
                swap(i, smallest)
                i = smallest
            } else {
                break
            }
        }
    }

    private fun swap(l: PriorityQueueNode, r: PriorityQueueNode) {
        val temp = r.value
        r.value = l.value
        l.value = temp
    }

    override operator fun contains(element: TGen): Boolean =
        (0 until size).any { PriorityQueueNode(it).value == element }

    override fun containsAll(elements: Collection<TGen>): Boolean {
        val thisSet = this.toSet()
        return elements.all { it in thisSet }
    }

    override fun isEmpty(): Boolean = size == 0
    override fun addAll(elements: Collection<TGen>): Boolean {
        for (e in elements) add(e)
        return elements.isNotEmpty()
    }

    override fun clear() {
        size = 0
    }

    //fun poll() = head

    override fun removeAll(elements: Collection<TGen>): Boolean {
        val temp = ArrayList(toList())
        val res = temp.removeAll(elements)
        clear()
        addAll(temp)
        return res
    }

    override fun retainAll(elements: Collection<TGen>): Boolean {
        val temp = ArrayList(toList())
        val res = temp.retainAll(elements)
        clear()
        addAll(temp)
        return res
    }

    override fun iterator(): MutableIterator<TGen> {
        var index = 0
        return object : MutableIterator<TGen> {
            override fun hasNext(): Boolean = index < size
            override fun next(): TGen = PriorityQueueNode(index++).value
            override fun remove() = TODO()
        }
    }

    fun toArraySorted(): Array<TGen> {
        val out = arrayOfNulls<Any>(size) as Array<TGen>
        for (n in 0 until size) out[n] = removeHead()
        for (v in out) add(v)
        return out
    }

    override fun toString(): String = toList().toString()

    override fun equals(other: Any?): Boolean =
        other is TGenPriorityQueue<*/*_TGen_*/> && this.data.contentEquals(other.data) && this.comparator == other.comparator

    override fun hashCode(): Int = data.contentHashCode()
}

// END OF GENERIC TEMPLATE ///////////////////////////////////

// AUTOGENERATED: DO NOT MODIFY MANUALLY STARTING FROM HERE!

// Int


@Suppress("UNCHECKED_CAST", "RemoveExplicitTypeArguments")
class IntPriorityQueue
@PublishedApi internal constructor(private var data: IntArray, val comparator: IntComparator) :
    MutableCollection<Int> {
    companion object {
        operator fun invoke(
            initialCapacity: Int,
            comparator: IntComparator,
            reversed: Boolean = false
        ): IntPriorityQueue =
            IntPriorityQueue(
                IntArray(initialCapacity) as IntArray,
                if (reversed) comparator.reversed() else comparator
            )

        operator fun invoke(comparator: IntComparator, reversed: Boolean = false): IntPriorityQueue =
            IntPriorityQueue(
                IntArray(16) as IntArray,
                if (reversed) comparator.reversed() else comparator
            )

        operator fun invoke(
            reversed: Boolean = false,
            comparator: (left: Int, right: Int) -> Int
        ): IntPriorityQueue =
            IntPriorityQueue(Comparator(comparator), reversed)

        operator fun  invoke(reversed: Boolean = false): IntPriorityQueue =
            IntPriorityQueue(comparator(), reversed)
    }

    private var PriorityQueueNode.value
        get() = data[this.index]
        set(value) {
            data[this.index] = value
        }
    private val PriorityQueueNode.isRoot: Boolean get() = this.index == 0
    private val PriorityQueueNode.parent: PriorityQueueNode get() = PriorityQueueNode((this.index - 1) / 2)
    private val PriorityQueueNode.left: PriorityQueueNode get() = PriorityQueueNode(2 * this.index + 1)
    private val PriorityQueueNode.right: PriorityQueueNode get() = PriorityQueueNode(2 * this.index + 2)

    private fun gt(a: Int, b: Int) = comparator.compare(a, b) > 0
    private fun lt(a: Int, b: Int) = comparator.compare(a, b) < 0

    private val capacity get() = data.size
    override var size = 0; private set
    val head: Int
        get() {
            if (size <= 0) throw IndexOutOfBoundsException()
            return data[0]
        }

    override fun add(element: Int): Boolean {
        size++
        ensure(size)
        var i = PriorityQueueNode(size - 1)
        i.value = element
        while (!i.isRoot && gt(i.parent.value, i.value)) {
            swap(i, i.parent)
            i = i.parent
        }
        return true
    }

    fun removeHead(): Int {
        if (size <= 0) throw IndexOutOfBoundsException()
        if (size == 1) {
            size--
            return PriorityQueueNode(0).value
        }
        val root = PriorityQueueNode(0).value
        PriorityQueueNode(0).value = PriorityQueueNode(size - 1).value
        size--
        minHeapify(0)
        return root
    }

    fun indexOf(element: Int): Int {
        for (n in 0 until size) {
            if (this.data[n] == element) return n
        }
        return -1
    }

    fun updateObject(element: Int) {
        val index = indexOf(element)
        if (index >= 0) updateAt(index)
    }

    fun updateAt(index: Int) {
        val value = PriorityQueueNode(index).value
        removeAt(index)
        add(value)
    }

    override fun remove(element: Int): Boolean {
        val index = indexOf(element)
        if (index >= 0) removeAt(index)
        return index >= 0
    }

    fun removeAt(index: Int) {
        var i = PriorityQueueNode(index)
        while (i.index != 0) {
            swap(i, i.parent)
            i = i.parent
        }
        removeHead()
    }

    private fun ensure(index: Int) {
        if (index >= capacity) {
            data = data.copyOf(2 + capacity * 2) as IntArray
        }
    }

    private fun minHeapify(index: Int) {
        var i = PriorityQueueNode(index)
        while (true) {
            val left = i.left
            val right = i.right
            var smallest = i
            if (left.index < size && lt(left.value, i.value)) smallest = left
            if (right.index < size && lt(right.value, smallest.value)) smallest = right
            if (smallest != i) {
                swap(i, smallest)
                i = smallest
            } else {
                break
            }
        }
    }

    private fun swap(l: PriorityQueueNode, r: PriorityQueueNode) {
        val temp = r.value
        r.value = l.value
        l.value = temp
    }

    override operator fun contains(element: Int): Boolean =
        (0 until size).any { PriorityQueueNode(it).value == element }

    override fun containsAll(elements: Collection<Int>): Boolean {
        val thisSet = this.toSet()
        return elements.all { it in thisSet }
    }

    override fun isEmpty(): Boolean = size == 0
    override fun addAll(elements: Collection<Int>): Boolean {
        for (e in elements) add(e)
        return elements.isNotEmpty()
    }

    override fun clear() {
        size = 0
    }

    //fun poll() = head

    override fun removeAll(elements: Collection<Int>): Boolean {
        val temp = ArrayList(toList())
        val res = temp.removeAll(elements)
        clear()
        addAll(temp)
        return res
    }

    override fun retainAll(elements: Collection<Int>): Boolean {
        val temp = ArrayList(toList())
        val res = temp.retainAll(elements)
        clear()
        addAll(temp)
        return res
    }

    override fun iterator(): MutableIterator<Int> {
        var index = 0
        return object : MutableIterator<Int> {
            override fun hasNext(): Boolean = index < size
            override fun next(): Int = PriorityQueueNode(index++).value
            override fun remove() = TODO()
        }
    }

    fun toArraySorted(): IntArray {
        val out = IntArray(size) as IntArray
        for (n in 0 until size) out[n] = removeHead()
        for (v in out) add(v)
        return out
    }

    override fun toString(): String = toList().toString()

    override fun equals(other: Any?): Boolean =
        other is IntPriorityQueue && this.data.contentEquals(other.data) && this.comparator == other.comparator

    override fun hashCode(): Int = data.contentHashCode()
}



// Double


@Suppress("UNCHECKED_CAST", "RemoveExplicitTypeArguments")
class DoublePriorityQueue
@PublishedApi internal constructor(private var data: DoubleArray, val comparator: DoubleComparator) :
    MutableCollection<Double> {
    companion object {
        operator fun invoke(
            initialCapacity: Int,
            comparator: DoubleComparator,
            reversed: Boolean = false
        ): DoublePriorityQueue =
            DoublePriorityQueue(
                DoubleArray(initialCapacity) as DoubleArray,
                if (reversed) comparator.reversed() else comparator
            )

        operator fun invoke(comparator: DoubleComparator, reversed: Boolean = false): DoublePriorityQueue =
            DoublePriorityQueue(
                DoubleArray(16) as DoubleArray,
                if (reversed) comparator.reversed() else comparator
            )

        operator fun invoke(
            reversed: Boolean = false,
            comparator: (left: Double, right: Double) -> Int
        ): DoublePriorityQueue =
            DoublePriorityQueue(Comparator(comparator), reversed)

        operator fun  invoke(reversed: Boolean = false): DoublePriorityQueue =
            DoublePriorityQueue(comparator(), reversed)
    }

    private var PriorityQueueNode.value
        get() = data[this.index]
        set(value) {
            data[this.index] = value
        }
    private val PriorityQueueNode.isRoot: Boolean get() = this.index == 0
    private val PriorityQueueNode.parent: PriorityQueueNode get() = PriorityQueueNode((this.index - 1) / 2)
    private val PriorityQueueNode.left: PriorityQueueNode get() = PriorityQueueNode(2 * this.index + 1)
    private val PriorityQueueNode.right: PriorityQueueNode get() = PriorityQueueNode(2 * this.index + 2)

    private fun gt(a: Double, b: Double) = comparator.compare(a, b) > 0
    private fun lt(a: Double, b: Double) = comparator.compare(a, b) < 0

    private val capacity get() = data.size
    override var size = 0; private set
    val head: Double
        get() {
            if (size <= 0) throw IndexOutOfBoundsException()
            return data[0]
        }

    override fun add(element: Double): Boolean {
        size++
        ensure(size)
        var i = PriorityQueueNode(size - 1)
        i.value = element
        while (!i.isRoot && gt(i.parent.value, i.value)) {
            swap(i, i.parent)
            i = i.parent
        }
        return true
    }

    fun removeHead(): Double {
        if (size <= 0) throw IndexOutOfBoundsException()
        if (size == 1) {
            size--
            return PriorityQueueNode(0).value
        }
        val root = PriorityQueueNode(0).value
        PriorityQueueNode(0).value = PriorityQueueNode(size - 1).value
        size--
        minHeapify(0)
        return root
    }

    fun indexOf(element: Double): Int {
        for (n in 0 until size) {
            if (this.data[n] == element) return n
        }
        return -1
    }

    fun updateObject(element: Double) {
        val index = indexOf(element)
        if (index >= 0) updateAt(index)
    }

    fun updateAt(index: Int) {
        val value = PriorityQueueNode(index).value
        removeAt(index)
        add(value)
    }

    override fun remove(element: Double): Boolean {
        val index = indexOf(element)
        if (index >= 0) removeAt(index)
        return index >= 0
    }

    fun removeAt(index: Int) {
        var i = PriorityQueueNode(index)
        while (i.index != 0) {
            swap(i, i.parent)
            i = i.parent
        }
        removeHead()
    }

    private fun ensure(index: Int) {
        if (index >= capacity) {
            data = data.copyOf(2 + capacity * 2) as DoubleArray
        }
    }

    private fun minHeapify(index: Int) {
        var i = PriorityQueueNode(index)
        while (true) {
            val left = i.left
            val right = i.right
            var smallest = i
            if (left.index < size && lt(left.value, i.value)) smallest = left
            if (right.index < size && lt(right.value, smallest.value)) smallest = right
            if (smallest != i) {
                swap(i, smallest)
                i = smallest
            } else {
                break
            }
        }
    }

    private fun swap(l: PriorityQueueNode, r: PriorityQueueNode) {
        val temp = r.value
        r.value = l.value
        l.value = temp
    }

    override operator fun contains(element: Double): Boolean =
        (0 until size).any { PriorityQueueNode(it).value == element }

    override fun containsAll(elements: Collection<Double>): Boolean {
        val thisSet = this.toSet()
        return elements.all { it in thisSet }
    }

    override fun isEmpty(): Boolean = size == 0
    override fun addAll(elements: Collection<Double>): Boolean {
        for (e in elements) add(e)
        return elements.isNotEmpty()
    }

    override fun clear() {
        size = 0
    }

    //fun poll() = head

    override fun removeAll(elements: Collection<Double>): Boolean {
        val temp = ArrayList(toList())
        val res = temp.removeAll(elements)
        clear()
        addAll(temp)
        return res
    }

    override fun retainAll(elements: Collection<Double>): Boolean {
        val temp = ArrayList(toList())
        val res = temp.retainAll(elements)
        clear()
        addAll(temp)
        return res
    }

    override fun iterator(): MutableIterator<Double> {
        var index = 0
        return object : MutableIterator<Double> {
            override fun hasNext(): Boolean = index < size
            override fun next(): Double = PriorityQueueNode(index++).value
            override fun remove() = TODO()
        }
    }

    fun toArraySorted(): DoubleArray {
        val out = DoubleArray(size) as DoubleArray
        for (n in 0 until size) out[n] = removeHead()
        for (v in out) add(v)
        return out
    }

    override fun toString(): String = toList().toString()

    override fun equals(other: Any?): Boolean =
        other is DoublePriorityQueue && this.data.contentEquals(other.data) && this.comparator == other.comparator

    override fun hashCode(): Int = data.contentHashCode()
}



// Float


@Suppress("UNCHECKED_CAST", "RemoveExplicitTypeArguments")
class FloatPriorityQueue
@PublishedApi internal constructor(private var data: FloatArray, val comparator: FloatComparator) :
    MutableCollection<Float> {
    companion object {
        operator fun invoke(
            initialCapacity: Int,
            comparator: FloatComparator,
            reversed: Boolean = false
        ): FloatPriorityQueue =
            FloatPriorityQueue(
                FloatArray(initialCapacity) as FloatArray,
                if (reversed) comparator.reversed() else comparator
            )

        operator fun invoke(comparator: FloatComparator, reversed: Boolean = false): FloatPriorityQueue =
            FloatPriorityQueue(
                FloatArray(16) as FloatArray,
                if (reversed) comparator.reversed() else comparator
            )

        operator fun invoke(
            reversed: Boolean = false,
            comparator: (left: Float, right: Float) -> Int
        ): FloatPriorityQueue =
            FloatPriorityQueue(Comparator(comparator), reversed)

        operator fun  invoke(reversed: Boolean = false): FloatPriorityQueue =
            FloatPriorityQueue(comparator(), reversed)
    }

    private var PriorityQueueNode.value
        get() = data[this.index]
        set(value) {
            data[this.index] = value
        }
    private val PriorityQueueNode.isRoot: Boolean get() = this.index == 0
    private val PriorityQueueNode.parent: PriorityQueueNode get() = PriorityQueueNode((this.index - 1) / 2)
    private val PriorityQueueNode.left: PriorityQueueNode get() = PriorityQueueNode(2 * this.index + 1)
    private val PriorityQueueNode.right: PriorityQueueNode get() = PriorityQueueNode(2 * this.index + 2)

    private fun gt(a: Float, b: Float) = comparator.compare(a, b) > 0
    private fun lt(a: Float, b: Float) = comparator.compare(a, b) < 0

    private val capacity get() = data.size
    override var size = 0; private set
    val head: Float
        get() {
            if (size <= 0) throw IndexOutOfBoundsException()
            return data[0]
        }

    override fun add(element: Float): Boolean {
        size++
        ensure(size)
        var i = PriorityQueueNode(size - 1)
        i.value = element
        while (!i.isRoot && gt(i.parent.value, i.value)) {
            swap(i, i.parent)
            i = i.parent
        }
        return true
    }

    fun removeHead(): Float {
        if (size <= 0) throw IndexOutOfBoundsException()
        if (size == 1) {
            size--
            return PriorityQueueNode(0).value
        }
        val root = PriorityQueueNode(0).value
        PriorityQueueNode(0).value = PriorityQueueNode(size - 1).value
        size--
        minHeapify(0)
        return root
    }

    fun indexOf(element: Float): Int {
        for (n in 0 until size) {
            if (this.data[n] == element) return n
        }
        return -1
    }

    fun updateObject(element: Float) {
        val index = indexOf(element)
        if (index >= 0) updateAt(index)
    }

    fun updateAt(index: Int) {
        val value = PriorityQueueNode(index).value
        removeAt(index)
        add(value)
    }

    override fun remove(element: Float): Boolean {
        val index = indexOf(element)
        if (index >= 0) removeAt(index)
        return index >= 0
    }

    fun removeAt(index: Int) {
        var i = PriorityQueueNode(index)
        while (i.index != 0) {
            swap(i, i.parent)
            i = i.parent
        }
        removeHead()
    }

    private fun ensure(index: Int) {
        if (index >= capacity) {
            data = data.copyOf(2 + capacity * 2) as FloatArray
        }
    }

    private fun minHeapify(index: Int) {
        var i = PriorityQueueNode(index)
        while (true) {
            val left = i.left
            val right = i.right
            var smallest = i
            if (left.index < size && lt(left.value, i.value)) smallest = left
            if (right.index < size && lt(right.value, smallest.value)) smallest = right
            if (smallest != i) {
                swap(i, smallest)
                i = smallest
            } else {
                break
            }
        }
    }

    private fun swap(l: PriorityQueueNode, r: PriorityQueueNode) {
        val temp = r.value
        r.value = l.value
        l.value = temp
    }

    override operator fun contains(element: Float): Boolean =
        (0 until size).any { PriorityQueueNode(it).value == element }

    override fun containsAll(elements: Collection<Float>): Boolean {
        val thisSet = this.toSet()
        return elements.all { it in thisSet }
    }

    override fun isEmpty(): Boolean = size == 0
    override fun addAll(elements: Collection<Float>): Boolean {
        for (e in elements) add(e)
        return elements.isNotEmpty()
    }

    override fun clear() {
        size = 0
    }

    //fun poll() = head

    override fun removeAll(elements: Collection<Float>): Boolean {
        val temp = ArrayList(toList())
        val res = temp.removeAll(elements)
        clear()
        addAll(temp)
        return res
    }

    override fun retainAll(elements: Collection<Float>): Boolean {
        val temp = ArrayList(toList())
        val res = temp.retainAll(elements)
        clear()
        addAll(temp)
        return res
    }

    override fun iterator(): MutableIterator<Float> {
        var index = 0
        return object : MutableIterator<Float> {
            override fun hasNext(): Boolean = index < size
            override fun next(): Float = PriorityQueueNode(index++).value
            override fun remove() = TODO()
        }
    }

    fun toArraySorted(): FloatArray {
        val out = FloatArray(size) as FloatArray
        for (n in 0 until size) out[n] = removeHead()
        for (v in out) add(v)
        return out
    }

    override fun toString(): String = toList().toString()

    override fun equals(other: Any?): Boolean =
        other is FloatPriorityQueue && this.data.contentEquals(other.data) && this.comparator == other.comparator

    override fun hashCode(): Int = data.contentHashCode()
}
