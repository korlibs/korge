package korlibs.image.font

import korlibs.datastructure.lock.*
import kotlin.coroutines.*

interface FontRegistry {
    operator fun get(name: String?): Font
    companion object {
        operator fun invoke(coroutineContext: CoroutineContext): DefaultFontRegistry = DefaultFontRegistry(coroutineContext)
    }
}

// TODO Can't use an object because that would be included in the JS output

private var SystemFontRegistryOrNull: DefaultFontRegistry? = null

fun SystemFontRegistry(coroutineContext: CoroutineContext): DefaultFontRegistry {
    if (SystemFontRegistryOrNull == null) {
        SystemFontRegistryOrNull = DefaultFontRegistry(coroutineContext)
        DefaultTtfFont.register(registry = SystemFontRegistryOrNull!!, name = "sans-serif")
    }
    return SystemFontRegistryOrNull!!
}
suspend fun SystemFontRegistry(): DefaultFontRegistry = SystemFontRegistry(coroutineContext)

open class DefaultFontRegistry(val coroutineContext: CoroutineContext) : FontRegistry {
    private val lock = Lock()
    private val registeredFonts = LinkedHashMap<String?, Font>()
    fun normalizeName(name: String?) = name?.toLowerCase()?.trim()
    fun register(font: Font, name: String = font.name): Font = lock { font.also { registeredFonts[normalizeName(name)] = it } }
    fun unregister(name: String): Font? = lock { registeredFonts.remove(name) }
    inline fun <T> registerTemporarily(font: Font, name: String = font.name, block: () -> T): T {
        register(font, name)
        try {
            return block()
        } finally {
            unregister(name)
        }
    }
    override operator fun get(name: String?): Font {
        return lock { registeredFonts[normalizeName(name)] ?: SystemFont(name ?: "default", coroutineContext) }
    }
}

fun <T : Font> T.register(registry: DefaultFontRegistry, name: String = this.name): T = this.also { registry.register(it, name) }
inline fun <T> Font.registerTemporarily(registry: DefaultFontRegistry, name: String = this.name, block: () -> T): T = registry.registerTemporarily(this, name, block)

suspend fun <T : Font> T.register(name: String = this.name): T = register(SystemFontRegistry(), name)
suspend inline fun <T> Font.registerTemporarily(name: String = this.name, block: () -> T): T = registerTemporarily(SystemFontRegistry(), name, block)
