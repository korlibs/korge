package com.soywiz.korio

import org.w3c.dom.url.*
import org.w3c.files.*

fun createURLForData(data: ByteArray, contentType: String): String {
	return URL.createObjectURL(Blob(arrayOf(data), BlobPropertyBag(contentType)))
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
