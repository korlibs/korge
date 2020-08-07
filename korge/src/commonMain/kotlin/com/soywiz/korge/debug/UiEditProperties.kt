package com.soywiz.korge.debug

import com.soywiz.kds.iterators.*
import com.soywiz.korge.animate.*
import com.soywiz.korge.view.*
import com.soywiz.korui.*
import kotlin.coroutines.*

class UiEditProperties(app: UiApplication, view: View?, val views: Views) : UiContainer(app) {
    var nodeTree: EditableNodeList? = null

    fun setView(view: View?, coroutineContext: CoroutineContext) {
        removeChildren()

        if (view != null) {
            if (view is KorgeDebugNode) {
                view.buildDebugComponent(views, this)
            }

            addChild(UiCollapsableSection(app, "View") {
                uiEditableValue(view::name)
                uiEditableValue(view::colorMul)
                uiEditableValue(view::blendMode, values = BlendMode.values().toList())
                uiEditableValue(view::alpha, min = 0.0, max = 1.0, clamp = true)
                uiEditableValue(view::speed, min = -1.0, max = 1.0, clamp = false)
                uiEditableValue(view::ratio, min = 0.0, max = 1.0, clamp = false)
                uiEditableValue(Pair(view::x, view::y), "position", min = -1000.0, max = +1000.0, clamp = false)
                if (view is RectBase) {
                    uiEditableValue(Pair(view::anchorX, view::anchorY), "anchor", min = 0.0, max = 1.0, clamp = false)
                    button("Center") {
                        onClick {
                            view.anchorX = 0.5
                            view.anchorY = 0.5
                        }
                    }
                }
                if (view is AnBaseShape) {
                    uiEditableValue(Pair(view::dxDouble, view::dyDouble), name = "dxy", clamp = false)
                    button("Center") {
                        onClick {
                            view.dx = (-view.width / 2).toFloat()
                            view.dy = (-view.height / 2).toFloat()
                        }
                    }
                }
                uiEditableValue(Pair(view::scaledWidth, view::scaledHeight), "size", min = -1000.0, max = 1000.0, clamp = false)
                uiEditableValue(view::scale, min = 0.0, max = 1.0, clamp = false)
                uiEditableValue(Pair(view::scaleX, view::scaleY), "scaleXY", min = 0.0, max = 1.0, clamp = false)
                uiEditableValue(view::rotationDegrees, min = -360.0, max = +360.0, clamp = true)
                uiEditableValue(Pair(view::skewXDegrees, view::skewYDegrees), "skew", min = -360.0, max = +360.0)
            })
        }

        obsProperties = this.findObservableProperties()

        obsProperties.fastForEach {
            it.onChange { update() }
        }

        root?.updateUI()
        root?.relayout()
        root?.repaintAll()
    }

    private var obsProperties: List<ObservableProperty<*>> = listOf()

    // Calls from time to time to update all the properties
    private var updating = false
    fun update() {
        if (updating) return
        updating = true
        try {
            //println("obsProperties[update]: $obsProperties")
            obsProperties.fastForEach { it.forceRefresh() }
        } finally {
            updating = false
        }
    }

    init {
        setView(view, EmptyCoroutineContext)
    }
}
