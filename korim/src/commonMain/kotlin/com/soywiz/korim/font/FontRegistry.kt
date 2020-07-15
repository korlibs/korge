package com.soywiz.korim.font

import com.soywiz.kds.CopyOnWriteFrozenMap
import kotlin.native.concurrent.ThreadLocal

interface FontRegistry {
    operator fun get(name: String): Font = SystemFont(name)
    companion object {
        operator fun invoke(): DefaultFontRegistry = DefaultFontRegistry()
    }
}

@ThreadLocal
object SystemFontRegistry : DefaultFontRegistry() {
    val DEFAULT_FONT = DefaultTtfFont.register(name = "sans-serif")
}

open class DefaultFontRegistry : FontRegistry {
    private val registeredFonts = CopyOnWriteFrozenMap<String, Font>()
    fun normalizeName(name: String) = name.toLowerCase().trim()
    fun register(font: Font, name: String = font.name) = font.also { registeredFonts[normalizeName(name)] = it }
    fun unregister(name: String) = registeredFonts.remove(name)
    inline fun <T> registerTemporarily(font: Font, name: String = font.name, block: () -> T): T {
        register(font, name)
        try {
            return block()
        } finally {
            unregister(name)
        }
    }
    override operator fun get(name: String): Font = registeredFonts[normalizeName(name)] ?: SystemFont(name)
}

fun <T : Font> T.register(registry: DefaultFontRegistry = SystemFontRegistry, name: String = this.name): T = this.also { registry.register(it, name) }
inline fun <T> Font.registerTemporarily(registry: DefaultFontRegistry = SystemFontRegistry, name: String = this.name, block: () -> T): T = registry.registerTemporarily(this, name, block)
