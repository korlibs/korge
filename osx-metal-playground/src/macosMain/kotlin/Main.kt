import korge.graphics.backend.metal.*

fun main() {
    MetalApplication("test") { device ->
        Renderer01(device)
    }.run()
}