package korlibs.korge.view

import korlibs.image.bitmap.*
import korlibs.image.color.*
import korlibs.korge.render.*
import korlibs.math.geom.*
import korlibs.math.geom.vector.*
import korlibs.memory.*

/** Creates a new [SolidTriangle] with points [[p1], [p1], [p3]] and color [color] and allows you to configure it via [callback]. Once created, it is added to this receiver [Container]. */
inline fun Container.solidTriangle(
  p1: Point, p2: Point, p3: Point,
  color: RGBA = Colors.WHITE,
  callback: @ViewDslMarker Mesh.() -> Unit = {}
): Mesh {
  val vertices = floatArrayOf(
    p1.x.toFloat(), p1.y.toFloat(),
    p2.x.toFloat(), p2.y.toFloat(),
    p3.x.toFloat(), p3.y.toFloat(),
  )
  val indices = Uint16Buffer(UShortArrayInt(shortArrayOf(0, 1, 2)))
  val uvs = Float32Buffer(12)
  return Mesh(vertices = Float32Buffer(vertices), indices = indices, uvs = uvs)
    .addTo(this, callback)
}
