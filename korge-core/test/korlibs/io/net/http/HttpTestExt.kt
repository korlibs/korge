package korlibs.io.net.http

import korlibs.io.lang.UTF8
import korlibs.io.lang.toString
import korlibs.io.stream.readAll

suspend fun HttpBodyContent.toDebugString(): String {
    return "$contentType\n${this.createAsyncStream().readAll().toString(UTF8)}"
}
