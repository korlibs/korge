package com.soywiz.korge.debug

import com.soywiz.korio.async.*
import kotlin.reflect.*

sealed class EditableNode {
    open fun getAllBaseEditableProperty(): List<BaseEditableProperty<*>> = listOf()
}

class EditableNodeList(val list: List<EditableNode>) : EditableNode() {
    constructor(vararg list: EditableNode) : this(list.toList())
    constructor(block: MutableList<EditableNode>.() -> Unit) : this(ArrayList<EditableNode>().apply(block))
    override fun getAllBaseEditableProperty(): List<BaseEditableProperty<*>> = this.list.flatMap { it.getAllBaseEditableProperty() }
}

class EditableSection(val title: String, val list: List<EditableNode>) : EditableNode() {
    constructor(title: String, vararg list: EditableNode) : this(title, list.toList())
    constructor(title: String, block: MutableList<EditableNode>.() -> Unit) : this(title, ArrayList<EditableNode>().apply(block))
    override fun getAllBaseEditableProperty(): List<BaseEditableProperty<*>> = this.list.flatMap { it.getAllBaseEditableProperty() }
}

abstract class BaseEditableProperty<T : Any>(initialValue: T) : EditableNode() {
    abstract val name: String
    abstract val clazz: KClass<T>
    val onChange = Signal<T>()
    //var prev: T = initialValue
    var value: T = initialValue
        set(newValue) {
            //this.prev = this.value
            if (field != newValue) {
                onChange(newValue)
                field = newValue
            }
        }
    override fun getAllBaseEditableProperty(): List<BaseEditableProperty<*>> = listOf(this)
}

data class InformativeProperty<T : Any>(
    val name: String,
    val value: T
) : EditableNode() {
    override fun getAllBaseEditableProperty(): List<BaseEditableProperty<*>> = listOf()
}

data class EditableEnumerableProperty<T : Any>(
    override val name: String,
    override val clazz: KClass<T>,
    val initialValue: T,
    var supportedValues: Set<T>
) : BaseEditableProperty<T>(initialValue) {
    val onUpdateSupportedValues = Signal<Set<T>>()

    fun updateSupportedValues(set: Set<T>) {
        supportedValues = set
        onUpdateSupportedValues(set)
    }
}

data class EditableNumericProperty<T : Number>(
    override val name: String,
    override val clazz: KClass<T>,
    val initialValue: T,
    val minimumValue: T? = null,
    val maximumValue: T? = null,
    val step: T? = null,
    val supportOutOfRange: Boolean = false
) : BaseEditableProperty<T>(initialValue) {
}

data class EditableButtonProperty(
    val title: String,
    val callback: () -> Unit
) : EditableNode() {
}
