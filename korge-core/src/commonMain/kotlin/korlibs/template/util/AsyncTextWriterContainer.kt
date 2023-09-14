package korlibs.template.util

interface AsyncTextWriterContainer {
    suspend fun write(writer: suspend (String) -> Unit)
}
