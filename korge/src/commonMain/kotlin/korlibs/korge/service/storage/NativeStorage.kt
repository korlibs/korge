package korlibs.korge.service.storage

import korlibs.datastructure.Extra
import korlibs.korge.view.Views
import korlibs.korge.view.ViewsContainer
import kotlin.native.concurrent.ThreadLocal

/** Cross-platform way of synchronously storing small data */
expect class NativeStorage(views: Views) : IStorage {
    fun keys(): List<String>
	override fun set(key: String, value: String)
	override fun getOrNull(key: String): String?
	override fun remove(key: String)
	override fun removeAll()
}

fun NativeStorage.toMap() = keys().associateWith { getOrNull(it) }

@ThreadLocal
val Views.storage: NativeStorage by Extra.PropertyThis<Views, NativeStorage> { NativeStorage(this) }

val ViewsContainer.storage: NativeStorage get() = this.views.storage