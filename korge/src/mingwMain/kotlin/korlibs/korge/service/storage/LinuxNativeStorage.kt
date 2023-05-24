package korlibs.korge.service.storage

import korlibs.korge.view.*

actual class NativeStorage actual constructor(views: Views) : IStorageWithKeys, DefaultNativeStorage(views)
