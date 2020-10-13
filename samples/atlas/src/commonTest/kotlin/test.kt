import com.soywiz.korge.tests.*
import com.soywiz.korge.view.*
import com.soywiz.korim.atlas.readAtlas
import com.soywiz.korim.bitmap.*
import com.soywiz.korio.async.*
import com.soywiz.korio.file.std.*
import com.soywiz.korma.geom.*
import kotlin.test.*

class AtlasTest : ViewsForTesting() {
    // @TODO: This fails. It is not generated?

	//@Test
	//fun test() = viewsTest {
	//	atlasMain()
	//	assertEquals(3, stage.numChildren)
	//	assertEquals(Size(68, 204), (stage[0] as Image).texture.bmp.size)
	//}

	//@Test
	//fun testAtlas() = suspendTest {
	//	val atlas = resourcesVfs["logos.atlas.json"].readAtlas()
	//	assertEquals(3, atlas.entries.size)
	//	assertEquals(Size(64, 64), atlas["korau.png"].size)
	//	assertEquals(Size(64, 64), atlas["korge.png"].size)
	//	assertEquals(Size(64, 64), atlas["korim.png"].size)
	//}

	private val BmpSlice.size get() = Size(width, height)
}
