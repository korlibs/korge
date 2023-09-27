package korlibs.template

import korlibs.template.dynamic.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlin.test.Test
import kotlin.test.assertEquals

class MultiThreadingTests {
    @Test
    fun testTemplateEvaluationOnBackgroundThread() = runBlocking {
        data class Model(val x: Int) : KorteDynamicType<Model> by KorteDynamicType({
            register(Model::x)
        })
        withContext(Dispatchers.Default) {
            val template = KorteTemplate("{{x+1}}")
            val rendered = template(Model(x = 2))
            assertEquals("3", rendered)
        }
    }
}
