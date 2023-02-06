import korge.graphics.backend.metal.*

fun main() {
    MetalApplication("test") { device ->
        Renderer02(device)
    }.run()
}
