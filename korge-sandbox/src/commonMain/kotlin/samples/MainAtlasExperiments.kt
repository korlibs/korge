package samples

import com.soywiz.korge.scene.Scene
import com.soywiz.korge.view.Container

class MainAtlasExperiments : Scene() {
    override suspend fun Container.sceneMain() {
        //    for (n in 0 until 1000) {
        //        val sw = Stopwatch().start()
        //        val atlas = MutableAtlasUnit(1024, 1024)
        //        atlas.add(Bitmap32(64, 64))
        //        //val ase = resourcesVfs["vampire.ase"].readImageData(ASE, atlas = atlas)
        //        //val slices = resourcesVfs["slice-example.ase"].readImageDataContainer(ASE, atlas = atlas)
        //        //resourcesVfs["korim.png"].readBitmapSlice().split(32, 32).toAtlas(atlas = atlas)
        //        //val korim = resourcesVfs["korim.png"].readBitmapSlice(atlas = atlas)
        //        val aseAll = resourcesVfs["characters.ase"].readImageDataContainer(ASE, atlas = atlas)
        //        val slices = resourcesVfs["slice-example.ase"].readImageDataContainer(ASE, atlas = atlas)
        //        val vampireSprite = aseAll["vampire"]
        //        val vampSprite = aseAll["vamp"]
        //        val tiledMap = resourcesVfs["Tilemap/untitled.tmx"].readTiledMap(atlas = atlas)
        //        //val ase = aseAll["vamp"]
        //        //for (n in 0 until 10000) {
        //        //    resourcesVfs["vampire.ase"].readImageData(ASE, atlas = atlas)
        //        //    resourcesVfs["slice-example.ase"].readImageDataContainer(ASE, atlas = atlas)
        //        //}
        //        println(sw.elapsed)
        //    }
    }
}
