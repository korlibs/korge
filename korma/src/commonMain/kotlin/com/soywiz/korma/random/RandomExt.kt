package com.soywiz.korma.random

import com.soywiz.korma.geom.*
import com.soywiz.korma.interpolation.*
import com.soywiz.korma.math.*
import kotlin.math.*
import kotlin.random.*

fun Random.ints(): Sequence<Int> = sequence { while (true) yield(nextInt()) }
fun Random.ints(from: Int, until: Int): Sequence<Int> = sequence { while (true) yield(nextInt(from, until)) }
fun Random.ints(range: IntRange): Sequence<Int> = ints(range.start, range.endInclusive + 1)

fun Random.doubles(): Sequence<Double> = sequence { while (true) yield(nextDouble()) }
fun Random.floats(): Sequence<Float> = sequence { while (true) yield(nextFloat()) }

fun <T> List<T>.random(random: Random = Random): T {
    if (this.isEmpty()) throw IllegalArgumentException("Empty list")
    return this[random.nextInt(this.size)]
}

fun <T> List<T>.randomWithWeights(weights: List<Double>, random: Random = Random): T = random.weighted(this.zip(weights).toMap())

operator fun Random.get(min: Double, max: Double): Double = min + nextDouble() * (max - min)
operator fun Random.get(min: Float, max: Float): Float = min + nextFloat() * (max - min)
operator fun Random.get(min: Int, max: Int): Int = min + nextInt(max - min)
operator fun Random.get(range: IntRange): Int = range.start + this.nextInt(range.endInclusive - range.start + 1)
operator fun Random.get(range: LongRange): Long = range.start + this.nextLong() % (range.endInclusive - range.start + 1)
operator fun <T : Interpolable<T>> Random.get(l: T, r: T): T = (this.nextInt(0x10001).toDouble() / 0x10000.toDouble()).interpolate(l, r)
operator fun <T> Random.get(list: List<T>): T = list[this[list.indices]]
operator fun Random.get(rectangle: Rectangle): IPoint = IPoint(this[rectangle.left, rectangle.right], this[rectangle.top, rectangle.bottom])
fun <T : MutableInterpolable<T>> T.setToRandom(min: T, max: T, random: Random = Random) = run { this.setToInterpolated(random.nextDouble(), min, max) }
operator fun <T : Comparable<T>> Random.get(range: ClosedRange<T>): T = (this.nextInt(0x10001).toDouble() / 0x10000.toDouble()).interpolateAny(range.start, range.endInclusive)

fun <T> Random.weighted(weights: Map<T, Double>): T = shuffledWeighted(weights).first()
fun <T> Random.weighted(weights: RandomWeights<T>): T = shuffledWeighted(weights).first()

fun <T> Random.shuffledWeighted(weights: Map<T, Double>): List<T> = shuffledWeighted(RandomWeights(weights))
fun <T> Random.shuffledWeighted(values: List<T>, weights: List<Double>): List<T> = shuffledWeighted(RandomWeights(values, weights))
fun <T> Random.shuffledWeighted(weights: RandomWeights<T>): List<T> {
    val randoms = (0 until weights.items.size).map { -(nextDouble().pow(1.0 / weights.normalizedWeights[it])) }
    val sortedIndices = (0 until weights.items.size).sortedWith(Comparator { a, b -> randoms[a].compareTo(randoms[b]) })
    return sortedIndices.map { weights.items[it] }
}

data class RandomWeights<T>(val weightsMap: Map<T, Double>) {
    constructor(vararg pairs: Pair<T, Double>) : this(mapOf(*pairs))
    constructor(values: List<T>, weights: List<Double>) : this(values.zip(weights).toMap())

    val items = weightsMap.keys.toList()
    val weights = weightsMap.values.toList()
    val normalizedWeights = normalizeWeights(weights)

    companion object {
        private fun normalizeWeights(weights: List<Double>): List<Double> {
            val min = weights.min() ?: 0.0
            return weights.map { (it + min) + 1 }
        }
    }
}
