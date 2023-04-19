import korge.graphics.backend.metal.*
import kotlinx.cinterop.*
import platform.Foundation.*

val environmentVariables: Map<String, String> by lazy {
    autoreleasepool { NSProcessInfo.processInfo.environment.map { it.key.toString() to it.value.toString() }.toMap() }
}

enum class RendererType {
    Renderer01Simple,
    Renderer01Complex,
    Renderer03;

    companion object {
        fun fromEnvironmentVariable(): RendererType {
            val renderer = environmentVariables["RENDERER"]
            return when (renderer) {
                "Renderer01Simple" -> Renderer01Simple
                "Renderer01Complex" -> Renderer01Complex
                "Renderer03" -> Renderer03
                else -> error("Unknown renderer type $renderer")
            }
        }
    }
}

fun main() {


    MetalApplication("test") { view ->
        when (RendererType.fromEnvironmentVariable()) {
            RendererType.Renderer01Simple -> Renderer01(view, simple = true)
            RendererType.Renderer01Complex -> Renderer01(view, simple = false)
            RendererType.Renderer03 -> Renderer03(view)
        }
    }.run()
}
