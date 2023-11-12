package korlibs.korge.datapresentation

import korlibs.image.color.*
import korlibs.korge.input.*
import korlibs.korge.testing.*
import korlibs.korge.view.*
import korlibs.math.geom.*
import org.junit.*

class PieChartTest {
  @Test
  fun test() = korgeScreenshotTest(Size(200, 200)) {
    val pie = pieChart(70f) {
      position(100, 100)
    }

    pie.setColors(
      listOf(
        Colors["#73ff5f"],
        Colors["#4058ff"],
        Colors["#1efff8"],
        Colors["#fff918"],
      )
    )
    pie.updateData(
      listOf(
        "example" to 10f,
        "another" to 30f,
        "very cool" to 5f,
        "exampleeee" to 15f,
      )
    )
    pie.getParts()[2].apply {
      onOver { colorMul = Colors.BLACK }
      onOut { colorMul = Colors.WHITE }
    }
    assertScreenshot(posterize = 5)

    pie.updateData(
      listOf(
        "different" to 20f,
        "values" to 30f,
        "here" to 5f,
        "exampleeee" to 15f,
      )
    )
    assertScreenshot(posterize = 5)

    pie.setColors(listOf(Colors["#73ff5f"],))
    pie.updateData(
      listOf(
        "not" to 1f,
        "all" to 2f,
        "colors" to 3f,
        "are" to 4f,
        "defined" to 5f,
      )
    )
    assertScreenshot(posterize = 5)
  }
}
