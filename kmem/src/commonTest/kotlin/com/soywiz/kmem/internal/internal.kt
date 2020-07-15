package com.soywiz.kmem.internal

private val formatRegex = Regex("%([-]?\\d+)?(\\w)")

internal fun String.format(vararg params: Any): String {
    var paramIndex = 0
    return formatRegex.replace(this) { mr ->
        val param = params[paramIndex++]
        //println("param: $param")
        val size = mr.groupValues[1]
        val type = mr.groupValues[2]
        val str = when (type.toLowerCase()) {
            "d" -> (param as Number).toLong().toString()
            "x" -> {
                val res = when (param) {
                    is Int -> param.toStringUnsigned(16)
                    else -> (param as Number).toLong().toStringUnsigned(16)
                }
                if (type == "X") res.toUpperCase() else res.toLowerCase()
            }
            else -> "$param"
        }
        val prefix = if (size.startsWith('0')) '0' else ' '
        val asize = size.toIntOrNull()
        var str2 = str
        if (asize != null) {
            while (str2.length < asize) {
                str2 = prefix + str2
            }
        }
        str2
    }
}

internal fun Int.toStringUnsigned(radix: Int): String = this.toUInt().toString(radix)
internal fun Long.toStringUnsigned(radix: Int): String = this.toLong().toString(radix)

fun byteArrayOf(vararg values: Int): ByteArray = ByteArray(values.size).also {
    for (n in 0 until values.size) it[n] = values[n].toByte()
}