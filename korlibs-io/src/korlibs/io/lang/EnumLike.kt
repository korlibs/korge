package korlibs.io.lang

interface EnumLike<T : EnumLike<T>> {
    object Scope

    companion object {
        inline fun <reified T : Enum<T>> getValues(enum: T): List<T> = enumValues<T>().toList()
        inline fun <reified T : EnumLike<T>> getValues(enumLike: EnumLike<T>): List<T> = enumLike.run { Scope.getValues() }
    }

    fun Scope.getValues(): List<T>
}
