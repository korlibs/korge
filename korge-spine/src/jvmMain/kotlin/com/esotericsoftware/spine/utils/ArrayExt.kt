package com.esotericsoftware.spine.utils

import java.util.*

inline fun <reified T> Array<out Any>.toArray(): Array<T> {
    return Arrays.copyOf(this, this.size, Array<T>::class.java)
}
