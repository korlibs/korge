package com.soywiz.korge.component

import com.soywiz.kds.FastArrayList
import com.soywiz.kds.getExtra
import com.soywiz.kds.setExtra
import com.soywiz.klock.TimeSpan
import com.soywiz.korge.baseview.BaseView
import com.soywiz.korge.util.CancellableGroup
import com.soywiz.korge.view.View
import com.soywiz.korge.view.Views
import com.soywiz.korge.view.getAllDescendantViews
import com.soywiz.korio.lang.Closeable

/**
 * **Important**: To use this component you have to call the [Views.registerStageComponent] extension method at the start of the APP.
 *
 * Component with [added] and [removed] methods that are executed
 * once the view is going to be displayed, and when the view has been removed
 */
interface StageComponent : TypedComponent<StageComponent> {
    companion object : ComponentType<StageComponent>
    override val type get() = Companion

    fun added(views: Views)
    fun removed(views: Views)
}

fun <T : View> T.onNewAttachDetach(views: Views? = null, onAttach: Views.(T) -> Unit = {}, onDetach: Views.(T) -> Unit = {}): Closeable {
    val view = this
    val closeable = CancellableGroup()
    if (views != null) {
        views.registerStageComponent()
    } else {
        closeable += view.addComponent(object : UpdateComponentWithViews {
            override val view: BaseView = this@onNewAttachDetach
            override fun update(views: Views, dt: TimeSpan) {
                this.removeFromView()
                views.registerStageComponent()
            }
        })
    }
    closeable += view.addComponent(object : StageComponent {
        override val view: BaseView get() = view
        override fun added(views: Views) {
            onAttach(views, view)
        }
        override fun removed(views: Views) {
            onDetach(views, view)
        }
    })
    return closeable
}

fun <T : View> T.onAttachDetach(views: Views? = null, onAttach: Views.(T) -> Unit = {}, onDetach: Views.(T) -> Unit = {}): T {
    val view = this
    if (views != null) {
        views.registerStageComponent()
    } else {
        view.addComponent(object : UpdateComponentWithViews {
            override val view: BaseView = this@onAttachDetach
            override fun update(views: Views, dt: TimeSpan) {
                this.removeFromView()
                views.registerStageComponent()
            }
        })
    }
    view.addComponent(object : StageComponent {
        override val view: BaseView get() = view
        override fun added(views: Views) {
            onAttach(views, view)
        }
        override fun removed(views: Views) {
            onDetach(views, view)
        }
    })
    return this
}

/**
 * Enables the use of [StageComponent] components.
 */
fun Views.registerStageComponent() {
    val EXTRA_ID = "Views.registerStageComponent"
    if (views.getExtra(EXTRA_ID) == true) return
    views.setExtra(EXTRA_ID, true)
    val componentsInStagePrev = FastArrayList<StageComponent>()
    val componentsInStageCur = linkedSetOf<StageComponent>()
    val componentsInStage = linkedSetOf<StageComponent>()
    val tempViews: FastArrayList<View> = FastArrayList()
    onBeforeRender {
        componentsInStagePrev.clear()
        componentsInStagePrev += componentsInStageCur
        componentsInStageCur.clear()

        val stagedViews = getAllDescendantViews(stage, tempViews)

        stagedViews.fastForEach { view ->
            view.getComponentsOfType(StageComponent)?.fastForEach {
                componentsInStageCur += it
                if (it !in componentsInStage) {
                    componentsInStage += it
                    it.added(views)
                }
            }
        }

        componentsInStagePrev.fastForEach {
            if (it !in componentsInStageCur) {
                it.removed(views)
            }
        }
    }
}
