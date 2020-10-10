package com.soywiz.korge.debug

import com.soywiz.kds.*
import com.soywiz.kds.iterators.*
import com.soywiz.korge.animate.*
import com.soywiz.korge.view.*
import com.soywiz.korui.*
import com.soywiz.korui.layout.*
import kotlin.coroutines.*

class UiEditProperties(app: UiApplication, view: View?, val views: Views) : UiContainer(app) {
    val propsContainer = scrollPanel(xbar = false)

    fun setView(view: View?) {
        propsContainer.removeChildren()

        view?.buildDebugComponent(views, this@UiEditProperties.propsContainer)

        root?.updateUI()
        root?.relayout()
        root?.repaintAll()

        update()
    }

    private val registeredProperties = WeakMap<ObservableProperty<*>, Boolean>()

    // Calls from time to time to update all the properties
    private var updating = false
    fun update() {
        if (updating) return
        updating = true
        try {
            //println("obsProperties[update]: $obsProperties")
            //obsProperties.fastForEach { it.forceRefresh() }
            val obsProperties = this.findObservableProperties()

            obsProperties.fastForEach {
                if (it !in registeredProperties) {
                    registeredProperties[it] = true
                    it.onChange { update() }
                }
                it.forceRefresh()
            }

        } finally {
            updating = false
        }
    }

    init {
        layout = UiFillLayout
        setView(view)
    }
}
