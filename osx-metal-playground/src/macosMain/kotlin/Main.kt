import com.soywiz.metal.*

fun main() {
    MetalApplication("test") { device ->
        Renderer01(device)
    }.run()
}
