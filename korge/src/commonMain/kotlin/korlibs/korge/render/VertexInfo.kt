package korlibs.korge.render

import korlibs.memory.*
import korlibs.image.color.Colors
import korlibs.image.color.RGBA
import korlibs.io.util.niceStr
import korlibs.math.geom.*

data class VertexInfo(
    var x: Float = 0f,
    var y: Float = 0f,
    var u: Float = 0f,
    var v: Float = 0f,
    var colorMul: RGBA = Colors.WHITE,
) {
    var texWidth: Int = -1
    var texHeight: Int = -1
    val xy get() = Point(x, y)
    val uv get() = Point(u, v)
    fun read(buffer: Buffer, n: Int) {
        val index = n * 6
        this.x = buffer.getFloat32(index + 0)
        this.y = buffer.getFloat32(index + 1)
        this.u = buffer.getFloat32(index + 2)
        this.v = buffer.getFloat32(index + 3)
        this.colorMul = RGBA(buffer.getInt32(index + 4))
    }

    fun toStringXY() = "[${x.niceStr},${y.niceStr}]"
    fun toStringXYUV() = "[(${x.niceStr},${y.niceStr}), (${(u * texWidth).toIntRound()}, ${(v * texHeight).toIntRound()})]"
}
