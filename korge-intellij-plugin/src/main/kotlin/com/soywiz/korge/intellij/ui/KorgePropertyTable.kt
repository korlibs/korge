package com.soywiz.korge.intellij.ui

import com.intellij.ui.table.*
import javax.swing.table.*
import kotlin.reflect.*
import kotlin.reflect.jvm.*

class KorgePropertyTable(var props: Properties) : JBTable() {
	private val myColumnNames = arrayOf("Property", "Value")

	init {
		model = object : AbstractTableModel() {
			override fun getColumnName(column: Int): String = myColumnNames[column]

			override fun getRowCount(): Int = props.props.size

			override fun getColumnCount(): Int = 2

			override fun getValueAt(row: Int, col: Int): Any? {
				return when (col) {
					0 -> props.props[row].name
					else -> props.props[row].get()
				}
			}

			override fun isCellEditable(row: Int, col: Int): Boolean {
				return col == 1
			}

			override fun setValueAt(value: Any?, row: Int, col: Int) {
				if (col == 1) {
					val prop = (props.props[row] as KMutableProperty0<Any?>)
					val clazz = (prop.returnType.javaType as Class<*>).kotlin
					when (clazz.javaObjectType) {
						Int::class.javaObjectType -> prop.set(value.toString().toIntOrNull() ?: 0)
						Float::class.javaObjectType -> prop.set(value.toString().toFloatOrNull() ?: 0f)
						Double::class.javaObjectType -> prop.set(value.toString().toDoubleOrNull() ?: 0.0)
						String::class.javaObjectType -> prop.set(value.toString())
						else -> prop.set(value)
					}
				}
			}
		}
		setShowColumns(true)
		getTableHeader().reorderingAllowed = false;
	}

	class Properties {
		val props: MutableList<KMutableProperty0<*>> = arrayListOf()
		fun register(prop: KMutableProperty0<*>) = this.apply {
			props.add(prop)
		}

		fun register(vararg props: KMutableProperty0<*>) = apply { this@Properties.props.addAll(props) }
	}
}