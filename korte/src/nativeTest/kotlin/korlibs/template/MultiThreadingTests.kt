package korlibs.template

import korlibs.template.dynamic.DynamicType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlin.test.Test
import kotlin.test.assertEquals

class MultiThreadingTests {
    @Test
    fun testTemplateEvaluationOnBackgroundThread() = runBlocking {
        data class Model(val x: Int) : DynamicType<Model> by DynamicType({
            register(Model::x)
        })
        withContext(Dispatchers.Default) {
            val template = Template("{{x+1}}")
            val rendered = template(Model(x = 2))
            assertEquals("3", rendered)
        }
    }
}
