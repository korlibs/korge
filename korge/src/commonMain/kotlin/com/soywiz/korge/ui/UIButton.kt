package com.soywiz.korge.ui

import com.soywiz.korge.input.*
import com.soywiz.korge.view.*
import com.soywiz.korio.async.*

inline fun Container.uiButton(
    width: Double = 128.0,
    height: Double = 64.0,
    skin: UISkin = defaultUISkin,
    block: @ViewDslMarker UIButton.() -> Unit = {}
): UIButton = UIButton(width, height, skin).addTo(this).apply(block)

open class UIButton(
	width: Double = 128.0,
	height: Double = 64.0,
	skin: UISkin = DefaultUISkin
) : UIView(width, height) {

	var forcePressed by uiObservable(false) { updateState() }
	var skin: UISkin by uiObservable(skin) { updateState() }
	protected open val rect = ninePatch(skin.normal, width, height, 10.0 / 64.0, 10.0 / 64.0, 54.0 / 64.0, 54.0 / 64.0)

	protected var bover by uiObservable(false) { updateState() }
	protected var bpressing by uiObservable(false) { updateState() }

	fun simulateOver() {
		bover = true
	}

	fun simulateOut() {
		bover = false
	}

	fun simulatePressing(value: Boolean) {
		bpressing = value
	}

	fun simulateDown() {
		bpressing = true
	}

	fun simulateUp() {
		bpressing = false
	}

    val onPress = Signal<TouchEvents.Info>()

	init {
        singleTouch {
            start {
                simulateDown()
            }
            endAnywhere {
                simulateUp()
            }
            tap {
                onPress(it)
            }
        }
		mouse {
			onOver {
				simulateOver()
			}
			onOut {
				simulateOut()
			}
		}
	}

	override fun updateState() {
		rect.tex = when {
			!enabled -> skin.disabled
			bpressing || forcePressed -> skin.down
			bover -> skin.over
			else -> skin.normal
		}
	}

	override fun onSizeChanged() {
		super.onSizeChanged()
		rect.width = width
		rect.height = height
		updateState()
	}
}
