package com.soywiz.korge.intellij

import com.intellij.ide.highlighter.XmlFileType
import com.intellij.javaee.ExternalResourceManager
import com.intellij.openapi.fileTypes.FileTypeConsumer
import com.intellij.openapi.fileTypes.FileTypeFactory
import com.intellij.openapi.fileTypes.UserBinaryFileType

class KorgeFileTypeFactory : FileTypeFactory() {
	companion object {
		val KORGE_ANI = UserBinaryFileType()
		val KORGE_AUDIO = UserBinaryFileType()
		val KORGE_TMX = XmlFileType.INSTANCE
		val KORGE_PEX = XmlFileType.INSTANCE
	}

	override fun createFileTypes(fileTypeConsumer: FileTypeConsumer) {
		fileTypeConsumer.consume(KORGE_ANI, "swf;ani")
		fileTypeConsumer.consume(KORGE_AUDIO, "wav;mp3;ogg")
		fileTypeConsumer.consume(KORGE_TMX, "tmx")
		fileTypeConsumer.consume(KORGE_PEX, "pex")
	}
}
