package korge.graphics

enum class GraphicalBackend {
    Metal,
    OpenGl
}

expect val supportedGraphicalBackend: List<GraphicalBackend>
