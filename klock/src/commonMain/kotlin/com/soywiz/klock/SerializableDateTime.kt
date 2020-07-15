package com.soywiz.klock

import com.soywiz.klock.internal.Serializable

@Deprecated("Use WDateTime")
data class SerializableDateTime(val dateTime: DateTime) : Serializable {
    companion object {
        @Suppress("MayBeConstant", "unused")
        private const val serialVersionUID = 1L
    }

	override fun toString(): String = dateTime.toString()
}

fun DateTime.serializable() = SerializableDateTime(this)
