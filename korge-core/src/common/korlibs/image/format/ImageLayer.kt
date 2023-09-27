package korlibs.image.format

open class ImageLayer constructor(
    var index: Int,
    val name: String?,
    val type: Type = Type.NORMAL,
) {
    enum class Type {
        NORMAL, // ordinal=0
        GROUP,  // ordinal=1
        TILEMAP,  // ordinal=2
        ;

        val isNormal: Boolean get() = this == NORMAL
        val isGroup: Boolean get() = this == GROUP
        val isTilemap: Boolean get() = this == TILEMAP

        companion object {
            val BY_ORDINAL = arrayOf(NORMAL, GROUP, TILEMAP)
        }
    }
}
