/******************************************************************************
 * Spine Runtimes License Agreement
 * Last updated January 1, 2020. Replaces all prior versions.
 *
 * Copyright (c) 2013-2020, Esoteric Software LLC
 *
 * Integration of the Spine Runtimes into software or otherwise creating
 * derivative works of the Spine Runtimes is permitted under the terms and
 * conditions of Section 2 of the Spine Editor License Agreement:
 * http://esotericsoftware.com/spine-editor-license
 *
 * Otherwise, it is permitted to integrate the Spine Runtimes into software
 * or otherwise create derivative works of the Spine Runtimes (collectively,
 * "Products"), provided that each user of the Products must obtain their own
 * Spine Editor license and redistribution of the Products in any form must
 * include this license and copyright notice.
 *
 * THE SPINE RUNTIMES ARE PROVIDED BY ESOTERIC SOFTWARE LLC "AS IS" AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL ESOTERIC SOFTWARE LLC BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES,
 * BUSINESS INTERRUPTION, OR LOSS OF USE, DATA, OR PROFITS) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THE SPINE RUNTIMES, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

/*
package com.esotericsoftware.spine.rendering

import com.esotericsoftware.spine.graphics.*
import Mesh.VertexDataType
import VertexAttributes.Usage
import com.esotericsoftware.spine.utils.Affine2
import com.esotericsoftware.spine.utils.MathUtils
import com.esotericsoftware.spine.utils.Matrix4
import com.esotericsoftware.spine.utils.*
import com.esotericsoftware.spine.utils.SpineUtils.arraycopy

/** A batch that renders polygons and performs tinting using a light and dark color.
 *
 *
 * Because an additional vertex attribute is used, the [Batch] and [PolygonBatch] methods that accept float[] vertex
 * data do not perform two color tinting. [.drawTwoColor] and
 * [.drawTwoColor] are provided to accept float[] vertex data that contains
 * two colors per vertex.  */
class TwoColorPolygonBatch(maxVertices: Int, maxTriangles: Int) : PolygonBatch {

    private val mesh: Mesh
    private val vertices: FloatArray
    private val triangles: ShortArray

    /** Flushes the batch.  */
    override var transformMatrix: Matrix4? = Matrix4()
        set(transform) {
            if (isDrawing) flush()
            this.transformMatrix!!.set(transform!!)
            if (isDrawing) setupMatrices()
        }

    /** Flushes the batch.  */
    override var projectionMatrix: Matrix4? = Matrix4()
        set(projection) {
            if (isDrawing) flush()
            this.projectionMatrix!!.set(projection!!)
            if (isDrawing) setupMatrices()
        }
    private val combinedMatrix = Matrix4()
    private var blendingDisabled: Boolean = false
    private val defaultShader: ShaderProgram
    private var _shader: ShaderProgram? = null
    private var vertexIndex: Int = 0
    private var triangleIndex: Int = 0
    private var lastTexture: Texture? = null
    private var invTexWidth = 0f
    private var invTexHeight = 0f
    override var isDrawing: Boolean = false
    override var blendSrcFunc = GL20.GL_SRC_ALPHA
    override var blendDstFunc = GL20.GL_ONE_MINUS_SRC_ALPHA
    override var blendSrcFuncAlpha = GL20.GL_SRC_ALPHA
    override var blendDstFuncAlpha = GL20.GL_ONE_MINUS_SRC_ALPHA
    private var premultipliedAlpha: Boolean = false

    override var color: Color? = Color(1f, 1f, 1f, 1f)
        set(tint) {
            color!!.set(tint!!)
            lightPacked = tint.toFloatBits()
        }
    var darkColor = Color(0f, 0f, 0f, 1f)
        set(tint) {
            darkColor.set(tint)
            darkPacked = tint.toFloatBits()
        }
    private var lightPacked = Color.WHITE.toFloatBits()
    private var darkPacked = Color.BLACK.toFloatBits()

    /** Number of rendering calls, ever. Will not be reset unless set manually.  */
    var totalRenderCalls = 0

    override var packedColor: Float
        get() = lightPacked
        set(packedColor) {
            Color.rgba8888ToColor(color!!, NumberUtils.floatToIntColor(packedColor))
            lightPacked = packedColor
        }

    var packedDarkColor: Float
        get() = darkPacked
        set(packedColor) {
            Color.rgba8888ToColor(darkColor, NumberUtils.floatToIntColor(packedColor))
            this.darkPacked = packedColor
        }

    override val isBlendingEnabled: Boolean
        get() = !blendingDisabled


    constructor(size: Int = 2000) : this(size, size * 2) {
    }

    override var shader: ShaderProgram?
        get() = _shader
        /** Flushes the batch if the shader was changed.  */
        set(newShader) {
            if (_shader == newShader) return
            if (isDrawing) {
                flush()
                _shader!!.end()
            }
            _shader = newShader ?: defaultShader
            if (isDrawing) {
                _shader!!.begin()
                setupMatrices()
            }
        }


    init {
        // 32767 is max vertex index.
        require(maxVertices <= 32767) { "Can't have more than 32767 vertices per batch: $maxTriangles" }

        var vertexDataType: Mesh.VertexDataType = Mesh.VertexDataType.VertexArray
        if (Gdx.gl30 != null) vertexDataType = VertexDataType.VertexBufferObjectWithVAO
        mesh = Mesh(vertexDataType, false, maxVertices, maxTriangles * 3, //
                VertexAttribute(Usage.Position, 2, "a_position"), //
                VertexAttribute(Usage.ColorPacked, 4, "a_light"), //
                VertexAttribute(Usage.ColorPacked, 4, "a_dark"), // Dark alpha is unused, but colors are packed as 4 byte floats.
                VertexAttribute(Usage.TextureCoordinates, 2, "a_texCoord0"))

        vertices = FloatArray(maxVertices * 6)
        triangles = ShortArray(maxTriangles * 3)
        defaultShader = createDefaultShader()
        _shader = defaultShader
        this.projectionMatrix!!.setToOrtho2D(0f, 0f, Gdx.graphics!!.width, Gdx.graphics!!.height)
    }

    override fun begin() {
        check(!isDrawing) { "end must be called before begin." }
        Gdx.gl!!.glDepthMask(false)
        shader!!.begin()
        setupMatrices()
        isDrawing = true
    }

    override fun end() {
        check(isDrawing) { "begin must be called before end." }
        if (vertexIndex > 0) flush()
        shader!!.end()
        Gdx.gl!!.glDepthMask(true)
        if (isBlendingEnabled) Gdx.gl!!.glDisable(GL20.GL_BLEND)

        lastTexture = null
        isDrawing = false
    }

    override fun setColor(r: Float, g: Float, b: Float, a: Float) {
        color!![r, g, b] = a
        lightPacked = color!!.toFloatBits()
    }

    fun setDarkColor(r: Float, g: Float, b: Float, a: Float) {
        darkColor[r, g, b] = a
        darkPacked = darkColor.toFloatBits()
    }

    /** Draws a polygon region with the bottom left corner at x,y having the width and height of the region.  */
    fun draw(region: PolygonRegion, x: Float, y: Float) {
        check(isDrawing) { "begin must be called before draw." }

        val triangles = this.triangles
        val regionTriangles = region.triangles
        val regionTrianglesLength = regionTriangles.size
        val regionVertices = region.vertices
        val regionVerticesLength = regionVertices.size

        val texture = region.region!!.texture
        if (texture != lastTexture)
            switchTexture(texture)
        else if (triangleIndex + regionTrianglesLength > triangles.size || vertexIndex + regionVerticesLength * VERTEX_SIZE / 2 > vertices.size)
            flush()

        var triangleIndex = this.triangleIndex
        var vertexIndex = this.vertexIndex
        val startVertex = vertexIndex / VERTEX_SIZE

        for (i in 0 until regionTrianglesLength)
            triangles[triangleIndex++] = (regionTriangles[i] + startVertex).toShort()
        this.triangleIndex = triangleIndex

        val vertices = this.vertices
        val light = this.lightPacked
        val dark = this.darkPacked
        val textureCoords = region.textureCoords

        var i = 0
        while (i < regionVerticesLength) {
            vertices[vertexIndex++] = regionVertices[i] + x
            vertices[vertexIndex++] = regionVertices[i + 1] + y
            vertices[vertexIndex++] = light
            vertices[vertexIndex++] = dark
            vertices[vertexIndex++] = textureCoords[i]
            vertices[vertexIndex++] = textureCoords[i + 1]
            i += 2
        }
        this.vertexIndex = vertexIndex
    }

    /** Draws a polygon region with the bottom left corner at x,y and stretching the region to cover the given width and height.  */
    fun draw(region: PolygonRegion, x: Float, y: Float, width: Float, height: Float) {
        check(isDrawing) { "begin must be called before draw." }

        val triangles = this.triangles
        val regionTriangles = region.triangles
        val regionTrianglesLength = regionTriangles.size
        val regionVertices = region.vertices
        val regionVerticesLength = regionVertices.size
        val textureRegion = region.region

        val texture = textureRegion!!.texture
        if (texture != lastTexture)
            switchTexture(texture)
        else if (triangleIndex + regionTrianglesLength > triangles.size || vertexIndex + regionVerticesLength * VERTEX_SIZE / 2 > vertices.size)
            flush()

        var triangleIndex = this.triangleIndex
        var vertexIndex = this.vertexIndex
        val startVertex = vertexIndex / VERTEX_SIZE

        run {
            var i = 0
            val n = regionTriangles.size
            while (i < n) {
                triangles[triangleIndex++] = (regionTriangles[i] + startVertex).toShort()
                i++
            }
        }
        this.triangleIndex = triangleIndex

        val vertices = this.vertices
        val light = this.lightPacked
        val dark = this.darkPacked
        val textureCoords = region.textureCoords
        val sX = width / textureRegion.regionWidth
        val sY = height / textureRegion.regionHeight

        var i = 0
        while (i < regionVerticesLength) {
            vertices[vertexIndex++] = regionVertices[i] * sX + x
            vertices[vertexIndex++] = regionVertices[i + 1] * sY + y
            vertices[vertexIndex++] = light
            vertices[vertexIndex++] = dark
            vertices[vertexIndex++] = textureCoords[i]
            vertices[vertexIndex++] = textureCoords[i + 1]
            i += 2
        }
        this.vertexIndex = vertexIndex
    }

    /** Draws the polygon region with the bottom left corner at x,y and stretching the region to cover the given width and height.
     * The polygon region is offset by originX, originY relative to the origin. Scale specifies the scaling factor by which the
     * polygon region should be scaled around originX, originY. Rotation specifies the angle of counter clockwise rotation of the
     * rectangle around originX, originY.  */
    fun draw(region: PolygonRegion, x: Float, y: Float, originX: Float, originY: Float, width: Float, height: Float,
             scaleX: Float, scaleY: Float, rotation: Float) {
        check(isDrawing) { "begin must be called before draw." }

        val triangles = this.triangles
        val regionTriangles = region.triangles
        val regionTrianglesLength = regionTriangles.size
        val regionVertices = region.vertices
        val regionVerticesLength = regionVertices.size
        val textureRegion = region.region

        val texture = textureRegion!!.texture
        if (texture != lastTexture)
            switchTexture(texture)
        else if (triangleIndex + regionTrianglesLength > triangles.size || vertexIndex + regionVerticesLength * VERTEX_SIZE / 2 > vertices.size)
            flush()

        var triangleIndex = this.triangleIndex
        var vertexIndex = this.vertexIndex
        val startVertex = vertexIndex / VERTEX_SIZE

        for (i in 0 until regionTrianglesLength)
            triangles[triangleIndex++] = (regionTriangles[i] + startVertex).toShort()
        this.triangleIndex = triangleIndex

        val vertices = this.vertices
        val light = this.lightPacked
        val dark = this.darkPacked
        val textureCoords = region.textureCoords

        val worldOriginX = x + originX
        val worldOriginY = y + originY
        val sX = width / textureRegion.regionWidth
        val sY = height / textureRegion.regionHeight
        val cos = MathUtils.cosDeg(rotation)
        val sin = MathUtils.sinDeg(rotation)

        var fx: Float
        var fy: Float
        var i = 0
        while (i < regionVerticesLength) {
            fx = (regionVertices[i] * sX - originX) * scaleX
            fy = (regionVertices[i + 1] * sY - originY) * scaleY
            vertices[vertexIndex++] = cos * fx - sin * fy + worldOriginX
            vertices[vertexIndex++] = sin * fx + cos * fy + worldOriginY
            vertices[vertexIndex++] = light
            vertices[vertexIndex++] = dark
            vertices[vertexIndex++] = textureCoords[i]
            vertices[vertexIndex++] = textureCoords[i + 1]
            i += 2
        }
        this.vertexIndex = vertexIndex
    }

    override fun draw(texture: Texture?, x: Float, y: Float, originX: Float, originY: Float, width: Float, height: Float, scaleX: Float,
                      scaleY: Float, rotation: Float, srcX: Int, srcY: Int, srcWidth: Int, srcHeight: Int, flipX: Boolean, flipY: Boolean) {
        check(isDrawing) { "begin must be called before draw." }

        val triangles = this.triangles
        val vertices = this.vertices

        if (texture != lastTexture)
            switchTexture(texture!!)
        else if (triangleIndex + 6 > triangles.size || vertexIndex + SPRITE_SIZE > vertices.size)
        //
            flush()

        var triangleIndex = this.triangleIndex
        val startVertex = vertexIndex / VERTEX_SIZE
        triangles[triangleIndex++] = startVertex.toShort()
        triangles[triangleIndex++] = (startVertex + 1).toShort()
        triangles[triangleIndex++] = (startVertex + 2).toShort()
        triangles[triangleIndex++] = (startVertex + 2).toShort()
        triangles[triangleIndex++] = (startVertex + 3).toShort()
        triangles[triangleIndex++] = startVertex.toShort()
        this.triangleIndex = triangleIndex

        // bottom left and top right corner points relative to origin
        val worldOriginX = x + originX
        val worldOriginY = y + originY
        var fx = -originX
        var fy = -originY
        var fx2 = width - originX
        var fy2 = height - originY

        // scale
        if (scaleX != 1f || scaleY != 1f) {
            fx *= scaleX
            fy *= scaleY
            fx2 *= scaleX
            fy2 *= scaleY
        }

        // construct corner points, start from top left and go counter clockwise
        val p1x = fx
        val p1y = fy
        val p2x = fx
        val p2y = fy2
        val p3x = fx2
        val p3y = fy2
        val p4x = fx2
        val p4y = fy

        var x1: Float
        var y1: Float
        var x2: Float
        var y2: Float
        var x3: Float
        var y3: Float
        var x4: Float
        var y4: Float

        // rotate
        if (rotation != 0f) {
            val cos = MathUtils.cosDeg(rotation)
            val sin = MathUtils.sinDeg(rotation)

            x1 = cos * p1x - sin * p1y
            y1 = sin * p1x + cos * p1y

            x2 = cos * p2x - sin * p2y
            y2 = sin * p2x + cos * p2y

            x3 = cos * p3x - sin * p3y
            y3 = sin * p3x + cos * p3y

            x4 = x1 + (x3 - x2)
            y4 = y3 - (y2 - y1)
        } else {
            x1 = p1x
            y1 = p1y

            x2 = p2x
            y2 = p2y

            x3 = p3x
            y3 = p3y

            x4 = p4x
            y4 = p4y
        }

        x1 += worldOriginX
        y1 += worldOriginY
        x2 += worldOriginX
        y2 += worldOriginY
        x3 += worldOriginX
        y3 += worldOriginY
        x4 += worldOriginX
        y4 += worldOriginY

        var u = srcX * invTexWidth
        var v = (srcY + srcHeight) * invTexHeight
        var u2 = (srcX + srcWidth) * invTexWidth
        var v2 = srcY * invTexHeight

        if (flipX) {
            val tmp = u
            u = u2
            u2 = tmp
        }

        if (flipY) {
            val tmp = v
            v = v2
            v2 = tmp
        }

        val light = this.lightPacked
        val dark = this.darkPacked
        var idx = this.vertexIndex
        vertices[idx++] = x1
        vertices[idx++] = y1
        vertices[idx++] = light
        vertices[idx++] = dark
        vertices[idx++] = u
        vertices[idx++] = v

        vertices[idx++] = x2
        vertices[idx++] = y2
        vertices[idx++] = light
        vertices[idx++] = dark
        vertices[idx++] = u
        vertices[idx++] = v2

        vertices[idx++] = x3
        vertices[idx++] = y3
        vertices[idx++] = light
        vertices[idx++] = dark
        vertices[idx++] = u2
        vertices[idx++] = v2

        vertices[idx++] = x4
        vertices[idx++] = y4
        vertices[idx++] = light
        vertices[idx++] = dark
        vertices[idx++] = u2
        vertices[idx++] = v
        this.vertexIndex = idx
    }

    override fun draw(texture: Texture?, x: Float, y: Float, width: Float, height: Float, srcX: Int, srcY: Int, srcWidth: Int,
                      srcHeight: Int, flipX: Boolean, flipY: Boolean) {
        check(isDrawing) { "begin must be called before draw." }

        val triangles = this.triangles
        val vertices = this.vertices

        if (texture != lastTexture)
            switchTexture(texture!!)
        else if (triangleIndex + 6 > triangles.size || vertexIndex + SPRITE_SIZE > vertices.size)
        //
            flush()

        var triangleIndex = this.triangleIndex
        val startVertex = vertexIndex / VERTEX_SIZE
        triangles[triangleIndex++] = startVertex.toShort()
        triangles[triangleIndex++] = (startVertex + 1).toShort()
        triangles[triangleIndex++] = (startVertex + 2).toShort()
        triangles[triangleIndex++] = (startVertex + 2).toShort()
        triangles[triangleIndex++] = (startVertex + 3).toShort()
        triangles[triangleIndex++] = startVertex.toShort()
        this.triangleIndex = triangleIndex

        var u = srcX * invTexWidth
        var v = (srcY + srcHeight) * invTexHeight
        var u2 = (srcX + srcWidth) * invTexWidth
        var v2 = srcY * invTexHeight
        val fx2 = x + width
        val fy2 = y + height

        if (flipX) {
            val tmp = u
            u = u2
            u2 = tmp
        }

        if (flipY) {
            val tmp = v
            v = v2
            v2 = tmp
        }

        val light = this.lightPacked
        val dark = this.darkPacked
        var idx = this.vertexIndex
        vertices[idx++] = x
        vertices[idx++] = y
        vertices[idx++] = light
        vertices[idx++] = dark
        vertices[idx++] = u
        vertices[idx++] = v

        vertices[idx++] = x
        vertices[idx++] = fy2
        vertices[idx++] = light
        vertices[idx++] = dark
        vertices[idx++] = u
        vertices[idx++] = v2

        vertices[idx++] = fx2
        vertices[idx++] = fy2
        vertices[idx++] = light
        vertices[idx++] = dark
        vertices[idx++] = u2
        vertices[idx++] = v2

        vertices[idx++] = fx2
        vertices[idx++] = y
        vertices[idx++] = light
        vertices[idx++] = dark
        vertices[idx++] = u2
        vertices[idx++] = v
        this.vertexIndex = idx
    }

    override fun draw(texture: Texture?, x: Float, y: Float, srcX: Int, srcY: Int, srcWidth: Int, srcHeight: Int) {
        check(isDrawing) { "begin must be called before draw." }

        val triangles = this.triangles
        val vertices = this.vertices

        if (texture != lastTexture)
            switchTexture(texture!!)
        else if (triangleIndex + 6 > triangles.size || vertexIndex + SPRITE_SIZE > vertices.size)
        //
            flush()

        var triangleIndex = this.triangleIndex
        val startVertex = vertexIndex / VERTEX_SIZE
        triangles[triangleIndex++] = startVertex.toShort()
        triangles[triangleIndex++] = (startVertex + 1).toShort()
        triangles[triangleIndex++] = (startVertex + 2).toShort()
        triangles[triangleIndex++] = (startVertex + 2).toShort()
        triangles[triangleIndex++] = (startVertex + 3).toShort()
        triangles[triangleIndex++] = startVertex.toShort()
        this.triangleIndex = triangleIndex

        val u = srcX * invTexWidth
        val v = (srcY + srcHeight) * invTexHeight
        val u2 = (srcX + srcWidth) * invTexWidth
        val v2 = srcY * invTexHeight
        val fx2 = x + srcWidth
        val fy2 = y + srcHeight

        val light = this.lightPacked
        val dark = this.darkPacked
        var idx = this.vertexIndex
        vertices[idx++] = x
        vertices[idx++] = y
        vertices[idx++] = light
        vertices[idx++] = dark
        vertices[idx++] = u
        vertices[idx++] = v

        vertices[idx++] = x
        vertices[idx++] = fy2
        vertices[idx++] = light
        vertices[idx++] = dark
        vertices[idx++] = u
        vertices[idx++] = v2

        vertices[idx++] = fx2
        vertices[idx++] = fy2
        vertices[idx++] = light
        vertices[idx++] = dark
        vertices[idx++] = u2
        vertices[idx++] = v2

        vertices[idx++] = fx2
        vertices[idx++] = y
        vertices[idx++] = light
        vertices[idx++] = dark
        vertices[idx++] = u2
        vertices[idx++] = v
        this.vertexIndex = idx
    }

    override fun draw(texture: Texture?, x: Float, y: Float, width: Float, height: Float, u: Float, v: Float, u2: Float, v2: Float) {
        check(isDrawing) { "begin must be called before draw." }

        val triangles = this.triangles
        val vertices = this.vertices

        if (texture != lastTexture)
            switchTexture(texture!!)
        else if (triangleIndex + 6 > triangles.size || vertexIndex + SPRITE_SIZE > vertices.size)
        //
            flush()

        var triangleIndex = this.triangleIndex
        val startVertex = vertexIndex / VERTEX_SIZE
        triangles[triangleIndex++] = startVertex.toShort()
        triangles[triangleIndex++] = (startVertex + 1).toShort()
        triangles[triangleIndex++] = (startVertex + 2).toShort()
        triangles[triangleIndex++] = (startVertex + 2).toShort()
        triangles[triangleIndex++] = (startVertex + 3).toShort()
        triangles[triangleIndex++] = startVertex.toShort()
        this.triangleIndex = triangleIndex

        val fx2 = x + width
        val fy2 = y + height

        val light = this.lightPacked
        val dark = this.darkPacked
        var idx = this.vertexIndex
        vertices[idx++] = x
        vertices[idx++] = y
        vertices[idx++] = light
        vertices[idx++] = dark
        vertices[idx++] = u
        vertices[idx++] = v

        vertices[idx++] = x
        vertices[idx++] = fy2
        vertices[idx++] = light
        vertices[idx++] = dark
        vertices[idx++] = u
        vertices[idx++] = v2

        vertices[idx++] = fx2
        vertices[idx++] = fy2
        vertices[idx++] = light
        vertices[idx++] = dark
        vertices[idx++] = u2
        vertices[idx++] = v2

        vertices[idx++] = fx2
        vertices[idx++] = y
        vertices[idx++] = light
        vertices[idx++] = dark
        vertices[idx++] = u2
        vertices[idx++] = v
        this.vertexIndex = idx
    }

    override fun draw(texture: Texture?, x: Float, y: Float) {
        draw(texture, x, y, texture!!.width, texture.height)
    }

    override fun draw(texture: Texture?, x: Float, y: Float, width: Float, height: Float) {
        check(isDrawing) { "begin must be called before draw." }

        val triangles = this.triangles
        val vertices = this.vertices

        if (texture != lastTexture)
            switchTexture(texture!!)
        else if (triangleIndex + 6 > triangles.size || vertexIndex + SPRITE_SIZE > vertices.size)
        //
            flush()

        var triangleIndex = this.triangleIndex
        val startVertex = vertexIndex / VERTEX_SIZE
        triangles[triangleIndex++] = startVertex.toShort()
        triangles[triangleIndex++] = (startVertex + 1).toShort()
        triangles[triangleIndex++] = (startVertex + 2).toShort()
        triangles[triangleIndex++] = (startVertex + 2).toShort()
        triangles[triangleIndex++] = (startVertex + 3).toShort()
        triangles[triangleIndex++] = startVertex.toShort()
        this.triangleIndex = triangleIndex

        val fx2 = x + width
        val fy2 = y + height
        val u = 0f
        val v = 1f
        val u2 = 1f
        val v2 = 0f

        val light = this.lightPacked
        val dark = this.darkPacked
        var idx = this.vertexIndex
        vertices[idx++] = x
        vertices[idx++] = y
        vertices[idx++] = light
        vertices[idx++] = dark
        vertices[idx++] = u
        vertices[idx++] = v

        vertices[idx++] = x
        vertices[idx++] = fy2
        vertices[idx++] = light
        vertices[idx++] = dark
        vertices[idx++] = u
        vertices[idx++] = v2

        vertices[idx++] = fx2
        vertices[idx++] = fy2
        vertices[idx++] = light
        vertices[idx++] = dark
        vertices[idx++] = u2
        vertices[idx++] = v2

        vertices[idx++] = fx2
        vertices[idx++] = y
        vertices[idx++] = light
        vertices[idx++] = dark
        vertices[idx++] = u2
        vertices[idx++] = v
        this.vertexIndex = idx
    }

    /** Draws polygons using the given vertices and triangles. There must be 4 vertices, each made up of 6 elements in this order:
     * x, y, lightColor, darkColor, u, v. The [.getColor] and [.getDarkColor] from the TwoColorPolygonBatch is not
     * applied.  */
    fun drawTwoColor(texture: Texture, polygonVertices: FloatArray, verticesOffset: Int, verticesCount: Int,
                     polygonTriangles: ShortArray, trianglesOffset: Int, trianglesCount: Int) {
        check(isDrawing) { "begin must be called before draw." }

        val triangles = this.triangles
        val vertices = this.vertices

        if (texture != lastTexture) {
            switchTexture(texture)
        } else if (triangleIndex + trianglesCount > triangles.size || vertexIndex + verticesCount > vertices.size)
        //
            flush()

        var triangleIndex = this.triangleIndex
        val vertexIndex = this.vertexIndex
        val startVertex = vertexIndex / 6

        var i = trianglesOffset
        val n = i + trianglesCount
        while (i < n) {
            triangles[triangleIndex++] = (polygonTriangles[i] + startVertex).toShort()
            i++
        }
        this.triangleIndex = triangleIndex

        arraycopy(polygonVertices, verticesOffset, vertices, vertexIndex, verticesCount)
        this.vertexIndex += verticesCount
    }

    /** Draws polygons using the given vertices and triangles in the [PolygonBatch] format. There must be 4 vertices, each
     * made up of 5 elements in this order: x, y, color, u, v. The [.getColor] and [.getDarkColor] from the
     * TwoColorPolygonBatch is not applied.  */
    fun draw(texture: Texture, polygonVertices: FloatArray, verticesOffset: Int, verticesCount: Int, polygonTriangles: ShortArray,
             trianglesOffset: Int, trianglesCount: Int) {
        check(isDrawing) { "begin must be called before draw." }

        val triangles = this.triangles
        val vertices = this.vertices

        if (texture != lastTexture) {
            switchTexture(texture)
        } else if (triangleIndex + trianglesCount > triangles.size || vertexIndex + verticesCount / 5 * 6 > vertices.size)
        //
            flush()

        var triangleIndex = this.triangleIndex
        val vertexIndex = this.vertexIndex
        val startVertex = vertexIndex / 6

        run {
            var i = trianglesOffset
            val n = i + trianglesCount
            while (i < n) {
                triangles[triangleIndex++] = (polygonTriangles[i] + startVertex).toShort()
                i++
            }
        }
        this.triangleIndex = triangleIndex

        var idx = this.vertexIndex
        var i = verticesOffset
        val n = verticesOffset + verticesCount
        while (i < n) {
            vertices[idx++] = polygonVertices[i]
            vertices[idx++] = polygonVertices[i + 1]
            vertices[idx++] = polygonVertices[i + 2]
            vertices[idx++] = 0f // dark
            vertices[idx++] = polygonVertices[i + 3]
            vertices[idx++] = polygonVertices[i + 4]
            i += 5
        }
        this.vertexIndex = idx
    }

    /** Draws rectangles using the given vertices. There must be 4 vertices, each made up of 6 elements in this order: x, y,
     * lightColor, darkColor, u, v. The [.getColor] and [.getDarkColor] from the TwoColorPolygonBatch is not
     * applied.  */
    fun drawTwoColor(texture: Texture, spriteVertices: FloatArray, offset: Int, count: Int) {
        check(isDrawing) { "begin must be called before draw." }

        val triangles = this.triangles
        val vertices = this.vertices

        val triangleCount = count / SPRITE_SIZE * 6
        if (texture != lastTexture)
            switchTexture(texture)
        else if (triangleIndex + triangleCount > triangles.size || vertexIndex + count > vertices.size)
        //
            flush()

        val vertexIndex = this.vertexIndex
        var triangleIndex = this.triangleIndex
        var vertex = (vertexIndex / VERTEX_SIZE).toShort()
        val n = triangleIndex + triangleCount
        while (triangleIndex < n) {
            triangles[triangleIndex] = vertex
            triangles[triangleIndex + 1] = (vertex + 1).toShort()
            triangles[triangleIndex + 2] = (vertex + 2).toShort()
            triangles[triangleIndex + 3] = (vertex + 2).toShort()
            triangles[triangleIndex + 4] = (vertex + 3).toShort()
            triangles[triangleIndex + 5] = vertex
            triangleIndex += 6
            vertex = (vertex + 4).toShort()
        }
        this.triangleIndex = triangleIndex

        arraycopy(spriteVertices, offset, vertices, vertexIndex, count)
        this.vertexIndex += count
    }

    /** Draws rectangles using the given vertices in the [Batch] format. There must be 4 vertices, each made up of 5 elements
     * in this order: x, y, color, u, v. The [.getColor] and [.getDarkColor] from the TwoColorPolygonBatch is not
     * applied.  */
    override fun draw(texture: Texture?, spriteVertices: FloatArray?, offset: Int, count: Int) {
        check(isDrawing) { "begin must be called before draw." }

        val triangles = this.triangles
        val vertices = this.vertices

        val triangleCount = count / 20 * 6
        if (texture != lastTexture)
            switchTexture(texture!!)
        else if (triangleIndex + triangleCount > triangles.size || vertexIndex + count / 5 * 6 > vertices.size)
        //
            flush()

        val vertexIndex = this.vertexIndex
        var triangleIndex = this.triangleIndex
        var vertex = (vertexIndex / VERTEX_SIZE).toShort()
        run {
            val n = triangleIndex + triangleCount
            while (triangleIndex < n) {
                triangles[triangleIndex] = vertex
                triangles[triangleIndex + 1] = (vertex + 1).toShort()
                triangles[triangleIndex + 2] = (vertex + 2).toShort()
                triangles[triangleIndex + 3] = (vertex + 2).toShort()
                triangles[triangleIndex + 4] = (vertex + 3).toShort()
                triangles[triangleIndex + 5] = vertex
                triangleIndex += 6
                vertex = (vertex + 4).toShort()
            }
        }
        this.triangleIndex = triangleIndex

        var idx = this.vertexIndex
        var i = offset
        val n = offset + count
        while (i < n) {
            vertices[idx++] = spriteVertices!![i]
            vertices[idx++] = spriteVertices[i + 1]
            vertices[idx++] = spriteVertices[i + 2]
            vertices[idx++] = 0f // dark
            vertices[idx++] = spriteVertices[i + 3]
            vertices[idx++] = spriteVertices[i + 4]
            i += 5
        }
        this.vertexIndex = idx
    }

    override fun draw(region: TextureAtlas.AtlasRegion?, x: Float, y: Float) {
        draw(region, x, y, region!!.regionWidth, region.regionHeight)
    }

    override fun draw(region: TextureAtlas.AtlasRegion?, x: Float, y: Float, width: Float, height: Float) {
        check(isDrawing) { "begin must be called before draw." }

        val triangles = this.triangles
        val vertices = this.vertices

        val texture = region!!.texture
        if (texture != lastTexture)
            switchTexture(texture)
        else if (triangleIndex + 6 > triangles.size || vertexIndex + SPRITE_SIZE > vertices.size)
        //
            flush()

        var triangleIndex = this.triangleIndex
        val startVertex = vertexIndex / VERTEX_SIZE
        triangles[triangleIndex++] = startVertex.toShort()
        triangles[triangleIndex++] = (startVertex + 1).toShort()
        triangles[triangleIndex++] = (startVertex + 2).toShort()
        triangles[triangleIndex++] = (startVertex + 2).toShort()
        triangles[triangleIndex++] = (startVertex + 3).toShort()
        triangles[triangleIndex++] = startVertex.toShort()
        this.triangleIndex = triangleIndex

        val fx2 = x + width
        val fy2 = y + height
        val u = region.u
        val v = region.v2
        val u2 = region.u2
        val v2 = region.v

        val light = this.lightPacked
        val dark = this.darkPacked
        var idx = this.vertexIndex
        vertices[idx++] = x
        vertices[idx++] = y
        vertices[idx++] = light
        vertices[idx++] = dark
        vertices[idx++] = u
        vertices[idx++] = v

        vertices[idx++] = x
        vertices[idx++] = fy2
        vertices[idx++] = light
        vertices[idx++] = dark
        vertices[idx++] = u
        vertices[idx++] = v2

        vertices[idx++] = fx2
        vertices[idx++] = fy2
        vertices[idx++] = light
        vertices[idx++] = dark
        vertices[idx++] = u2
        vertices[idx++] = v2

        vertices[idx++] = fx2
        vertices[idx++] = y
        vertices[idx++] = light
        vertices[idx++] = dark
        vertices[idx++] = u2
        vertices[idx++] = v
        this.vertexIndex = idx
    }

    override fun draw(region: TextureAtlas.AtlasRegion?, x: Float, y: Float, originX: Float, originY: Float, width: Float, height: Float,
                      scaleX: Float, scaleY: Float, rotation: Float) {
        check(isDrawing) { "begin must be called before draw." }

        val triangles = this.triangles
        val vertices = this.vertices

        val texture = region!!.texture
        if (texture != lastTexture)
            switchTexture(texture)
        else if (triangleIndex + 6 > triangles.size || vertexIndex + SPRITE_SIZE > vertices.size)
        //
            flush()

        var triangleIndex = this.triangleIndex
        val startVertex = vertexIndex / VERTEX_SIZE
        triangles[triangleIndex++] = startVertex.toShort()
        triangles[triangleIndex++] = (startVertex + 1).toShort()
        triangles[triangleIndex++] = (startVertex + 2).toShort()
        triangles[triangleIndex++] = (startVertex + 2).toShort()
        triangles[triangleIndex++] = (startVertex + 3).toShort()
        triangles[triangleIndex++] = startVertex.toShort()
        this.triangleIndex = triangleIndex

        // bottom left and top right corner points relative to origin
        val worldOriginX = x + originX
        val worldOriginY = y + originY
        var fx = -originX
        var fy = -originY
        var fx2 = width - originX
        var fy2 = height - originY

        // scale
        if (scaleX != 1f || scaleY != 1f) {
            fx *= scaleX
            fy *= scaleY
            fx2 *= scaleX
            fy2 *= scaleY
        }

        // construct corner points, start from top left and go counter clockwise
        val p1x = fx
        val p1y = fy
        val p2x = fx
        val p2y = fy2
        val p3x = fx2
        val p3y = fy2
        val p4x = fx2
        val p4y = fy

        var x1: Float
        var y1: Float
        var x2: Float
        var y2: Float
        var x3: Float
        var y3: Float
        var x4: Float
        var y4: Float

        // rotate
        if (rotation != 0f) {
            val cos = MathUtils.cosDeg(rotation)
            val sin = MathUtils.sinDeg(rotation)

            x1 = cos * p1x - sin * p1y
            y1 = sin * p1x + cos * p1y

            x2 = cos * p2x - sin * p2y
            y2 = sin * p2x + cos * p2y

            x3 = cos * p3x - sin * p3y
            y3 = sin * p3x + cos * p3y

            x4 = x1 + (x3 - x2)
            y4 = y3 - (y2 - y1)
        } else {
            x1 = p1x
            y1 = p1y

            x2 = p2x
            y2 = p2y

            x3 = p3x
            y3 = p3y

            x4 = p4x
            y4 = p4y
        }

        x1 += worldOriginX
        y1 += worldOriginY
        x2 += worldOriginX
        y2 += worldOriginY
        x3 += worldOriginX
        y3 += worldOriginY
        x4 += worldOriginX
        y4 += worldOriginY

        val u = region.u
        val v = region.v2
        val u2 = region.u2
        val v2 = region.v

        val light = this.lightPacked
        val dark = this.darkPacked
        var idx = this.vertexIndex
        vertices[idx++] = x1
        vertices[idx++] = y1
        vertices[idx++] = light
        vertices[idx++] = dark
        vertices[idx++] = u
        vertices[idx++] = v

        vertices[idx++] = x2
        vertices[idx++] = y2
        vertices[idx++] = light
        vertices[idx++] = dark
        vertices[idx++] = u
        vertices[idx++] = v2

        vertices[idx++] = x3
        vertices[idx++] = y3
        vertices[idx++] = light
        vertices[idx++] = dark
        vertices[idx++] = u2
        vertices[idx++] = v2

        vertices[idx++] = x4
        vertices[idx++] = y4
        vertices[idx++] = light
        vertices[idx++] = dark
        vertices[idx++] = u2
        vertices[idx++] = v
        this.vertexIndex = idx
    }

    override fun draw(region: TextureAtlas.AtlasRegion?, x: Float, y: Float, originX: Float, originY: Float, width: Float, height: Float,
                      scaleX: Float, scaleY: Float, rotation: Float, clockwise: Boolean) {
        check(isDrawing) { "begin must be called before draw." }

        val triangles = this.triangles
        val vertices = this.vertices

        val texture = region!!.texture
        if (texture != lastTexture)
            switchTexture(texture)
        else if (triangleIndex + 6 > triangles.size || vertexIndex + SPRITE_SIZE > vertices.size)
        //
            flush()

        var triangleIndex = this.triangleIndex
        val startVertex = vertexIndex / VERTEX_SIZE
        triangles[triangleIndex++] = startVertex.toShort()
        triangles[triangleIndex++] = (startVertex + 1).toShort()
        triangles[triangleIndex++] = (startVertex + 2).toShort()
        triangles[triangleIndex++] = (startVertex + 2).toShort()
        triangles[triangleIndex++] = (startVertex + 3).toShort()
        triangles[triangleIndex++] = startVertex.toShort()
        this.triangleIndex = triangleIndex

        // bottom left and top right corner points relative to origin
        val worldOriginX = x + originX
        val worldOriginY = y + originY
        var fx = -originX
        var fy = -originY
        var fx2 = width - originX
        var fy2 = height - originY

        // scale
        if (scaleX != 1f || scaleY != 1f) {
            fx *= scaleX
            fy *= scaleY
            fx2 *= scaleX
            fy2 *= scaleY
        }

        // construct corner points, start from top left and go counter clockwise
        val p1x = fx
        val p1y = fy
        val p2x = fx
        val p2y = fy2
        val p3x = fx2
        val p3y = fy2
        val p4x = fx2
        val p4y = fy

        var x1: Float
        var y1: Float
        var x2: Float
        var y2: Float
        var x3: Float
        var y3: Float
        var x4: Float
        var y4: Float

        // rotate
        if (rotation != 0f) {
            val cos = MathUtils.cosDeg(rotation)
            val sin = MathUtils.sinDeg(rotation)

            x1 = cos * p1x - sin * p1y
            y1 = sin * p1x + cos * p1y

            x2 = cos * p2x - sin * p2y
            y2 = sin * p2x + cos * p2y

            x3 = cos * p3x - sin * p3y
            y3 = sin * p3x + cos * p3y

            x4 = x1 + (x3 - x2)
            y4 = y3 - (y2 - y1)
        } else {
            x1 = p1x
            y1 = p1y

            x2 = p2x
            y2 = p2y

            x3 = p3x
            y3 = p3y

            x4 = p4x
            y4 = p4y
        }

        x1 += worldOriginX
        y1 += worldOriginY
        x2 += worldOriginX
        y2 += worldOriginY
        x3 += worldOriginX
        y3 += worldOriginY
        x4 += worldOriginX
        y4 += worldOriginY

        val u1: Float
        val v1: Float
        val u2: Float
        val v2: Float
        val u3: Float
        val v3: Float
        val u4: Float
        val v4: Float
        if (clockwise) {
            u1 = region.u2
            v1 = region.v2
            u2 = region.u
            v2 = region.v2
            u3 = region.u
            v3 = region.v
            u4 = region.u2
            v4 = region.v
        } else {
            u1 = region.u
            v1 = region.v
            u2 = region.u2
            v2 = region.v
            u3 = region.u2
            v3 = region.v2
            u4 = region.u
            v4 = region.v2
        }

        val light = this.lightPacked
        val dark = this.darkPacked
        var idx = this.vertexIndex
        vertices[idx++] = x1
        vertices[idx++] = y1
        vertices[idx++] = light
        vertices[idx++] = dark
        vertices[idx++] = u1
        vertices[idx++] = v1

        vertices[idx++] = x2
        vertices[idx++] = y2
        vertices[idx++] = light
        vertices[idx++] = dark
        vertices[idx++] = u2
        vertices[idx++] = v2

        vertices[idx++] = x3
        vertices[idx++] = y3
        vertices[idx++] = light
        vertices[idx++] = dark
        vertices[idx++] = u3
        vertices[idx++] = v3

        vertices[idx++] = x4
        vertices[idx++] = y4
        vertices[idx++] = light
        vertices[idx++] = dark
        vertices[idx++] = u4
        vertices[idx++] = v4
        this.vertexIndex = idx
    }

    override fun draw(region: TextureAtlas.AtlasRegion?, width: Float, height: Float, transform: Affine2?) {
        check(isDrawing) { "begin must be called before draw." }

        val triangles = this.triangles
        val vertices = this.vertices

        val texture = region!!.texture
        if (texture != lastTexture)
            switchTexture(texture)
        else if (triangleIndex + 6 > triangles.size || vertexIndex + SPRITE_SIZE > vertices.size)
        //
            flush()

        var triangleIndex = this.triangleIndex
        val startVertex = vertexIndex / VERTEX_SIZE
        triangles[triangleIndex++] = startVertex.toShort()
        triangles[triangleIndex++] = (startVertex + 1).toShort()
        triangles[triangleIndex++] = (startVertex + 2).toShort()
        triangles[triangleIndex++] = (startVertex + 2).toShort()
        triangles[triangleIndex++] = (startVertex + 3).toShort()
        triangles[triangleIndex++] = startVertex.toShort()
        this.triangleIndex = triangleIndex

        // construct corner points
        val x1 = transform!!.m02
        val y1 = transform.m12
        val x2 = transform.m01 * height + transform.m02
        val y2 = transform.m11 * height + transform.m12
        val x3 = transform.m00 * width + transform.m01 * height + transform.m02
        val y3 = transform.m10 * width + transform.m11 * height + transform.m12
        val x4 = transform.m00 * width + transform.m02
        val y4 = transform.m10 * width + transform.m12

        val u = region.u
        val v = region.v2
        val u2 = region.u2
        val v2 = region.v

        val light = this.lightPacked
        val dark = this.darkPacked
        var idx = vertexIndex
        vertices[idx++] = x1
        vertices[idx++] = y1
        vertices[idx++] = light
        vertices[idx++] = dark
        vertices[idx++] = u
        vertices[idx++] = v

        vertices[idx++] = x2
        vertices[idx++] = y2
        vertices[idx++] = light
        vertices[idx++] = dark
        vertices[idx++] = u
        vertices[idx++] = v2

        vertices[idx++] = x3
        vertices[idx++] = y3
        vertices[idx++] = light
        vertices[idx++] = dark
        vertices[idx++] = u2
        vertices[idx++] = v2

        vertices[idx++] = x4
        vertices[idx++] = y4
        vertices[idx++] = light
        vertices[idx++] = dark
        vertices[idx++] = u2
        vertices[idx++] = v
        vertexIndex = idx
    }

    override fun flush() {
        if (vertexIndex == 0) return

        totalRenderCalls++

        lastTexture!!.bind()
        val mesh = this.mesh
        mesh.setVertices(vertices, 0, vertexIndex)
        mesh.setIndices(triangles, 0, triangleIndex)
        Gdx.gl!!.glEnable(GL20.GL_BLEND)
        if (blendSrcFunc != -1) Gdx.gl!!.glBlendFuncSeparate(blendSrcFunc, blendDstFunc, blendSrcFuncAlpha, blendDstFuncAlpha)
        mesh.render(shader, GL20.GL_TRIANGLES, 0, triangleIndex)

        vertexIndex = 0
        triangleIndex = 0
    }

    override fun disableBlending() {
        flush()
        blendingDisabled = true
    }

    override fun enableBlending() {
        flush()
        blendingDisabled = false
    }

    override fun dispose() {
        mesh.dispose()
        shader!!.dispose()
    }

    /** Specifies whether the texture colors have premultiplied alpha. Required for correct dark color tinting. Does not change the
     * blending function. Flushes the batch if the setting was changed.  */
    fun setPremultipliedAlpha(premultipliedAlpha: Boolean) {
        if (this.premultipliedAlpha == premultipliedAlpha) return
        if (isDrawing) flush()
        this.premultipliedAlpha = premultipliedAlpha
        if (isDrawing) setupMatrices()
    }

    private fun setupMatrices() {
        combinedMatrix.set(this.projectionMatrix!!).mul(this.transformMatrix!!)
        shader!!.setUniformf("u_pma", if (premultipliedAlpha) 1 else 0)
        shader!!.setUniformMatrix("u_projTrans", combinedMatrix)
        shader!!.setUniformi("u_texture", 0)
    }

    private fun switchTexture(texture: Texture) {
        flush()
        lastTexture = texture
        invTexWidth = 1.0f / texture.width
        invTexHeight = 1.0f / texture.height
    }

    /** Flushes the batch if the blend function was changed.  */
    override fun setBlendFunction(srcFunc: Int, dstFunc: Int) {
        setBlendFunctionSeparate(srcFunc, dstFunc, srcFunc, dstFunc)
    }

    /** Flushes the batch if the blend function was changed.  */
    override fun setBlendFunctionSeparate(srcFuncColor: Int, dstFuncColor: Int, srcFuncAlpha: Int, dstFuncAlpha: Int) {
        if (blendSrcFunc == srcFuncColor && blendDstFunc == dstFuncColor && blendSrcFuncAlpha == srcFuncAlpha
                && blendDstFuncAlpha == dstFuncAlpha)
            return
        flush()
        blendSrcFunc = srcFuncColor
        blendDstFunc = dstFuncColor
        blendSrcFuncAlpha = srcFuncAlpha
        blendDstFuncAlpha = dstFuncAlpha
    }

    private fun createDefaultShader(): ShaderProgram {
        val vertexShader = ("attribute vec4 a_position;\n" //

                + "attribute vec4 a_light;\n" //

                + "attribute vec4 a_dark;\n" //

                + "attribute vec2 a_texCoord0;\n" //

                + "uniform mat4 u_projTrans;\n" //

                + "varying vec4 v_light;\n" //

                + "varying vec4 v_dark;\n" //

                + "varying vec2 v_texCoords;\n" //

                + "\n" //

                + "void main()\n" //

                + "{\n" //

                + "  v_light = a_light;\n" //

                + "  v_light.a = v_light.a * (255.0/254.0);\n" //

                + "  v_dark = a_dark;\n" //

                + "  v_texCoords = a_texCoord0;\n" //

                + "  gl_Position = u_projTrans * a_position;\n" //

                + "}\n")
        val fragmentShader = ("#ifdef GL_ES\n" //

                + "#define LOWP lowp\n" //

                + "precision mediump float;\n" //

                + "#else\n" //

                + "#define LOWP \n" //

                + "#endif\n" //

                + "varying LOWP vec4 v_light;\n" //

                + "varying LOWP vec4 v_dark;\n" //

                + "uniform float u_pma;\n" //

                + "varying vec2 v_texCoords;\n" //

                + "uniform sampler2D u_texture;\n" //

                + "void main()\n"//

                + "{\n" //

                + "  vec4 texColor = texture2D(u_texture, v_texCoords);\n" //

                + "  gl_FragColor.a = texColor.a * v_light.a;\n" //

                + "  gl_FragColor.rgb = ((texColor.a - 1.0) * u_pma + 1.0 - texColor.rgb) * v_dark.rgb + texColor.rgb * v_light.rgb;\n" //

                + "}")

        val shader = ShaderProgram(vertexShader, fragmentShader)
        require(shader.isCompiled != false) { "Error compiling shader: " + shader.log!! }
        return shader
    }

    companion object {
        internal val VERTEX_SIZE = 2 + 1 + 1 + 2
        internal val SPRITE_SIZE = 4 * VERTEX_SIZE
    }
}
*/
