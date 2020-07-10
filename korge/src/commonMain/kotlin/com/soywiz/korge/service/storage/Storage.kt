package com.soywiz.korge.service.storage

import com.soywiz.korge.view.*
import com.soywiz.korinject.*

//@Singleton
open class Storage(views: Views) : IStorage by NativeStorage(views)
