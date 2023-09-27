package korlibs.memory

fun byteArrayOf(vararg values: Int): ByteArray = ByteArray(values.size).also {
    for (n in 0 until values.size) it[n] = values[n].toByte()
}
