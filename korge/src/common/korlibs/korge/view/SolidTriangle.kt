package korlibs.korge.view

import korlibs.image.color.*
import korlibs.math.geom.*
import korlibs.memory.*


class SolidTriangle(
  p1: Point, p2: Point, p3: Point,
  color: RGBA = Colors.WHITE,
) : Mesh(
  vertices = Float32Buffer(
    floatArrayOf(
      p1.x.toFloat(), p1.y.toFloat(),
      p2.x.toFloat(), p2.y.toFloat(),
      p3.x.toFloat(), p3.y.toFloat(),
    )
  ),
  indices = Uint16Buffer(UShortArrayInt(shortArrayOf(0, 1, 2))),
  uvs = Float32Buffer(12)
) {
  var p1: Point = p1
    set(value) {
      updateTriangle()
      updatedVertices()
      field = value
    }
  var p2: Point = p2
    set(value) {
      updateTriangle()
      updatedVertices()
      field = value
    }
  var p3: Point = p3
    set(value) {
      updateTriangle()
      updatedVertices()
      field = value
    }

  init {
    colorMul = color
  }

  fun updateTriangle() {
    vertices = Float32Buffer(
      floatArrayOf(
        p1.x.toFloat(), p1.y.toFloat(),
        p2.x.toFloat(), p2.y.toFloat(),
        p3.x.toFloat(), p3.y.toFloat(),
      )
    )
  }
}

/** Creates a new [SolidTriangle] with points [[p1], [p1], [p3]] and color [color] and allows you to configure it via [callback]. Once created, it is added to this receiver [Container]. */
inline fun Container.solidTriangle(
  p1: Point, p2: Point, p3: Point,
  color: RGBA = Colors.WHITE,
  callback: @ViewDslMarker Mesh.() -> Unit = {}
) = SolidTriangle(p1, p2, p3, color).addTo(this, callback)
