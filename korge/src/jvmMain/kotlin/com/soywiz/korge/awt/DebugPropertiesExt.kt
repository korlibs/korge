package com.soywiz.korge.awt

import com.soywiz.korge.debug.*
import com.soywiz.korio.async.*
import java.awt.*
import javax.swing.*
import javax.swing.border.*
import kotlin.coroutines.*

val INDENTATION_SIZE = 24

fun EditableNode.toComponent(context: CoroutineContext, indentation: Int = 0): Component = when (this) {
    is EditableNodeList -> {
        JPanel().also {
            it.layout = BoxLayout(it, BoxLayout.Y_AXIS)
            for (item in list) it.add(item.toComponent(context, indentation))
        }
    }
    is EditableSection -> {
        Section(indentation, this.title, this.list.map { it.toComponent(context, indentation + 1) })
    }
    is InformativeProperty<*> -> {
        JPanel(GridLayout(1, 2)).also {
            it.maximumSize = Dimension(1024, 32)
            it.add(JLabel(name).also { it.border = CompoundBorder(it.border, EmptyBorder(10, 10 + INDENTATION_SIZE * indentation, 10, 10)) })
            it.add(JLabel(value.toString()).also { it.border = CompoundBorder(it.border, EmptyBorder(10, 10, 10, 10)) })
        }
    }
    is EditableEnumerableProperty<*> -> {
        EditableListValue(context, this as EditableEnumerableProperty<Any>, indentation)
    }
    is EditableNumericProperty<*> -> {
        EditableNumberValue(context, this, indentation)
    }
    is EditableButtonProperty -> {
        JButton(title).apply {
            addActionListener {
                launchImmediately(context) {
                    callback()
                }
            }
        }
    }
    is BaseEditableProperty<*> -> TODO()
}
