package com.soywiz.korui.light.log

import com.soywiz.kds.*
import com.soywiz.korui.light.*

class LogLightComponents : LightComponents() {
	val log = arrayListOf<String>()
	val lastIdPerType = LinkedHashMap<LightType, Int>()

	override fun create(type: LightType, config: Any?): LightComponentInfo {
		val id = lastIdPerType.incr(type, 1) - 1
		log += "create($type)=$id"
		return LightComponentInfo("$type$id")
	}

	override fun setParent(c: Any, parent: Any?) {
		log += "setParent($c,$parent)"
	}

	override fun setBounds(c: Any, x: Int, y: Int, width: Int, height: Int) {
		log += "setBounds($c,$x,$y,$width,$height)"
	}

	override fun <T> setProperty(c: Any, key: LightProperty<T>, value: T) {
		log += "setProperty($c,$key,$value)"
	}
}
