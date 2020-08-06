package com.soywiz.korui.light

import com.soywiz.korag.*

actual object NativeLightsComponentsFactory : LightComponentsFactory {
	actual override fun create(): LightComponents = NativeLightComponents()
}

class NativeLightComponents : LightComponents() {
	override fun create(type: LightType, config: Any?): LightComponentInfo {
		var agg: AG? = null
		val handle: Any = when (type) {
			LightType.FRAME -> Any()
			LightType.CONTAINER -> Any()
			LightType.BUTTON -> Any()
			LightType.IMAGE -> Any()
			LightType.PROGRESS -> Any()
			LightType.LABEL -> Any()
			LightType.TEXT_FIELD -> Any()
			LightType.TEXT_AREA -> Any()
			LightType.CHECK_BOX -> Any()
			LightType.SCROLL_PANE -> Any()
			LightType.AGCANVAS -> {
				agg = agFactory.create()
				agg.nativeComponent
			}
			else -> throw UnsupportedOperationException("Type: $type")
		}
		return LightComponentInfo(handle).apply {
			if (agg != null) {
				this.ag = agg!!
			}
		}
	}
}
