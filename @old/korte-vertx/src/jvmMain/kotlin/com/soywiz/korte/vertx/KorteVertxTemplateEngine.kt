package com.soywiz.korte.vertx

import com.soywiz.korte.*
import com.soywiz.korte.vertx.internal.*
import io.vertx.core.*
import io.vertx.core.buffer.*
import kotlin.coroutines.*

class KorteVertxTemplateEngine(
    val coroutineContext: CoroutineContext,
    val templates: Templates,
    val prefix: String = "",
    val suffix: String = ".html",
    val stripPrefix: String = "templates/"
) : io.vertx.ext.web.common.template.TemplateEngine {
    override fun isCachingEnabled(): Boolean = templates.cache

    override fun render(
        context: MutableMap<String, Any>,
        templateFileName: String,
        handler: Handler<AsyncResult<Buffer>>
    ) {
        handler.handle(coroutineContext) {
            Buffer.buffer(templates.render("$prefix${templateFileName.removePrefix(stripPrefix)}$suffix", context), "UTF-8")
        }
    }
}
