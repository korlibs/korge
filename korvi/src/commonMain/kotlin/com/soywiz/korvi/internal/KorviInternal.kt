package com.soywiz.korvi.internal

import com.soywiz.klock.minutes
import com.soywiz.korio.file.VfsFile
import com.soywiz.korio.stream.AsyncStream
import com.soywiz.korvi.DummyKorviVideoLL
import com.soywiz.korvi.KorviVideo
import com.soywiz.korvi.KorviVideoFromLL
import com.soywiz.korvi.KorviVideoLL

internal expect val korviInternal: KorviInternal

internal open class KorviInternal {
    open suspend fun createHighLevel(file: VfsFile): KorviVideo = KorviVideoFromLL(createContainer(file.open()))
    open fun createContainer(stream: AsyncStream): KorviVideoLL = DummyKorviVideoLL(3.minutes)
}
