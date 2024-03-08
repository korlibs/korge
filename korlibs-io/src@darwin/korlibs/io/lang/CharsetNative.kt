package korlibs.io.lang

actual val platformCharsetProvider: CharsetProvider = CharsetProvider { normalizedName, _ -> null }
