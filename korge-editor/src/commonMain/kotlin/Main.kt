import com.soywiz.klock.*
import com.soywiz.korev.*
import com.soywiz.korge.*
import com.soywiz.korge.view.*
import com.soywiz.korge.view.animation.*
import com.soywiz.korim.atlas.*
import com.soywiz.korim.format.*
import com.soywiz.korio.*
import com.soywiz.korio.file.std.*
import com.soywiz.korio.stream.*
import kotlinx.coroutines.*

suspend fun main() = Korge {
    mainVampire()
    //mainCompression()
    //println("HELLO WORLD!")
    //withContext(Dispatchers.Unconfined) {
}


suspend fun Stage.mainVampire() {
    val atlas = MutableAtlas<Unit>(512, 512)
    //val ase = resourcesVfs["vampire.ase"].readImageData(ASE, atlas = atlas)
    //val slices = resourcesVfs["slice-example.ase"].readImageDataContainer(ASE, atlas = atlas)
    val sw = Stopwatch().start()
    val ase = resourcesVfs["vampire.ase"].readImageData(ASE, atlas = atlas)
    val slices = resourcesVfs["slice-example.ase"].readImageDataContainer(ASE, atlas = atlas)
    //for (n in 0 until 10000) {
    //    resourcesVfs["vampire.ase"].readImageData(ASE, atlas = atlas)
    //    resourcesVfs["slice-example.ase"].readImageDataContainer(ASE, atlas = atlas)
    //}
    println(sw.elapsed)

    //image(atlas.bitmap)

    container {
        scale = 2.0
        imageDataView(slices["wizHat"]).xy(0, 50)
        imageDataView(slices["giantHilt"]).xy(32, 50)
        imageDataView(slices["pumpkin"]).xy(64, 50)
    }

    //val ase2 = resourcesVfs["vampire.ase"].readImageData(ASE, atlas = atlas)
    //val ase3 = resourcesVfs["vampire.ase"].readImageData(ASE, atlas = atlas)
    //for (bitmap in atlas.allBitmaps) image(bitmap) // atlas generation

    val character = imageDataView(ase, "down") {
        stop()
    }

    addUpdater {
        val left = keys[Key.LEFT]
        val right = keys[Key.RIGHT]
        val up = keys[Key.UP]
        val down = keys[Key.DOWN]
        if (left) character.x -= 2.0
        if (right) character.x += 2.0
        if (up) character.y -= 2.0
        if (down) character.y += 2.0
        character.animation = when {
            left -> "left"; right -> "right"; up -> "up"; down -> "down"
            else -> character.animation
        }
        if (left || right || up || down) {
            character.play()
        } else {
            character.stop()
            character.rewind()
        }
    }
}

suspend fun mainCompression() {
    //run {
    withContext(Dispatchers.Unconfined) {
        val mem = MemoryVfsMix()
        val zipFile = localVfs("c:/temp", async = true)["1.zip"]
        val zipBytes = zipFile.readAll()
        println("ELAPSED TIME [NATIVE]: " + measureTime {
            //localVfs("c:/temp")["1.zip"].openAsZip()["2012-07-15-wheezy-raspbian.img"].copyTo(mem["test.img"])
            //localVfs("c:/temp")["iTunes64Setup.zip"].openAsZip().listSimple().first().copyTo(mem["test.out"])
            //localVfs("c:/temp")["iTunes64Setup.zip"].readAll().openAsync().openAsZip().listSimple().first().copyTo(mem["test.out"])
            //localVfs("c:/temp")["iTunes64Setup.zip"].openAsZip().listSimple().first().copyTo(mem["test.out"])
            //localVfs("c:/temp")["1.zip"].readAll().openAsync().openAsZip()["2012-07-15-wheezy-raspbian.img"].copyTo(localVfs("c:/temp/temp.img"))
            //localVfs("c:/temp")["1.zip"].openAsZip(useNativeDecompression = true)["2012-07-15-wheezy-raspbian.img"].copyTo(localVfs("c:/temp/temp.img"))
            zipBytes.openAsync().openAsZip(useNativeDecompression = true)["2012-07-15-wheezy-raspbian.img"].copyTo(
                DummyAsyncOutputStream
            )
        })
        println("ELAPSED TIME [PORTABLE]: " + measureTime {
            //localVfs("c:/temp")["1.zip"].openAsZip()["2012-07-15-wheezy-raspbian.img"].copyTo(mem["test.img"])
            //localVfs("c:/temp")["iTunes64Setup.zip"].openAsZip().listSimple().first().copyTo(mem["test.out"])
            //localVfs("c:/temp")["iTunes64Setup.zip"].readAll().openAsync().openAsZip().listSimple().first().copyTo(mem["test.out"])
            //localVfs("c:/temp")["iTunes64Setup.zip"].openAsZip().listSimple().first().copyTo(mem["test.out"])
            //localVfs("c:/temp")["1.zip"].readAll().openAsync().openAsZip()["2012-07-15-wheezy-raspbian.img"].copyTo(localVfs("c:/temp/temp.img"))
            //localVfs("c:/temp")["1.zip"].openAsZip(useNativeDecompression = false)["2012-07-15-wheezy-raspbian.img"].copyTo(localVfs("c:/temp/temp2.img"))
            //localVfs("c:/temp", async = true)["1.zip"].openAsZip(useNativeDecompression = false)["2012-07-15-wheezy-raspbian.img"].copyTo(DummyAsyncOutputStream)
            zipBytes.openAsync().openAsZip(useNativeDecompression = false)["2012-07-15-wheezy-raspbian.img"].copyTo(
                DummyAsyncOutputStream
            )
        })
    }
}
