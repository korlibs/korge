package com.soywiz.korau

import com.soywiz.klock.*
import com.soywiz.korau.format.*
import com.soywiz.korio.async.*
import com.soywiz.korio.file.std.*
import com.soywiz.korio.net.http.*
import org.junit.Test
import kotlin.test.*

class KorauCatalogOverHttpVfsTest {
    @Test
    fun test() = suspendTest {
        val log = arrayListOf<String>()
        val http = FakeHttpServerClient {
            router {
                prehook {
                    log += "${it.method} ${it.path} ${it.headers}"
                }
                static("/", MemoryVfsMix(
                    "/\$catalog.json" to """[
                        {"name": "placeholder.mp3", "size": 36072, "modifiedTime": 0, "createTime": 0, "isDirectory": false},
                    ]""",
                    "placeholder.mp3" to resourcesVfs["placeholder.mp3"].readBytes()
                ))
            }
        }

        val vfs = UrlVfs("https://127.0.0.1:8080/", client = http).withCatalogJail()
        val info = vfs["placeholder.mp3"].readSoundInfo()
        assertEquals(2507.712.milliseconds, info!!.duration)
        assertEquals("""
            GET /${'$'}catalog.json Headers()
            GET /placeholder.mp3 Headers((range, [bytes=0-32767]))
            GET /placeholder.mp3 Headers((range, [bytes=32768-65535]))
        """.trimIndent(), log.joinToString("\n"))
    }

    @Test
    fun testNoCatalog() = suspendTest {
        val log = arrayListOf<String>()
        val http = FakeHttpServerClient {
            router {
                prehook {
                    log += "${it.method} ${it.path} ${it.headers}"
                }
                static("/", MemoryVfsMix(
                    "placeholder.mp3" to resourcesVfs["placeholder.mp3"].readBytes()
                ))
            }
        }

        val vfs = UrlVfs("https://127.0.0.1:8080/", client = http).withCatalogJail()
        val info = vfs["placeholder.mp3"].readSoundInfo()
        assertEquals(2507.712.milliseconds, info!!.duration)
        assertEquals("""
            GET /${'$'}catalog.json Headers()
            HEAD /placeholder.mp3 Headers()
            GET /placeholder.mp3 Headers((range, [bytes=0-32767]))
            GET /placeholder.mp3 Headers((range, [bytes=32768-65535]))
        """.trimIndent(), log.joinToString("\n"))
    }
}
