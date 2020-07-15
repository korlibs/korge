package com.soywiz.korte.ktor

import com.soywiz.korte.*
import com.soywiz.korte.dynamic.*
import io.ktor.application.*
import io.ktor.http.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.testing.*
import kotlinx.coroutines.*
import org.junit.Test
import kotlin.test.*

class KorteKtorTest {
    @Test
    fun test() {
        class MyModel(val hello: String) : DynamicType<MyModel> by DynamicType({ register(MyModel::hello) })

        runBlocking {
            withTestApplication {
                application.apply {
                    install(Korte) {
                        cache(true)
                        //cache(false)
                        root(
                            TemplateProvider(
                                "demo.tpl" to "Hello {{ hello }}"
                            )
                        )
                    }
                    routing {
                        get("/") {
                            //call.respondKorte("demo.tpl", mapOf("hello" to "world"))
                            call.respondKorte("demo.tpl", MyModel(hello = "world"))
                            //call.respondText("Hello world")
                        }
                    }
                    assertEquals("Hello world", handleRequest(HttpMethod.Get, "/") { }.response.content)
                }
            }
        }
    }
}
