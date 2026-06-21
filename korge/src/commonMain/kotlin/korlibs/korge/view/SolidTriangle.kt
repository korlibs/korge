package korlibs.korge.view

import korlibs.image.color.*
import korlibs.math.geom.*
import korlibs.memory.*


class SolidTriangle(
  p1: Point, p2: Point, p3: Point,
  color: RGBA = Colors.WHITE,
) : Mesh(
  vertices = Float32Buffer(6),
  indices = Uint16Buffer(UShortArrayInt(shortArrayOf(0, 1, 2))),
  uvs = Float32Buffer(12)
) {
  var p1: Point = p1
    set(value) {
      field = value
      updateTriangle()
    }
  var p2: Point = p2
    set(value) {
      field = value
      updateTriangle()
    }
  var p3: Point = p3
    set(value) {
      field = value
      updateTriangle()
    }

  init {
    colorMul = color
    updateTriangle()
  }

  private fun updateTriangle() {
    vertices[0] = p1.x.toFloat()
    vertices[1] = p1.y.toFloat()
    vertices[2] = p2.x.toFloat()
    vertices[3] = p2.y.toFloat()
    vertices[4] = p3.x.toFloat()
    vertices[5] = p3.y.toFloat()
    updatedVertices()
  }
}

/** Creates a new [SolidTriangle] with points [[p1], [p1], [p3]] and color [color] and allows you to configure it via [callback]. Once created, it is added to this receiver [Container]. */
inline fun Container.solidTriangle(
  p1: Point, p2: Point, p3: Point,
  color: RGBA = Colors.WHITE,
  callback: @ViewDslMarker Mesh.() -> Unit = {}
) = SolidTriangle(p1, p2, p3, color).addTo(this, callback)
