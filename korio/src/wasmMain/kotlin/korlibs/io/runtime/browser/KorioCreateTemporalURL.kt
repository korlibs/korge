package korlibs.io.runtime.browser

import korlibs.io.util.*
import org.w3c.dom.url.*
import org.w3c.files.*
import korlibs.io.wasm.jsArrayOf

fun createURLForData(data: ByteArray, contentType: String): String {
	return URL.createObjectURL(Blob(jsArrayOf(data.toInt8Array()).unsafeCast(), BlobPropertyBag(contentType)))
}

fun revokeRLForData(url: String) {
	URL.revokeObjectURL(url)
}

inline fun <T> createTemporalURLForData(data: ByteArray, contentType: String, callback: (url: String) -> T): T {
	val blobURL = createURLForData(data, contentType)
	try {
		return callback(blobURL)
	} finally {
		revokeRLForData(blobURL)
	}
}
