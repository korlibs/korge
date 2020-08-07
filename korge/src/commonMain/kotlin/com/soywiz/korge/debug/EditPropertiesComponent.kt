package com.soywiz.korge.debug

import com.soywiz.kds.iterators.*
import com.soywiz.korge.view.*
import com.soywiz.korui.*
import kotlin.coroutines.*

class EditPropertiesUiComponent(app: UiApplication, view: View?, val views: Views) : UiContainer(app) {
    var nodeTree: EditableNodeList? = null

    fun setView(view: View?, coroutineContext: CoroutineContext) {
        removeChildren()
        /*
        this.nodeTree = null
        if (view == null) return
        val nodes = ArrayList<EditableNode>()
        if (view is KorgeDebugNode) {
            view.getDebugProperties(views)?.let {
                nodes.add(it)
            }
        }
        nodes.add(EditableSection("View") {
            add(view::name.toEditableProperty())
            add(view::colorMul.toEditableProperty(views = views))
            add(view::blendMode.toEditableProperty(BlendMode.values()))
            add(view::alpha.toEditableProperty(0.0, 1.0))
            add(view::speed.toEditableProperty(0.0, 1.0, supportOutOfRange = true))
            add(view::ratio.toEditableProperty(0.0, 1.0, supportOutOfRange = true))
            add(view::x.toEditableProperty(supportOutOfRange = true))
            add(view::y.toEditableProperty(supportOutOfRange = true))
            if (view is RectBase) {
                add(view::anchorX.toEditableProperty(0.0, 1.0, supportOutOfRange = true))
                add(view::anchorY.toEditableProperty(0.0, 1.0, supportOutOfRange = true))
                add(EditableButtonProperty("center") {
                    view.anchorX = 0.5
                    view.anchorY = 0.5
                    //view.anchorX = -view.width / 2
                    //view.anchorY = -view.height / 2
                })
            }
            if (view is AnBaseShape) {
                add(view::dx.toEditableProperty(name = "dx", supportOutOfRange = true))
                add(view::dy.toEditableProperty(name = "dy", supportOutOfRange = true))
                add(EditableButtonProperty("center") {
                    view.dx = (-view.width / 2).toFloat()
                    view.dy = (-view.height / 2).toFloat()
                })
            }
            add(view::scaledWidth.toEditableProperty(name = "width", supportOutOfRange = true))
            add(view::scaledHeight.toEditableProperty(name = "height", supportOutOfRange = true))
            add(view::scale.toEditableProperty(0.01, 2.0, supportOutOfRange = true))
            add(view::scaleY.toEditableProperty(0.01, 2.0, supportOutOfRange = true))
            add(view::scaleX.toEditableProperty(0.01, 2.0, supportOutOfRange = true))
            add(view::rotationDegrees.toEditableProperty(-360.0, 360.0, supportOutOfRange = false))
            add(view::skewX.toEditableProperty(0.0, 2.0, supportOutOfRange = true))
            add(view::skewY.toEditableProperty(0.0, 2.0, supportOutOfRange = true))
        })
        val nodeTree = EditableNodeList(nodes)
        this.nodeTree = nodeTree
        val propertyList = nodeTree.allBaseEditableProperty
        var updating = false
        propertyList.fastForEach { property ->
            property.onChange {
                if (it.triggeredByUser) {
                    updating = true
                    nodeTree.synchronizeProperties()
                    view.stage?.views?.debugSaveView(view)
                }
            }
        }
        addChild(UiButton(app).also {
            it.minimumSize = Size(100.pt, 100.pt)
            it.text = "test"
        })
        */
        //add(PropertyPanel(nodeTree, coroutineContext) { view })
        //revalidate()

        if (view != null) {
            addChild(UiCollapsableSection(app, "View", listOf(
                //view::name.toEditableValue(app),
                view::alpha.toEditableValue(app, min = 0.0, max = 1.0),
                view::speed.toEditableValue(app, min = 0.0, max = 1.0),
                view::ratio.toEditableValue(app, min = 0.0, max = 1.0),
                Pair(view::x, view::y).toEditableValue(app, "position", min = -1000.0, max = +1000.0, clamp = false),
                Pair(view::width, view::height).toEditableValue(app, "size", min = -1000.0, max = 1000.0, clamp = false),
                view::rotationDegrees.toEditableValue(app, min = -360.0, max = +360.0),
                Pair(view::skewXDegrees, view::skewYDegrees).toEditableValue(app, "skew", min = -360.0, max = +360.0),
                view::scale.toEditableValue(app, min = 0.0, max = 1.0, clamp = false),
                Pair(view::scaleX, view::scaleY).toEditableValue(app, "scaleXY", min = 0.0, max = 1.0, clamp = false),
            )))
        }

        obsProperties = this.findObservableProperties()

        //println("obsProperties[create]: $obsProperties")

        repaintAll()
        updateUI()
        relayout()
    }

    var obsProperties: List<ObservableProperty<*>> = listOf()

    // Calls from time to time to update all the properties
    fun update() {
        //println("obsProperties[update]: $obsProperties")
        obsProperties.fastForEach {
            try {
                it.forceRefresh()
            } catch (e: Throwable) {

            }
        }
    }

    init {
        setView(view, EmptyCoroutineContext)
    }
}
