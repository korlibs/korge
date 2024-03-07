package korlibs.image.tiles

import korlibs.image.bitmap.*
import korlibs.math.geom.*
import korlibs.math.geom.slice.*

fun TileMapInfo.render(): NativeImage {
    val tileSet = this.tileSet ?: return NativeImage(1, 1, premultiplied = true)
    val out = NativeImage(data.width * tileSet.width, data.height * tileSet.height, premultiplied = true)
    renderTo(out, 0, 0)
    return out
}

fun TileMapInfo.renderTo(out: Bitmap, x: Int = 0, y: Int = 0) {
    val map = this
    val tileSet = this.tileSet ?: return
    val tiles = LinkedHashMap<Pair<Int, SliceOrientation>, Bitmap32>()

    val tileSetWidth = tileSet.width
    val tileSetHeight = tileSet.height
    val scaleOffsetX = map.offsetKind.scale(tileSetWidth.toFloat())
    val scaleOffsetY = map.offsetKind.scale(tileSetHeight.toFloat())

    out.context2d {
        val ctx = this
        map.eachPosition { tx, ty ->
            val maxLevel = map.getStackLevel(tx, ty)
            for (l in 0 until maxLevel) {
                val tile = map[x, y, l]
                val info = tileSet.getInfo(tile.tile)
                if (info != null) {
                    val px = (x + (tx * tileSetWidth) + (tile.offsetX * scaleOffsetX))
                    val py = (y + (ty * tileSetHeight) + (tile.offsetY * scaleOffsetY))
                    // @TODO: Use matrices to render drawImage without extracting pixels for each transformed tile
                    val image = tiles.getOrPut(tile.tile to tile.orientation) {
                        info.slice.copy(orientation = info.slice.orientation.transformed(tile.orientation)).extract().toBMP32IfRequired()
                        //info.slice.extract().oriented(tile.orientation).toBMP32IfRequired()
                    }
                    ctx.drawImage(image, Point(px, py))
                }
            }
        }
    }
}
