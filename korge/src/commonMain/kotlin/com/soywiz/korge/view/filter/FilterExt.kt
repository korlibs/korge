package com.soywiz.korge.view.filter

import com.soywiz.kds.*
import com.soywiz.korge.debug.*
import com.soywiz.korge.view.*
import com.soywiz.korio.lang.*
import com.soywiz.korui.*

var Views.registerFilterSerialization: Boolean by Extra.Property { false }
fun ViewsContainer.registerFilterSerialization() {
    if (views.registerFilterSerialization) return
    views.registerFilterSerialization = true
    //views.serializer.registerExtension(PhysicsKTreeSerializerExtension)
    views.viewExtraBuildDebugComponent.add { views, view, container ->
        val contentContainer = container.container {  }
        fun updateContentContainer() {
            contentContainer.removeChildren()
            val filter = view.filter
            if (filter != null) {
                val filters = filter.allFilters
                for (filter in filters) {
                    contentContainer.uiCollapsableSection(filter::class.simpleName) {
                        button("Remove Filter") {
                            view.removeFilter(filter)
                            updateContentContainer()
                        }
                        filter.buildDebugComponent(views, this)
                    }
                }
            }
            contentContainer.button("Add filter") {
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
            }
            contentContainer.root?.relayout()
            contentContainer.root?.repaintAll()
        }
        updateContentContainer()
    }
}

private inline fun <reified T : Filter> filterFactory(noinline block: () -> T): Pair<String, () -> T> {
    return (T::class.simpleName ?: "Unknown") to block
}
