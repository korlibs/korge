# Korag - Kotlin cORoutines Accelerated Graphics

[![Build Status](https://travis-ci.org/korlibs/korag.svg?branch=master)](https://travis-ci.org/korlibs/korag)

![](https://raw.githubusercontent.com/soywiz/kor/master/logos/128/korag.png)

[All KOR libraries](https://github.com/soywiz/kor)

Depends on korio and korim.
Used by korui.

Provides an unified API for accelerated graphics: opengl, webgl, stage3d, directx, vulkan, metal...
Provides an AST for Shaders and a DSL for building them in code.
And integrates perfectly with korio, korim and korui.

Right now provides implementations for: webgl, awt+opengl and GL|es Android.

### Simple example:

Blue background + one single red triangle covering half the screen with a
predefined shader and vertex attribute layout and a Matrix4 uniform:

```kotlin
ag.clear(Colors.BLUE)
ag.createVertexBuffer(floatArrayOf(
  0f, 0f,
  640f, 0f,
  640f, 480f
)).use { vertices ->
    ag.draw(
        vertices,
        program = DefaultShaders.PROGRAM_DEBUG_WITH_PROJ,
        type = AG.DrawType.TRIANGLES,
        vertexLayout = DefaultShaders.LAYOUT_DEBUG,
        vertexCount = 3,
        uniforms = mapOf(
            DefaultShaders.u_ProjMat to Matrix4().setToOrtho(0f, 0f, 640f, 480f, -1f, +1f)
        )
    )
}
```

### Basic API

```kotlin
class AG {
    val nativeComponent: Any
    val backWidth: Int
    val backHeight: Int
    var onRender: (AG) -> Unit
    fun repaint()
    fun resized()
    
    fun createTexture(): Texture
    fun createTexture(bmp: Bitmap, mipmaps: Boolean = false): Texture
    fun createBuffer(kind: BufferKind): Buffer
    fun createIndexBuffer(): Buffer
    fun createVertexBuffer(): Buffer
    fun createIndexBuffer(data: ShortArray, offset: Int = 0, length: Int = data.size - offset): Buffer
    fun createVertexBuffer(data: FloatArray, offset: Int = 0, length: Int = data.size - offset): Buffer
    fun draw(vertices: Buffer, indices: Buffer, program: Program, type: DrawType, vertexLayout: VertexLayout, vertexCount: Int, offset: Int = 0, blending: BlendMode = BlendMode.OVERLAY, uniforms: Map<Uniform, Any> = mapOf()): Unit
    fun draw(vertices: Buffer, program: Program, type: DrawType, vertexLayout: VertexLayout, vertexCount: Int, offset: Int = 0, blending: BlendMode = BlendMode.OVERLAY, uniforms: Map<Uniform, Any> = mapOf()): Unit
    fun flip()
    fun clear(color: Int = RGBA(0, 0, 0, 0xFF), depth: Float = 0f, stencil: Int = 0, clearColor: Boolean = true, clearDepth: Boolean = true, clearStencil: Boolean = true)
    inline fun renderToTexture(width: Int, height: Int, callback: () -> Unit): RenderTexture
    inline fun renderToBitmap(bmp: Bitmap32, callback: () -> Unit)
    
    class Texture : Closeable {
        enum class Kind { RGBA, LUMINANCE }
        var mipmaps: Boolean
        fun upload(bmp: Bitmap, mipmaps: Boolean = false): Texture
        fun uploadBuffer(data: ByteBuffer, width: Int, height: Int, kind: Kind)
        fun uploadBitmap32(bmp: Bitmap32)
        fun uploadBitmap8(bmp: Bitmap8)
        fun close()
    }
    class TextureUnit {
        var texture: AG.Texture? = null
        var linear: Boolean = true
    }
    class Buffer : Closeable {
        enum class Kind { INDEX, VERTEX }
        var dirty = false
        fun upload(data: ByteBuffer, offset: Int = 0, length: Int = data.limit()): Buffer
        fun upload(data: ByteArray, offset: Int = 0, length: Int = data.size): Buffer
        fun upload(data: FloatArray, offset: Int = 0, length: Int = data.size): Buffer
        fun upload(data: IntArray, offset: Int = 0, length: Int = data.size): Buffer
        fun upload(data: ShortArray, offset: Int = 0, length: Int = data.size): Buffer
        fun upload(data: FBuffer, offset: Int = 0, length: Int = data.length): Buffer
        fun close()
    }
    class RenderBuffer : Closeable {
        val tex: Texture
        fun start(width: Int, height: Int): Unit
        fun end(): Unit
        fun readBitmap(bmp: Bitmap32): Unit
        fun close(): Unit
    }
    class RenderTexture(
        val tex: Texture,
        val width: Int,
        val height: Int
    )
    enum class DrawType { TRIANGLES }
}
```

### Already defined shaders:

```kotlin
object DefaultShaders {
	val u_Tex: Uniform
	val u_ProjMat: Uniform
	val a_Pos: Attribute // float,float
	val a_Tex: Attribute // float,float
	val a_Col: Attribute // int
	val v_Tex: Varying
	val v_Col: Varying
	val t_Temp1: Temp
	val textureUnit: AG.TextureUnit
	val LAYOUT_DEFAULT: VertexFormat // a_Pos,a_Tex,a_Col
	val VERTEX_DEFAULT: VertexShader
	val FRAGMENT_DEFAULT: FragmentShader
	val FRAGMENT_SOLID_COLOR: FragmentShader
	val PROGRAM_TINTED_TEXTURE: Program
	val PROGRAM_SOLID_COLOR: Program
	val LAYOUT_DEBUG: VertexFormat // a_Pos
	val PROGRAM_DEBUG : Program
	val PROGRAM_DEBUG_WITH_PROJ: Program
	val PROGRAM_DEFAULT: Program
}
```

### Defining program elements:

```kotlin
val u_Tex = Uniform("u_Tex", VarType.TextureUnit)

val u_ProjMat = Uniform("u_ProjMat", VarType.Mat4)
val a_Pos = Attribute("a_Pos", VarType.Float2, normalized = false)
val a_Tex = Attribute("a_Tex", VarType.Float2, normalized = false)
val a_Col = Attribute("a_Col", VarType.Byte4, normalized = true)
val v_Tex = Varying("v_Tex", VarType.Float2)
val v_Col = Varying("v_Col", VarType.Byte4)

val t_Temp1 = Temp(0, VarType.Float4)

val textureUnit = AG.TextureUnit()

val LAYOUT_DEFAULT = VertexLayout(a_Pos, a_Tex, a_Col)
```

### Shader AST + DSL:

```kotlin
val PROGRAM_DEBUG_WITH_PROJ = Program(
	vertex = VertexShader {
		SET(out, u_ProjMat * vec4(a_Pos, 0f.lit, 1f.lit))
	},
	fragment = FragmentShader {
		SET(out, vec4(1f.lit, 0f.lit, 0f.lit, 1f.lit))
	}
)
```
