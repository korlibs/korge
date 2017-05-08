package com.soywiz.korge.service.storage

import com.soywiz.korio.inject.Singleton
import com.soywiz.korio.service.Services

@Singleton
class Storage : IStorage by Services.load(StorageBase::class.java)
