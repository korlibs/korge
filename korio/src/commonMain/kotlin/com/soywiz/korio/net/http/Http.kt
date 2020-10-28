package com.soywiz.korio.net.http

import com.soywiz.klock.*
import com.soywiz.korio.lang.*
import com.soywiz.korio.net.*
import com.soywiz.korio.util.encoding.*
import com.soywiz.krypto.encoding.*

interface Http {
	companion object {
		//Sun, 06 Nov 1994 08:49:37 GMT  ; RFC 822, updated by RFC 1123
		//Sunday, 06-Nov-94 08:49:37 GMT ; RFC 850, obsoleted by RFC 1036
		//Sun Nov  6 08:49:37 1994       ; ANSI C's asctime() format
		val Date = DateFormat("EEE, dd MMM yyyy HH:mm:ss z")

		fun TemporalRedirect(uri: String) = RedirectException(code = 307, redirectUri = uri)
		fun PermanentRedirect(uri: String) = RedirectException(code = 301, redirectUri = uri)
	}

	enum class Methods : Method {
		ALL,
		OPTIONS,
		GET,
		HEAD,
		POST,
		PUT,
		DELETE,
		TRACE,
		CONNECT,
		PATCH,
	}

	interface Method {
		val name: String

		companion object {
			val OPTIONS = Methods.OPTIONS
			val GET = Methods.GET
			val HEAD = Methods.HEAD
			val POST = Methods.POST
			val PUT = Methods.PUT
			val DELETE = Methods.DELETE
			val TRACE = Methods.TRACE
			val CONNECT = Methods.CONNECT
			val PATCH = Methods.PATCH

			val _values = listOf(OPTIONS, GET, HEAD, POST, PUT, DELETE, TRACE, CONNECT, PATCH)
			fun values() = _values
			val valuesMap = _values.map { it.name to it }.toMap()

			operator fun get(name: String): Method =
				valuesMap.getOrElse(name.toUpperCase().trim()) { CustomMethod(name) }

			operator fun invoke(name: String): Method = this[name]
		}
	}

	data class CustomMethod(val _name: String) : Method {
		val nameUC = _name.trim().toUpperCase()
		override val name get() = nameUC
		override fun toString(): String = nameUC
	}

	open class HttpException(
		val statusCode: Int,
		val msg: String = "Error$statusCode",
		val statusText: String = HttpStatusMessage.CODES[statusCode] ?: "Error$statusCode",
		val headers: Http.Headers = Http.Headers()
	) : IOException("$statusCode $statusText - $msg") {
		companion object {
			fun unauthorizedBasic(realm: String = "Realm", msg: String = "Unauthorized"): Nothing =
				throw Http.HttpException(
					401,
					msg = msg,
					headers = Http.Headers("WWW-Authenticate" to "Basic realm=\"$realm\"")
				)
			//fun unauthorizedDigest(realm: String = "My Domain", msg: String = "Unauthorized"): Nothing = throw Http.HttpException(401, msg = msg, headers = Http.Headers("WWW-Authenticate" to "Digest realm=\"$realm\""))
		}
	}

	data class Auth(
		val user: String,
		val pass: String,
		val digest: String
	) {
		companion object {
			fun parse(auth: String): Auth {
				val parts = auth.split(' ', limit = 2)
				if (parts[0].equals("basic", ignoreCase = true)) {
					val parts = parts[1].fromBase64().toString(UTF8).split(':', limit = 2)
					return Auth(user = parts[0], pass = parts[1], digest = "")
				} else if (parts[0].isEmpty()) {
					return Auth(user = "", pass = "", digest = "")
				} else {
					invalidOp("Just supported basic auth")
				}
			}
		}

		fun validate(expectedUser: String, expectedPass: String, realm: String = "Realm"): Boolean {
			if (this.user == expectedUser && this.pass == expectedPass) return true
			return false
		}

		suspend fun checkBasic(realm: String = "Realm", check: suspend Auth.() -> Boolean) {
			if (user.isEmpty() || !check(this)) Http.HttpException.unauthorizedBasic(
				realm = "Domain",
				msg = "Invalid auth"
			)
		}
	}

	class Request(
		val uri: String,
		val headers: Http.Headers
	) {
		private val parts by lazy { uri.split('?', limit = 2) }
		val path: String by lazy { parts[0] }
		val queryString: String by lazy { parts.getOrElse(1) { "" } }
		val getParams by lazy { QueryString.decode(queryString) }
		val absoluteURI: String by lazy { uri }
	}

	class Response {
		val headers = arrayListOf<Pair<String, String>>()

		fun header(key: String, value: String) {
			headers += key to value
		}
	}

	data class Headers(val items: List<Pair<String, String>>) : Iterable<Pair<String, String>> {
		constructor(vararg items: Pair<String, String>) : this(items.toList())
		constructor(map: Map<String, String>) : this(map.map { it.key to it.value })
		constructor(str: String?) : this(parse(str).items)

		override fun iterator(): Iterator<Pair<String, String>> = items.iterator()

		operator fun get(key: String): String? = getFirst(key)
		fun getAll(key: String): List<String> =
			items.filter { it.first.equals(key, ignoreCase = true) }.map { it.second }

		fun getFirst(key: String): String? = items.firstOrNull { it.first.equals(key, ignoreCase = true) }?.second

		fun toListGrouped(): List<Pair<String, List<String>>> {
			return this.items.groupBy { it.first.toLowerCase() }
				.map { it.value.first().first to it.value.map { it.second } }.sortedBy { it.first.toLowerCase() }
		}

		fun withAppendedHeaders(newHeaders: List<Pair<String, String>>): Headers =
			Headers(this.items + newHeaders.toList())

		fun withReplaceHeaders(newHeaders: List<Pair<String, String>>): Headers {
			val replaceKeys = newHeaders.map { it.first.toLowerCase() }.toSet()
			return Headers(this.items.filter { it.first.toLowerCase() !in replaceKeys } + newHeaders.toList())
		}

		fun withAppendedHeaders(vararg newHeaders: Pair<String, String>): Headers =
			withAppendedHeaders(newHeaders.toList())

		fun withReplaceHeaders(vararg newHeaders: Pair<String, String>): Headers =
			withReplaceHeaders(newHeaders.toList())

        fun withReplaceHeaders(newHeaders: Headers): Headers =
            withReplaceHeaders(newHeaders.toList())

        fun containsAll(other: Http.Headers): Boolean = other.items.all { this.contains(it) }

		operator fun plus(that: Headers): Headers = withAppendedHeaders(that.items)

		override fun toString(): String = "Headers(${toListGrouped().joinToString(", ")})"

        class Builder {
            private val items = arrayListOf<Pair<String, String>>()
            fun put(key: String, value: String) = run { items += key to value }
            fun build() = Headers(items)
        }

        companion object {
            fun build(block: Builder.() -> Unit): Headers = Builder().apply(block).build()

			fun fromListMap(map: Map<String?, List<String>>): Headers {
				return Headers(map.flatMap { pair -> if (pair.key == null) listOf() else pair.value.map { value -> pair.key!! to value } })
			}

			fun parse(str: String?): Headers {
				if (str == null) return Headers()
				return Headers(str.split("\n").map {
					val parts = it.trim().split(':', limit = 2)
					if (parts.size >= 2) parts[0].trim() to parts[1].trim() else null
				}.filterNotNull())
			}

			val ContentLength = "Content-Length"
			val ContentType = "Content-Type"
		}
	}

	data class RedirectException(val code: Int = 307, val redirectUri: String) : Http.HttpException(code, HttpStatusMessage(code))
}
