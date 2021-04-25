package com.soywiz.korge3d

import com.soywiz.kds.*
import com.soywiz.korag.AG
import com.soywiz.korag.shader.VertexLayout
import com.soywiz.korge3d.internal.toFBuffer
import com.soywiz.korge3d.internal.vector3DTemps
import com.soywiz.korim.color.Colors
import com.soywiz.korim.color.RGBA
import com.soywiz.korio.util.buildList
import com.soywiz.korma.geom.Vector3D
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Korge3DExperimental
class MeshBuilder3D(
    val drawType: AG.DrawType = AG.DrawType.TRIANGLES
) {
    val layout = VertexLayout(
        Shaders3D.a_pos,
        Shaders3D.a_norm,
        Shaders3D.a_tex
    )

    operator fun invoke(callback: MeshBuilder3D.() -> Unit): Mesh3D = this.apply(callback).build()

    companion object {
        operator fun invoke(callback: MeshBuilder3D.() -> Unit): Mesh3D = MeshBuilder3D().apply(callback).build()
    }

    val vertexData = floatArrayListOf()
    var nextVertexIndex = 0
    val indexData = ShortArrayList()

    private var _material: Material3D? = null

    fun reset() {
        vertexData.clear()
        indexData.clear()
        nextVertexIndex = 0
    }

    fun material(
        emission: Material3D.Light = Material3D.LightColor(Colors.BLACK),
        //default with some ambient so that we see something if material not set
        ambient: Material3D.Light = Material3D.LightColor(RGBA(0x2, 0x2, 0x2, 0xFF)),
        diffuse: Material3D.Light = Material3D.LightColor(Colors.BLACK),
        specular: Material3D.Light = Material3D.LightColor(Colors.BLACK),
        shininess: Float = .5f,
        indexOfRefraction: Float = 1f
    ) {
        _material = Material3D(emission, ambient, diffuse, specular, shininess, indexOfRefraction)
    }

    fun addVertex(pos: Vector3D, normal: Vector3D = Vector3D(), texcoords: Vector3D = Vector3D()): Int {
        return addVertex(pos.x, pos.y, pos.z, normal.x, normal.y, normal.z, texcoords.x, texcoords.y)
    }

    fun addVertex(
        px: Float,
        py: Float,
        pz: Float,
        nx: Float = 0f,
        ny: Float = 0f,
        nz: Float = 1f,
        u: Float = 0f,
        v: Float = 0f
    ): Int {
        vertexData.add(px)
        vertexData.add(py)
        vertexData.add(pz)
        vertexData.add(nx)
        vertexData.add(ny)
        vertexData.add(nz)
        vertexData.add(u)
        vertexData.add(v)
        return nextVertexIndex++
    }

    fun addIndex(index: Int) {
        indexData.add(index.toShort())
    }

    fun addIndices(vararg indices: Int) = indices.forEach { addIndex(it) }

    fun faceTriangle(v1: Vector3D, v2: Vector3D, v3: Vector3D) {
        vector3DTemps {
            val u = v2 - v1
            val v = v3 - v1
            val nx = (u.y * v.z) - (u.z * v.y)
            val ny = (u.z * v.x) - (u.x * v.z)
            val nz = (u.x * v.y) - (u.y * v.x)

            val i1 = addVertex(v1.x, v1.y, v1.z, nx, ny, nz)
            val i2 = addVertex(v2.x, v2.y, v2.z, nx, ny, nz)
            val i3 = addVertex(v3.x, v3.y, v3.z, nx, ny, nz)
            addIndices(i1, i2, i3)
        }
    }

    fun faceTriangle(
        v1: Vector3D, v2: Vector3D, v3: Vector3D,
        nx: Float, ny: Float, nz: Float
    ) {
        val i1 = addVertex(v1.x, v1.y, v1.z, nx, ny, nz)
        val i2 = addVertex(v2.x, v2.y, v2.z, nx, ny, nz)
        val i3 = addVertex(v3.x, v3.y, v3.z, nx, ny, nz)
        addIndices(i1, i2, i3)
    }

    fun faceRectangle(v1: Vector3D, v2: Vector3D, v3: Vector3D, v4: Vector3D) {
        vector3DTemps {
            val u = v2 - v1
            val v = v3 - v1
            val nx = (u.y * v.z) - (u.z * v.y)
            val ny = (u.z * v.x) - (u.x * v.z)
            val nz = (u.x * v.y) - (u.y * v.x)

            val i1 = addVertex(v1.x, v1.y, v1.z, nx, ny, nz )
            val i2 = addVertex(v2.x, v2.y, v2.z, nx, ny, nz)
            val i3 = addVertex(v3.x, v3.y, v3.z, nx, ny, nz)
            val i4 = addVertex(v4.x, v4.y, v4.z, nx, ny, nz)
            addIndices(i1, i2, i3)
            addIndices(i3, i4, i1)
        }
    }

    fun faceRectangle(
        v1: Vector3D, v2: Vector3D, v3: Vector3D, v4: Vector3D,
        t1: Vector3D, t2: Vector3D, t3: Vector3D, t4: Vector3D
    ) {
        vector3DTemps {
            val u = v2 - v1
            val v = v3 - v1
            val nx = (u.y * v.z) - (u.z * v.y)
            val ny = (u.z * v.x) - (u.x * v.z)
            val nz = (u.x * v.y) - (u.y * v.x)

            val i1 = addVertex(v1.x, v1.y, v1.z, nx, ny, nz, t1.x, t1.y)
            val i2 = addVertex(v2.x, v2.y, v2.z, nx, ny, nz, t2.x, t2.y)
            val i3 = addVertex(v3.x, v3.y, v3.z, nx, ny, nz, t3.x, t3.y)
            val i4 = addVertex(v4.x, v4.y, v4.z, nx, ny, nz, t4.x, t4.y)
            addIndices(i1, i2, i3)
            addIndices(i3, i4, i1)
        }
    }

    fun pyramidTriangleBase(v1: Vector3D, v2: Vector3D, v3: Vector3D, v4: Vector3D) {
        // cannot reuse vertices because the normals need to be different!
        faceTriangle(v1, v2, v3)
        faceTriangle(v1, v2, v4)
        faceTriangle(v1, v3, v4)
        faceTriangle(v2, v4, v3)
    }

    fun pyramidRectangleBase() {
        TODO()
    }

    fun prismTriangle() {
        TODO()
    }

    fun cuboid(width: Float, height: Float, depth: Float) {
        val hx = width / 2f
        val hy = height / 2f
        val hz = depth / 2f

        // front face, clockwise
        val v1 = Vector3D(-hx, +hy, -hz)
        val v2 = Vector3D(+hx, +hy, -hz)
        val v3 = Vector3D(+hx, -hy, -hz)
        val v4 = Vector3D(-hx, -hy, -hz)

        // back face, clockwise
        val v5 = Vector3D(-hx, +hy, +hz)
        val v6 = Vector3D(+hx, +hy, +hz)
        val v7 = Vector3D(+hx, -hy, +hz)
        val v8 = Vector3D(-hx, -hy, +hz)

        // cannot reuse vertices because the normals need to be different!
        faceRectangle(v1, v2, v3, v4) //front
        faceRectangle(v2, v6, v7, v3) // right
        faceRectangle(v5, v6, v7, v8) // back
        faceRectangle(v1, v4, v8, v5) // left
        faceRectangle(v1, v5, v6, v2) // top
        faceRectangle(v3, v7, v8, v4) // bottom
    }

    fun cube(size: Float = 1f) = cuboid(size, size, size)

    fun sphere(radius: Float = 1f, longitudeLines: Int = 10, latitudeLines: Int = 10) = ellipsoid(radius, radius, radius, longitudeLines, latitudeLines)

    fun ellipsoid(rx: Float = 1f, ry: Float = 1f, rz: Float = 1f, longitudeLines: Int = 10, latitudeLines: Int = 10) = parametric(longitudeLines, latitudeLines) { u, v ->
        val x = cos(u) * sin(v) * rx
        val y = cos(v) * ry
        val z = sin(u) * sin(v) * rz
        Vector3D(x, y, z)
    }

    fun parametric(longitudeLines: Int = 10, latitudeLines: Int = 10, F: (u: Float, v: Float) -> Vector3D) {
        // modified from [https://stackoverflow.com/questions/7687148/drawing-sphere-in-opengl-without-using-glusphere]
        val startU = 0
        val startV = 0
        val endU = PI * 2
        val endV = PI
        val stepU = (endU - startU) / longitudeLines // step size between U-points on the grid
        val stepV = (endV - startV) / latitudeLines // step size between V-points on the grid
        //TODO: addVertices then indices to reduce number of vertices uploaded
        for (i in 0 until longitudeLines) { // U-points
            for (j in 0 until latitudeLines) { // V-points
                val u = (i * stepU + startU).toFloat()
                val v = (j * stepV + startV).toFloat()
                val un = (if (i + 1 == longitudeLines) endU else (i + 1) * stepU + startU).toFloat()
                val vn = (if (j + 1 == latitudeLines) endV else (j + 1) * stepV + startV).toFloat()
                // Find the four points of the grid
                // square by evaluating the parametric
                // surface function
                val v0 = F(u, v)
                val v1 = F(u, vn)
                val v2 = F(un, v)
                val v3 = F(un, vn)
                // NOTE: For spheres, the normal is just the normalized
                // version of each vertex point; this generally won't be the case for
                // other parametric surfaces.
                // Output the first triangle of this grid square
                //faceTriangle(v0, v2, v1)
                // Output the other triangle of this grid square
                //faceTriangle(v3, v1, v2)
                faceRectangle(v0,v1,v2,v3)
            }
        }
    }

    fun build(): Mesh3D = Mesh3D(
        listOf(BufferWithVertexLayout(
            buffer = vertexData.toFBuffer(),
            layout = layout
        )),
        indexData.toFBuffer(),
        AG.IndexType.USHORT,
        indexData.size,
        null,
        drawType,
        true,
        maxWeights = 0,
        skin = null,
        material = _material
    )
}
