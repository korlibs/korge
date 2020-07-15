package com.soywiz.korio.net.http

import com.soywiz.korio.dynamic.mapper.*
import com.soywiz.korio.dynamic.serialization.*
import com.soywiz.korio.lang.*
import com.soywiz.korio.serialization.json.*
import com.soywiz.korio.stream.*

class HttpRestClient(val endpoint: HttpClientEndpoint) {
	suspend fun request(method: Http.Method, path: String, request: Any?, mapper: ObjectMapper = Mapper): Any {
		val requestContent = request?.let { Json.stringifyTyped(it, mapper) }
		val result = endpoint.request(
			method,
			path,
			content = requestContent?.openAsync(),
			headers = Http.Headers(
				Http.Headers.ContentType to "application/json"
			)
		)
		result.checkErrors()
		val stringResult = result.readAllString()
		//println(stringResult)
		return try {
			Json.parse(stringResult) ?: mapOf<String, String>()
		} catch (e: IOException) {
			mapOf<String, String>()
		}
	}

	suspend fun head(path: String): Any = request(Http.Method.HEAD, path, null)
	suspend fun delete(path: String): Any = request(Http.Method.DELETE, path, null)
	suspend fun get(path: String): Any = request(Http.Method.GET, path, null)
	suspend fun put(path: String, request: Any): Any = request(Http.Method.PUT, path, request)
	suspend fun post(path: String, request: Any): Any = request(Http.Method.POST, path, request)
}

fun HttpClientEndpoint.rest() = HttpRestClient(this)
fun HttpClient.rest(endpoint: String) = HttpRestClient(this.endpoint(endpoint))
fun HttpFactory.createRestClient(endpoint: String, mapper: ObjectMapper) = createClient().endpoint(endpoint).rest()
