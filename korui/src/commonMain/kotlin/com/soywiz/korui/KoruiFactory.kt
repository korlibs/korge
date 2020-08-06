package com.soywiz.korui

import com.soywiz.kds.*
import com.soywiz.korev.*
import com.soywiz.korio.lang.*

expect val defaultKoruiFactory: KoruiFactory

object DummyKoruiFactory : KoruiFactory

interface KoruiFactory {
    fun createWindow(): UiWindow = object : UiWindow, Extra by Extra.Mixin() { override val factory = this@KoruiFactory }
    fun createContainer(): UiContainer = object : UiContainer, Extra by Extra.Mixin() { override val factory = this@KoruiFactory }
    fun createButton(): UiButton = object : UiButton, Extra by Extra.Mixin() { override val factory = this@KoruiFactory }
    fun createLabel(): UiLabel = object : UiLabel, Extra by Extra.Mixin() { override val factory = this@KoruiFactory }
    fun createTextField(): UiTextField = object : UiTextField, Extra by Extra.Mixin() { override val factory = this@KoruiFactory }
    fun <T> createComboBox(): UiComboBox<T> = object : UiComboBox<T>, Extra by Extra.Mixin() { override val factory = this@KoruiFactory }
}
