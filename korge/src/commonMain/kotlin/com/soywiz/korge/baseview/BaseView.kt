package com.soywiz.korge.baseview

import com.soywiz.korev.*

interface InvalidateNotifier {
    fun invalidatedView(view: BaseView?)
}

//open class BaseView : BaseEventListener() {
open class BaseView : BaseEventListener() {
    val baseParent: BaseView? get() = eventListenerParent as? BaseView?

    open fun invalidateRender() {
    }
}
