import com.soywiz.klock.*
import com.soywiz.korev.*
import com.soywiz.korge.*
import com.soywiz.korge.component.docking.*
import com.soywiz.korge.view.*
import com.soywiz.korge.view.animation.*
import com.soywiz.korim.atlas.*
import com.soywiz.korim.color.*
import com.soywiz.korim.format.*
import com.soywiz.korio.file.std.*
import com.soywiz.korio.stream.*
import com.soywiz.korma.geom.collider.*
import com.soywiz.korma.geom.vector.*
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
    val aseAll = resourcesVfs["characters.ase"].readImageDataContainer(ASE, atlas = atlas)
    val slices = resourcesVfs["slice-example.ase"].readImageDataContainer(ASE, atlas = atlas)
    val vampireSprite = aseAll["vampire"]
    val vampSprite = aseAll["vamp"]
    //val ase = aseAll["vamp"]
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

    val gg = graphics {
        fill(Colors.RED) {
            rect(300, 0, 100, 100)
        }
        fill(Colors.RED) {
            circle(400, 400, 50)
        }
        fill(Colors.BLUE) {
            star(5, 30.0, 100.0, x = 400.0, y = 300.0)
            //star(400, 400, 50)
        }
    }
    //val path = buildPath { rect(300, 0, 100, 100) }
    val collider = gg.toCollider()

    container {
        keepChildrenSortedByY()

        val character1 = imageDataView(vampireSprite, "down") {
            stop()
            xy(100, 100)
        }

        val character2 = imageDataView(vampSprite, "down") {
            stop()
            xy(120, 110)
        }

        controlWithKeyboard(character1, collider, up = Key.UP, right = Key.RIGHT, down = Key.DOWN, left = Key.LEFT)
        controlWithKeyboard(character2, collider, up = Key.W, right = Key.D, down = Key.S, left = Key.A)
    }
}

fun Stage.controlWithKeyboard(
    char: ImageDataView,
    collider: MovementCollider,
    up: Key = Key.UP,
    right: Key = Key.RIGHT,
    down: Key = Key.DOWN,
    left: Key = Key.LEFT,
) {
    addUpdater {
        val speed = 2.0
        var dx = 0.0
        var dy = 0.0
        val pressingLeft = keys[left]
        val pressingRight = keys[right]
        val pressingUp = keys[up]
        val pressingDown = keys[down]
        if (pressingLeft) dx = -speed
        if (pressingRight) dx = +speed
        if (pressingUp) dy = -speed
        if (pressingDown) dy = +speed
        if (dx != 0.0 || dy != 0.0) {
            char.moveWithCollider(dx, dy, collider)
        }
        char.animation = when {
            pressingLeft -> "left"
            pressingRight -> "right"
            pressingUp -> "up"
            pressingDown -> "down"
            else -> char.animation
        }
        if (pressingLeft || pressingRight || pressingUp || pressingDown) {
            char.play()
        } else {
            char.stop()
            char.rewind()
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
