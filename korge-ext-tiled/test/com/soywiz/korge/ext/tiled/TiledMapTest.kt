package com.soywiz.korge.ext.tiled

import com.soywiz.korge.resources.Path
import com.soywiz.korge.tests.ViewsForTesting
import com.soywiz.korim.format.disableNativeImageLoading
import org.junit.Assert
import org.junit.Test

class TiledMapTest : ViewsForTesting() {
	@Test
	fun name() = syncTest {
		disableNativeImageLoading {
			class Demo(@Path("sample.tmx") val map: TiledMap)

			val demo = injector.get<Demo>()
			val map = demo.map
			Assert.assertEquals(1, map.tilesets.size)
			Assert.assertEquals(1, map.tilesets.first().firstgid)
			Assert.assertEquals(256, map.tilesets.first().tileset.textures.size)
			Assert.assertEquals(3, map.allLayers.size)
			Assert.assertEquals(1, map.imageLayers.size)
			Assert.assertEquals(1, map.objectLayers.size)
			Assert.assertEquals(1, map.patternLayers.size)
			//println(map)
			//println(demo.map)
		}
	}
}
