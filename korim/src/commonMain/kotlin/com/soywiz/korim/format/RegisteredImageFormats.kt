package com.soywiz.korim.format

import com.soywiz.korio.stream.*
import kotlin.native.concurrent.*

@ThreadLocal
//private var RegisteredImageFormats_formats: ImageFormats = ImageFormats(PNG) // Do not register anything to not include inflater if not required
private var RegisteredImageFormats_formats: ImageFormats = ImageFormats(BMP, TGA)

object RegisteredImageFormats : ImageFormat() {
    var formats: ImageFormats
        get() = RegisteredImageFormats_formats
        set(value) { RegisteredImageFormats_formats = value }

    fun register(vararg formats: ImageFormat) {
        this.formats = ImageFormats(this.formats.formats + formats)
    }

    fun unregister(vararg formats: ImageFormat) {
        this.formats = ImageFormats(this.formats.formats - formats)
    }

    inline fun <T> temporalRegister(vararg formats: ImageFormat, callback: () -> T): T {
        val oldFormats = this.formats
        try {
            register(*formats)
            return callback()
        } finally {
            this.formats = oldFormats
        }
    }

    override fun readImage(s: SyncStream, props: ImageDecodingProps): ImageData = formats.readImage(s, props)
    override fun writeImage(image: ImageData, s: SyncStream, props: ImageEncodingProps) = formats.writeImage(image, s, props)
    override fun decodeHeader(s: SyncStream, props: ImageDecodingProps): ImageInfo? = formats.decodeHeader(s, props)
    override fun toString(): String = "RegisteredImageFormats($formats)"
}

