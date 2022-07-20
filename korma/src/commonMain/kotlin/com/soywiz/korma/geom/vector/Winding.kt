package com.soywiz.korma.geom.vector

enum class Winding(val str: String) {
    /**
     * https://en.wikipedia.org/wiki/Even-odd_rule
     **/
    EVEN_ODD("evenOdd"),
    /**
     * **DEFAULT**
     *
     * https://en.wikipedia.org/wiki/Nonzero-rule
     **/
    NON_ZERO("nonZero");

    companion object {
        val DEFAULT: Winding get() = NON_ZERO
    }
}
