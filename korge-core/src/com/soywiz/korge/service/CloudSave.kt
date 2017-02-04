package com.soywiz.korge.service

import com.soywiz.korio.error.unsupported
import java.util.*

open class CloudSave protected constructor() {
    companion object {
        operator fun invoke() = ServiceLoader.load(CloudSave::class.java).firstOrNull() ?: unsupported("Not ${CloudSave::class.java.name} implementation found")
    }
}