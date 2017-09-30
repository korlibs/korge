package com.soywiz.korge.service.storage

import com.soywiz.korio.inject.Singleton

@Singleton
class Storage : IStorage by NativeStorage
