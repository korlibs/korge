package com.soywiz.kds

class BooleanArrayList {
    val array = IntArrayList()

    var size: Int
        get() = array.size
        set(value) {
            array.size = value
        }

    operator fun get(index: Int): Boolean = getAt(index)
    operator fun set(index: Int, value: Boolean) = setAt(index, value)

    fun getAt(index: Int): Boolean = array.getAt(index) != 0
    fun setAt(index: Int, value: Boolean) = array.setAt(index, if (value) 1 else 0)

    fun removeIndex(index: Int): Boolean = array.removeAt(index) != 0
}
