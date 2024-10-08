package korlibs.korge.gradle.util

import com.google.gson.*
import com.google.gson.JsonParser
import groovy.json.*
import java.net.*
import java.util.*
import java.util.concurrent.*

open class SimpleHttpClient(
    val user: String? = null,
    val pass: String? = null
) {
    open fun requestWithRetry(url: String, body: Any? = null, nretries: Int = 15): JsonElement {
        var retryCount = 0
        while (true) {
            try {
                return request(url, body)
            } catch (e: SimpleHttpException) {
                when (e.responseCode) {
                    in 500..599 -> { // Sometimes  HTTP Error 502 Bad Gateway
                        e.printStackTrace()
                        retryCount++
                        if (retryCount >= nretries) throw RuntimeException("Couldn't access $url after $nretries retries :: ${e.responseCode} : ${e.message}", e)
                        println("Retrying... retryCount=$retryCount/$nretries")
                        Thread.sleep(15_000L + (retryCount * 5_000L))
                        continue
                    }
                    else -> {
                        throw e
                    }
                }
            }
        }
    }

    open fun request(url: String, body: Any? = null): JsonElement {
        val post = (URL(url).openConnection()) as HttpURLConnection
        post.connectTimeout = 300 * 1000 // 300 seconds // 5 minutes
        post.readTimeout = 300 * 1000 // 300 seconds // 5 minutes
        post.requestMethod = (if (body != null) "POST" else "GET")
        if (user != null && pass != null) {
            val authBasic = Base64.getEncoder().encodeToString("${user}:${pass}".toByteArray(Charsets.UTF_8))
            post.setRequestProperty("Authorization", "Basic $authBasic")
        }
        post.setRequestProperty("Accept", "application/json")
        if (body != null) {
            post.doOutput = true
            post.setRequestProperty("Content-Type", "application/json")
            val bodyText = if (body is String) body.toString() else JsonOutput.toJson(body)
            //println(bodyText)
            post.outputStream.write(bodyText.toByteArray(Charsets.UTF_8))
        }
        val postRC = post.responseCode
        val postMessage = post.responseMessage
        //println(postRC)
        if (postRC < 400) {
            return JsonParser.parseString(post.inputStream.reader(Charsets.UTF_8).readText())
        } else {
            val errorString = try {
                post.errorStream?.reader(Charsets.UTF_8)?.readText()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                null
            }
            throw SimpleHttpException(postRC, postMessage, url, errorString)
        }
    }
}

class SimpleHttpException(val responseCode: Int, val responseMessage: String, val url: String, val errorString: String?) :
    RuntimeException("HTTP Error $responseCode $responseMessage - $url - $errorString")
