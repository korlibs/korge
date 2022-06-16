import com.soywiz.kds.extraProperty
import com.soywiz.kds.fastArrayListOf
import com.soywiz.klock.Stopwatch
import com.soywiz.klock.measureTime
import com.soywiz.klock.milliseconds
import com.soywiz.korev.Key
import com.soywiz.korge.Korge
import com.soywiz.korge.component.docking.dockedTo
import com.soywiz.korge.component.docking.keepChildrenSortedByY
import com.soywiz.korge.input.draggable
import com.soywiz.korge.input.keys
import com.soywiz.korge.input.mouse
import com.soywiz.korge.input.onClick
import com.soywiz.korge.input.onMouseDrag
import com.soywiz.korge.tiled.TiledMapView
import com.soywiz.korge.tiled.readTiledMap
import com.soywiz.korge.tiled.tiledMapView
import com.soywiz.korge.ui.UIButtonToggleableGroup
import com.soywiz.korge.ui.UIText
import com.soywiz.korge.ui.UITreeViewList
import com.soywiz.korge.ui.UITreeViewNode
import com.soywiz.korge.ui.group
import com.soywiz.korge.ui.tooltip
import com.soywiz.korge.ui.uiButton
import com.soywiz.korge.ui.uiGridFill
import com.soywiz.korge.ui.uiHorizontalStack
import com.soywiz.korge.ui.uiImage
import com.soywiz.korge.ui.uiTooltipContainer
import com.soywiz.korge.ui.uiTreeView
import com.soywiz.korge.ui.uiVerticalStack
import com.soywiz.korge.view.SolidRect
import com.soywiz.korge.view.Stage
import com.soywiz.korge.view.View
import com.soywiz.korge.view.addUpdater
import com.soywiz.korge.view.alpha
import com.soywiz.korge.view.anchor
import com.soywiz.korge.view.animation.ImageDataView
import com.soywiz.korge.view.animation.imageDataView
import com.soywiz.korge.view.centered
import com.soywiz.korge.view.circle
import com.soywiz.korge.view.container
import com.soywiz.korge.view.graphics
import com.soywiz.korge.view.image
import com.soywiz.korge.view.line
import com.soywiz.korge.view.moveWithCollisions
import com.soywiz.korge.view.outline
import com.soywiz.korge.view.rotation
import com.soywiz.korge.view.scale
import com.soywiz.korge.view.size
import com.soywiz.korge.view.solidRect
import com.soywiz.korge.view.text
import com.soywiz.korge.view.xy
import com.soywiz.korim.annotation.KorimInternal
import com.soywiz.korim.atlas.MutableAtlasUnit
import com.soywiz.korim.bitmap.Bitmap
import com.soywiz.korim.bitmap.Bitmap32
import com.soywiz.korim.bitmap.BitmapSlice
import com.soywiz.korim.bitmap.context2d
import com.soywiz.korim.bitmap.flippedX
import com.soywiz.korim.bitmap.rotatedRight
import com.soywiz.korim.bitmap.slice
import com.soywiz.korim.bitmap.trace.trace
import com.soywiz.korim.bitmap.transformed
import com.soywiz.korim.color.Colors
import com.soywiz.korim.color.mix
import com.soywiz.korim.font.DefaultTtfFont
import com.soywiz.korim.font.SystemFont
import com.soywiz.korim.font.readTtfFont
import com.soywiz.korim.font.withFallback
import com.soywiz.korim.format.ASE
import com.soywiz.korim.format.readBitmap
import com.soywiz.korim.format.readBitmapSlice
import com.soywiz.korim.format.readImageDataContainer
import com.soywiz.korim.format.toAtlas
import com.soywiz.korim.text.text
import com.soywiz.korio.file.std.MemoryVfsMix
import com.soywiz.korio.file.std.localVfs
import com.soywiz.korio.file.std.openAsZip
import com.soywiz.korio.file.std.resourcesVfs
import com.soywiz.korio.stream.DummyAsyncOutputStream
import com.soywiz.korio.stream.openAsync
import com.soywiz.korma.geom.Anchor
import com.soywiz.korma.geom.Matrix
import com.soywiz.korma.geom.Point
import com.soywiz.korma.geom.Ray
import com.soywiz.korma.geom.Rectangle
import com.soywiz.korma.geom.RectangleInt
import com.soywiz.korma.geom.ScaleMode
import com.soywiz.korma.geom.Size
import com.soywiz.korma.geom.cosine
import com.soywiz.korma.geom.degrees
import com.soywiz.korma.geom.ds.BVH2D
import com.soywiz.korma.geom.minus
import com.soywiz.korma.geom.plus
import com.soywiz.korma.geom.shape.Shape2d
import com.soywiz.korma.geom.shape.buildPath
import com.soywiz.korma.geom.sine
import com.soywiz.korma.geom.vector.circle
import com.soywiz.korma.geom.vector.rect
import com.soywiz.korma.geom.vector.rectHole
import com.soywiz.korma.geom.vector.roundRect
import com.soywiz.korma.geom.vector.star
import com.soywiz.korma.geom.vector.write
import com.soywiz.korma.random.get
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.random.Random

suspend fun main() = Korge(
    bgcolor = Colors.DARKCYAN.mix(Colors.BLACK, 0.8),
    clipBorders = false,
    //scaleMode = ScaleMode.EXACT,
    //debug = true,
    multithreaded = true,
    //debugAg = true,
) {
    //mainArc()

    //mainStrokesExperiment3()
    //mainStrokesExperiment2()
    //mainStrokesExperiment()

    //mainGpuVectorRendering3()
    //mainGpuVectorRendering2()
    mainGpuVectorRendering()

    //mainSvgAnimation()

    //mainDpi()
    //mainZIndex()
    //mainCircleColor()
    //mainFilterSwitch()
    //mainVectorFill()
    //mainEasing()
    //mainTweenPoint()
    //mainBezier()
    //mainClipping()
    //mainTextureIssue()
    //mainTilemapTest()
    //mainTransition()
    //mainClipping()
    //mainGifAnimation()
    //mainDraggable()
    //mainSkybox()
    //mainHaptic()
    //mainMasks()
    //mainColorPicker()
    //mainFiltersRenderToBitmap()
    //mainBlur()
    //mainCustomSolidRectShader()
    //mainMipmaps()
    //mainColorTransformFilter()
    //mainExifTest()
    //mainFilterScale()
    //mainVectorRendering()
    //mainRenderText()
    //mainTextMetrics()
    //mainBitmapTexId()
    //mainFiltersSample()
    //mainKorviSample()
    //mainUIImageTester()

    /*
    val atlas = MutableAtlasUnit(4096)
    image(resourcesVfs["Portrait_3.jpg"].readBitmapSlice(atlas = atlas)).scale(0.2)
    image(resourcesVfs["Portrait_3.jpg"].readBitmapSliceWithOrientation(atlas = atlas)).scale(0.2).xy(300, 0)
    image(atlas.bitmap).scale(0.2).xy(600, 0)
    */


    //rotatedTexture()
    //mainUITreeView()
    //mainUIImageTester()
    //mainEditor()
    //mainTrimmedAtlas()
    //mainRotateCircle()
    //mainImageTrace()
    //mainEmoji()
    //mainUITreeView()
    //Bunnymark().apply { bunnymarkMain() }
    //bezierSample()
    //particlesMain()
    //terminalEmulatorMain()
    //mainBVH()
    //mainCircles()
    //mainVampire()
    //mainCompression()
    //println("HELLO WORLD!")
    //withContext(Dispatchers.Unconfined) {
}

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

suspend fun Stage.mainCircleColor() {
    circle(100.0).also { shape ->
    //roundRect(100.0, 200.0, 50.0, 50.0).also { shape ->
        shape.x = 100.0
        shape.y = 100.0
        //it.colorMul = Colors.RED.withAd(0.9)
        shape.stroke = Colors.RED
        shape.fill = Colors.GREEN
        shape.strokeThickness = 16.0
        addUpdater { shape.radius += 1.0 }
    }
}

suspend fun Stage.rotatedTexture() {
    //val tex = resourcesVfs["korim.png"].readBitmapSlice().rotateRight()
    //val tex = resourcesVfs["korim.png"].readBitmapSlice().flipY()
    //val tex = resourcesVfs["korim.png"].readBitmapSlice().transformed(Matrix().scale(.5f, .5f)).sliceWithSize(0, 0, 10, 10)
    //val tex = resourcesVfs["korim.png"].readBitmapSlice().transformed(Matrix().scale(.5f, .5f))
    val tex = resourcesVfs["korim.png"].readBitmapSlice().transformed(Matrix().skew(30.degrees, 0.degrees)).flippedX().rotatedRight()
    println("tex=$tex")
    println("size=${tex.width},${tex.height}")
    image(tex)
}

suspend fun Stage.mainTrimmedAtlas() {
    val bmp = BitmapSlice(
        Bitmap32(64, 64) { x, y -> Colors.PURPLE },
        RectangleInt(0, 0, 64, 64),
        virtFrame = RectangleInt(64, 64, 196, 196)
    )
    val image = image(bmp).anchor(0.5, 1.0).xy(200, 200).rotation(30.degrees)
    val image2 = image(bmp).anchor(0.0, 0.0).xy(200, 200)
    //addUpdater { image.rotation += 4.degrees }
}

suspend fun Stage.mainRotateCircle() {
    //val circle = circle(radius = 50.0, fill = Colors.RED, stroke = Colors.BLUE, strokeThickness = 20.0).xy(0, 0).also {
    //val circle = circle(radius = 50.0, fill = Colors.RED, stroke = Colors.BLUE, strokeThickness = 20.0).xy(0, 0).centered.also {
    solidRect(300.0, 300.0, Colors.YELLOW).xy(250, 250).centered
    val circle = circle(radius = 150.0, fill = Colors.RED, stroke = Colors.BLUE, strokeThickness = 40.0).xy(250, 250).centered.also {
        //val circle = circle(radius = 50.0, fill = Colors.RED).xy(100, 100).centered.also {
        it.autoScaling = false
        //it.autoScaling = true
        //it.preciseAutoScaling = true
        //it.useNativeRendering = false
    }
    graphics {
        fill(Colors.PURPLE) {
            rect(-50, -50, 60, 60)
        }
    }
    stage.keys {
        downFrame(Key.LEFT) { circle.rotation -= 10.degrees }
        downFrame(Key.RIGHT) { circle.rotation += 10.degrees }
    }
}


suspend fun Stage.mainImageTrace() {
    val bmp = Bitmap32(300, 200).context2d {
        fill(Colors.WHITE) {
            rect(Rectangle.fromBounds(2, 2, 18, 18))
            rectHole(Rectangle.fromBounds(6, 6, 9, 12))
            rectHole(Rectangle.fromBounds(10, 5, 15, 12))
            rect(Rectangle.fromBounds(50, 2, 68, 18))
            circle(100, 100, 60)
            circle(100, 100, 30)
            roundRect(200, 50, 50, 50, 5, 5)
            circle(140, 100, 30)
        }
    }
    val path = bmp.trace()
    image(bmp)
    graphics { fill(Colors.RED) { write(path) } }.xy(50, 50).scale(3).alpha(0.5)
    //image(bmp)
    /*
    Bitmap2(bmp.width, bmp.height).also {
        for (y in 0 until)
        it[x, y] =
    }
    bmp.
    bmp.trace()
     */
}

@OptIn(KorimInternal::class)
suspend fun Stage.mainEmoji() {
    println("coroutineContext: $coroutineContext")
    image(resourcesVfs["korge.png"].readBitmap())
    //val fontEmojiOther = localVfs("C:/temp/emoji.ttf").takeIfExists()?.readTtfFont()
    val fontEmojiOther = SystemFont("emoji")
    val fontEmojiApple = localVfs("C:/temp/AppleColorEmoji.ttf").takeIfExists()?.readTtfFont()
    val fontEmojiSystem = SystemFont.getEmojiFont()
    val font0 = DefaultTtfFont.withFallback(SystemFont.getDefaultFont())
    println("fontEmojiOther=$fontEmojiOther")
    println("fontEmojiApple=$fontEmojiApple")
    println("fontEmojiSystem=$fontEmojiSystem")
    println("font0=$font0")
    println("fontList=${kotlin.runCatching { localVfs("/system/fonts").listNames() }}")
    println("/System/Library/Fonts/Cache=${kotlin.runCatching { localVfs("/System/Library/Fonts/Cache").listNames() }}")
    println("/System/Library/Fonts=${kotlin.runCatching { localVfs("/System/Library/Fonts").listNames() }}")
    println("/System/Library/Fonts/Core=${kotlin.runCatching { localVfs("/System/Library/Fonts/Core").listNames() }}")
    println("listFontNamesWithFiles=${kotlin.runCatching { SystemFont.listFontNamesWithFiles() }}")
    //val font0 = localVfs("C:/temp/FrankRuhlHofshi-Regular-ttf.ttf").readTtfFont()
    //val font0 = SystemFont.getDefaultFont().ttf
    //val font0 = SystemFont("Arial Unicode").ttf
    //val font0 = localVfs("c:/temp/arialuni.ttf").readTtfFont()
    val font1 = font0.withFallback(fontEmojiApple, fontEmojiSystem)
    val font2 = font0.withFallback(fontEmojiSystem)
    val font3 = font0.withFallback(fontEmojiOther)

    text("HELLO　зклмн 😃😀😁😂🥰🤩🦍", font = font1, textSize = 90.0).xy(100, 100)
    text("HELLO　쌍디귿 😃😀😁😂🥰🤩🦍", font = font2, textSize = 90.0).xy(100, 228)
    text("HELLO　あかめ私 😃\uD83D\uDDB9", font = font3, textSize = 90.0).xy(100, 368)

    graphics {
        fill(Colors.RED) {
            text("h̷̷̶̨͋ͩ̏ͣ̒̉ͤ͛̓̄͢͡͠͡͏͈̬̜̲̙̤̙̤̯e̷͛̒ͪ́ͤ̒̃͏̶͠͏̞̰̻͙̟̜͕̞̮͟͟͡ļ̸̥͎̼̪̘̜̞͓̩ͧ̈̌ͣͨ́̕͡͞ͅl̡̡̛̦̫͖̞̯̻̓̆͆̑̅ͣ̑̕̕͡ͅǫ̴̸̊͐̈́̈̀͛̾́͏̸̡̡̦̤̦͚̬̯͔͉͇́͞HELLO　зклмн 쌍디귿 あかめ私 😃\uD83D\uDDB9", font = font3, textSize = 90.0, x = 100.0, y = 368.0)
        }
    }
}

var SolidRect.movingDirection by extraProperty { -1 }

suspend fun Stage.mainBVH() {
    val bvh = BVH2D<View>()
    val rand = Random(0)
    val rects = arrayListOf<SolidRect>()
    for (n in 0 until 2_000) {
        val x = rand[0.0, width]
        val y = rand[0.0, height]
        val width = rand[1.0, 50.0]
        val height = rand[1.0, 50.0]
        val view = solidRect(width, height, rand[Colors.RED, Colors.BLUE]).xy(x, y)
        view.movingDirection = if (rand.nextBoolean()) -1 else +1
        rects += view
        bvh.insertOrUpdate(view.globalBounds, view)
    }
    addUpdater {
        for (n in rects.size - 100 until rects.size) {
            val view = rects[n]
            if (view.x < 0) {
                view.movingDirection = +1
            }
            if (view.x > stage.width) {
                view.movingDirection = -1
            }
            view.x += view.movingDirection
            bvh.insertOrUpdate(view.globalBounds, view)
        }
    }
    val center = Point(width / 2, height / 2)
    val dir = Point(-1, -1)
    val ray = Ray(center, dir)
    val statusText = text("", font = views.debugBmpFont)
    var selectedRectangle = Rectangle(Point(100, 100) - Point(50, 50), Size(100, 100))
    val rayLine = line(center, center + (dir * 1000), Colors.WHITE)
    val selectedRect = outline(buildPath { rect(selectedRectangle) })
    //outline(buildPath { star(5, 50.0, 100.0, x = 100.0, y = 100.0) })
    //debugLine(center, center + (dir * 1000), Colors.WHITE)
    fun updateRay() {
        var allObjectsSize = 0
        var rayObjectsSize = 0
        var rectangleObjectsSize = 0
        val allObjects = bvh.search(Rectangle(0.0, 0.0, width, height))
        val time = measureTime {
            val rayObjects = bvh.intersect(ray)
            val rectangleObjects = bvh.search(selectedRectangle)
            for (result in allObjects) result.value?.alpha = 0.2
            for (result in rectangleObjects) result.value?.alpha = 0.8
            for (result in rayObjects) result.obj.value?.alpha = 1.0
            allObjectsSize = allObjects.size
            rayObjectsSize = rayObjects.size
            rectangleObjectsSize = rectangleObjects.size
        }
        statusText.text = "All objects: ${allObjectsSize}, raycast = ${rayObjectsSize}, rect = ${rectangleObjectsSize}, time = $time"
    }
    updateRay()

    addUpdater {
        //println("moved")
        val mousePos = stage.mouseXY
        val angle = Point.angleFull(center, mousePos)
        //println("center=$center, mousePos=$mousePos, angle = $angle")
        dir.setTo(angle.cosine, angle.sine)
        rayLine.setPoints(center, center + (dir * 1000))

        updateRay()
    }

    mouse {
        onDown {
            selectedRectangle = Rectangle(stage.mouseXY - Point(50, 50), Size(100, 100))
            selectedRect.vectorPath = buildPath { rect(selectedRectangle) }
        }
        onMouseDrag {
            selectedRectangle = Rectangle(stage.mouseXY - Point(50, 50), Size(100, 100))
            selectedRect.vectorPath = buildPath { rect(selectedRectangle) }
        }
    }
}

suspend fun Stage.mainCircles() {
    // @TODO: USe BVH2D to limit collision view checkings
    lateinit var collisionViews: List<View>
    val rect1 = circle(50.0, fill = Colors.RED).xy(300, 300).centered
    val rect1b = circle(50.0, fill = Colors.RED).xy(520, 300).centered
    val rect2 = circle(50.0, fill = Colors.GREEN).xy(120, 0).draggable(autoMove = false) {
        //it.view.xy(it.viewPrevXY)
        it.view.moveWithCollisions(collisionViews, it.viewDeltaXY)
    }
    collisionViews = fastArrayListOf<View>(rect1, rect1b, rect2)
    println(rect1.hitShape2d)
    println(rect2.hitShape2d)
    addUpdater { dt ->
        val dx = keys.getDeltaAxis(Key.LEFT, Key.RIGHT)
        val dy = keys.getDeltaAxis(Key.UP, Key.DOWN)
        //if (dx != 0.0 || dy != 0.0) {
            val speed = (dt / 16.milliseconds) * 5.0
            rect2.moveWithCollisions(collisionViews, dx * speed, dy * speed)
        //}
        //rect2.alpha = if (rect1.collidesWith(rect2, kind = CollisionKind.SHAPE)) 1.0 else 0.3
    }
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
            hitShape2d = Shape2d.Rectangle.fromBounds(-8.0, -3.0, +8.0, +3.0)
        }

        val character2 = imageDataView(characters["vamp"], "down") {
            stop()
            xy(160, 110)
        }

        //val hitTestable = listOf(tiledMapView, gg).toHitTestable()
        val hitTestable = listOf(gg)

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
    collider: List<View>,
    up: Key = Key.UP,
    right: Key = Key.RIGHT,
    down: Key = Key.DOWN,
    left: Key = Key.LEFT,
) {
    addUpdater { dt ->
        val speed = 5.0 * (dt / 16.0.milliseconds)
        val dx = keys.getDeltaAxis(left, right)
        val dy = keys.getDeltaAxis(up, down)
        if (dx != 0.0 || dy != 0.0) {
            val dpos = Point(dx, dy).normalized * speed
            char.moveWithCollisions(collider, dpos.x, dpos.y)
        }
        char.animation = when {
            dx < 0.0 -> "left"
            dx > 0.0 -> "right"
            dy < 0.0 -> "up"
            dy > 0.0 -> "down"
            else -> char.animation
        }
        if (dx != 0.0 || dy != 0.0) {
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

suspend fun Stage.mainUIImageTester() {
    solidRect(10, 10, Colors.RED).anchor(Anchor.TOP_LEFT).dockedTo(Anchor.TOP_LEFT)
    solidRect(10, 10, Colors.GREEN).anchor(Anchor.TOP_RIGHT).dockedTo(Anchor.TOP_RIGHT)
    solidRect(10, 10, Colors.BLUE).anchor(Anchor.BOTTOM_RIGHT).dockedTo(Anchor.BOTTOM_RIGHT)
    solidRect(10, 10, Colors.PURPLE).anchor(Anchor.BOTTOM_LEFT).dockedTo(Anchor.BOTTOM_LEFT)

    val korimPng = resourcesVfs["korim.png"].readBitmapSlice()
    val bunnysPng = resourcesVfs["bunnys.png"].readBitmapSlice()
    val vampireAse = resourcesVfs["vampire.ase"].readBitmap(ASE).slice()

    val image = uiImage(300, 170, korimPng, scaleMode = ScaleMode.COVER, contentAnchor = Anchor.MIDDLE_CENTER).xy(200, 200)
    image.bgcolor = Colors["#17334f"]

    uiTooltipContainer { tooltips ->
        uiGridFill(100.0, 100.0, cols = 3, rows = 3) {
            val group = UIButtonToggleableGroup()
            for (y in 0 until 3) {
                for (x in 0 until 3) {
                    uiButton(text = "X") {
                        val anchor = Anchor(x * 0.5, y * 0.5)
                        tooltip(tooltips, anchor.toNamedString())
                        this.group(group, pressed = x == 1 && y == 1)
                        onClick { image.contentAnchor = anchor }
                    }
                }
            }
        }
        uiVerticalStack {
            xy(200.0, 0.0)
            uiHorizontalStack {
                val group = UIButtonToggleableGroup()
                uiButton(text = "COVER").group(group, pressed = true).onClick { image.scaleMode = ScaleMode.COVER }
                uiButton(text = "FIT").group(group).onClick { image.scaleMode = ScaleMode.FIT }
                uiButton(text = "EXACT").group(group).onClick { image.scaleMode = ScaleMode.EXACT }
                uiButton(text = "NO_SCALE").group(group).onClick { image.scaleMode = ScaleMode.NO_SCALE }
            }
            uiHorizontalStack {
                val group = UIButtonToggleableGroup()
                uiButton(text = "SQUARE").group(group).onClick { image.size(300, 300) }
                uiButton(text = "HRECT").group(group, pressed = true).onClick { image.size(300, 170) }
                uiButton(text = "VRECT").group(group).onClick { image.size(170, 300) }
            }
            uiHorizontalStack {
                val group = UIButtonToggleableGroup()
                uiButton(text = "korim.png").group(group, pressed = true).onClick { image.bitmap = korimPng }
                uiButton(text = "bunnys.png").group(group).onClick { image.bitmap = bunnysPng }
                uiButton(text = "vampire.ase").group(group).onClick { image.bitmap = vampireAse }
            }
        }
    }
}


suspend fun Stage.mainUITreeView() {
    uiTooltipContainer { tooltips ->
        uiTreeView(UITreeViewList(listOf(
            UITreeViewNode("hello"),
            UITreeViewNode("world",
                UITreeViewNode("test"),
                UITreeViewNode("demo",
                    UITreeViewNode("demo"),
                    UITreeViewNode("demo"),
                    UITreeViewNode("demo",
                        UITreeViewNode("demo")
                    ),
                ),
            ),
            UITreeViewNode("hello"),
            UITreeViewNode("hello"),
            UITreeViewNode("hello"),
            UITreeViewNode("hello"),
            UITreeViewNode("hello"),
            UITreeViewNode("hello"),
            UITreeViewNode("hello"),
            UITreeViewNode("hello"),
            UITreeViewNode("hello"),
            UITreeViewNode("hello"),
            UITreeViewNode("hello"),
            UITreeViewNode("hello"),
            UITreeViewNode("hello"),
            UITreeViewNode("hello"),
            UITreeViewNode("hello"),
            UITreeViewNode("hello"),
            UITreeViewNode("hello"),
            UITreeViewNode("hello"),
            UITreeViewNode("hello"),
            UITreeViewNode("hello"),
            UITreeViewNode("hello"),
            UITreeViewNode("hello"),
        ), height = 16.0, genView = {
            UIText("$it").tooltip(tooltips, "Tooltip for $it")
        }))
    }
    
}
