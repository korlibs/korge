package korlibs.graphics.shader.gl

enum class GLVariant {
    //WEBGL,
    ES, DESKTOP;

    //val isES: Boolean get() = this == WEBGL || this == ES
    val isES: Boolean get() = this == ES
}
