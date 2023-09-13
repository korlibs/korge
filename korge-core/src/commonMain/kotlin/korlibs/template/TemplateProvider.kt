package korlibs.template

interface NewTemplateProvider {
    suspend fun newGet(template: String): TemplateContent?
}

interface TemplateProvider : NewTemplateProvider {
	class NotFoundException(val template: String) : RuntimeException("Can't find template '$template'")

    override suspend fun newGet(template: String): TemplateContent? = get(template)?.let { TemplateContent(it) }
	suspend fun get(template: String): String?
}

suspend fun NewTemplateProvider.newGetSure(template: String) = newGet(template)
    ?: throw TemplateProvider.NotFoundException(template)

suspend fun TemplateProvider.getSure(template: String) = get(template)
    ?: throw TemplateProvider.NotFoundException(template)

fun TemplateProvider(map: Map<String, String>): TemplateProvider = object : TemplateProvider {
	override suspend fun get(template: String): String? = map[template]
}

fun TemplateProvider(vararg map: Pair<String, String>): TemplateProvider = TemplateProvider(map.toMap())

fun NewTemplateProvider(map: Map<String, TemplateContent>): NewTemplateProvider = object : NewTemplateProvider {
    override suspend fun newGet(template: String): TemplateContent? = map[template]
}
fun NewTemplateProvider(vararg map: Pair<String, TemplateContent>): NewTemplateProvider = NewTemplateProvider(map.toMap())
