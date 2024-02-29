package korlibs.math

public inline fun fract(value: Float): Float = value - value.toIntFloor()
public inline fun fract(value: Double): Double = value - value.toIntFloor()
