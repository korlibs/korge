package com.soywiz.korge.ui

import com.soywiz.kmem.clamp
import com.soywiz.korge.annotations.KorgeExperimental
import com.soywiz.korge.render.RenderContext
import com.soywiz.korge.view.Container
import com.soywiz.korge.view.DummyView
import com.soywiz.korge.view.View
import com.soywiz.korge.view.ViewDslMarker
import com.soywiz.korge.view.addTo
import com.soywiz.korge.view.addUpdater
import com.soywiz.korge.view.getVisibleGlobalArea
import com.soywiz.korge.view.getVisibleWindowArea
import com.soywiz.korge.view.position
import com.soywiz.korge.view.size
import com.soywiz.korma.geom.Matrix
import com.soywiz.korma.geom.Point
import com.soywiz.korma.geom.Rectangle

@KorgeExperimental
inline fun Container.uiVerticalList(
    provider: UIVerticalList.Provider,
    width: Double = 256.0,
    block: @ViewDslMarker Container.(UIVerticalList) -> Unit = {}
): UIVerticalList = UIVerticalList(provider, width)
    .addTo(this).also { block(it) }

@KorgeExperimental
open class UIVerticalList(provider: Provider, width: Double = 200.0) : UIView(width) {
    interface Provider {
        val numItems: Int
        val fixedHeight: Double?
        fun getItemY(index: Int): Double = (fixedHeight ?: 20.0) * index
        fun getItemHeight(index: Int): Double
        fun getItemView(index: Int, vlist: UIVerticalList): View

        object Dummy : Provider {
            override val numItems: Int get() = 0
            override val fixedHeight: Double? = null
            override fun getItemHeight(index: Int): Double = 0.0
            override fun getItemView(index: Int, vlist: UIVerticalList): View = DummyView()
        }
    }

    private var dirty = false
    private val viewsByIndex = LinkedHashMap<Int, View>()
    private val lastArea = Rectangle()
    private val lastPoint = Point()
    private val tempRect = Rectangle()
    private val tempPoint = Point()
    var provider: Provider = provider
        set(value) {
            field = value
            dirty = true
            updateList()
        }

    init {
        updateList()
        addUpdater {
            updateList()
        }
    }

    override fun renderInternal(ctx: RenderContext) {
        updateList()
        super.renderInternal(ctx)
    }

    private fun getIndexAtY(y: Double): Int {
        val index = y / (provider.fixedHeight?.toDouble() ?: 20.0)
        return index.toInt()
    }

    fun invalidateList() {
        dirty = true
        removeChildren()
        viewsByIndex.clear()
        updateList()
    }

    private val tempTransform = Matrix.Transform()

    fun updateList() {
        if (parent == null) return
        //if (stage == null) return
        val area = getVisibleGlobalArea(tempRect)
        //val area = getVisibleWindowArea(tempRect)
        val point = globalXY(tempPoint)
        val numItems = provider.numItems
        if (dirty || area != lastArea || point != lastPoint) {
            dirty = false
            lastArea.copyFrom(area)
            lastPoint.copyFrom(point)

            //val nheight = provider.fixedHeight?.toDouble() ?: 20.0
            //val nItems = (area.height / nheight).toIntCeil()
            //println(nItems)
            //removeChildren()

            //val fromIndex = (startIndex).clamp(0, numItems - 1)
            //val toIndex = (startIndex + nItems).clamp(0, numItems - 1)

            //println("----")

            //println("point=$point")

            val transform = parent!!.globalMatrix.toTransform(tempTransform)
            //println("transform=${transform.scaleAvg}")
            val fromIndex = getIndexAtY((-point.y + tempRect.top) / transform.scaleY).clamp(0, numItems - 1)
            var toIndex = fromIndex
            for (index in fromIndex until numItems) {
                val view = viewsByIndex.getOrPut(index) {
                    val itemHeight = provider.getItemHeight(index)
                    provider.getItemView(index, this)
                        .also { addChild(it) }
                        .position(0.0, provider.getItemY(index))
                        .size(width, itemHeight.toDouble())
                }
                view.zIndex = index.toDouble()
                toIndex = index

                //val localViewY = view.localToGlobalY(0.0, view.height)

                //println(":: ${view.localToGlobalY(0.0, view.height)}, ${area.bottom}")

                //if (view.localToRenderY(0.0, view.height) >= area.bottom) {
                if (view.localToGlobalY(0.0, view.height) >= area.bottom) {
                //if (view.localToWindowY(stage!!.views, 0.0, view.height) >= area.bottom) {
                //if (false) {
                    //println("localViewY=localViewY, globalY=${view.localToGlobalY(0.0, view.height)}")
                    break
                }
            }
            //println("area=$area, point=$point, nItems=${toIndex - fromIndex}, fromIndex=$fromIndex, toIndex=$toIndex, globalBounds=${this.globalBounds}")

            val removeIndices = viewsByIndex.keys.filter { it !in fromIndex .. toIndex }.toSet()

            viewsByIndex.forEach { (index, view) ->
                if (index in removeIndices) {
                    view.removeFromParent()
                }
            }
            for (index in removeIndices) viewsByIndex.remove(index)
        }
        setSize(width, provider.getItemY(numItems - 1) + provider.getItemHeight(numItems - 1))
        //println("height=$height")
    }
}
