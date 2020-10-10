package com.soywiz.korui.native.util

import com.soywiz.kds.*
import com.soywiz.korui.*
import java.awt.*
import javax.swing.*

private val standardCursorToAwt = mapOf(
    UiStandardCursor.DEFAULT to Cursor.DEFAULT_CURSOR,
    UiStandardCursor.CROSSHAIR to Cursor.CROSSHAIR_CURSOR,
    UiStandardCursor.TEXT to Cursor.TEXT_CURSOR,
    UiStandardCursor.HAND to Cursor.HAND_CURSOR,
    UiStandardCursor.MOVE to Cursor.MOVE_CURSOR,
    UiStandardCursor.WAIT to Cursor.WAIT_CURSOR,
    UiStandardCursor.RESIZE_EAST to Cursor.E_RESIZE_CURSOR,
    UiStandardCursor.RESIZE_WEST to Cursor.W_RESIZE_CURSOR,
    UiStandardCursor.RESIZE_SOUTH to Cursor.S_RESIZE_CURSOR,
    UiStandardCursor.RESIZE_NORTH to Cursor.N_RESIZE_CURSOR,
    UiStandardCursor.RESIZE_NORTH_EAST to Cursor.NE_RESIZE_CURSOR,
    UiStandardCursor.RESIZE_NORTH_WEST to Cursor.NW_RESIZE_CURSOR,
    UiStandardCursor.RESIZE_SOUTH_EAST to Cursor.SE_RESIZE_CURSOR,
    UiStandardCursor.RESIZE_SOUTH_WEST to Cursor.SW_RESIZE_CURSOR
)
private val standardCursorToAwtRev = standardCursorToAwt.flip()

fun UiCursor?.toAwt(): Cursor {
    return when (this) {
        null -> Cursor(Cursor.DEFAULT_CURSOR)
        is UiStandardCursor -> Cursor(standardCursorToAwt[this] ?: Cursor.DEFAULT_CURSOR)
        else -> {
            TODO()
        }
    }
}
