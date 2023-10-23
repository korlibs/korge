@file:Suppress("PackageDirectoryMismatch")

package korlibs.datastructure.closeable

@OptIn(ExperimentalStdlibApi::class)
//interface Closeable : AutoCloseable {
//interface Closeable : AutoCloseable {
interface Closeable {
    fun close(): Unit

    companion object {
        operator fun invoke(callback: () -> Unit) = object : Closeable {
            override fun close() = callback()
        }
    }
}
