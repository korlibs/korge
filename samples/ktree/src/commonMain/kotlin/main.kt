import com.soywiz.klock.*
import com.soywiz.korev.*
import com.soywiz.korge.*
import com.soywiz.korge.input.*
import com.soywiz.korge.view.*
import com.soywiz.korge.view.ktree.*
import com.soywiz.korim.atlas.*
import com.soywiz.korim.color.*
import com.soywiz.korio.async.*
import com.soywiz.korio.file.std.*
import com.soywiz.korio.serialization.xml.*

suspend fun main() = Korge(bgcolor = Colors["#172335"]) {
	addChild(resourcesVfs["scene.ktree"].readKTree(views))
}
