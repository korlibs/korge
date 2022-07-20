package com.soywiz.korge.view.filter

import com.soywiz.kds.Extra
import com.soywiz.korge.debug.uiCollapsibleSection
import com.soywiz.korge.view.Views
import com.soywiz.korge.view.ViewsContainer
import com.soywiz.korui.UiMenuItem
import com.soywiz.korui.button
import com.soywiz.korui.container
import kotlin.native.concurrent.ThreadLocal

@ThreadLocal
var Views.registerFilterSerialization: Boolean by Extra.Property { false }
fun ViewsContainer.registerFilterSerialization() {
    if (views.registerFilterSerialization) return
    views.registerFilterSerialization = true
    views.viewExtraBuildDebugComponent.add { views, view, container ->
        val contentContainer = container.container {  }
        fun updateContentContainer() {
            contentContainer.removeChildren()
            val filter = view.filter
            if (filter != null) {
                val filters = filter.allFilters
                for (filter in filters) {
                    contentContainer.uiCollapsibleSection(filter::class.simpleName) {
                        button("Remove Filter").onClick {
                            view.removeFilter(filter)
                            updateContentContainer()
                        }
                        filter.buildDebugComponent(views, this)
                    }
                }
            }
            contentContainer.button("Add filter", onClick = {
                val button = this

                button.showPopupMenu(listOf(
                    filterFactory { BlurFilter() },
                    filterFactory { ColorMatrixFilter(ColorMatrixFilter.GRAYSCALE_MATRIX) },
                    filterFactory { Convolute3Filter(Convolute3Filter.KERNEL_EDGE_DETECTION) },
                    filterFactory { IdentityFilter },
                    filterFactory { PageFilter() },
                    filterFactory { SwizzleColorsFilter() },
                    filterFactory { WaveFilter() },
                ).map { (name, filterFactory) ->
                    UiMenuItem(name) {
                        view.addFilter(filterFactory())
                        updateContentContainer()
                    }
                })
            })
            contentContainer.root?.relayout()
            contentContainer.root?.repaintAll()
        }
        updateContentContainer()
    }
}

private inline fun <reified T : Filter> filterFactory(noinline block: () -> T): Pair<String, () -> T> {
    return (T::class.simpleName ?: "Unknown") to block
}
