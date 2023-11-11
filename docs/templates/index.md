---
permalink: /templates/
group: templates
layout: default
title: "Templates"
title_short: Introduction
fa-icon: far fa-file-code
priority: 0
artifact: 'com.soywiz.korge:korge-core'
package: korlibs.template
---

<img alt="KorTE" src="/i/logos/korte.svg" width="128" height="128" style="float: left;" />

KorTE is a asynchronous template engine for Multiplatform Kotlin.

It is a non-strict super set of [twig](https://twig.symfony.com/) / [django](https://docs.djangoproject.com/en/2.1/topics/templates/) / [atpl.js](https://github.com/soywiz/atpl.js) / [jinjava](https://github.com/HubSpot/jinjava) template engines and can support [liquid template engine](https://shopify.github.io/liquid/) too with frontmatter.

It has out of the box support for [ktor](https://ktor.io/) and [vert.x](https://vertx.io/).

It works on JVM and JS out of the box.
But can also work on Native when using untyped model data or making models to implement the `DynamicType` interface.

It allows to call suspend methods from within templates.

Live demo (editable) [[source code]](https://github.com/korlibs/korte-samples/blob/master/korte-sample-browser/src/main/kotlin/main.kt){:target="_blank",:rel="noopener"}:

<embed src="https://korlibs.github.io/korte-samples/korte-sample-browser/web/" style="width: 100%;height:50vh;" />



{% raw %}

## Pages

{% include toc.html context="/korte/" %}

## Usage

### Raw Usage

Manual usage:

```kotlin
import com.soywiz.korte.Template

val template = Template("hello {{ who }}")
val rendered = template(mapOf("who" to "world"))
assertEquals("hello world", rendered)
```

Managed with KorIO's Vfs and optional cache:

```kotlin
import com.soywiz.korte.Templates

//val myvfs = resourcesVfs["templates"] // To read templates from a 'templates' folder in the application resources
val myvfs = MemoryVfsMix(
    "index.html" to "hello {{ who }}"
)

val templates = Templates(myvfs, cache = true)
val rendered = templates.render("index.html", mapOf("who" to "world"))
assertEquals("hello world", rendered)
```

### Ktor

```kotlin
dependencies {
    jvmMainApi "com.soywiz:korte-ktor-jvm:$korteVersion"
}
```

```kotlin
fun Application.module() {
    install(Korte) {
        cache(true)
        root(
            // resourcesVfs
            MemoryVfsMix(
                "demo.tpl" to "Hello {{ hello }}"
            )
        )
    }
    routing {
        get("/") {
            call.respondKorte("demo.tpl", MyModel(hello = "world"))
        }
    }
    assertEquals("Hello world", handleRequest(HttpMethod.Get, "/") { }.response.content)
}
```

### Vert.x

```kotlin
dependencies {
    jvmMainApi "com.soywiz:korte-vertx-jvm:$korteVersion"
}
```

```kotlin
val port = 0
val host = "127.0.0.1"
val vertx = Vertx.vertx()
val router = Router.router(vertx)
val template = TemplateHandler.create(
    KorteVertxTemplateEngine(
        coroutineContext, Templates(
            MemoryVfsMix(
                "index.html" to "hello world {{ 1 + 2 }}!",
                "hello.html" to "Nice :)!"
            )
        )
    )
)

router.get("/*").handler(template)

val server: HttpServer = run {
    val server = vertx.createHttpServer()
    server.requestHandler(router)
    vx { server.listen(port, host, it) }
}
val actualPort = server.actualPort()

try {
    val client = vertx.createHttpClient()
    assertEquals("hello world 3!", client.get(actualPort, "127.0.0.1", "/").readString())
    assertEquals("Nice :)!", client.get(actualPort, "127.0.0.1", "/hello").readString())
} finally {
    server.close()
}
```

### Native

Since Kotlin/Native doesn't provide any kind of reflective functionality yet (and kotlinx.serialization don't allow to call methods), you have to help it a bit to understand your typed models to be able to call them.

```kotlin
data class Person(val name: String, val surname: String) :
    DynamicType<Person> by DynamicType({ register(Person::name, Person::surname) })
```

```kotlin
class TestMethods : DynamicType<TestMethods> by DynamicType({
        register("mytest123") { mytest123() }
        register("sum") { sum(it[0].toDynamicInt(), it[1].toDynamicInt()) }
    }), DynamicContext {
        var field = 1

        suspend fun mytest123(): Int {
            var r = withContext(Dispatchers.Unconfined) { field }
            return r + 7
        }

        @JsName("sum")
        suspend fun sum(a: Int, b: Int): Int {
            return a + b
        }
    }
```


{% endraw %}
