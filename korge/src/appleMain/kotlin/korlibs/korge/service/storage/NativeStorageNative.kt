package korlibs.korge.service.storage

import korlibs.io.lang.*
import korlibs.korge.native.*
import korlibs.korge.view.*

open class DefaultNativeStorage(views: Views) : FiledBasedNativeStorage(views) {
    override fun mkdirs(folder: String) = KorgeSimpleNativeSyncIO.mkdirs(folder)
    override fun saveStr(data: String) = KorgeSimpleNativeSyncIO.writeBytes(gameStorageFile, data.toByteArray(UTF8))
    override fun loadStr(): String = KorgeSimpleNativeSyncIO.readBytes(gameStorageFile).toString(UTF8)
}
