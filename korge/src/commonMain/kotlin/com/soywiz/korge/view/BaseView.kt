package com.soywiz.korge.view

import com.soywiz.korev.*

open class BaseView : BaseEventListener() {
    val baseParent: BaseView? get() = eventListenerParent as? BaseView?

    open fun invalidateRender() {
    }
}
