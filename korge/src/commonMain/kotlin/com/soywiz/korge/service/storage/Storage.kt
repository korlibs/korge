package com.soywiz.korge.service.storage

import com.soywiz.korinject.*

//@Singleton
open class Storage : IStorage by NativeStorage
