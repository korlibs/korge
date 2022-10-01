/**
 * The MIT License (MIT)
 *
 * Copyright (c) 2012-2018 DragonBones team and other contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.soywiz.korge.dragonbones

import com.dragonbones.animation.*
import com.dragonbones.armature.*
import com.dragonbones.core.*
import com.dragonbones.event.*
import com.dragonbones.model.*
import com.dragonbones.util.*
import com.soywiz.kds.*
import com.soywiz.korge.debug.*
import com.soywiz.korge.view.*
import com.soywiz.korim.color.*
import com.soywiz.korma.geom.vector.*
import com.soywiz.korui.*

/**
 * @inheritDoc
 */
class KorgeDbArmatureDisplay : Container(), IArmatureProxy {
	private val _events = FastArrayList<EventObject>()
	private val _eventsReturnQueue: FastArrayList<BaseObject> = FastArrayList()

	/**
	 * @private
	 */
	var debugDraw: Boolean = false
	private var _debugDraw: Boolean = false
	// private var _disposeProxy: boolean = false;
	//private var _armature: Armature = null as any
	private var _armature: Armature? = null
	private var _debugDrawer: Container? = null
	/**
	 * @inheritDoc
	 */
	override fun dbInit(armature: Armature) {
		this._armature = armature
	}

	// Do not use the time from DragonBones, but the UpdateComponent
	init {
        addUpdater {
			returnEvents()
			//_armature?.advanceTimeForChildren(it.toDouble() / 1000.0)
			_armature?.advanceTime(it.seconds)
			dispatchQueuedEvents()
            invalidate() // TODO: Check if we changed something
		}
	}

	/**
	 * @inheritDoc
	 */
	override fun dbClear() {
		if (this._debugDrawer !== null) {
			//this._debugDrawer.destroy(true) // Note: Korge doesn't require this
		}

		this._armature = null
		this._debugDrawer = null

		//super.destroy() // Note: Korge doesn't require this
	}

	/**
	 * @inheritDoc
	 */
	override fun dbUpdate() {
		val armature = this._armature ?: return
		val drawed = DragonBones.debugDraw || this.debugDraw
		if (drawed || this._debugDraw) {
			this._debugDraw = drawed
			if (this._debugDraw) {
				if (this._debugDrawer === null) {
					//this._debugDrawer = Image(Bitmaps.transparent)
                    this._debugDrawer = Container()
					val boneDrawer = CpuGraphics()
					this._debugDrawer?.addChild(boneDrawer)
				}

				this.addChild(this._debugDrawer!!)
				val boneDrawer = this._debugDrawer?.getChildAt(0) as CpuGraphics

				val bones = armature.getBones()
				//for (let i = 0, l = bones.length; i < l; ++i) {
                boneDrawer.updateShape {
                    for (i in 0 until bones.length) {
                        val bone = bones[i]
                        val boneLength = bone.boneData.length
                        val startX = bone.globalTransformMatrix.txf
                        val startY = bone.globalTransformMatrix.tyf
                        val endX = startX + bone.globalTransformMatrix.af * boneLength
                        val endY = startY + bone.globalTransformMatrix.bf * boneLength

                        stroke(Colors.PURPLE.withAd(0.7), StrokeInfo(thickness = 2.0)) {
                            moveTo(startX.toDouble(), startY.toDouble())
                            lineTo(endX, endY)
                        }
                        fill(Colors.PURPLE.withAd(0.7)) {
                            circle(startX.toDouble(), startY.toDouble(), 3.0)
                        }
                    }
                }

				val slots = armature.getSlots()
				//for (let i = 0, l = slots.length; i < l; ++i) {
				for (i in 0 until slots.length) {
					val slot = slots[i]
					val boundingBoxData = slot.boundingBoxData

					if (boundingBoxData != null) {
						var child = this._debugDrawer?.getChildByName(slot.name) as? CpuGraphics?
						if (child == null) {
							child = CpuGraphics()
							child.name = slot.name
							this._debugDrawer?.addChild(child)
						}

                        child.updateShape {
                            stroke(Colors.RED.withAd(0.7), StrokeInfo(thickness = 2.0)) {

                                when (boundingBoxData.type) {
                                    BoundingBoxType.Rectangle -> {
                                        rect(
                                            -boundingBoxData.width * 0.5,
                                            -boundingBoxData.height * 0.5,
                                            boundingBoxData.width,
                                            boundingBoxData.height
                                        )
                                    }

                                    BoundingBoxType.Ellipse -> {
                                        rect(
                                            -boundingBoxData.width * 0.5,
                                            -boundingBoxData.height * 0.5,
                                            boundingBoxData.width,
                                            boundingBoxData.height
                                        )
                                    }

                                    BoundingBoxType.Polygon -> {
                                        val vertices = (boundingBoxData as PolygonBoundingBoxData).vertices
                                        //for (let i = 0, l = vertices.length; i < l; i += 2) {
                                        for (i in 0 until vertices.size step 2) {
                                            val x = vertices[i]
                                            val y = vertices[i + 1]

                                            if (i == 0) {
                                                moveTo(x, y)
                                            } else {
                                                lineTo(x, y)
                                            }
                                        }

                                        lineTo(vertices[0], vertices[1])
                                    }

                                    else -> {

                                    }
                                }
                            }
                        }
						slot.updateTransformAndMatrix()
						slot.updateGlobalTransform()

						val transform = slot.global
						//println("SET TRANSFORM: $transform")
						child.setMatrix(slot.globalTransformMatrix)
					} else {
						val child = this._debugDrawer?.getChildByName(slot.name)
						if (child != null) {
							this._debugDrawer?.removeChild(child)
						}
					}
				}
            } else if (this._debugDrawer !== null && this._debugDrawer?.parent === this) {
				this.removeChild(this._debugDrawer)
			}
		}
	}

	/**
	 * @inheritDoc
	 */
	override fun dispose(disposeProxy: Boolean) {
		if (this._armature != null) {
			this._armature?.dispose()
			this._armature = null
		}
	}

	/**
	 * @inheritDoc
	 */
	fun destroy() {
		//this.dispose() // Note: Korge doesn't require this!
	}

	private var eventListeners = LinkedHashMap<EventStringType, FastArrayList<(EventObject) -> Unit>>()

	/**
	 * @private
	 */
	override fun dispatchDBEvent(type: EventStringType, eventObject: EventObject) {
		//println("dispatchDBEvent:$type")
		val listeners = eventListeners[type]
		if (listeners != null) {
			listeners.fastForEach { listener ->
				listener(eventObject)
			}
		}
	}

	/**
	 * @inheritDoc
	 */
	override fun hasDBEventListener(type: EventStringType): Boolean {
		return eventListeners.containsKey(type).apply {
			//println("hasDBEventListener:$type:$this")
		}
	}

	/**
	 * @inheritDoc
	 */
	override fun addDBEventListener(type: EventStringType, listener: (event: EventObject) -> Unit) {
		//println("addDBEventListener:$type")
		eventListeners.getOrPut(type) { FastArrayList() } += listener
	}

	/**
	 * @inheritDoc
	 */
	override fun removeDBEventListener(type: EventStringType, listener: (event: EventObject) -> Unit) {
		//println("removeDBEventListener:$type")
		eventListeners[type]?.remove(listener)
	}

	override fun queueEvent(value: EventObject) {
		if (!this._events.contains(value)) {
			this._events.add(value)
		}
	}

	private fun queueReturnEvent(obj: BaseObject?) {
		if (obj != null && !this._eventsReturnQueue.contains(obj)) this._eventsReturnQueue.add(obj)
	}

	private fun dispatchQueuedEvents() {
		if (this._events.size <= 0) return
		for (i in 0 until this._events.size) {
			val eventObject = this._events[i]
			val armature = eventObject.armature

			if (armature._armatureData != null) { // May be armature disposed before advanceTime.
				armature.eventDispatcher.dispatchDBEvent(eventObject.type, eventObject)
				if (eventObject.type == EventObject.SOUND_EVENT) {
					dispatchDBEvent(eventObject.type, eventObject)
				}
			}

			queueReturnEvent(eventObject)
		}

		this._events.clear()
	}

	private fun returnEvents() {
		if (this._eventsReturnQueue.size <= 0) return
		this._eventsReturnQueue.fastForEach { obj ->
			obj.returnToPool()
		}
		this._eventsReturnQueue.clear()
	}

	fun on(type: EventStringType, listener: (event: EventObject) -> Unit) {
		addDBEventListener(type, listener)
	}

	/**
	 * @inheritDoc
	 */
	override val armature: Armature get() = this._armature!!
	/**
	 * @inheritDoc
	 */
	override val animation: Animation get() = this._armature!!.animation

    val animationNames get() = animation.animationNames

    override fun buildDebugComponent(views: Views, container: UiContainer) {
        container.uiCollapsibleSection("DragonBones") {
            addChild(UiRowEditableValue(app, "animation", UiListEditableValue(app, { animationNames }, ObservableProperty(
                name = "animation",
                internalSet = { animationName -> animation.play(animationName) },
                internalGet = { animation.lastAnimationName }
            ))))
        }
        super.buildDebugComponent(views, container)
    }
}
