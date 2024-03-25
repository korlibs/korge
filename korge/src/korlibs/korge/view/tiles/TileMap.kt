package korlibs.korge.view.tiles

import korlibs.datastructure.*
import korlibs.datastructure.iterators.*
import korlibs.time.*
import korlibs.memory.*
import korlibs.korge.internal.*
import korlibs.korge.render.*
import korlibs.korge.view.*
import korlibs.image.bitmap.*
import korlibs.image.tiles.*
import korlibs.math.*
import korlibs.math.geom.*
import korlibs.math.geom.collider.*
import kotlin.math.*

inline fun Container.tileMap(
    map: TileMapData,
    smoothing: Boolean = true,
    callback: @ViewDslMarker TileMap.() -> Unit = {},
) = TileMap(map, map.tileSet, smoothing, map.tileSet.tileSize).repeat(map.repeatX, map.repeatY).addTo(this, callback)

@Deprecated("Use TileMapInfo variant instead")
inline fun Container.tileMap(
    map: IStackedIntArray2,
    tileset: TileSet,
    repeatX: TileMapRepeat = TileMapRepeat.NONE,
    repeatY: TileMapRepeat = repeatX,
    smoothing: Boolean = true,
    tileSize: SizeInt = tileset.tileSize,
    callback: @ViewDslMarker TileMap.() -> Unit = {},
) = TileMap(map, tileset, smoothing, tileSize).repeat(repeatX, repeatY).addTo(this, callback)

@Deprecated("Use TileMapInfo variant instead")
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
@Deprecated("Use TileMapInfo variant instead")
internal fun Bitmap32.toIntArray2() = IntArray2(width, height, ints)

@Deprecated("Use korlibs.image.tiles.TileMapRepeat instead", replaceWith = ReplaceWith("korlibs.image.tiles.TileMapRepeat"))
typealias TileMapRepeat = korlibs.image.tiles.TileMapRepeat

@Deprecated("Use korlibs.image.tiles.Tile instead", replaceWith = ReplaceWith("korlibs.image.tiles.Tile"))
typealias TileInfo = korlibs.image.tiles.Tile

class TileMap(
    var map: TileMapData = TileMapData(1, 1),
    tileset: TileSet = map.tileSet,
    var smoothing: Boolean = true,
    var tileSize: SizeInt = tileset.tileSize,
//) : BaseTileMap(intMap, smoothing, staggerAxis, staggerIndex, tileSize) {
) : View() {
    @Deprecated("Use map instead", level = DeprecationLevel.WARNING)
    var stackedIntMap: IStackedIntArray2
        get() = map.data.asInt()
        set(value) { map = TileMapData(value.asLong()) }

    // Analogous to Bitmap32.locking
    @Deprecated("Not required anymore")
    fun lock() {
    }
    @Deprecated("Not required anymore")
    fun unlock() {
        //map.contentVersion++
    }
    @Deprecated("Not required anymore")
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

    var repeatX = map.repeatX
    var repeatY = map.repeatY

    private var animationVersion = 0
    private var cachedContentVersion = -1
    private var cachedAnimationVersion = -1

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
    private var lastVirtualRect = Rectangle(-1, -1, -1, -1)
    private var currentVirtualRect = Rectangle(-1, -1, -1, -1)

    private val tempX = FloatArray(4)
    private val tempY = FloatArray(4)

    /**
     * Each tile support an offset,
     * if we are using maps that have tiles with offsets like in LDtk,
     * we might need to set this to 1 or 2 depending on the specific case.
     **/
    var overdrawTiles = 0

    // @TODO: Use instanced rendering to support much more tiles at once
    private fun computeVertexIfRequired(ctx: RenderContext) {
        currentVirtualRect = Rectangle(ctx.virtualLeft, ctx.virtualTop, ctx.virtualRight, ctx.virtualBottom)
        if (currentVirtualRect != lastVirtualRect) {
            dirtyVertices = true
            lastVirtualRect = currentVirtualRect
        }

        if (!dirtyVertices && cachedContentVersion == map.contentVersion && cachedAnimationVersion == animationVersion) return

        //println("currentVirtualRect=$currentVirtualRect")

        cachedContentVersion = map.contentVersion
        cachedAnimationVersion = animationVersion
        dirtyVertices = false
        val m = globalMatrix

        val renderTilesCounter = ctx.stats.counter("renderedTiles")

        val (posX, posY) = m.transform(Point.ZERO)
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

        val ymin = korlibs.math.min(my0, my1, my2, my3)
        val ymax = korlibs.math.max(my0, my1, my2, my3)
        val xmin = korlibs.math.min(mx0, mx1, mx2, mx3)
        val xmax = korlibs.math.max(mx0, mx1, mx2, mx3)

        //println("$xmin,$xmax")

        val doRepeatX = repeatX != TileMapRepeat.NONE
        val doRepeatY = repeatY != TileMapRepeat.NONE
        val doRepeatAny = doRepeatX || doRepeatY // Since if it is rotated, we might have problems. For no rotation we could repeat separately

        val ymin2 = (if (doRepeatAny) ymin else ymin.clamp(map.startY, map.endY)) - 1 - overdrawTiles
        val ymax2 = (if (doRepeatAny) ymax else ymax.clamp(map.startY, map.endY)) + 1 + overdrawTiles
        val xmin2 = (if (doRepeatAny) xmin else xmin.clamp(map.startX, map.endX)) - 1 - overdrawTiles
        val xmax2 = (if (doRepeatAny) xmax else xmax.clamp(map.startX, map.endX)) + 1 + overdrawTiles

        //println("xyminmax2=${xmin2},${ymin2} - ${xmax2},${ymax2}")

        val yheight = ymax2 - ymin2
        val xwidth = xmax2 - xmin2

        val ntiles = xwidth * yheight * map.maxLevel

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

        //val scaleOffsetX = map.offsetKind.scale(tileWidth)
        //val scaleOffsetY = map.offsetKind.scale(tileHeight)

        val scaleOffsetX = (1f / tileWidth) * map.offsetScale
        val scaleOffsetY = (1f / tileHeight) * map.offsetScale

        //println("TILE RANGE: ($xmin,$ymin)-($xmax,$ymax)  :: ($xmin2,$ymin2)-($xmax2,$ymax2) :: (${stackedIntMap.startX},${stackedIntMap.startY})-(${stackedIntMap.endX},${stackedIntMap.endY})")

        // @TODO: Try to reduce xy/min/max so we reduce continue. Maybe we can do a bisect or something, to allow huge out scalings
        for (y in ymin2 until ymax2) {
            for (x in xmin2 until xmax2) {
                iterationCount++
                val rx = repeatX.get(x, map.width)
                val ry = repeatY.get(y, map.height)

                if (rx < map.startX || rx >= map.endX) continue
                if (ry < map.startY || ry >= map.endY) continue
                for (level in 0 until map.getStackLevel(rx, ry)) {

                    //println("x=$x, y=$y, rx=$rx, ry=$ry, level=$level")

                    val cell = map[rx, ry, level]
                    if (cell.isInvalid) continue

                    val cellData = cell.tile
                    val flipX = cell.flipX
                    val flipY = cell.flipY
                    val rotate = cell.rotate
                    val offsetX = cell.offsetX
                    val offsetY = cell.offsetY
                    val rationalOffsetX = offsetX * scaleOffsetX
                    val rationalOffsetY = offsetY * scaleOffsetY

                    //if (offsetX != 0 || offsetY != 0) println("x=$x, y=$y, offsetX=$offsetX, offsetY=$offsetY, rationalOffsetX=$rationalOffsetX, rationalOffsetY=$rationalOffsetY, scaleOffsetX=$scaleOffsetX, scaleOffsetY=$scaleOffsetY")

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

    @Deprecated("Use TileMapInfo variant instead")
    constructor(
        map: IStackedIntArray2,
        tileset: TileSet,
        smoothing: Boolean = true,
        tileSize: SizeInt = tileset.tileSize,
    ) : this(TileMapData(map.asLong()), tileset, smoothing, tileSize)

    @Deprecated("Use TileMapInfo variant instead")
    constructor(
        map: IntArray2,
        tileset: TileSet,
        smoothing: Boolean = true,
        tileSize: SizeInt = tileset.tileSize,
    ) : this(map.toStacked(), tileset, smoothing, tileSize)

    @Deprecated("Use TileMapInfo variant instead")
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
        if (!map.inside(tileX, tileY)) return true
        val tile = map.getLast(tileX, tileY)
        // @TODO: Handle rotations, etc. that should transform coordinates.
        val collision = tileset.tilesMap[tile.tile]?.collision ?: return false
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
                        animationVersion++
                    }
                }
            }
        }
    }

    override fun getLocalBoundsInternal() = Rectangle(0f, 0f, tileWidth * map.width, tileHeight * map.height)

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
