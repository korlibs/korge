package com.soywiz.korge.intellij.util

import com.intellij.uiDesigner.core.*
import java.awt.*
import java.awt.event.*
import javax.swing.*

data class TdPos(val col: Int, val row: Int)

@DslMarker
annotation class TableTagMarker

@TableTagMarker
class TableBuilder {
	val items = LinkedHashMap<TdPos, Pair<Component, GridConstraints>>()
	var row = 0
	var col = 0

	var fill: TdFill = TdFill.BOTH
	var anchor: TdAlign = TdAlign.CENTER_CENTER
	var hpolicy: TdSize = TdSize.CAN_SHRINK_GROW
	var vpolicy: TdSize = TdSize.CAN_SHRINK_GROW
	var minWidth: Int? = null
	var minHeight: Int? = null
	var maxWidth: Int? = null
	var maxHeight: Int? = null

	val nrows get() = items.values.map { it.second.row + it.second.rowSpan }.max() ?: 0
	val ncols get() = items.values.map { it.second.column + it.second.colSpan }.max() ?: 0
}

fun table(callback: TableBuilder.() -> Unit): JPanel {
	val tb = TableBuilder()
	tb.callback()
	val panel = JPanel(GridLayoutManager(tb.nrows, tb.ncols, Insets(0, 0, 0, 0), 0, 0, false, false))
	for (item in tb.items.values) {
		panel.add(item.first, item.second)
	}
	//add(panel)
	return panel
}

fun TableBuilder.tr(
	fill: TdFill = TdFill.BOTH,
	align: TdAlign = TdAlign.CENTER_CENTER,
	policy: TdSize = TdSize.CAN_SHRINK_GROW,
	hpolicy: TdSize = policy,
	vpolicy: TdSize = policy,
	minWidth: Int? = null,
	maxWidth: Int? = null,
	minHeight: Int? = null,
	maxHeight: Int? = null,
	callback: TableBuilder.() -> Unit
) {
	this.fill = fill
	this.anchor = align
	this.hpolicy = hpolicy
	this.vpolicy = vpolicy
	this.minWidth = minWidth
	this.maxWidth = maxWidth
	this.minHeight = minHeight
	this.maxHeight = maxHeight
	callback()
	row++
	col = 0
}

//fun TableBuilder.td(colspan: Int = 1, rowspan: Int = 1, callback: TableBuilder.() -> Unit) {
//    GridConstraints()
//}

fun <T : Component> TableBuilder.td(
	component: T, colspan: Int = 1, rowspan: Int = 1,
	fill: TdFill? = null,
	anchor: TdAlign? = null,
	hpolicy: TdSize? = null,
	vpolicy: TdSize? = null,
	minWidth: Int? = null,
	maxWidth: Int? = null,
	minHeight: Int? = null,
	maxHeight: Int? = null
): T {
	items[TdPos(col, row)] = Pair(
		component, GridConstraints(
			row, col,
			rowspan, colspan,
			(anchor ?: this.anchor).value,
			(fill ?: this.fill).value,
			(hpolicy ?: this.hpolicy).value,
			(vpolicy ?: this.vpolicy).value,
			Dimension((minWidth ?: this.minWidth) ?: -1, (minHeight ?: this.minHeight) ?: -1),
			Dimension(-1, -1),
			Dimension((maxWidth ?: this.maxWidth) ?: -1, (maxHeight ?: this.maxHeight) ?: -1),
			0,
			false
		)
	)
	col++
	return component
}

enum class TdFill(val value: Int) {
	NONE(GridConstraints.FILL_NONE),
	HORIZONTAL(GridConstraints.FILL_HORIZONTAL),
	VERTICAL(GridConstraints.FILL_VERTICAL),
	BOTH(GridConstraints.FILL_BOTH)
}

enum class TdAlign(val value: Int) {
	TOP_LEFT(GridConstraints.ANCHOR_NORTHWEST),
	TOP_CENTER(GridConstraints.ANCHOR_NORTH),
	TOP_RIGHT(GridConstraints.ANCHOR_NORTHEAST),

	CENTER_LEFT(GridConstraints.ANCHOR_WEST),
	CENTER_CENTER(GridConstraints.ANCHOR_CENTER),
	CENTER_RIGHT(GridConstraints.ANCHOR_EAST),

	BOTTOM_LEFT(GridConstraints.ANCHOR_SOUTHWEST),
	BOTTOM_CENTER(GridConstraints.ANCHOR_SOUTH),
	BOTTOM_RIGHT(GridConstraints.ANCHOR_SOUTHEAST),
}

enum class TdSize(val value: Int) {
	FIXED(GridConstraints.SIZEPOLICY_FIXED),
	CAN_SHRINK(GridConstraints.SIZEPOLICY_CAN_SHRINK),
	CAN_GROW(GridConstraints.SIZEPOLICY_CAN_GROW),
	CAN_SHRINK_GROW(GridConstraints.SIZEPOLICY_CAN_SHRINK or GridConstraints.SIZEPOLICY_CAN_SHRINK),
	WANT_GROW(GridConstraints.SIZEPOLICY_WANT_GROW),
}

val <T> JComboBox<T>.items get() = (0 until this.itemCount).map { getItemAt(it) }

fun <T : JButton> T.onClick(callback: () -> Unit): T = this.apply {
	addMouseListener(object : MouseAdapter() {
		override fun mouseClicked(e: MouseEvent) {
			callback()
		}
	})
}