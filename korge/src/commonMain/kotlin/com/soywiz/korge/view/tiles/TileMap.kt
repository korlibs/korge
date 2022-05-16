package com.soywiz.korge.view.tiles

import com.soywiz.kds.FastIdentityMap
import com.soywiz.kds.IntArray2
import com.soywiz.kds.Pool
import com.soywiz.kds.clear
import com.soywiz.kds.getOrPut
import com.soywiz.kds.iterators.fastForEach
import com.soywiz.kds.iterators.fastForEachWithIndex
import com.soywiz.klock.milliseconds
import com.soywiz.kmem.extract
import com.soywiz.kmem.isEven
import com.soywiz.kmem.isOdd
import com.soywiz.kmem.nextPowerOfTwo
import com.soywiz.kmem.umod
import com.soywiz.korge.internal.KorgeInternal
import com.soywiz.korge.render.RenderContext
import com.soywiz.korge.render.TexturedVertexArray
import com.soywiz.korge.tiled.TiledMap
import com.soywiz.korge.view.Container
import com.soywiz.korge.view.HitTestDirection
import com.soywiz.korge.view.View
import com.soywiz.korge.view.ViewDslMarker
import com.soywiz.korge.view.addTo
import com.soywiz.korge.view.addUpdater
import com.soywiz.korim.bitmap.Bitmap
import com.soywiz.korim.bitmap.Bitmap32
import com.soywiz.korim.bitmap.BitmapCoords
import com.soywiz.korim.bitmap.Bitmaps
import com.soywiz.korma.geom.Point
import com.soywiz.korma.geom.Rectangle
import com.soywiz.korma.geom.Size
import com.soywiz.korma.geom.setTo
import kotlin.math.max
import kotlin.math.min

inline fun Container.tileMap(
    map: IntArray2,
    tileset: TileSet,
    repeatX: BaseTileMap.Repeat = BaseTileMap.Repeat.NONE,
    repeatY: BaseTileMap.Repeat = repeatX,
    smoothing: Boolean = true,
    orientation: TiledMap.Orientation? = null,
    staggerAxis: TiledMap.StaggerAxis? = null,
    staggerIndex: TiledMap.StaggerIndex? = null,
    tileSize: Size = Size(tileset.width.toDouble(), tileset.height.toDouble()),
    callback: @ViewDslMarker TileMap.() -> Unit = {},
) = TileMap(map, tileset, smoothing, orientation, staggerAxis, staggerIndex, tileSize).repeat(repeatX, repeatY).addTo(this, callback)

inline fun Container.tileMap(
    map: Bitmap32,
    tileset: TileSet,
    repeatX: BaseTileMap.Repeat = BaseTileMap.Repeat.NONE,
    repeatY: BaseTileMap.Repeat = repeatX,
    smoothing: Boolean = true,
    orientation: TiledMap.Orientation? = null,
    staggerAxis: TiledMap.StaggerAxis? = null,
    staggerIndex: TiledMap.StaggerIndex? = null,
    tileSize: Size = Size(tileset.width.toDouble(), tileset.height.toDouble()),
    callback: @ViewDslMarker TileMap.() -> Unit = {},
) = TileMap(map.toIntArray2(), tileset, smoothing, orientation, staggerAxis, staggerIndex, tileSize).repeat(repeatX, repeatY).addTo(this, callback)

@PublishedApi
internal fun Bitmap32.toIntArray2() = IntArray2(width, height, data.ints)

abstract class BaseTileMap(
    val intMap: IntArray2,
    var smoothing: Boolean = true,
    val staggerAxis: TiledMap.StaggerAxis? = null,
    val staggerIndex: TiledMap.StaggerIndex? = null,
    var tileSize: Size = Size()
) : View() {
    abstract val tilesetTextures: Array<BitmapCoords?>

    var tileWidth: Double = 0.0
    var tileHeight: Double = 0.0

    var repeatX = Repeat.NONE
    var repeatY = Repeat.NONE

    enum class Repeat(val get: (v: Int, max: Int) -> Int) {
        NONE({ v, max -> v }),
        REPEAT({ v, max -> v umod max }),
        MIRROR({ v, max ->
            val r = v umod max
            if ((v / max) % 2 == 0) r else max - 1 - r
        })
    }

    private val t0 = Point(0, 0)
    private val tt0 = Point(0, 0)
    private val tt1 = Point(0, 0)
    private val tt2 = Point(0, 0)
    private val tt3 = Point(0, 0)

    protected var contentVersion = 0
    private var cachedContentVersion = 0

    // @TODO: Use a TextureVertexBuffer or something
    @KorgeInternal
    private class Info(var tex: Bitmap, var vertices: TexturedVertexArray) {
        var vcount = 0
        var icount = 0
    }

    private val verticesPerTex = FastIdentityMap<Bitmap, Info>()
    private val infos = arrayListOf<Info>()

    companion object {
        private val dummyTexturedVertexArray = TexturedVertexArray.EMPTY

        fun computeIndices(flipX: Boolean, flipY: Boolean, rotate: Boolean, indices: IntArray = IntArray(4)): IntArray {
            // @TODO: const val optimization issue in Kotlin/Native: https://youtrack.jetbrains.com/issue/KT-46425
            indices[0] = 0 // 0/*TL*/
            indices[1] = 1 // 1/*TR*/
            indices[2] = 2 // 2/*BR*/
            indices[3] = 3 // 3/*BL*/

            if (rotate) {
                indices.swap(1/*TR*/, 3/*BL*/)
            }
            if (flipY) {
                indices.swap(0/*TL*/, 3/*BL*/)
                indices.swap(1/*TR*/, 2/*BR*/)
            }
            if (flipX) {
                indices.swap(0/*TL*/, 1/*TR*/)
                indices.swap(3/*BL*/, 2/*BR*/)
            }
            return indices
        }

        private fun IntArray.swap(a: Int, b: Int): IntArray = this.apply {
            val t = this[a]
            this[a] = this[b]
            this[b] = t
        }

        //private const val TL = 0
        //private const val TR = 1
        //private const val BR = 2
        //private const val BL = 3
    }

    private val infosPool = Pool { Info(Bitmaps.transparent.bmpBase, dummyTexturedVertexArray) }
    private var lastVirtualRect = Rectangle(-1, -1, -1, -1)
    private var currentVirtualRect = Rectangle(-1, -1, -1, -1)

    private val indices = IntArray(4)
    private val tempX = FloatArray(4)
    private val tempY = FloatArray(4)

    private fun computeVertexIfRequired(ctx: RenderContext) {
        if (!dirtyVertices && cachedContentVersion == contentVersion) return
        cachedContentVersion = contentVersion
        dirtyVertices = false
        val m = globalMatrix

        val renderTilesCounter = ctx.stats.counter("renderedTiles")

        val posX = m.transformX(0.0, 0.0)
        val posY = m.transformY(0.0, 0.0)
        val dUX = m.transformX(tileWidth, 0.0) - posX
        val dUY = m.transformY(tileWidth, 0.0) - posY
        val dVX = m.transformX(0.0, tileHeight) - posX
        val dVY = m.transformY(0.0, tileHeight) - posY
        val initY = if (staggerAxis != null) {
            val it = (tileSize.height - tileHeight)
            min(m.transformX(it, 0.0) - posX, m.transformY(0.0, it))
        } else {
            0.0
        }
        val nextTileX = (tileSize.width / if (staggerAxis == TiledMap.StaggerAxis.X) 2.0 else 1.0).let { width ->
            min(m.transformX(width, 0.0) - posX, m.transformY(0.0, width) - posY)
        }
        val nextTileY = (tileSize.height / if (staggerAxis == TiledMap.StaggerAxis.Y) 2.0 else 1.0).let { height ->
            min(m.transformX(height, 0.0) - posX, m.transformY(0.0, height) - posY)
        }
        val staggerX = (tileWidth / 2.0).let{ width ->
            min(m.transformX(width, 0.0) - posX, m.transformY(0.0, width) - posY)
        }
        val staggerY = (tileSize.height / 2.0).let{ height ->
            min(m.transformX(height, 0.0) - posX, m.transformY(0.0, height) - posY)
        }

        val colMul = renderColorMul
        val colAdd = renderColorAdd

        // @TODO: Bounds in clipped view
        val pp0 = globalToLocal(t0.setTo(currentVirtualRect.left, currentVirtualRect.top), tt0)
        val pp1 = globalToLocal(t0.setTo(currentVirtualRect.right, currentVirtualRect.bottom), tt1)
        val pp2 = globalToLocal(t0.setTo(currentVirtualRect.right, currentVirtualRect.top), tt2)
        val pp3 = globalToLocal(t0.setTo(currentVirtualRect.left, currentVirtualRect.bottom), tt3)
        val mapTileWidth = tileSize.width
        val mapTileHeight = tileSize.height / if (staggerAxis == TiledMap.StaggerAxis.Y) 2.0 else 1.0
        val mx0 = ((pp0.x / mapTileWidth) + 1).toInt()
        val mx1 = ((pp1.x / mapTileWidth) + 1).toInt()
        val mx2 = ((pp2.x / mapTileWidth) + 1).toInt()
        val mx3 = ((pp3.x / mapTileWidth) + 1).toInt()
        val my0 = ((pp0.y / mapTileHeight) + 1).toInt()
        val my1 = ((pp1.y / mapTileHeight) + 1).toInt()
        val my2 = ((pp2.y / mapTileHeight) + 1).toInt()
        val my3 = ((pp3.y / mapTileHeight) + 1).toInt()

        val ymin = min(min(min(my0, my1), my2), my3) - 1
        val ymax = max(max(max(my0, my1), my2), my3)
        val xmin = min(min(min(mx0, mx1), mx2), mx3) - 1
        val xmax = max(max(max(mx0, mx1), mx2), mx3)

        val yheight = ymax - ymin
        val xwidth = xmax - xmin
        val ntiles = xwidth * yheight
        val allocTiles = ntiles.nextPowerOfTwo
        //println("(mx0=$mx0, my0=$my0)-(mx1=$mx1, my1=$my1)-(mx2=$mx2, my2=$my2)-(mx3=$mx3, my3=$my3) ($xwidth, $yheight)")
        infos.fastForEach { infosPool.free(it) }
        verticesPerTex.clear()
        infos.clear()

        var count = 0
        val passes = if (staggerAxis == TiledMap.StaggerAxis.X) 2 else 1

        for (y in ymin until ymax) {
            // interlace rows when staggered on X to ensure proper z-index
            for (pass in 0 until passes) {
                for (x in xmin until xmax) {
                    val rx = repeatX.get(x, intMap.width)
                    val ry = repeatY.get(y, intMap.height)

                    if (rx < 0 || rx >= intMap.width) continue
                    if (ry < 0 || ry >= intMap.height) continue
                    if (staggerAxis == TiledMap.StaggerAxis.X) {
                        val firstPass = staggerIndex == TiledMap.StaggerIndex.ODD && rx.isEven ||
                            staggerIndex == TiledMap.StaggerIndex.EVEN && rx.isOdd
                        val secondPass = staggerIndex == TiledMap.StaggerIndex.ODD && rx.isOdd ||
                            staggerIndex == TiledMap.StaggerIndex.EVEN && rx.isEven
                        if (pass == 0 && !firstPass) continue
                        if (pass == 1 && !secondPass) continue
                    }
                    val odd = if (staggerAxis == TiledMap.StaggerAxis.Y) ry.isOdd else rx.isOdd
                    val staggered = if (odd) staggerIndex == TiledMap.StaggerIndex.ODD else staggerIndex == TiledMap.StaggerIndex.EVEN
                    val cell = intMap[rx, ry]
                    val cellData = cell.extract(0, 28)
                    val flipX = cell.extract(31)
                    val flipY = cell.extract(30)
                    val rotate = cell.extract(29)

                    val staggerOffsetX = when (staggerAxis.takeIf { staggered }) {
                        TiledMap.StaggerAxis.Y -> staggerX
                        TiledMap.StaggerAxis.X -> 0.0
                        else -> 0.0
                    }
                    val staggerOffsetY = when (staggerAxis.takeIf { staggered }) {
                        TiledMap.StaggerAxis.Y -> 0.0
                        TiledMap.StaggerAxis.X -> staggerY
                        else -> 0.0
                    }

                    //println("staggerOffsetX=$staggerOffsetX, staggerOffsetY=$staggerOffsetY, initY=$initY")

                    count++

                    //println("CELL_DATA: $cellData")

                    val tex = tilesetTextures[cellData] ?: continue

                    //println("CELL_DATA_TEX: $tex")

                    val info = verticesPerTex.getOrPut(tex.base) {
                        infosPool.alloc().also { info ->
                            info.tex = tex.base
                            if (info.vertices.initialVcount < allocTiles * 4) {
                                info.vertices = TexturedVertexArray(allocTiles * 4, TexturedVertexArray.quadIndices(allocTiles))
                                //println("ALLOC TexturedVertexArray")
                            }
                            info.vcount = 0
                            info.icount = 0
                            infos += info
                        }
                    }

                    run {
                        val p0X = posX + (nextTileX * x) + (dVX * y) + staggerOffsetX
                        val p0Y = posY + (dUY * x) + (nextTileY * y) + staggerOffsetY + initY

                        val p1X = p0X + dUX
                        val p1Y = p0Y + dUY

                        val p2X = p0X + dUX + dVX
                        val p2Y = p0Y + dUY + dVY

                        val p3X = p0X + dVX
                        val p3Y = p0Y + dVY

                        tempX[0] = tex.tl_x
                        tempX[1] = tex.tr_x
                        tempX[2] = tex.br_x
                        tempX[3] = tex.bl_x

                        tempY[0] = tex.tl_y
                        tempY[1] = tex.tr_y
                        tempY[2] = tex.br_y
                        tempY[3] = tex.bl_y

                        computeIndices(flipX = flipX, flipY = flipY, rotate = rotate, indices = indices)

                        info.vertices.quadV(info.vcount++, p0X, p0Y, tempX[indices[0]], tempY[indices[0]], colMul, colAdd)
                        info.vertices.quadV(info.vcount++, p1X, p1Y, tempX[indices[1]], tempY[indices[1]], colMul, colAdd)
                        info.vertices.quadV(info.vcount++, p2X, p2Y, tempX[indices[2]], tempY[indices[2]], colMul, colAdd)
                        info.vertices.quadV(info.vcount++, p3X, p3Y, tempX[indices[3]], tempY[indices[3]], colMul, colAdd)
                    }

                    info.icount += 6
                }
            }
        }
        renderTilesCounter.increment(count)
    }

    override fun renderInternal(ctx: RenderContext) {
        if (!visible) return
        currentVirtualRect.setBounds(ctx.virtualLeft, ctx.virtualTop, ctx.virtualRight, ctx.virtualBottom)
        if (currentVirtualRect != lastVirtualRect) {
            dirtyVertices = true
            lastVirtualRect.copyFrom(currentVirtualRect)
        }
        computeVertexIfRequired(ctx)

        ctx.useBatcher { batch ->
            infos.fastForEach { buffer ->
                batch.drawVertices(
                    buffer.vertices, ctx.getTex(buffer.tex), smoothing, renderBlendMode.factors, buffer.vcount, buffer.icount
                )
            }
        }

        //ctx.flush()
    }
}

@OptIn(KorgeInternal::class)
open class TileMap(
    intMap: IntArray2,
    val tileset: TileSet,
    smoothing: Boolean = true,
    val orientation: TiledMap.Orientation? = null,
    staggerAxis: TiledMap.StaggerAxis? = null,
    staggerIndex: TiledMap.StaggerIndex? = null,
    tileSize: Size = Size(tileset.width.toDouble(), tileset.height.toDouble()),
) : BaseTileMap(intMap, smoothing, staggerAxis, staggerIndex, tileSize) {
    override val tilesetTextures = Array(tileset.textures.size) { tileset.textures[it] }
    val animationIndex = Array(tileset.textures.size) { 0 }
    val animationElapsed = Array(tileset.textures.size) { 0.0 }

    constructor(
        map: Bitmap32,
        tileset: TileSet,
        smoothing: Boolean = true,
        orientation: TiledMap.Orientation? = null,
        staggerAxis: TiledMap.StaggerAxis? = null,
        staggerIndex: TiledMap.StaggerIndex? = null,
        tileSize: Size = Size(tileset.width.toDouble(), tileset.height.toDouble()),
    ) : this(map.toIntArray2(), tileset, smoothing, orientation, staggerAxis, staggerIndex, tileSize)

    fun pixelHitTest(x: Int, y: Int, direction: HitTestDirection): Boolean {
        //if (x < 0 || y < 0) return false // Outside bounds
        if (x < 0 || y < 0) return true // Outside bounds
        val tw = tileset.width
        val th = tileset.height
        return pixelHitTest(x / tw, y / th, x % tw, y % th, direction)
    }

    fun pixelHitTest(tileX: Int, tileY: Int, x: Int, y: Int, direction: HitTestDirection): Boolean {
        //println("pixelHitTestByte: tileX=$tileX, tileY=$tileY, x=$x, y=$y")
        //println(tileset.collisions.toList())
        if (!intMap.inside(tileX, tileY)) return true
        val tile = intMap[tileX, tileY]
        val collision = tileset.collisions[tile] ?: return false
        return collision.hitTestAny(x.toDouble(), y.toDouble(), direction)
    }

    // Analogous to Bitmap32.locking
    fun lock() {
    }
    fun unlock() {
        contentVersion++
    }
    inline fun <T> lock(block: () -> T): T {
        lock()
        try {
            return block()
        } finally {
            unlock()
        }
    }

    init {
        tileWidth = tileset.width.toDouble()
        tileHeight = tileset.height.toDouble()

        addUpdater { dt ->
            tileset.infos.fastForEachWithIndex { tileIndex, info ->
                if (info != null && info.frames.isNotEmpty()) {
                    val aindex = animationIndex[tileIndex]
                    val currentFrame = info.frames[aindex]
                    animationElapsed[tileIndex] += dt.milliseconds
                    if (animationElapsed[tileIndex].milliseconds >= currentFrame.duration) {
                        //println("Changed ${info.id} [${info.id}] -> ${info.frames}")
                        val nextIndex = (aindex + 1) % info.frames.size
                        animationElapsed[tileIndex] -= currentFrame.duration.milliseconds
                        animationIndex[tileIndex] = nextIndex
                        tilesetTextures[tileIndex] = tileset.textures[info.frames[nextIndex].tileId]
                        contentVersion++
                    }
                }
            }
        }
    }

    override fun getLocalBoundsInternal(out: Rectangle) {
        out.setTo(0, 0, tileWidth * intMap.width, tileHeight * intMap.height)
    }

    //override fun hitTest(x: Double, y: Double): View? {
    //    return if (checkGlobalBounds(x, y, 0.0, 0.0, tileWidth * intMap.width, tileHeight * intMap.height)) this else null
    //}
}

fun <T : BaseTileMap> T.repeat(repeatX: BaseTileMap.Repeat, repeatY: BaseTileMap.Repeat = repeatX): T {
    this.repeatX = repeatX
    this.repeatY = repeatY
    return this
}

fun <T : BaseTileMap> T.repeat(repeatX: Boolean = false, repeatY: Boolean = false): T {
    this.repeatX = if (repeatX) BaseTileMap.Repeat.REPEAT else BaseTileMap.Repeat.NONE
    this.repeatY = if (repeatY) BaseTileMap.Repeat.REPEAT else BaseTileMap.Repeat.NONE
    return this
}
