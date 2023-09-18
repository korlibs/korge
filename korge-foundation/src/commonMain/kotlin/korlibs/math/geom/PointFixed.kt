package korlibs.math.geom

import korlibs.number.*

data class PointFixed(val x: Fixed, val y: Fixed) {
    operator fun unaryMinus(): PointFixed = PointFixed(-this.x, -this.y)
    operator fun unaryPlus(): PointFixed = this

    operator fun plus(that: PointFixed): PointFixed = PointFixed(this.x + that.x, this.y + that.y)
    operator fun minus(that: PointFixed): PointFixed = PointFixed(this.x - that.x, this.y - that.y)
    operator fun times(that: PointFixed): PointFixed = PointFixed(this.x * that.x, this.y * that.y)
    operator fun times(that: Fixed): PointFixed = PointFixed(this.x * that, this.y * that)
    operator fun div(that: PointFixed): PointFixed = PointFixed(this.x / that.x, this.y / that.y)
    operator fun rem(that: PointFixed): PointFixed = PointFixed(this.x % that.x, this.y % that.y)

    override fun toString(): String = "($x, $y)"
}
