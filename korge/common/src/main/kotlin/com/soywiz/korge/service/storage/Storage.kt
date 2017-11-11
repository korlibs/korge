package com.soywiz.korge.service.storage

import com.soywiz.korinject.Singleton

@Singleton
class Storage : IStorage by NativeStorage
