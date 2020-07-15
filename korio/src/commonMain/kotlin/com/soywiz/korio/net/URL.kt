package com.soywiz.korio.net

import com.soywiz.kmem.*
import com.soywiz.korio.util.*
import com.soywiz.korio.file.*
import com.soywiz.korio.lang.*
import com.soywiz.korio.util.encoding.*

data class URL private constructor(
	val isOpaque: Boolean,
	val scheme: String?,
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
		return "URL(" + listOf(::scheme, ::userInfo, ::host, ::path, ::query, ::fragment)
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

		operator fun invoke(
			scheme: String?,
			userInfo: String?,
			host: String?,
			path: String,
			query: String?,
			fragment: String?,
			opaque: Boolean = false,
			port: Int = DEFAULT_PORT
		): URL = URL(opaque, scheme, userInfo, host, path, query, fragment, port)

		private val schemeRegex = Regex("\\w+:")

		operator fun invoke(url: String): URL {
			val r = StrReader(url)
			val schemeColon = r.tryRegex(schemeRegex)
			return when {
				schemeColon != null -> {
					val isHierarchical = r.tryLit("//") != null
					val nonScheme = r.readRemaining()
					val scheme = schemeColon.dropLast(1)

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

					URL(
						opaque = !isHierarchical,
						scheme = scheme,
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
					URL(
						opaque = false,
						scheme = null,
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

		fun resolve(base: String, access: String): String = when {
			isAbsolute(access) -> access
			access.startsWith("/") -> URL(base).copy(path = access).fullUrl
			else -> URL(base).run { copy(path = "/${("${path.substringBeforeLast('/')}/$access").pathInfo.normalize().trimStart('/')}").fullUrl }
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
						for (n in 1 downTo 0) sb.append(Hex.DIGITS_UPPER[c.toInt().ushr(n * 4) and 0xF])
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
