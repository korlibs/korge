package korlibs.io.net

import korlibs.memory.ByteArrayBuilder
import korlibs.io.file.normalize
import korlibs.io.file.pathInfo
import korlibs.io.util.StrReader
import korlibs.io.util.substringAfterOrNull
import korlibs.io.util.substringBeforeOrNull
import korlibs.encoding.Hex
import korlibs.encoding.toBase64
import korlibs.io.lang.*

data class URL private constructor(
	val isOpaque: Boolean,
	val scheme: String?,
	val subScheme: String?,
	val userInfo: String?,
	val host: String?,
	val path: String,
	val query: String?,
	val fragment: String?,
	val defaultPort: Int
) {
	val user: String? get() = userInfo?.substringBefore(':')
	val password: String? get() = userInfo?.substringAfter(':')
	val isHierarchical get() = !isOpaque
    val isSecureScheme get() = scheme == "https" || scheme == "wss" || scheme == "ftps"

    val defaultSchemePort: Int get() = defaultPortForScheme(scheme)
	val port: Int get() = (if (defaultPort == DEFAULT_PORT) defaultSchemePort else defaultPort)

	val fullUrl: String by lazy { toUrlString().toString() }

	val fullUrlWithoutScheme: String by lazy { toUrlString(includeScheme = false).toString() }

	val pathWithQuery: String by lazy {
		if (query != null) {
			"$path?$query"
		} else {
			path
		}
	}

	fun toUrlString(includeScheme: Boolean = true, out: StringBuilder = StringBuilder()): StringBuilder {
		if (includeScheme && scheme != null) {
			out.append("$scheme:")
            if (subScheme != null) out.append("$subScheme:")
			if (!isOpaque) out.append("//")
		}
		if (userInfo != null) out.append("$userInfo@")
		if (host != null) out.append(host)
        if (port != DEFAULT_PORT && port != defaultSchemePort) out.append(':').append(port)
		out.append(path)
		if (query != null) out.append("?$query")
		if (fragment != null) out.append("#$fragment")
		return out
	}

	val isAbsolute get() = (scheme != null)

	override fun toString(): String = fullUrl
	fun toComponentString(): String {
		return "URL(" + listOf(::scheme, ::subScheme, ::userInfo, ::host, ::path, ::query, ::fragment)
			.map { it.name to it.get() }
			.filter { it.second != null }
			.joinToString(", ") { "${it.first}=${it.second}" } + ")"
	}

	fun resolve(path: URL): URL = URL(resolve(this.fullUrl, path.fullUrl))

	companion object {
		val DEFAULT_PORT = 0

        fun defaultPortForScheme(scheme: String?): Int = when (scheme) {
            "ftp" -> 21
            "ftps" -> 990
            "http", "ws" -> 80
            "https", "wss" -> 443
            else -> -1
        }

        fun fromComponents(
            scheme: String? = null,
            subScheme: String? = null,
            userInfo: String? = null,
            host: String? = null,
            path: String = "",
            query: String? = null,
            fragment: String? = null,
            opaque: Boolean = false,
            port: Int = DEFAULT_PORT
        ): URL = URL(
            isOpaque = opaque,
            scheme = scheme?.lowercase(),
            subScheme = subScheme?.lowercase(),
            userInfo = userInfo,
            host = host,
            path = path,
            query = query,
            fragment = fragment,
            defaultPort = port
        )

        @Deprecated(
            message = "Use URL.fromComponents",
            replaceWith = ReplaceWith("URL.fromComponents(scheme, subScheme, userInfo, host, path, query, fragment, opaque)")
        )
        operator fun invoke(
            scheme: String?,
            userInfo: String?,
            host: String?,
            path: String,
            query: String?,
            fragment: String?,
            opaque: Boolean = false,
            port: Int = DEFAULT_PORT,
            subScheme: String? = null,
        ): URL = this.fromComponents(
            opaque = opaque,
            scheme = scheme?.lowercase(),
            subScheme = subScheme?.lowercase(),
            userInfo = userInfo,
            host = host,
            path = path,
            query = query,
            fragment = fragment,
            port = port
        )

		private val schemeRegex = Regex("^([a-zA-Z0-9+.-]+)(?::([a-zA-Z]+))?:")

		operator fun invoke(url: String): URL {
			val r = StrReader(url)
			val schemeColon = r.tryRegex(schemeRegex)
			return when {
				schemeColon != null -> {
					val isHierarchical = r.tryLit("//") != null
					val nonScheme = r.readRemaining()
                    val schemeParts = schemeColon.dropLast(1).split(":")
					val scheme = schemeParts[0]
                    val subScheme = schemeParts.getOrNull(1)

                    val nonFragment = nonScheme.substringBefore('#')
                    val fragment = nonScheme.substringAfterOrNull('#')

                    val nonQuery = nonFragment.substringBefore('?')
                    val query = nonFragment.substringAfterOrNull('?')

                    val authority = nonQuery.substringBefore('/')
                    val path = nonQuery.substringAfterOrNull('/')

                    val hostWithPort = authority.substringAfter('@')
                    val userInfo = authority.substringBeforeOrNull('@')

                    val host = hostWithPort.substringBefore(':')
                    val port = hostWithPort.substringAfterOrNull(':')

					this.fromComponents(
						opaque = !isHierarchical,
						scheme = scheme,
						subScheme = subScheme,
						userInfo = userInfo,
						host = host.takeIf { it.isNotEmpty() },
						path = if (path != null) "/$path" else "",
						query = query,
						fragment = fragment,
                        port = port?.toIntOrNull() ?: DEFAULT_PORT
					)
				}
				else -> {
                    val nonFragment = url.substringBefore('#')
                    val fragment = url.substringAfterOrNull('#')
                    val path = nonFragment.substringBefore('?')
                    val query = nonFragment.substringAfterOrNull('?')
					this.fromComponents(
						opaque = false,
						scheme = null,
                        subScheme = null,
						userInfo = null,
						host = null,
						path = path,
						query = query,
						fragment = fragment
					)
				}
			}
		}

		fun isAbsolute(url: String): Boolean = StrReader(url).tryRegex(schemeRegex) != null

        fun resolveOrNull(base: String, access: String): String? = kotlin.runCatching { resolve(base, access) }.getOrNull()

		fun resolve(base: String, access: String): String {
            // if access url is relative protocol then copy it from base
            val refinedAccess = if (access.startsWith("//") && base.contains(":")) "${base.substringBefore(":")}:$access" else access
            if (isAbsolute(refinedAccess)) return refinedAccess
            if (base.isBlank()) {
                throw MalformedInputException("The base URL should not be empty, or the access URL must be absolute.")
            }
            if (!isAbsolute(base)) {
                throw MalformedInputException("At least one of the base URL or access URL must be absolute.")
            }
            return when {
                refinedAccess.isEmpty() -> base
                refinedAccess.startsWith("/") -> URL(base).copy(path = refinedAccess.normalizeUrl(), query = null).fullUrl
                else -> URL(base).run {
                    val refinedPath = if(refinedAccess.startsWith("?") || refinedAccess.startsWith("#")) {
                        "${path}$refinedAccess"
                    } else {
                        "${path.substringBeforeLast('/')}/$refinedAccess"
                    }
                    copy(path = "/${refinedPath.normalizeUrl().trimStart('/')}", query = null).fullUrl
                }
            }
		}

		fun decodeComponent(s: String, charset: Charset = UTF8, formUrlEncoded: Boolean = false): String {
			val bos = ByteArrayBuilder()
			val len = s.length
			var n = 0
			while (n < len) {
				val c = s[n]
				when (c) {
					'%' -> {
						bos.append(s.substr(n + 1, 2).toInt(16).toByte())
						n += 2
					}
					'+' -> if (formUrlEncoded) {
						bos.append(' '.toInt().toByte())
					} else {
						bos.append('+'.toInt().toByte())
					}
					else -> bos.append(c.toByte())
				}
				n++
			}
			return bos.toByteArray().toString(charset)
		}

		fun encodeComponent(s: String, charset: Charset = UTF8, formUrlEncoded: Boolean = false): String {
			val sb = StringBuilder(s.length)
			val data = s.toByteArray(charset)
			//for (byte c : data) System.out.printf("%02X\n", c & 0xFF);
			for (n in 0 until data.size) {
				val c = data[n]
				val cc = c.toChar()
				when (cc) {
					' ' -> if (formUrlEncoded) sb.append("+") else sb.append("%20")
					in 'a'..'z', in 'A'..'Z', in '0'..'9', '-', '_', '.', '*' -> sb.append(cc)
					else -> {
						sb.append('%')
						for (n in 1 downTo 0) sb.append(Hex.encodeCharUpper(c.toInt().ushr(n * 4) and 0xF))
					}
				}
			}
			return sb.toString()
		}
	}
}

fun createBase64URLForData(data: ByteArray, contentType: String): String {
	return "data:$contentType;base64,${data.toBase64()}"
}

fun String.normalizeUrl(): String {
    // Split with the query string or fragment, whichever comes first,
    // to avoid normalizing query string and fragment
    val paramFlag = this.find { it == '?' || it == '#' } ?: '?'
    val pathParts = this.split(paramFlag).toMutableList()
    pathParts[0] = pathParts[0].pathInfo.normalize(removeEndSlash = false)
    return pathParts.joinToString(paramFlag.toString())
}
