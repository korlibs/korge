package com.soywiz.korge.intellij.module

import com.intellij.openapi.module.*
import com.soywiz.korge.intellij.*
import javax.swing.*

//open class KorgeModuleType : JpsJavaModuleType("korge") {
//open class KorgeModuleType : JavaModuleType("korge") {
open class KorgeModuleType : ModuleType<KorgeModuleBuilder>("korge") {
	//open class KorgeModuleType : EmptyModuleType("korge") {
	companion object {
		val INSTANCE = KorgeModuleType()
		val NAME = "Korge"
		val DESCRIPTION = "KorGE Game Engine"
		val BIG_ICON = KorgeIcons.KORGE
		val ICON = KorgeIcons.KORGE
	}

	override fun createModuleBuilder() = KorgeModuleBuilder()
	override fun getName(): String = NAME
	override fun getDescription(): String = DESCRIPTION
	override fun getIcon(): Icon = BIG_ICON
	override fun getNodeIcon(isOpened: Boolean): Icon = ICON
}
