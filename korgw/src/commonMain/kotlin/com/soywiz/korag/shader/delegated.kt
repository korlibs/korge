package com.soywiz.korag.shader

import com.soywiz.kmem.*
import com.soywiz.korag.*
import com.soywiz.korma.geom.*
import kotlin.reflect.*

class DoubleDelegatedUniform(val uniform: Uniform, val values: FloatArray, val index: Int, val onSet: (Double) -> Unit, default: Double) {
	init {
		values[index] = default.toFloat()
	}

	operator fun getValue(obj: Any, prop: KProperty<*>): Double = values[index].toDouble()
	operator fun setValue(obj: Any, prop: KProperty<*>, value: Double) {
		values[index] = value.toFloat()
		onSet(value)
	}
}

class FloatDelegatedUniform(val uniform: Uniform, val values: FloatArray, val index: Int, val onSet: (Float) -> Unit, default: Float) {
	init {
		values[index] = default
	}

	operator fun getValue(obj: Any, prop: KProperty<*>): Float = values[index]
	operator fun setValue(obj: Any, prop: KProperty<*>, value: Float) {
		values[index] = value
		onSet(value)
	}
}

class IntDelegatedUniform(val uniform: Uniform, val values: FloatArray, val index: Int, val onSet: (Int) -> Unit, default: Int) {
	init {
		values[index] = default.toFloat()
	}

	operator fun getValue(obj: Any, prop: KProperty<*>): Int = values[index].toInt()

	operator fun setValue(obj: Any, prop: KProperty<*>, value: Int) {
		values[index] = value.toFloat()
		onSet(value)
	}
}

class BoolDelegatedUniform(val uniform: Uniform, val values: FloatArray, val index: Int, val onSet: (Boolean) -> Unit, default: Boolean) {
	init {
		values[index] = default.toInt().toFloat()
	}

	operator fun getValue(obj: Any, prop: KProperty<*>): Boolean = values[index] != 0f

	operator fun setValue(obj: Any, prop: KProperty<*>, value: Boolean) {
		values[index] = value.toInt().toFloat()
		onSet(value)
	}
}

class UniformFloatStorage(val uniforms: AG.UniformValues, val uniform: Uniform, val array: FloatArray) {
	init {
		uniforms[uniform] = array
	}

	fun doubleDelegate(index: Int, default: Double = 0.0, onSet: (Double) -> Unit = {}) =
        DoubleDelegatedUniform(uniform, array, index, onSet, default)
	fun doubleDelegateX(default: Double = 0.0, onSet: (Double) -> Unit = {}) = doubleDelegate(0, default, onSet)
	fun doubleDelegateY(default: Double = 0.0, onSet: (Double) -> Unit = {}) = doubleDelegate(1, default, onSet)
	fun doubleDelegateZ(default: Double = 0.0, onSet: (Double) -> Unit = {}) = doubleDelegate(2, default, onSet)
	fun doubleDelegateW(default: Double = 0.0, onSet: (Double) -> Unit = {}) = doubleDelegate(3, default, onSet)

	fun floatDelegate(index: Int, default: Float = 0f, onSet: (Float) -> Unit = {}) =
        FloatDelegatedUniform(uniform, array, index, onSet, default)
	fun floatDelegateX(default: Float = 0f, onSet: (Float) -> Unit = {}) = floatDelegate(0, default, onSet)
	fun floatDelegateY(default: Float = 0f, onSet: (Float) -> Unit = {}) = floatDelegate(1, default, onSet)
	fun floatDelegateZ(default: Float = 0f, onSet: (Float) -> Unit = {}) = floatDelegate(2, default, onSet)
	fun floatDelegateW(default: Float = 0f, onSet: (Float) -> Unit = {}) = floatDelegate(3, default, onSet)

	fun intDelegate(index: Int, default: Int = 0, onSet: (Int) -> Unit = {}) =
        IntDelegatedUniform(uniform, array, index, onSet, default)
	fun intDelegateX(default: Int = 0, onSet: (Int) -> Unit = {}) = intDelegate(0, default, onSet)
	fun intDelegateY(default: Int = 0, onSet: (Int) -> Unit = {}) = intDelegate(1, default, onSet)
	fun intDelegateZ(default: Int = 0, onSet: (Int) -> Unit = {}) = intDelegate(2, default, onSet)
	fun intDelegateW(default: Int = 0, onSet: (Int) -> Unit = {}) = intDelegate(3, default, onSet)

	fun boolDelegate(index: Int, default: Boolean = false, onSet: (Boolean) -> Unit = {}) =
        BoolDelegatedUniform(uniform, array, index, onSet, default)
	fun boolDelegateX(default: Boolean = false, onSet: (Boolean) -> Unit = {}) = boolDelegate(0, default, onSet)
	fun boolDelegateY(default: Boolean = false, onSet: (Boolean) -> Unit = {}) = boolDelegate(1, default, onSet)
	fun boolDelegateZ(default: Boolean = false, onSet: (Boolean) -> Unit = {}) = boolDelegate(2, default, onSet)
	fun boolDelegateW(default: Boolean = false, onSet: (Boolean) -> Unit = {}) = boolDelegate(3, default, onSet)
}

class UniformValueStorage<T : Any>(val uniforms: AG.UniformValues, val uniform: Uniform, val value: T) {
	init {
		uniforms[uniform] = value
	}

	fun delegate() = this

	operator fun getValue(obj: Any, prop: KProperty<*>): T = value
	operator fun setValue(obj: Any, prop: KProperty<*>, value: T) {
		uniforms[uniform] = value
	}
}

class UniformValueStorageMatrix3D(val uniforms: AG.UniformValues, val uniform: Uniform, val value: Matrix3D) {
    init {
        uniforms[uniform] = value
    }

    fun delegate() = this

    fun setMatrix(value: Matrix3D) {
        this.value.copyFrom(value)
        uniforms[uniform] = this.value
    }

    operator fun getValue(obj: Any, prop: KProperty<*>): Matrix3D = value
    operator fun setValue(obj: Any, prop: KProperty<*>, value: Matrix3D) = setMatrix(value)
}

fun AG.UniformValues.storageFor(uniform: Uniform, array: FloatArray = FloatArray(4)) =
    UniformFloatStorage(this, uniform, array)
//fun AG.UniformValues.storageForMatrix2(uniform: Uniform, matrix: Matrix3D = Matrix3D()) = UniformValueStorage(this, uniform, matrix)
//fun AG.UniformValues.storageForMatrix3(uniform: Uniform, matrix: Matrix3D = Matrix3D()) = UniformValueStorage(this, uniform, matrix)
//fun AG.UniformValues.storageForMatrix4(uniform: Uniform, matrix: Matrix3D = Matrix3D()) = UniformValueStorage(this, uniform, matrix)
fun AG.UniformValues.storageForMatrix3D(uniform: Uniform, matrix: Matrix3D = Matrix3D()): UniformValueStorageMatrix3D {
    return UniformValueStorageMatrix3D(this, uniform, Matrix3D()).also {
        it.setMatrix(matrix)
    }
}

