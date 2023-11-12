package korlibs.korge.datapresentation

import korlibs.datastructure.*
import korlibs.image.color.*
import korlibs.image.text.*
import korlibs.korge.view.*
import korlibs.logger.Console.warn
import korlibs.math.geom.*
import korlibs.math.geom.vector.*

fun Angle.getXYfromR() = Point(sin(this), cos(this))

inline fun Container.pieChart(diameter: Float, callback: @ViewDslMarker PieChart.() -> Unit = {}) =
  PieChart(diameter).addTo(this, callback)

class PieChart(val diameter: Float) : Container() {
  private val chartsContainer = container {}
  private val txtsContainer = container {}
  private val chartParts = mutableListOf<Container>()
  fun getParts() = chartParts
  private var colors = listOf(
    Colors["#ff3d43"],
    Colors["#73ff5f"],
    Colors["#4058ff"],
    Colors["#1efff8"],
    Colors["#fff918"],
  )

  fun setColors(newColors: List<RGBA>) {
    colors = newColors
  }

  fun updateData(datas: List<Pair<String, Float>>) {
    chartsContainer.removeChildren()
    txtsContainer.removeChildren()
    chartParts.removeAll { true }

    val sum = datas.map { it.second }.sum()
    val normalizedValues = datas.map { it.second }.map { it / sum }
    var totalTraversed = 0f

    ensureColorsAreDefined(datas)

    for (i in datas.indices) {
      val entryName = datas[i].first
      val entryValue = normalizedValues[i]
      val color = colors[i]
      chartsContainer.container {
        chartParts.add(this)
        graphics {
          val beginV = totalTraversed
          val endV = beginV + entryValue
          val begin = (Angle.FULL * beginV).getXYfromR() * diameter
          val textR = Angle.FULL * ((beginV + endV) / 2.0)
          val textXY = textR.getXYfromR()
          val textPos = textXY * diameter * 1.1
          val displacement = textXY * diameter * 0.1

          txtsContainer.text(entryName, alignment = TextAlignment.MIDDLE_CENTER, textSize = 22.0f) {
            position(textPos)
            val angle = Angle.QUARTER - (textR % Angle.HALF)
            rotation(angle)
          }
          fillStroke(color, color, strokeInfo = StrokeInfo(thickness = 2.0f)) {
            moveTo(begin + displacement)
            val spacing = 0.005
            var curChunk = beginV + spacing
            val chunkSize = 0.01f
            while (curChunk < endV - spacing) {
              curChunk += chunkSize
              val end = (Angle.FULL * curChunk).getXYfromR() * diameter
              lineTo(end + displacement)
            }
            lineTo(displacement)
          }
          totalTraversed += entryValue
        }
      }
    }
  }

  private fun ensureColorsAreDefined(datas: List<Pair<String, Float>>) {
    if (datas.size > colors.size) {
      warn("Defined colors set is smaller than input data. Please call setColors to set color for each input data.")
      val colorList = Colors.colorsByName.values.toList()
      colors = colors + (0..datas.size - colors.size).map { colorList.getCyclic(it * 15) }
    }
  }
}
