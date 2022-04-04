package com.soywiz.korio.net.http

import com.soywiz.korio.file.*
import com.soywiz.korio.lang.*
import com.soywiz.korio.net.*
import com.soywiz.korio.stream.*
import com.soywiz.korio.util.*

interface HttpBodyContent {
    val contentType: String
    suspend fun createAsyncStream(): AsyncInputStreamWithLength

    companion object {
        operator fun invoke(contentType: String, content: ByteArray): HttpBodyContent = HttpBodyContent(contentType) { content.openAsync() }
        operator fun invoke(contentType: String, content: String, charset: Charset = UTF8): HttpBodyContent = HttpBodyContent(contentType) { content.openAsync(charset) }
        operator fun invoke(contentType: String, build: suspend () -> AsyncInputStreamWithLength): HttpBodyContent = object : HttpBodyContent {
            override val contentType: String = contentType
            override suspend fun createAsyncStream(): AsyncInputStreamWithLength = build()
        }
    }
}

class HttpBodyContentFormUrlEncoded(val items: List<Pair<String, String>>) : HttpBodyContent {
    constructor(vararg items: Pair<String, String>) : this(items.toList())
    override val contentType: String = "application/x-www-form-urlencoded"
    override suspend fun createAsyncStream(): AsyncInputStreamWithLength =
        FormUrlEncoded.encode(items).openAsync(Charsets.UTF8)
}

class HttpBodyContentMultiPartFormData(
    val boundary: String = "---------------------------${UUID.randomUUID()}",
) : HttpBodyContent {
    override val contentType: String = "multipart/form-data; boundary=$boundary"

    inner class Entry(val name: String, val size: Long, val getContent: suspend () -> AsyncInputStreamWithLength, val fileName: String? = null, val contentType: String? = null) {
        val headerString = buildList<String> {
            add("--$boundary")
            add(buildString {
                append("Content-Disposition: form-data")
                // @TODO: QUOTE and ensure we don't have line breaks
                append("; name=\"$name\"")
                if (fileName != null) {
                    // @TODO: QUOTE and ensure we don't have line breaks
                    append("; filename=\"$fileName\"")
                }
            })
            if (contentType != null) {
                add("Content-Type: $contentType")
            }
        }.joinToString("\n") + "\n\n"
        val headerBytes = headerString.toByteArray()
        val headerStream = headerBytes.openAsync()
    }

    private val entries = arrayListOf<Entry>()

    fun add(name: String, size: Long, getContent: suspend () -> AsyncInputStreamWithLength, fileName: String? = null, contentType: String? = null): HttpBodyContentMultiPartFormData {
        entries.add(Entry(name, size, getContent, fileName, contentType))
        return this
    }

    fun add(name: String, content: ByteArray, fileName: String? = null, contentType: String? = null): HttpBodyContentMultiPartFormData {
        return add(name, content.size.toLong(), { content.openAsync() }, fileName, contentType)
    }

    fun add(name: String, content: String, contentCharset: Charset = UTF8, fileName: String? = null, contentType: String? = null): HttpBodyContentMultiPartFormData {
        return add(name, content.toByteArray(contentCharset), fileName, contentType)
    }

    suspend fun add(name: String, content: AsyncStream, fileName: String? = null, contentType: String? = null): HttpBodyContentMultiPartFormData {
        return add(name, content.getLength(), { content.sliceStart(closeParent = false) }, fileName, contentType)
    }

    suspend fun add(name: String, file: VfsFile, fileName: String? = file.baseName, contentType: String? = fileName?.let { MimeType.getByExtensionOrNull(PathInfo(fileName).extensionLC)?.mime }) {
        val size = file.size()
        add(name, size, { file.openRead().slice(0L until size) }, fileName, contentType)
    }

    override suspend fun createAsyncStream(): AsyncInputStreamWithLength = buildList<AsyncInputStreamWithLength> {
        for ((index, entry) in entries.withIndex()) {
            if (index != 0) {
                add("\n".openAsync())
            }
            add(entry.headerStream.sliceStart())
            add(entry.getContent())
        }
        add("\n--$boundary--".openAsync())
    }.combine()
}
