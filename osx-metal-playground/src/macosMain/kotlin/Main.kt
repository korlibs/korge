import korge.graphics.backend.metal.*

fun main() {
    MetalApplication("test") { view ->
        Renderer01(view, simple = false)
    }.run()
}
