package com.soywiz.korge3d

import com.soywiz.korge3d.internal.vector3DTemps
import com.soywiz.korma.geom.Matrix3D
import com.soywiz.korma.geom.Vector3D
import com.soywiz.korma.geom.scale

@Korge3DExperimental
fun Container3D.shape3D(width: Double=1.0, height: Double=1.0, depth: Double=1.0, drawCommands: MeshBuilder3D.() -> Unit): Shape3D {
   return  Shape3D(width, height, depth, drawCommands).addTo(this)
}

/*
 * Note: To draw solid quads, you can use [Bitmaps.white] + [AgBitmapTextureManager] as texture and the [colorMul] as quad color.
 */
@Korge3DExperimental
class Shape3D(
    initWidth: Double, initHeight: Double, initDepth: Double,
    drawCommands: MeshBuilder3D.() -> Unit
) : ViewWithMesh3D(createMesh(drawCommands).copy()) {

    var width: Double = initWidth
    var height: Double = initHeight
    var depth: Double = initDepth

    override fun prepareExtraModelMatrix(mat: Matrix3D) {
        mat.identity().scale(width, height, depth)
    }

    companion object {

        fun createMesh(drawCommands: MeshBuilder3D.() -> Unit) = MeshBuilder3D {
            drawCommands()
        }
    }
}


@Korge3DExperimental
@Deprecated("Kotlin/Native boxes inline+Number")
inline fun Container3D.cube(width: Number, height: Number, depth: Number, callback: Cube3D.() -> Unit = {}): Cube3D = cube(width.toDouble(), height.toDouble(), depth.toDouble(), callback)

@Korge3DExperimental
inline fun Container3D.cube(
    width: Double = 1.0,
    height: Double = width,
    depth: Double = height,
    callback: Cube3D.() -> Unit = {}
): Cube3D = Cube3D(width, height, depth).addTo(this, callback)

@Korge3DExperimental
class Cube3D(var width: Double, var height: Double, var depth: Double) : ViewWithMesh3D(mesh.copy()) {
    override fun prepareExtraModelMatrix(mat: Matrix3D) {
        mat.identity().scale(width, height, depth)
    }

    var material: Material3D?
        get() = mesh.material
        set(value) {
            mesh.material = value
        }

    fun material(material: Material3D?): Cube3D {
        this.material = material
        return this
    }

    companion object {
        val mesh = MeshBuilder3D {
            vector3DTemps {
                fun face(pos: Vector3D) {
                    val dims = (0 until 3).filter { pos[it] == 0f }
                    val normal = Vector3D().setToFunc { if (pos[it] != 0f) 1f else 0f }
                    val dirs = Array(2) { dim -> Vector3D().setToFunc { if (it == dims[dim]) .5f else 0f } }
                    val dx = dirs[0]
                    val dy = dirs[1]

                    vertex(pos - dx - dy, normal, Vector3D(0f, 0f, 0f))
                    vertex(pos + dx - dy, normal, Vector3D(1f, 0f, 0f))
                    vertex(pos - dx + dy, normal, Vector3D(0f, 1f, 0f))

                    vertex(pos - dx + dy, normal, Vector3D(0f, 1f, 0f))
                    vertex(pos + dx - dy, normal, Vector3D(1f, 0f, 0f))
                    vertex(pos + dx + dy, normal, Vector3D(1f, 1f, 0f))
                }

                face(Vector3D(0f, +.5f, 0f))
                face(Vector3D(0f, -.5f, 0f))

                face(Vector3D(+.5f, 0f, 0f))
                face(Vector3D(-.5f, 0f, 0f))

                face(Vector3D(0f, 0f, +.5f))
                face(Vector3D(0f, 0f, -.5f))
            }
        }
    }
}
