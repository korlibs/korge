package korlibs.platform

public enum class Endian {
    LITTLE_ENDIAN, BIG_ENDIAN;

    val isLittle: Boolean get() = this == LITTLE_ENDIAN
    val isBig: Boolean get() = this == BIG_ENDIAN

    public companion object {
        val isLittleEndian: Boolean get() = currentIsLittleEndian
        val isBigEndian: Boolean get() = !currentIsLittleEndian

        public val NATIVE: Endian = if (currentIsLittleEndian) LITTLE_ENDIAN else BIG_ENDIAN
    }
}
