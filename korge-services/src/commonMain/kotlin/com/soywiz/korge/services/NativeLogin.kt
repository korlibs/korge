package com.soywiz.korge.services

import com.soywiz.korge.service.ServiceBaseId
import com.soywiz.korge.view.Views

abstract class NativeLogin(val views: Views) {
    open suspend fun login() {
    }
}

expect fun CreateNativeLogin(views: Views): NativeLogin
