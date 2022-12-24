package korge.graphics

actual val supportedGraphicalBackend: List<GraphicalBackend>
    get() = listOf(GraphicalBackend.Metal, GraphicalBackend.OpenGl)
