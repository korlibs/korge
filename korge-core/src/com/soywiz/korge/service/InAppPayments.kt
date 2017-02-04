package com.soywiz.korge.service

import com.soywiz.korio.error.unsupported
import java.util.*

open class InAppPayments protected constructor() {
    companion object {
        operator fun invoke() = ServiceLoader.load(InAppPayments::class.java).firstOrNull() ?: unsupported("Not ${InAppPayments::class.java.name} implementation found")
    }
}