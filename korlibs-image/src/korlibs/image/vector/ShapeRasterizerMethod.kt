package korlibs.image.vector

enum class ShapeRasterizerMethod(val scale: Double) {
    NONE(1.0), X1(1.0), X2(2.0), X4(4.0);
    val isNone get() = NONE
}
