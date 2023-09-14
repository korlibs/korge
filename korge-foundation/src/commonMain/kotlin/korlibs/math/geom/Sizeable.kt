package korlibs.math.geom

interface Sizeable {
    val size: Size

    companion object {
        operator fun invoke(size: Size): Sizeable = object : Sizeable {
            override val size: Size get() = size
        }
    }
}
