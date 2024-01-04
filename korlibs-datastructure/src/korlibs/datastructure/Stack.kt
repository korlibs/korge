@file:Suppress("ReplaceSizeZeroCheckWithIsEmpty", "RemoveExplicitTypeArguments")

package korlibs.datastructure

import korlibs.datastructure.annotations.*

typealias Stack<TGen> = TGenStack<TGen>

// @NOTE: AUTOGENERATED: ONLY MODIFY FROM  GENERIC TEMPLATE to END OF GENERIC TEMPLATE
// Then use ./gradlew generate to regenerate the rest of the file.

// GENERIC TEMPLATE //////////////////////////////////////////

@Template
inline class TGenStack<TGen>(private val items: FastArrayList<TGen> = FastArrayList<TGen>()) : Collection<TGen> {
    override val size: Int get() = items.size
    override fun isEmpty() = size == 0

    companion object {
        operator fun <TGen> invoke(vararg items: TGen): TGenStack<TGen> {
            val out = TGenStack<TGen>(FastArrayList<TGen>(items.size))
            for (item in items) out.push(item)
            return out
            //return TGenStack(FastArrayList(items.toList()))
        }
    }

    fun push(v: TGen) {
        items.add(v)
    }

    fun pop(): TGen = items.removeAt(items.size - 1)
    fun peek(): TGen? = items.lastOrNull()
    fun clear() {
        items.clear()
    }

    override fun contains(element: TGen): Boolean = items.contains(element)
    override fun containsAll(elements: Collection<TGen>): Boolean = items.containsAll(elements)
    override fun iterator(): Iterator<TGen> = items.iterator()

    //override fun hashCode(): Int = items.hashCode()
    //override fun equals(other: Any?): Boolean = (other is TGenStack<*/*TGen*/>) && items == other.items
}

// END OF GENERIC TEMPLATE ///////////////////////////////////

// AUTOGENERATED: DO NOT MODIFY MANUALLY STARTING FROM HERE!

// Int


@Template
inline class IntStack(private val items: IntArrayList = IntArrayList()) : Collection<Int> {
    override val size: Int get() = items.size
    override fun isEmpty() = size == 0

    companion object {
        operator fun invoke(vararg items: Int): IntStack {
            val out = IntStack(IntArrayList(items.size))
            for (item in items) out.push(item)
            return out
            //return IntStack(ArrayList(items.toList()))
        }
    }

    fun push(v: Int) {
        items.add(v)
    }

    fun pop(): Int = items.removeAt(items.size - 1)
    fun peek(): Int? = items.lastOrNull()
    fun clear() {
        items.clear()
    }

    override fun contains(element: Int): Boolean = items.contains(element)
    override fun containsAll(elements: Collection<Int>): Boolean = items.containsAll(elements)
    override fun iterator(): Iterator<Int> = items.iterator()

    //override fun hashCode(): Int = items.hashCode()
    //override fun equals(other: Any?): Boolean = (other is IntStack) && items == other.items
}



// Double


@Template
inline class DoubleStack(private val items: DoubleArrayList = DoubleArrayList()) : Collection<Double> {
    override val size: Int get() = items.size
    override fun isEmpty() = size == 0

    companion object {
        operator fun invoke(vararg items: Double): DoubleStack {
            val out = DoubleStack(DoubleArrayList(items.size))
            for (item in items) out.push(item)
            return out
            //return DoubleStack(ArrayList(items.toList()))
        }
    }

    fun push(v: Double) {
        items.add(v)
    }

    fun pop(): Double = items.removeAt(items.size - 1)
    fun peek(): Double? = items.lastOrNull()
    fun clear() {
        items.clear()
    }

    override fun contains(element: Double): Boolean = items.contains(element)
    override fun containsAll(elements: Collection<Double>): Boolean = items.containsAll(elements)
    override fun iterator(): Iterator<Double> = items.iterator()

    //override fun hashCode(): Int = items.hashCode()
    //override fun equals(other: Any?): Boolean = (other is DoubleStack) && items == other.items
}



// Float


@Template
inline class FloatStack(private val items: FloatArrayList = FloatArrayList()) : Collection<Float> {
    override val size: Int get() = items.size
    override fun isEmpty() = size == 0

    companion object {
        operator fun invoke(vararg items: Float): FloatStack {
            val out = FloatStack(FloatArrayList(items.size))
            for (item in items) out.push(item)
            return out
            //return FloatStack(ArrayList(items.toList()))
        }
    }

    fun push(v: Float) {
        items.add(v)
    }

    fun pop(): Float = items.removeAt(items.size - 1)
    fun peek(): Float? = items.lastOrNull()
    fun clear() {
        items.clear()
    }

    override fun contains(element: Float): Boolean = items.contains(element)
    override fun containsAll(elements: Collection<Float>): Boolean = items.containsAll(elements)
    override fun iterator(): Iterator<Float> = items.iterator()

    //override fun hashCode(): Int = items.hashCode()
    //override fun equals(other: Any?): Boolean = (other is FloatStack) && items == other.items
}
