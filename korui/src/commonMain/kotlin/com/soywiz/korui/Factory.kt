package com.soywiz.korui

import com.soywiz.kds.*

expect val DEFAULT_UI_FACTORY: UiFactory

object DummyUiFactory : UiFactory

interface UiFactory {
    fun createWindow(): UiWindow = object : UiWindow, Extra by Extra.Mixin() { override val factory = this@UiFactory }
    fun createContainer(): UiContainer = object : UiContainer, Extra by Extra.Mixin() { override val factory = this@UiFactory }
    fun createScrollPanel(): UiScrollPanel = object : UiScrollPanel, Extra by Extra.Mixin() { override val factory = this@UiFactory }
    fun createButton(): UiButton = object : UiButton, Extra by Extra.Mixin() { override val factory = this@UiFactory }
    fun createLabel(): UiLabel = object : UiLabel, Extra by Extra.Mixin() { override val factory = this@UiFactory }
    fun createCheckBox(): UiCheckBox = object : UiCheckBox, Extra by Extra.Mixin() { override val factory = this@UiFactory }
    fun createTextField(): UiTextField = object : UiTextField, Extra by Extra.Mixin() { override val factory = this@UiFactory }
    fun <T> createComboBox(): UiComboBox<T> = object : UiComboBox<T>, Extra by Extra.Mixin() { override val factory = this@UiFactory }
    fun createTree(): UiTree = object : UiTree, Extra by Extra.Mixin() { override val factory = this@UiFactory }
}
