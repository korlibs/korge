package com.soywiz.korge3d

import com.soywiz.korge3d.internal.vector3DTemps
import com.soywiz.korma.geom.Matrix3D
import com.soywiz.korma.geom.Vector3D
import com.soywiz.korma.geom.scale
import kotlin.math.*

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
inline fun Container3D.cube(width: Int, height: Int, depth: Int, callback: Cube3D.() -> Unit = {}): Cube3D = cube(width.toDouble(), height.toDouble(), depth.toDouble(), callback)

@Korge3DExperimental
inline fun Container3D.cube(
    width: Double = 1.0,
    height: Double = width,
    depth: Double = height,
    callback: Cube3D.() -> Unit = {}
): Cube3D = Cube3D(width, height, depth).addTo(this, callback)

@Korge3DExperimental
abstract class BaseViewWithMesh3D(mesh: Mesh3D) : ViewWithMesh3D(mesh.copy()) {
    var material: Material3D?
        get() = mesh.material
        set(value) {
            mesh.material = value
        }
}

fun <T : BaseViewWithMesh3D> T.material(material: Material3D?): T {
    this.material = material
    return this
}

@Korge3DExperimental
class Cube3D(var width: Double, var height: Double, var depth: Double) : BaseViewWithMesh3D(mesh) {
    override fun prepareExtraModelMatrix(mat: Matrix3D) {
        mat.identity().scale(width, height, depth)
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

                    val i0 = addVertex(pos - dx - dy, normal, Vector3D(0f, 0f, 0f))
                    val i1 = addVertex(pos + dx - dy, normal, Vector3D(1f, 0f, 0f))
                    val i2 = addVertex(pos - dx + dy, normal, Vector3D(0f, 1f, 0f))

                    val i3 = addVertex(pos - dx + dy, normal, Vector3D(0f, 1f, 0f))
                    val i4 = addVertex(pos + dx - dy, normal, Vector3D(1f, 0f, 0f))
                    val i5 = addVertex(pos + dx + dy, normal, Vector3D(1f, 1f, 0f))

                    addIndices(i0, i1, i2, i3, i4, i5)
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

@Korge3DExperimental
inline fun Container3D.sphere(radius: Int, callback: Sphere3D.() -> Unit = {}): Sphere3D = sphere(radius.toDouble(), callback)

@Korge3DExperimental
inline fun Container3D.sphere(radius: Float, callback: Sphere3D.() -> Unit = {}): Sphere3D = sphere(radius.toDouble(), callback)

@Korge3DExperimental
inline fun Container3D.sphere(
    radius: Double = 1.0,
    callback: Sphere3D.() -> Unit = {}
): Sphere3D = Sphere3D(radius).addTo(this, callback)

@Korge3DExperimental
class Sphere3D(var radius: Double) : BaseViewWithMesh3D(mesh) {
    override fun prepareExtraModelMatrix(mat: Matrix3D) {
        mat.identity().scale(radius, radius, radius)
    }

    companion object {
        private const val PIf = PI.toFloat()

        val mesh = MeshBuilder3D {
            val N = 16
            val M = 16

            val p = Vector3D()
            val nv = Vector3D()

            for (m in 0..M) {
                for (n in 0..N) {
                    p.z = (sin(PIf * m/M) * cos(2 * PIf * n/N)) / 2f
                    p.x = (sin(PIf * m/M) * sin(2 * PIf * n/N)) / 2f
                    p.y = (cos(PIf * m/M)) / 2f

                    nv.copyFrom(p)
                    nv.normalize()
                    addVertex(p, nv, Vector3D(0f, 0f, 0f))
                }
            }

            for (m in 1 .. M) {
                val row0 = (m - 1) * N
                val row1 = (m + 0) * N
                for (n in 0..N) {
                    val r0 = row0 + n
                    val r1 = row1 + n
                    addIndices(r0 + 0, r0 + 1, r1 + 0)
                    addIndices(r0 + 1, r1 + 1, r1 + 0)
                }
            }
        }
    }
}
