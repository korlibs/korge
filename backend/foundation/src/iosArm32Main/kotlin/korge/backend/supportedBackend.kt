package korge.backend

actual val supportedBackend: List<Backend>
    get() = listOf(Backend.Metal, Backend.OpenGl)
