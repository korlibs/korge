import com.soywiz.korge.*
import com.soywiz.korge.ext.swf.*
import com.soywiz.korge.view.*
import com.soywiz.korgw.*
import com.soywiz.korim.color.*
import com.soywiz.korim.format.*
import com.soywiz.korim.vector.*
import com.soywiz.korim.vector.filler.*
import com.soywiz.korim.vector.paint.*
import com.soywiz.korio.file.std.*
import com.soywiz.korma.geom.*
import com.soywiz.korma.geom.vector.*

suspend fun main() = Korge(quality = GameWindow.Quality.PERFORMANCE, title = "SWF", bgcolor = Colors.DARKGREY) {
    //val swf = resourcesVfs["dog.swf"].readSWF(views, atlasPacking = false)
    val swf = resourcesVfs["test1.swf"].readSWF(views, SWFExportConfig(rasterizerMethod = ShapeRasterizerMethod.NONE), false)
    //val swf = resourcesVfs["test4.swf"].readSWF(views, SWFExportConfig(rasterizerMethod = ShapeRasterizerMethod.X4), false)
    //val swf = resourcesVfs["test4.swf"].readSWF(views, SWFExportConfig(rasterizerMethod = ShapeRasterizerMethod.NONE), false)

    this += swf.createMainTimeLine()

    /*
    graphics {
        //fill(LinearGradientPaint(0.0, 0.0, 200.0, 0.0, transform = Matrix().pretranslate(50.0, 0.0).scale(1.0)).addColorStop(0.3, Colors.RED).addColorStop(0.7, Colors.BLUE)) {
        fill(GradientPaint.fromGradientBox(GradientKind.RADIAL, 50.0, 100.0, 0.degrees, 25.0, 0.0).addColorStop(0.0, Colors.GREEN).addColorStop(1.0, Colors.BLUE)) {
        //fill(GradientPaint.fromGradientBox(GradientKind.LINEAR, 100.0, 50.0, 90.0.degrees, 0.0, 50.0).addColorStop(0.0, Colors.GREEN).addColorStop(1.0, Colors.BLUE)) {
        //fill(GradientPaint.fromGradientBox(GradientKind.LINEAR, 100.0, 50.0, 90.0.degrees, 0.0, 0.0).addColorStop(0.0, Colors.GREEN).addColorStop(1.0, Colors.BLUE)) {
        //fill(GradientPaint.fromGradientBox(GradientKind.LINEAR, 50.0, 100.0, 0.0.degrees, 0.0, 0.0).addColorStop(0.0, Colors.GREEN).addColorStop(1.0, Colors.BLUE)) {
        //fill(GradientPaint.fromGradientBox(GradientKind.LINEAR, 100.0, 100.0, 45.0.degrees, 0.0, 0.0).addColorStop(0.0, Colors.GREEN).addColorStop(1.0, Colors.BLUE)) {
        //fill(GradientPaint.fromGradientBox(GradientKind.LINEAR, 100.0, 100.0, 45.0.degrees, 0.0, 0.0).addColorStop(0.0, Colors.GREEN).addColorStop(1.0, Colors.BLUE)) {
            rect(100.0, 100.0, 100.0, 100.0)
        }
    }
     */

    /*
    val bmp = resourcesVfs["korge.png"].readBitmapOptimized().toBMP32().scaled(96, 96)
    val shape = buildShape {
        scale(2.0)
        fill(BitmapPaint(bmp, Matrix().translate(64, 0))) {
            fillRect(64.0, 0.0, 96.0, 96.0)
        }
    }
    graphics {
        shape(shape)
    }

     */
}
