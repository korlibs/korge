package korlibs.math.geom

interface SizeableInt {
    val size: SizeInt
    companion object {
        operator fun invoke(size: SizeInt): SizeableInt = object : SizeableInt {
            override val size: SizeInt get() = size
        }
        operator fun invoke(width: Int, height: Int): SizeableInt = invoke(SizeInt(width, height))
    }
}