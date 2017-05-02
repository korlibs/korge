package com.soywiz.korge.intellij

import com.intellij.openapi.fileTypes.FileTypeConsumer
import com.intellij.openapi.fileTypes.FileTypeFactory
import com.intellij.openapi.fileTypes.UserBinaryFileType

class KorgeFileTypeFactory : FileTypeFactory() {
	companion object {
		val KORGE_ANI = UserBinaryFileType()
		val KORGE_AUDIO = UserBinaryFileType()
	}

	override fun createFileTypes(fileTypeConsumer: FileTypeConsumer) {
		fileTypeConsumer.consume(KORGE_ANI, "swf;ani")
		fileTypeConsumer.consume(KORGE_AUDIO, "wav;mp3;ogg")
	}
}
