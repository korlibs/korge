import com.soywiz.korge.*
import com.soywiz.korge.ext.swf.*
import com.soywiz.korge.view.*
import com.soywiz.korgw.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korim.color.*
import com.soywiz.korim.format.*
import com.soywiz.korim.vector.*
import com.soywiz.korim.paint.*
import com.soywiz.korim.paint.*
import com.soywiz.korio.file.std.*
import com.soywiz.korma.geom.*
import com.soywiz.korma.geom.vector.*

suspend fun main() = Korge(quality = GameWindow.Quality.PERFORMANCE, title = "SWF", bgcolor = Colors.DARKGREY) {
    this += resourcesVfs["dog.swf"].readSWF(views, SWFExportConfig(rasterizerMethod = ShapeRasterizerMethod.X4), false).createMainTimeLine()
    this += resourcesVfs["test1.swf"].readSWF(views, SWFExportConfig(rasterizerMethod = ShapeRasterizerMethod.X4), false).createMainTimeLine().position(400, 0)
}
