package com.soywiz.korge.tiled

import com.soywiz.kmem.*
import com.soywiz.korge.internal.*
import com.soywiz.korge.tests.*
import com.soywiz.korge.view.tiles.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korim.color.*
import com.soywiz.korim.format.*
import com.soywiz.korio.async.*
import com.soywiz.korio.file.std.*
import kotlin.test.*

class TiledMapTest : ViewsForTesting() {
	@Test
	@Ignore // Must fix mapping first
	fun name() = viewsTest {
		disableNativeImageLoading {
			//class Demo(@Path("sample.tmx") val map: TiledMap)
			class Demo(val map: TiledMap)

			val demo = injector.get<Demo>()
			val map = demo.map
			assertEquals(1, map.tilesets.size)
			assertEquals(1, map.tilesets.first().firstgid)
			assertEquals(256, map.tilesets.first().tileset.textures.size)
			assertEquals(3, map.allLayers.size)
			assertEquals(1, map.imageLayers.size)
			assertEquals(1, map.objectLayers.size)
			assertEquals(1, map.patternLayers.size)
			//println(map)
			//println(demo.map)
		}
	}

	@Test
	fun testRenderInBounds() {
		val renderTilesCounter = views.stats.counter("renderedTiles")
		val tileset = TileSet(Bitmap32(32, 32).slice(), 32, 32)
		val map = TileMap(Bitmap32(200, 200), tileset)
		views.stage += map
		views.frameUpdateAndRender()
		assertEquals(DefaultViewport.WIDTH, views.actualVirtualWidth)
		assertEquals(DefaultViewport.HEIGHT, views.actualVirtualHeight)
		views.render()
		//assertEquals(300, count)
		//assertEquals(336, renderTilesCounter.countThisFrame) // Update if optimized when no decimal scrolling
		assertEquals(943, renderTilesCounter.countThisFrame) // Update if optimized when no decimal scrolling
	}

	@Test
    @Ignore
	fun testObjProps() = suspendTest {
		val data = resourcesVfs["tiled/library1.tmx"].readTiledMapData()
		val librarian = data.getObjectByName("librarian")!!
		assertEquals("hair-girl1", librarian.objprops["hair"])
		assertTrue(librarian.objprops["script"].toString().isNotBlank())
		assertTrue(librarian.objprops["script"].toString().contains("wait(1.5.seconds)"))
		assertTrue(librarian.objprops["script"].toString().contains("move(\"librarian\")"))
	}

    @Test
    fun testUnsignedIntUid() = suspendTestNoJs {
        resourcesVfs["tiled/Spaceship 3.tmx"].readTiledMapData()
        resourcesVfs["tiled/Spaceship 3b.tmx"].readTiledMapData()
        resourcesVfs["tiled/Spaceship 3c.tmx"].readTiledMapData()
        resourcesVfs["tiled/Spaceship 3d.tmx"].readTiledMapData()
        resourcesVfs["tiled/Spaceship 3e.tmx"].readTiledMapData()
        resourcesVfs["tiled/Spaceship 3f.tmx"].readTiledMapData()
    }

    @Test
    fun testMultiTexture() = suspendTestNoJs {
        val tileSet = TileSet(listOf(Bitmap32(32, 32, Colors.RED).slice(), Bitmap32(32, 32, Colors.BLUE).slice()), 32, 32)
        val tileMap = TileMap(Bitmap32(32, 32), tileSet)
        tileMap.intMap[0, 0] = 0
        tileMap.intMap[1, 0] = 1
        tileMap.render(views.renderContext)
    }

    @Test
    fun testTileMapFlipRotateIndices() {
        assertEquals(
            "0123, 0321, 3210, 1230, 1032, 3012, 2301, 2103",
            (0 until 8).joinToString(", ") {
                TileMap.computeIndices(flipX = it.extract(2), flipY = it.extract(1), rotate = it.extract(0))
                    .joinToString("")
            }
        )
    }
}
