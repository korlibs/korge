package com.soywiz.korge.ext.spriter.com.brashmonkey.spriter

import kotlin.reflect.*

class Meta {

	var vars: Array<Var>? = null

	class Var {
		var id: Int = 0
		var name: String? = null
		var default: Value? = null
		var keys: Array<Key>? = null

		class Key {
			var id: Int = 0
			var time: Long = 0
			var value: Value? = null

			val type: KClass<*>
				get() = this.value!!::class
		}

		class Value {
			var value: Any? = null

			val int: Int
				get() = (value as Int?)!!

			val long: Long
				get() = (value as Long?)!!

			val string: String
				get() = value as String
		}

		operator fun get(time: Long): Key? {
			for (key in this.keys!!)
				if (key.time == time)
					return key
			return null
		}

		fun has(time: Long): Boolean {
			return this[time] != null
		}
	}

	fun getVar(time: Long): Var? {
		for (`var` in this.vars!!)
			if (`var`[time] != null)
				return `var`
		return null
	}
}
