import com.soywiz.klock.*
import com.soywiz.korev.*
import com.soywiz.korge.*
import com.soywiz.korge.component.docking.*
import com.soywiz.korge.tiled.*
import com.soywiz.korge.view.*
import com.soywiz.korge.view.animation.*
import com.soywiz.korim.atlas.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korim.color.*
import com.soywiz.korim.format.*
import com.soywiz.korio.file.std.*
import com.soywiz.korio.stream.*
import com.soywiz.korma.geom.*
import com.soywiz.korma.geom.vector.*
import kotlinx.coroutines.*

//suspend fun main() {
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
//}

suspend fun main() = Korge {
    mainVampire()
    //mainCompression()
    //println("HELLO WORLD!")
    //withContext(Dispatchers.Unconfined) {
}

suspend fun Stage.mainVampire() {
    val atlas = MutableAtlasUnit(1024, 512, border = 2)

    val sw = Stopwatch().start()

    resourcesVfs["korim.png"].readBitmapSlice().split(32, 32).toAtlas(atlas = atlas)
    val korim = resourcesVfs["korim.png"].readBitmapSlice(atlas = atlas)
    val characters = resourcesVfs["characters.ase"].readImageDataContainer(ASE, atlas = atlas)
    val slices = resourcesVfs["slice-example.ase"].readImageDataContainer(ASE, atlas = atlas)
    val tiledMap = resourcesVfs["Tilemap/untitled.tmx"].readTiledMap(atlas = atlas)

    println(sw.elapsed)

    //image(korim)

    //image(atlas.bitmap);return

    lateinit var tiledMapView: TiledMapView

    container {
        scale(2.0)
        //tiledMapView(tiledMap, smoothing = false)
        tiledMapView = tiledMapView(tiledMap, smoothing = true)
    }

    container {
        scale = 2.0
        imageDataView(slices["wizHat"]).xy(0, 50)
        imageDataView(slices["giantHilt"]).xy(32, 50)
        imageDataView(slices["pumpkin"]).xy(64, 50)
    }

    //image(tiledMapView.collisionToBitmap()).scale(2.0)

    //val ase2 = resourcesVfs["vampire.ase"].readImageData(ASE, atlas = atlas)
    //val ase3 = resourcesVfs["vampire.ase"].readImageData(ASE, atlas = atlas)
    //for (bitmap in atlas.allBitmaps) image(bitmap) // atlas generation

    //val gg = buildPath {
    //    rect(300, 0, 100, 100)
    //    circle(400, 400, 50)
    //    star(5, 30.0, 100.0, x = 400.0, y = 300.0)
    //    //star(400, 400, 50)
    //}

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

    container {
        keepChildrenSortedByY()

        val character1 = imageDataView(characters["vampire"], "down") {
            stop()
            xy(200, 200)
        }

        val character2 = imageDataView(characters["vamp"], "down") {
            stop()
            xy(160, 110)
        }

        val hitTestable = listOf(tiledMapView, gg).toHitTestable()

        controlWithKeyboard(character1, hitTestable, up = Key.UP, right = Key.RIGHT, down = Key.DOWN, left = Key.LEFT,)
        controlWithKeyboard(character2, hitTestable, up = Key.W, right = Key.D, down = Key.S, left = Key.A)
    }
}

fun TiledMapView.collisionToBitmap(): Bitmap {
    val bmp = Bitmap32(this.width.toInt(), this.height.toInt())
    for (y in 0 until bmp.height) for (x in 0 until bmp.width) {
        bmp[x, y] = if (pixelHitTest(x, y) != null) Colors.WHITE else Colors.TRANSPARENT_BLACK
    }
    return bmp
}

fun Stage.controlWithKeyboard(
    char: ImageDataView,
    collider: HitTestable,
    up: Key = Key.UP,
    right: Key = Key.RIGHT,
    down: Key = Key.DOWN,
    left: Key = Key.LEFT,
) {
    addUpdater { dt ->
        val speed = 5.0 * (dt / 16.0.milliseconds)
        var dx = 0.0
        var dy = 0.0
        val pressingLeft = keys[left]
        val pressingRight = keys[right]
        val pressingUp = keys[up]
        val pressingDown = keys[down]
        if (pressingLeft) dx = -1.0
        if (pressingRight) dx = +1.0
        if (pressingUp) dy = -1.0
        if (pressingDown) dy = +1.0
        if (dx != 0.0 || dy != 0.0) {
            val dpos = Point(dx, dy).normalized * speed
            char.moveWithHitTestable(collider, dpos.x, dpos.y)
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
