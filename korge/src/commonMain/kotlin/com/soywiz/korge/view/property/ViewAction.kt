package com.soywiz.korge.view.property

import com.soywiz.korge.view.*

/**
 * Used by the debugger to make a button to appear
 */
//annotation class ViewAction
class ViewAction(val name: String, val action: Views.(instance: Any) -> Unit)

class ViewActionList(val actions: List<ViewAction>) {
    constructor(vararg actions: ViewAction) : this(actions.toList())
}
