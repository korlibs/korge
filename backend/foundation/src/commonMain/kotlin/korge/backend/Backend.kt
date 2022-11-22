package korge.backend

enum class Backend {
    Metal,
    OpenGl
}

expect val supportedBackend: List<Backend>
