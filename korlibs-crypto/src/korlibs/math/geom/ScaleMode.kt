package korlibs.math.geom

class ScaleMode(
    val name: String? = null,
    val transform: (item: Size, container: Size) -> Size
) {
    override fun toString(): String = "ScaleMode($name)"

    operator fun invoke(item: Size, container: Size): Size = transform(item, container)
    operator fun invoke(item: SizeInt, container: SizeInt): SizeInt = transform(item.toFloat(), container.toFloat()).toInt()

    companion object {
        val COVER: ScaleMode = ScaleMode("COVER") { i, c -> i * (c / i).toVector2().maxComponent() }
        val SHOW_ALL: ScaleMode = ScaleMode("SHOW_ALL") { i, c -> i * (c / i).toVector2().minComponent() }
        val FIT: ScaleMode get() = SHOW_ALL
        val FILL: ScaleMode get() = EXACT
        val EXACT: ScaleMode = ScaleMode("EXACT") { i, c -> c }
        val NO_SCALE: ScaleMode = ScaleMode("NO_SCALE") { i, c -> i }
    }
}

fun Rectangle.applyScaleMode(
    container: Rectangle, mode: ScaleMode, anchor: Anchor
): Rectangle = this.size.applyScaleMode(container, mode, anchor)

fun SizeInt.applyScaleMode(container: RectangleInt, mode: ScaleMode, anchor: Anchor): RectangleInt = this.toFloat().applyScaleMode(container.toFloat(), mode, anchor).toInt()
fun SizeInt.applyScaleMode(container: SizeInt, mode: ScaleMode): SizeInt = mode(this, container)
fun SizeInt.fitTo(container: SizeInt): SizeInt = applyScaleMode(container, ScaleMode.SHOW_ALL)

fun Size.applyScaleMode(container: Rectangle, mode: ScaleMode, anchor: Anchor): Rectangle {
    val outSize = this.applyScaleMode(container.size, mode)
    return Rectangle(
        (container.x + anchor.sx * (container.width - outSize.width)),
        (container.y + anchor.sy * (container.height - outSize.height)),
        outSize.width,
        outSize.height
    )
}
fun Size.applyScaleMode(container: Size, mode: ScaleMode): Size = mode(this, container)
fun Size.fitTo(container: Size): Size = applyScaleMode(container, ScaleMode.SHOW_ALL)
