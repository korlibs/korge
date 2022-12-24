package korge.graphics.backend.metal.shader

import com.soywiz.korag.*
import com.soywiz.korag.shader.*
import com.soywiz.korag.shader.gl.*
import com.soywiz.korio.util.*
import io.kotest.core.spec.style.*
import io.kotest.matchers.*
import kotlin.test.*

class MetalShaderGeneratorTest : StringSpec({

    "test" {
        val vs = VertexShader {
            IF(true.lit) {
                DefaultShaders.t_Temp0 setTo 1.lit * 2.lit
            } ELSE {
                DefaultShaders.t_Temp0 setTo 3.lit * 4.lit
            }
        }

        assertEqualsShader(vs) {
            "void main()" {
                +"vec4 temp0;"
                "if (true)" {
                    +"temp0 = (1 * 2);"
                }
                "else" {
                    +"temp0 = (3 * 4);"
                }
            }
        }
    }
})


fun assertEqualsShader(shader: Shader, version: Int = GlslGenerator.DEFAULT_VERSION, gles: Boolean = false, block: Indenter.() -> Unit) {
    assertEquals(
        Indenter {
            block()
        }.toString(),
        shader.toNewGlslStringResult(gles = gles, version = version).result
    )
}
