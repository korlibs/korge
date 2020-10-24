package com.soywiz.korge.resources

import com.soywiz.korinject.*
import com.soywiz.korio.resources.*

suspend fun resources(): Resources = injector().get()
