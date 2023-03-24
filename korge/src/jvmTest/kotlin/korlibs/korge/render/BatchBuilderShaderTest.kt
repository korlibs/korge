package korlibs.korge.render

import korlibs.graphics.shader.*
import korlibs.graphics.shader.gl.*
import korlibs.korge.test.*
import kotlin.test.*

class BatchBuilderShaderTest {
    @Test
    fun testDefault() {
        val program = BatchBuilder2D.PROGRAM
        val fragmentText = program.fragment.toNewGlslString(GlslConfig(programConfig = ProgramConfig.DEFAULT))
        assertEqualsFileReference("korge/render/Default.frag.log", fragmentText)
    }

    //@Test
    //fun testExternalTextureSampler() {
    //    val program = BatchBuilder2D.getTextureLookupProgram(BatchBuilder2D.AddType.POST_ADD)
    //    val fragmentText = program.fragment.toNewGlslString(GlslConfig(programConfig = ProgramConfig.EXTERNAL_TEXTURE_SAMPLER))
    //    assertEqualsFileReference("korge/render/ExternalTextureSampler.frag.log", fragmentText)
    //}
}