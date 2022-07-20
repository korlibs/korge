package samples.fleks.utils

import kotlin.random.Random

fun ClosedFloatingPointRange<Double>.random() = Random.nextDouble(start, endInclusive)
fun ClosedFloatingPointRange<Float>.random() = Random.nextDouble(start.toDouble(), endInclusive.toDouble()).toFloat()

fun random(radius: Double) = (-radius..radius).random()
