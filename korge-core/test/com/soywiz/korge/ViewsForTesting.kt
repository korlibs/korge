package com.soywiz.korge

import com.soywiz.korag.log.LogAG
import com.soywiz.korge.input.Input
import com.soywiz.korge.view.Views
import com.soywiz.korio.inject.AsyncInjector

open class ViewsForTesting {
    val injector = AsyncInjector()
    val ag = LogAG()
    val input = Input()
    val views = Views(ag, injector, input)
}