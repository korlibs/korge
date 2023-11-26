package korlibs.audio.internal

import korlibs.datastructure.FloatArrayDeque

private val temp = FloatArray(1)

internal fun FloatArrayDeque.write(value: Float) {
    temp[0] = value
    write(temp, 0, 1)
}
