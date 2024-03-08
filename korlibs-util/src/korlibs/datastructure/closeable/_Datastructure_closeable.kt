@file:Suppress("PackageDirectoryMismatch")

package korlibs.datastructure.closeable

@OptIn(ExperimentalStdlibApi::class)
interface Closeable : AutoCloseable {
    companion object {
        operator fun invoke(callback: () -> Unit) = object : Closeable {
            override fun close() = callback()
        }
    }
}

object DummyCloseable : Closeable {
    override fun close() {
    }
}

interface OptionalCloseable : Closeable {
    override fun close(): Unit = Unit
}
