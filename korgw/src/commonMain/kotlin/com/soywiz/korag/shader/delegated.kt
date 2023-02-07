package com.soywiz.korag.shader

import com.soywiz.kmem.toInt
import com.soywiz.korag.*
import com.soywiz.korma.geom.Matrix3D
import com.soywiz.korma.geom.Vector3D
import kotlin.reflect.KProperty

class DoubleDelegatedUniform(val uniform: Uniform, val values: FloatArray, val index: Int, val onSet: (Double) -> Unit, default: Double, val storage: UniformFloatStorage) {
	init {
		values[index] = default.toFloat()
        storage.update()
	}

	operator fun getValue(obj: Any, prop: KProperty<*>): Double = values[index].toDouble()
	operator fun setValue(obj: Any, prop: KProperty<*>, value: Double) {
		values[index] = value.toFloat()
		onSet(value)
        storage.update()
	}
}

class FloatDelegatedUniform(val uniform: Uniform, val values: FloatArray, val index: Int, val onSet: (Float) -> Unit, default: Float, val storage: UniformFloatStorage) {
	init {
		values[index] = default
        storage.update()
	}

	operator fun getValue(obj: Any, prop: KProperty<*>): Float = values[index]
	operator fun setValue(obj: Any, prop: KProperty<*>, value: Float) {
		values[index] = value
		onSet(value)
        storage.update()
	}
}

class IntDelegatedUniform(val uniform: Uniform, val values: FloatArray, val index: Int, val onSet: (Int) -> Unit, default: Int, val storage: UniformFloatStorage) {
	init {
		values[index] = default.toFloat()
        storage.update()
	}

	operator fun getValue(obj: Any, prop: KProperty<*>): Int = values[index].toInt()

	operator fun setValue(obj: Any, prop: KProperty<*>, value: Int) {
		values[index] = value.toFloat()
		onSet(value)
        storage.update()
	}
}

class Vector4DelegatedUniform(val uniform: Uniform, val values: FloatArray, val index: Int, val storage: UniformFloatStorage) {
    init {
        for (n in 0 until 4) values[index + n] = 0f
        storage.update()
    }

    operator fun getValue(obj: Any, prop: KProperty<*>): Vector3D = Vector3D(values[0], values[1], values[2], values[3])

    operator fun setValue(obj: Any, prop: KProperty<*>, value: Vector3D) {
        values[0] = value.x
        values[1] = value.y
        values[2] = value.z
        values[3] = value.w
        storage.update()
    }
}


class BoolDelegatedUniform(val uniform: Uniform, val values: FloatArray, val index: Int, val onSet: (Boolean) -> Unit, default: Boolean, val storage: UniformFloatStorage) {
	init {
		values[index] = default.toInt().toFloat()
        storage.update()
	}

	operator fun getValue(obj: Any, prop: KProperty<*>): Boolean = values[index] != 0f

	operator fun setValue(obj: Any, prop: KProperty<*>, value: Boolean) {
		values[index] = value.toInt().toFloat()
		onSet(value)
        storage.update()
	}
}

class UniformFloatStorage(val uniforms: AGUniformValues, val uniform: Uniform, val array: FloatArray) {
	init {
		uniforms[uniform] = array
	}

    fun update() {
        uniforms[uniform] = array
    }

    var x: Float ; get() = array[0] ; set(value) { array[0] = value }
    var y: Float ; get() = array[1] ; set(value) { array[1] = value }
    var z: Float ; get() = array[2] ; set(value) { array[2] = value }
    var w: Float ; get() = array[3] ; set(value) { array[3] = value }

	fun doubleDelegate(index: Int, default: Double = 0.0, onSet: (Double) -> Unit = {}) =
        DoubleDelegatedUniform(uniform, array, index, onSet, default, this)
	fun doubleDelegateX(default: Double = 0.0, onSet: (Double) -> Unit = {}) = doubleDelegate(0, default, onSet)
	fun doubleDelegateY(default: Double = 0.0, onSet: (Double) -> Unit = {}) = doubleDelegate(1, default, onSet)
	fun doubleDelegateZ(default: Double = 0.0, onSet: (Double) -> Unit = {}) = doubleDelegate(2, default, onSet)
	fun doubleDelegateW(default: Double = 0.0, onSet: (Double) -> Unit = {}) = doubleDelegate(3, default, onSet)

	fun floatDelegate(index: Int, default: Float = 0f, onSet: (Float) -> Unit = {}) =
        FloatDelegatedUniform(uniform, array, index, onSet, default, this)
	fun floatDelegateX(default: Float = 0f, onSet: (Float) -> Unit = {}) = floatDelegate(0, default, onSet)
	fun floatDelegateY(default: Float = 0f, onSet: (Float) -> Unit = {}) = floatDelegate(1, default, onSet)
	fun floatDelegateZ(default: Float = 0f, onSet: (Float) -> Unit = {}) = floatDelegate(2, default, onSet)
	fun floatDelegateW(default: Float = 0f, onSet: (Float) -> Unit = {}) = floatDelegate(3, default, onSet)

	fun intDelegate(index: Int, default: Int = 0, onSet: (Int) -> Unit = {}) =
        IntDelegatedUniform(uniform, array, index, onSet, default, this)
	fun intDelegateX(default: Int = 0, onSet: (Int) -> Unit = {}) = intDelegate(0, default, onSet)
	fun intDelegateY(default: Int = 0, onSet: (Int) -> Unit = {}) = intDelegate(1, default, onSet)
	fun intDelegateZ(default: Int = 0, onSet: (Int) -> Unit = {}) = intDelegate(2, default, onSet)
	fun intDelegateW(default: Int = 0, onSet: (Int) -> Unit = {}) = intDelegate(3, default, onSet)

	fun boolDelegate(index: Int, default: Boolean = false, onSet: (Boolean) -> Unit = {}) =
        BoolDelegatedUniform(uniform, array, index, onSet, default, this)
	fun boolDelegateX(default: Boolean = false, onSet: (Boolean) -> Unit = {}) = boolDelegate(0, default, onSet)
	fun boolDelegateY(default: Boolean = false, onSet: (Boolean) -> Unit = {}) = boolDelegate(1, default, onSet)
	fun boolDelegateZ(default: Boolean = false, onSet: (Boolean) -> Unit = {}) = boolDelegate(2, default, onSet)
	fun boolDelegateW(default: Boolean = false, onSet: (Boolean) -> Unit = {}) = boolDelegate(3, default, onSet)

    fun vector4Delegate(index: Int = 0) = Vector4DelegatedUniform(uniform, array, index, this)
}

class UniformValueStorageMatrix3D(val uniforms: AGUniformValues, val uniform: Uniform, val value: Matrix3D) {
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

fun AGUniformValues.storageFor(uniform: Uniform, array: FloatArray = FloatArray(4)) =
    UniformFloatStorage(this, uniform, array)
//fun AGUniformValues.storageForMatrix2(uniform: Uniform, matrix: Matrix3D = Matrix3D()) = UniformValueStorage(this, uniform, matrix)
//fun AGUniformValues.storageForMatrix3(uniform: Uniform, matrix: Matrix3D = Matrix3D()) = UniformValueStorage(this, uniform, matrix)
//fun AGUniformValues.storageForMatrix4(uniform: Uniform, matrix: Matrix3D = Matrix3D()) = UniformValueStorage(this, uniform, matrix)
fun AGUniformValues.storageForMatrix3D(uniform: Uniform, matrix: Matrix3D = Matrix3D()): UniformValueStorageMatrix3D {
    return UniformValueStorageMatrix3D(this, uniform, Matrix3D()).also {
        it.setMatrix(matrix)
    }
}
