package com.soywiz.korvi.internal

import com.soywiz.klock.*
import com.soywiz.klock.hr.hr
import com.soywiz.korio.file.VfsFile
import com.soywiz.korio.stream.*
import com.soywiz.korvi.*

internal expect val korviInternal: KorviInternal

internal open class KorviInternal {
    open suspend fun createHighLevel(file: VfsFile): KorviVideo = KorviVideoFromLL(createContainer(file.open()))
    open fun createContainer(stream: AsyncStream): KorviVideoLL = DummyKorviVideoLL(3.minutes)
}
