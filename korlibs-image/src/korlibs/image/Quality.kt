package korlibs.image

fun Quality(level: Float, name: String? = null): Quality = QualityImpl(level, name)
//interface Quality : Comparable<Quality> {
interface Quality {
    val level: Float

    operator fun compareTo(other: Quality): Int = this.level.compareTo(other.level)

    companion object {
        val LOWEST: Quality = QualityImpl(0f, "LOWEST")
        val LOW: Quality = QualityImpl(.25f, "LOW")
        val MEDIUM: Quality = QualityImpl(.5f, "MEDIUM")
        val HIGH: Quality = QualityImpl(.75f, "HIGH")
        val HIGHEST: Quality = QualityImpl(1f, "HIGHEST")

        val LIST = listOf(LOWEST, LOW, MEDIUM, HIGH, HIGHEST)
    }
}

val Quality.isLow: Boolean get() = level <= 0.25f
val Quality.isMedium: Boolean get() = !isLow && !isHigh
val Quality.isHigh: Boolean get() = level >= 0.75f

private data class QualityImpl(override val level: Float, val name: String? = null) : Quality {
    override fun toString(): String = name ?: super.toString()
}
