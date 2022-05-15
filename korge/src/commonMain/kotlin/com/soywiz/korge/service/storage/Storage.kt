package com.soywiz.korge.service.storage

import com.soywiz.korge.view.Views

//@Singleton
open class Storage(views: Views) : IStorage by NativeStorage(views)
