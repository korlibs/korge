package com.soywiz.korge.view.property

import com.soywiz.korio.async.*
import com.soywiz.korio.experimental.*
import kotlin.reflect.*

/**
 * Used by the debugger to make a property to appear in the debug panel.
 */
@Suppress("unused")
@SuppressIfAnnotated
annotation class ViewProperty(
    val min: Double = 0.0,
    val max: Double = 2000.0,
    val clampMin: Boolean = false,
    val clampMax: Boolean = false,
    val decimalPlaces: Int = 2,
    val groupName: String = "",
    val order: Int = 0,
    val name: String = "",
    val editable: Boolean = true,
)

annotation class ViewPropertyProvider(val provider: KClass<out Any>)
annotation class ViewPropertyFileRef(val extensions: Array<String>)
annotation class ViewPropertySubTree()
