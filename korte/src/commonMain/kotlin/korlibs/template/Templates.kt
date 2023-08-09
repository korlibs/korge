package korlibs.template

import korlibs.template.internal.AsyncCache
import korlibs.template.util.AsyncTextWriterContainer

open class Templates(
    var root: NewTemplateProvider,
    var includes: NewTemplateProvider = root,
    var layouts: NewTemplateProvider = root,
    val config: TemplateConfig = TemplateConfig(),
    var cache: Boolean = true
) {
    @PublishedApi
    internal val tcache = AsyncCache()

    fun invalidateCache() {
        tcache.invalidateAll()
    }

    @PublishedApi
    internal suspend fun cache(name: String, callback: suspend () -> Template): Template = when {
		cache -> tcache.call(name) { callback() }
        else -> callback()
    }

    open suspend fun getInclude(name: String): Template = cache("include/$name") {
        Template(name, this@Templates, includes.newGetSure(name), config).init()
    }

    open suspend fun getLayout(name: String): Template = cache("layout/$name") {
        Template(name, this@Templates, layouts.newGetSure(name), config).init()
    }

    open suspend fun get(name: String): Template = cache("base/$name") {
        Template(name, this@Templates, root.newGetSure(name), config).init()
    }

    suspend fun render(name: String, vararg args: Pair<String, Any?>): String = get(name).invoke(*args)
    suspend fun render(name: String, args: Any?): String {
        val template = get(name)
        val renderered = template(args)
        return renderered
    }
    suspend fun prender(name: String, vararg args: Pair<String, Any?>): AsyncTextWriterContainer =
        get(name).prender(*args)

    suspend fun prender(name: String, args: Any?): AsyncTextWriterContainer = get(name).prender(args)
}
