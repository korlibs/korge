package com.soywiz.korte.ktor

import com.soywiz.korte.*
import io.ktor.application.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.response.*
import io.ktor.util.*

class KorteContent(
    val template: String,
    val model: Any?,
    val etag: String? = null,
    val contentType: ContentType = ContentType.Text.Html.withCharset(Charsets.UTF_8)
)

suspend fun ApplicationCall.respondKorte(
    template: String,
    model: Any? = null,
    etag: String? = null,
    contentType: ContentType = ContentType.Text.Html.withCharset(Charsets.UTF_8)
) = respond(KorteContent(template, model, etag, contentType))

class Korte(private val config: Configuration) {
    class Configuration : TemplateConfigWithTemplates()

    companion object Feature : ApplicationFeature<ApplicationCallPipeline, Configuration, Korte> {
        val VERSION get() = com.soywiz.korte.Korte.VERSION

        override val key: AttributeKey<Korte> = AttributeKey("korte")

        override fun install(pipeline: ApplicationCallPipeline, configure: Configuration.() -> Unit): Korte {
            val config = Configuration().apply(configure)
            val feature = Korte(config)
            pipeline.sendPipeline.intercept(ApplicationSendPipeline.Transform) { value ->
                if (value is KorteContent) {
                    val response = feature.process(value)
                    proceedWith(response)
                }
            }
            return feature
        }
    }

    private suspend fun process(content: KorteContent): OutgoingContent {
        try {
            val rendered = config.templates.render(content.template, content.model)
            return TextContent(rendered, content.contentType)
        } catch (e: Throwable) {
            throw e
        }
    }

    /*
    private suspend fun process(content: KorteContent): OutgoingContent = KorteOutgoingContent(
        config.templates.get(content.template),
        content.model,
        content.etag,
        content.contentType
    )

    private class KorteOutgoingContent(
        val template: Template,
        val model: Any?,
        etag: String?,
        override val contentType: ContentType
    ) : OutgoingContent.WriteChannelContent() {
        override suspend fun writeTo(channel: ByteWriteChannel) {
            template.prender(model).write { channel.writeStringUtf8(it) }
        }

        init {
            if (etag != null)
                versions += EntityTagVersion(etag)
        }
    }
    */
}
