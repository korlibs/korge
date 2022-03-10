import com.soywiz.korge.view.*
import com.soywiz.korge.view.filter.*
import com.soywiz.korim.color.*

suspend fun Stage.mainColorTransformFilter() {
    //val rect = solidRect(100, 100, )
    val rect = solidRect(100, 100, Colors.DARKGRAY)
    //rect.colorAdd = ColorAdd(+100, 0, 0, 0)
    rect.filter = ColorTransformFilter(ColorTransform(add = ColorAdd(+127, 0, +127, +255)))
}
