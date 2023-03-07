package com.soywiz.korge.component

import com.soywiz.kds.*
import com.soywiz.korev.*
import com.soywiz.korge.view.*
import com.soywiz.korio.async.*
import com.soywiz.korio.lang.Closeable
import com.soywiz.korio.lang.CancellableGroup

/**
 * **Important**: To use this component you have to call the [Views.registerStageComponent] extension method at the start of the APP.
 *
 * Component with [added] and [removed] methods that are executed
 * once the view is going to be displayed, and when the view has been removed
 */
private class ViewStageComponent(val view: View) {
    val added: Signal<Views> = Signal()
    val removed: Signal<Views> = Signal()
}

private const val __VIEW_STAGE_COMPONENT_NAME = "__viewStageComponent"
private val View.viewStageComponent by Extra.PropertyThis(__VIEW_STAGE_COMPONENT_NAME) { ViewStageComponent(this) }

fun <T : View> T.onNewAttachDetach(onAttach: Views.(T) -> Unit = {}, onDetach: Views.(T) -> Unit = {}): Closeable {
    val view = this
    val closeable = CancellableGroup()
    val viewStageComponent = this.viewStageComponent
    view.deferWithViews { it.registerStageComponent() }
    closeable += viewStageComponent.added.add { onAttach(it, view) }
    closeable += viewStageComponent.removed.add { onDetach(it, view) }
    return closeable
}

fun <T : View> T.onAttachDetach(onAttach: Views.(T) -> Unit = {}, onDetach: Views.(T) -> Unit = {}): T {
    onNewAttachDetach(onAttach, onDetach)
    return this
}

/**
 * Enables the use of [StageComponent] components.
 */
fun Views.registerStageComponent() {
    val EXTRA_ID = "Views.registerStageComponent"
    if (views.getExtra(EXTRA_ID) == true) return
    views.setExtra(EXTRA_ID, true)
    val componentsInStagePrev = FastArrayList<ViewStageComponent>()
    val componentsInStageCur = linkedSetOf<ViewStageComponent>()
    val componentsInStage = linkedSetOf<ViewStageComponent>()
    val tempViews: FastArrayList<View> = FastArrayList()
    onBeforeRender {
        componentsInStagePrev.clear()
        componentsInStagePrev += componentsInStageCur
        componentsInStageCur.clear()

        val stagedViews = getAllDescendantViews(stage, tempViews)

        stagedViews.fastForEach { view ->
            if (view.hasExtra(__VIEW_STAGE_COMPONENT_NAME)) {
                val it = view.viewStageComponent
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
