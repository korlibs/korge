@file:Suppress("PackageDirectoryMismatch")

package korlibs.math.random

import korlibs.math.geom.*
import korlibs.math.interpolation.*
import kotlin.math.*
import kotlin.random.*

fun Random.ints(): Sequence<Int> = sequence { while (true) yield(nextInt()) }
fun Random.ints(from: Int, until: Int): Sequence<Int> = sequence { while (true) yield(nextInt(from, until)) }
fun Random.ints(range: IntRange): Sequence<Int> = ints(range.first, range.last + 1)

fun Random.doubles(): Sequence<Double> = sequence { while (true) yield(nextDouble()) }
fun Random.floats(): Sequence<Float> = sequence { while (true) yield(nextFloat()) }

fun <T> List<T>.random(random: Random = Random): T {
    if (this.isEmpty()) throw IllegalArgumentException("Empty list")
    return this[random.nextInt(this.size)]
}

fun <T> List<T>.randomWithWeights(weights: List<Double>, random: Random = Random): T = random.weighted(this.zip(weights).toMap())

fun Random.nextDoubleInclusive() = (this.nextInt(0x1000001).toDouble() / 0x1000000.toDouble())
fun Random.nextRatio(): Ratio = nextDouble().toRatio()
fun Random.nextRatioInclusive(): Ratio = nextDoubleInclusive().toRatio()

operator fun Random.get(min: Ratio, max: Ratio): Ratio = Ratio(get(min.value, max.value))
operator fun Random.get(min: Double, max: Double): Double = min + nextDouble() * (max - min)
operator fun Random.get(min: Float, max: Float): Float = min + nextFloat() * (max - min)
operator fun Random.get(min: Int, max: Int): Int = min + nextInt(max - min)
operator fun Random.get(range: IntRange): Int = range.first + this.nextInt(range.last - range.first + 1)
operator fun Random.get(range: LongRange): Long = range.first + this.nextLong() % (range.last - range.first + 1)
operator fun <T : Interpolable<T>> Random.get(l: T, r: T): T = (this.nextDoubleInclusive()).toRatio().interpolate(l, r)
operator fun Random.get(l: Angle, r: Angle): Angle = this.nextRatioInclusive().interpolateAngleDenormalized(l, r)
operator fun <T> Random.get(list: List<T>): T = list[this[list.indices]]
operator fun Random.get(rectangle: Rectangle): Point = Point(this[rectangle.left, rectangle.right], this[rectangle.top, rectangle.bottom])
fun <T : MutableInterpolable<T>> T.setToRandom(min: T, max: T, random: Random = Random) { this.setToInterpolated(random.nextDouble().toRatio(), min, max) }

fun <T> Random.weighted(weights: Map<T, Double>): T = shuffledWeighted(weights).first()
fun <T> Random.weighted(weights: RandomWeights<T>): T = shuffledWeighted(weights).first()

fun <T> Random.shuffledWeighted(weights: Map<T, Double>): List<T> = shuffledWeighted(RandomWeights(weights))
fun <T> Random.shuffledWeighted(values: List<T>, weights: List<Double>): List<T> = shuffledWeighted(RandomWeights(values, weights))
fun <T> Random.shuffledWeighted(weights: RandomWeights<T>): List<T> {
    val randoms = (0 until weights.items.size).map { -(nextDouble().pow(1.0 / weights.normalizedWeights[it])) }
    val sortedIndices = (0 until weights.items.size).sortedWith { a, b -> randoms[a].compareTo(randoms[b]) }
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
            val min = weights.minOrNull() ?: 0.0
            return weights.map { (it + min) + 1 }
        }
    }
}
