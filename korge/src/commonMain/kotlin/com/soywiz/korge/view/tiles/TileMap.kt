package com.soywiz.korge.view.tiles

import com.soywiz.kds.*
import com.soywiz.kds.iterators.*
import com.soywiz.klock.*
import com.soywiz.kmem.*
import com.soywiz.korge.internal.*
import com.soywiz.korge.render.*
import com.soywiz.korge.view.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korim.tiles.*
import com.soywiz.korma.geom.*
import com.soywiz.korma.math.*
import kotlin.math.*

inline fun Container.tileMap(
    map: IStackedIntArray2,
    tileset: TileSet,
    repeatX: TileMapRepeat = TileMapRepeat.NONE,
    repeatY: TileMapRepeat = repeatX,
    smoothing: Boolean = true,
    tileSize: SizeInt = tileset.tileSize,
    callback: @ViewDslMarker TileMap.() -> Unit = {},
) = TileMap(map, tileset, smoothing, tileSize).repeat(repeatX, repeatY).addTo(this, callback)

inline fun Container.tileMap(
    map: IntArray2,
    tileset: TileSet,
    repeatX: TileMapRepeat = TileMapRepeat.NONE,
    repeatY: TileMapRepeat = repeatX,
    smoothing: Boolean = true,
    tileSize: SizeInt = tileset.tileSize,
    callback: @ViewDslMarker TileMap.() -> Unit = {},
) = TileMap(map, tileset, smoothing, tileSize).repeat(repeatX, repeatY).addTo(this, callback)

@PublishedApi
internal fun Bitmap32.toIntArray2() = IntArray2(width, height, ints)

enum class TileMapRepeat(val get: (v: Int, max: Int) -> Int) {
    NONE({ v, max -> v }),
    REPEAT({ v, max -> v umod max }),
    MIRROR({ v, max ->
        val r = v umod max
        if ((v / max) % 2 == 0) r else max - 1 - r
    })
}

inline class TileInfo(val data: Int) {
    val isValid: Boolean get() = data != -1
    val isInvalid: Boolean get() = data == -1

    val tile: Int get() = data.extract(0, 18)
    val offsetX: Int get() = data.extract5(18)
    val offsetY: Int get() = data.extract5(23)
    val rotate: Boolean get() = data.extract(29)
    val flipY: Boolean get() = data.extract(30)
    val flipX: Boolean get() = data.extract(31)

    constructor(tile: Int, offsetX: Int = 0, offsetY: Int = 0, flipX: Boolean = false, flipY: Boolean = false, rotate: Boolean = false) : this(0
        .insert(tile, 0, 18)
        .insert(offsetX, 18, 5)
        .insert(offsetY, 23, 5)
        .insert(rotate, 29)
        .insert(flipY, 30)
        .insert(flipX, 31)
    )

}

class TileMap(
    var stackedIntMap: IStackedIntArray2 = StackedIntArray2(1, 1, 0),
    tileset: TileSet = TileSet.EMPTY,
    var smoothing: Boolean = true,
    var tileSize: SizeInt = tileset.tileSize,
//) : BaseTileMap(intMap, smoothing, staggerAxis, staggerIndex, tileSize) {
) : View() {
    @Deprecated("Use stackedIntMap instead", level = DeprecationLevel.HIDDEN)
    var intMap: IntArray2
        get() = (stackedIntMap as StackedIntArray2).data.first()
        set(value) {
            lock {
                stackedIntMap = StackedIntArray2(value)
            }
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

    private var tileWidth: Float = 0f
    private var tileHeight: Float = 0f

    var repeatX = TileMapRepeat.NONE
    var repeatY = TileMapRepeat.NONE

    protected var contentVersion = 0
    private var cachedContentVersion = 0

    // @TODO: Use a TextureVertexBuffer or something
    @KorgeInternal
    private class Info(var tex: Bitmap, var vertices: ShrinkableTexturedVertexArray) {
        val verticesList = FastArrayList<ShrinkableTexturedVertexArray>().also {
            it.add(vertices)
        }

        fun addNewVertices(vertices: ShrinkableTexturedVertexArray) {
            vertices.reset()
            this.vertices = vertices
            verticesList.add(vertices)
        }

        fun reset() {
            vertices.icount = 0
            vertices.vcount = 0
        }
    }

    private val verticesPerTex = FastIdentityCacheMap<Bitmap, Info>()
    private val infos = arrayListOf<Info>()

    companion object {
        private fun transformIndex(flipX: Boolean, flipY: Boolean, rotate: Boolean): Int {
            return 0.insert(flipX, 0).insert(flipY, 1).insert(rotate, 2)
        }

        private val INDICES = Array(8) {
            computeIndices(it.extract(0), it.extract(1), it.extract(2))
        }

        private fun computeIndices(flipX: Boolean, flipY: Boolean, rotate: Boolean, indices: IntArray = IntArray(4)): IntArray {
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

    private val infosPool = Pool(reset = { it.reset() }) { Info(Bitmaps.transparent.bmp, ShrinkableTexturedVertexArray(TexturedVertexArray.EMPTY)) }
    private var lastVirtualRect = MRectangle(-1, -1, -1, -1)
    private var currentVirtualRect = MRectangle(-1, -1, -1, -1)

    private val tempX = FloatArray(4)
    private val tempY = FloatArray(4)

    // @TODO: Use instanced rendering to support much more tiles at once
    private fun computeVertexIfRequired(ctx: RenderContext) {
        currentVirtualRect.setBounds(ctx.virtualLeft, ctx.virtualTop, ctx.virtualRight, ctx.virtualBottom)
        if (currentVirtualRect != lastVirtualRect) {
            dirtyVertices = true
            lastVirtualRect.copyFrom(currentVirtualRect)
        }

        if (!dirtyVertices && cachedContentVersion == contentVersion) return

        //println("currentVirtualRect=$currentVirtualRect")

        cachedContentVersion = contentVersion
        dirtyVertices = false
        val m = globalMatrix

        val renderTilesCounter = ctx.stats.counter("renderedTiles")

        val posX = m.transformX(0f, 0f)
        val posY = m.transformY(0f, 0f)
        val dUX = m.transformX(tileWidth, 0f) - posX
        val dUY = m.transformY(tileWidth, 0f) - posY
        val dVX = m.transformX(0f, tileHeight) - posX
        val dVY = m.transformY(0f, tileHeight) - posY
        val nextTileX = (tileSize.width).let { width ->
            min(m.transformX(width.toDouble(), 0.0) - posX, m.transformY(0.0, width.toDouble()) - posY)
        }
        val nextTileY = (tileSize.height).let { height ->
            min(m.transformX(height.toDouble(), 0.0) - posX, m.transformY(0.0, height.toDouble()) - posY)
        }

        val colMul = renderColorMul

        // @TODO: Bounds in clipped view
        val pp0 = globalToLocal(Point(currentVirtualRect.left, currentVirtualRect.top))
        val pp1 = globalToLocal(Point(currentVirtualRect.right, currentVirtualRect.bottom))
        val pp2 = globalToLocal(Point(currentVirtualRect.right, currentVirtualRect.top))
        val pp3 = globalToLocal(Point(currentVirtualRect.left, currentVirtualRect.bottom))
        val mapTileWidth = tileSize.width
        val mapTileHeight = tileSize.height
        val mx0 = ((pp0.x / mapTileWidth)).toIntCeil()
        val mx1 = ((pp1.x / mapTileWidth)).toIntCeil()
        val mx2 = ((pp2.x / mapTileWidth)).toIntCeil()
        val mx3 = ((pp3.x / mapTileWidth)).toIntCeil()
        val my0 = ((pp0.y / mapTileHeight)).toIntCeil()
        val my1 = ((pp1.y / mapTileHeight)).toIntCeil()
        val my2 = ((pp2.y / mapTileHeight)).toIntCeil()
        val my3 = ((pp3.y / mapTileHeight)).toIntCeil()

        //println("currentVirtualRect=$currentVirtualRect, mx=[$mx0, $mx1, $mx2, $mx3], my=[$my0, $my1, $my2, $my3], pp0=$pp0, pp1=$pp1, pp2=$pp2, pp3=$pp3")

        val ymin = min(my0, my1, my2, my3)
        val ymax = max(my0, my1, my2, my3)
        val xmin = min(mx0, mx1, mx2, mx3)
        val xmax = max(mx0, mx1, mx2, mx3)

        //println("$xmin,$xmax")

        val doRepeatX = repeatX != TileMapRepeat.NONE
        val doRepeatY = repeatY != TileMapRepeat.NONE
        val doRepeatAny = doRepeatX || doRepeatY // Since if it is rotated, we might have problems. For no rotation we could repeat separately

        val ymin2 = (if (doRepeatAny) ymin else ymin.clamp(stackedIntMap.startY, stackedIntMap.endY)) - 1
        val ymax2 = (if (doRepeatAny) ymax else ymax.clamp(stackedIntMap.startY, stackedIntMap.endY)) + 1
        val xmin2 = (if (doRepeatAny) xmin else xmin.clamp(stackedIntMap.startX, stackedIntMap.endX)) - 1
        val xmax2 = (if (doRepeatAny) xmax else xmax.clamp(stackedIntMap.startX, stackedIntMap.endX)) + 1

        //println("xyminmax2=${xmin2},${ymin2} - ${xmax2},${ymax2}")

        val yheight = ymax2 - ymin2
        val xwidth = xmax2 - xmin2

        val ntiles = xwidth * yheight * stackedIntMap.maxLevel

        //println("ntiles=$ntiles")

        val allocTiles = ntiles.nextPowerOfTwo

        //val MAX_TILES = 64 * 1024 - 16
        val MAX_TILES = 16 * 1024 - 16
        //val MAX_TILES = 1 * 1024
        //val MAX_TILES = 32
        //val allocTilesClamped = min(allocTiles, MAX_TILES)
        val allocTilesClamped = min(allocTiles, MAX_TILES)
        //println("(mx0=$mx0, my0=$my0)-(mx1=$mx1, my1=$my1)-(mx2=$mx2, my2=$my2)-(mx3=$mx3, my3=$my3) ($xwidth, $yheight)")
        infos.fastForEach { infosPool.free(it) }
        verticesPerTex.clear()
        infos.clear()

        var iterationCount = 0
        var count = 0
        var nblocks = 0

        val quadIndexData = TexturedVertexArray.quadIndices(allocTilesClamped)

        val invTileWidth  = 1f / tileWidth
        val invTileHeight = 1f / tileHeight

        //println("TILE RANGE: ($xmin,$ymin)-($xmax,$ymax)  :: ($xmin2,$ymin2)-($xmax2,$ymax2) :: (${stackedIntMap.startX},${stackedIntMap.startY})-(${stackedIntMap.endX},${stackedIntMap.endY})")

        // @TODO: Try to reduce xy/min/max so we reduce continue. Maybe we can do a bisect or something, to allow huge out scalings
        for (y in ymin2 until ymax2) {
            for (x in xmin2 until xmax2) {
                iterationCount++
                val rx = repeatX.get(x, stackedIntMap.width)
                val ry = repeatY.get(y, stackedIntMap.height)

                if (rx < stackedIntMap.startX || rx >= stackedIntMap.endX) continue
                if (ry < stackedIntMap.startY || ry >= stackedIntMap.endY) continue
                for (level in 0 until stackedIntMap.getStackLevel(rx, ry)) {

                    //println("x=$x, y=$y, rx=$rx, ry=$ry, level=$level")

                    val cell = TileInfo(stackedIntMap[rx, ry, level])
                    if (cell.isInvalid) continue

                    val cellData = cell.tile
                    val flipX = cell.flipX
                    val flipY = cell.flipY
                    val rotate = cell.rotate
                    val offsetX = cell.offsetX
                    val offsetY = cell.offsetY
                    val rationalOffsetX = offsetX * invTileWidth
                    val rationalOffsetY = offsetY * invTileHeight

                    //println("CELL_DATA: $cellData")

                    val tex = tilesetTextures.getOrNull(cellData) ?: continue

                    count++

                    //println("CELL_DATA_TEX: $tex")

                    val info = verticesPerTex.getOrPut(tex.base) {
                        infosPool.alloc().also { info ->
                            info.tex = tex.base
                            info.verticesList.clear()
                            info.addNewVertices(
                                ShrinkableTexturedVertexArray(
                                    TexturedVertexArray(
                                        allocTilesClamped * 4,
                                        quadIndexData
                                    )
                                )
                            )
                            infos += info
                            nblocks++
                        }
                    }
                    //println("info=${info.identityHashCode()}")

                    run {
                        val px = x + rationalOffsetX
                        val py = y + rationalOffsetY
                        val p0X = posX + (nextTileX * px) + (dVX * py)
                        val p0Y = posY + (dUY * px) + (nextTileY * py)

                        val p1X = p0X + dUX
                        val p1Y = p0Y + dUY

                        val p2X = p0X + dUX + dVX
                        val p2Y = p0Y + dUY + dVY

                        val p3X = p0X + dVX
                        val p3Y = p0Y + dVY

                        tempX[0] = tex.tlX
                        tempX[1] = tex.trX
                        tempX[2] = tex.brX
                        tempX[3] = tex.blX

                        tempY[0] = tex.tlY
                        tempY[1] = tex.trY
                        tempY[2] = tex.brY
                        tempY[3] = tex.blY

                        val indices = INDICES[transformIndex(flipX, flipY, rotate)]
                        //computeIndices(flipX = flipX, flipY = flipY, rotate = rotate, indices = indices)

                        info.vertices.quadV(p0X, p0Y, tempX[indices[0]], tempY[indices[0]], colMul)
                        info.vertices.quadV(p1X, p1Y, tempX[indices[1]], tempY[indices[1]], colMul)
                        info.vertices.quadV(p2X, p2Y, tempX[indices[2]], tempY[indices[2]], colMul)
                        info.vertices.quadV(p3X, p3Y, tempX[indices[3]], tempY[indices[3]], colMul)
                    }

                    info.vertices.icount += 6

                    //println("info.icount=${info.icount}")

                    if (info.vertices.icount >= MAX_TILES - 1) {
                        info.addNewVertices(
                            ShrinkableTexturedVertexArray(
                                TexturedVertexArray(
                                    allocTilesClamped * 4,
                                    quadIndexData
                                )
                            )
                        )
                        nblocks++
                    }
                }
            }
        }
        renderTilesCounter.increment(count)
        totalIterationCount = iterationCount
        //totalTilesRendered = count
        //totalVertexCount = count * 4
        //totalIndexCount = count * 6
        //totalBatchCount = nblocks
    }

    var totalIterationCount: Int = 0
    val totalGroupsCount: Int get() = infos.size
    val totalVertexCount: Int get() = infos.sumOf { it.verticesList.sumOf { it.vcount } }
    val totalIndexCount: Int get() = infos.sumOf { it.verticesList.sumOf { it.icount } }
    val totalBatchCount: Int get() = infos.sumOf { it.verticesList.size }
    val totalTilesRendered: Int get() = totalVertexCount / 4

    override fun renderInternal(ctx: RenderContext) {
        if (!visible) return
        computeVertexIfRequired(ctx)

        //println("---")

        ctx.useBatcher { batch ->
            infos.fastForEach { info ->
                info.verticesList.fastForEach { vertices ->
                    //println("VERTICES: $vertices")
                    batch.drawVertices(
                        vertices.vertices,
                        ctx.getTex(info.tex),
                        smoothing, renderBlendMode, vertices.vcount, vertices.icount,
                    )
                }
            }
            //batch.flush()
        }
    }

    var tilesetTextures: Array<BitmapCoords?> = emptyArray<BitmapCoords?>()
    var animationIndex: IntArray = IntArray(0)
    var animationElapsed: DoubleArray = DoubleArray(0)

    var tileset: TileSet = tileset
        set(value) {
            if (field === value) return
            lock {
                field = value
                updatedTileSet()
            }
        }

    private fun updatedTileSet() {
        tilesetTextures = Array(tileset.textures.size) { tileset.textures[it] }
        animationIndex = IntArray(tileset.textures.size) { 0 }
        animationElapsed = DoubleArray(tileset.textures.size) { 0.0 }
        tileSize = tileset.tileSize
        tileWidth = tileset.width.toFloat()
        tileHeight = tileset.height.toFloat()
    }

    constructor(
        map: IntArray2,
        tileset: TileSet,
        smoothing: Boolean = true,
        tileSize: SizeInt = tileset.tileSize,
    ) : this(map.toStacked(), tileset, smoothing, tileSize)

    constructor(
        map: Bitmap32,
        tileset: TileSet,
        smoothing: Boolean = true,
        tileSize: SizeInt = tileset.tileSize,
    ) : this(map.toIntArray2().toStacked(), tileset, smoothing, tileSize)

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
        if (!stackedIntMap.inside(tileX, tileY)) return true
        val tile = stackedIntMap.getLast(tileX, tileY)
        val collision = tileset.tilesMap[tile]?.collision ?: return false
        return collision.hitTestAny(Point(x, y), direction)
    }

    init {
        updatedTileSet()
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

    override fun getLocalBoundsInternal() = Rectangle(0f, 0f, tileWidth * stackedIntMap.width, tileHeight * stackedIntMap.height)

    fun repeat(repeatX: TileMapRepeat, repeatY: TileMapRepeat = repeatX): TileMap {
        this.repeatX = repeatX
        this.repeatY = repeatY
        return this
    }

    fun repeat(repeatX: Boolean = false, repeatY: Boolean = false): TileMap {
        this.repeatX = if (repeatX) TileMapRepeat.REPEAT else TileMapRepeat.NONE
        this.repeatY = if (repeatY) TileMapRepeat.REPEAT else TileMapRepeat.NONE
        return this
    }

    //override fun hitTest(x: Double, y: Double): View? {
    //    return if (checkGlobalBounds(x, y, 0.0, 0.0, tileWidth * intMap.width, tileHeight * intMap.height)) this else null
    //}
}

