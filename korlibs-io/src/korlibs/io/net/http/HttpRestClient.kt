package korlibs.io.net.http

import korlibs.io.lang.*
import korlibs.io.serialization.json.*
import korlibs.io.stream.*

class HttpRestClient(val endpoint: HttpClientEndpoint) {
	suspend fun request(method: Http.Method, path: String, contentJsonString: String?): Any {
		val result = endpoint.request(
			method,
			path,
			content = contentJsonString?.openAsync(),
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
	suspend fun put(path: String, contentJsonString: String): Any = request(Http.Method.PUT, path, contentJsonString)
    suspend fun post(path: String, contentJsonString: String): Any = request(Http.Method.POST, path, contentJsonString)
    suspend fun put(path: String, content: Any?): Any = put(path, contentJsonString = Json.stringify(content))
    suspend fun post(path: String, content: Any?): Any = post(path, contentJsonString = Json.stringify(content))
}

fun HttpClientEndpoint.rest() = HttpRestClient(this)
fun HttpClient.rest(endpoint: String) = HttpRestClient(this.endpoint(endpoint))
