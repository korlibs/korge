import korge.graphics.backend.metal.*

fun main() {
    MetalApplication("test") { device ->
        Renderer03(device)
    }.run()
}
