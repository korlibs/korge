package korlibs.korge.view

import korlibs.event.*

open class BaseView : BaseEventListener() {
    val baseParent: BaseView? get() = eventListenerParent as? BaseView?

    open fun invalidateRender() {
    }
}
