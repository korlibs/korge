package korlibs.math

///** Check if [this] floating point value is not a number or infinite */
//public fun Float.isNanOrInfinite(): Boolean = this.isNaN() || this.isInfinite()
///** Check if [this] floating point value is not a number or infinite */
//public fun Double.isNanOrInfinite(): Boolean = this.isNaN() || this.isInfinite()


fun Double.isNanOrInfinite() = this.isNaN() || this.isInfinite()

fun Float.isNanOrInfinite() = this.isNaN() || this.isInfinite()
