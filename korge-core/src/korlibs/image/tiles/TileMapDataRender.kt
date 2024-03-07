package korlibs.image.tiles

import korlibs.image.bitmap.*
import korlibs.math.geom.*
import korlibs.math.geom.slice.*

fun TileMapData.render(): Bitmap {
    val tileSet = this.tileSet
    val out = NativeImage(data.width * tileSet.width, data.height * tileSet.height, premultiplied = true)
    renderTo(out, 0, 0)
    return out
}

fun TileMapData.renderTo(out: Bitmap, x: Int = 0, y: Int = 0) {
    val map = this
    val tileSet = this.tileSet.takeIf { it != TileSet.EMPTY } ?: return
    val tiles = LinkedHashMap<Pair<Int, SliceOrientation>, Bitmap32>()

    val tileSetWidth = tileSet.width
    val tileSetHeight = tileSet.height
    val offsetScale = map.offsetScale

    out.context2d {
        val ctx = this
        for (l in 0 until map.maxLevel) {
            map.eachPosition { tx, ty ->
                val tile = map[tx, ty, l]
                val info = tileSet.getInfo(tile.tile)
                if (info != null) {
                    val px = ((x + (tx * tileSetWidth) + (tile.offsetX * offsetScale))).toInt()
                    val py = ((y + (ty * tileSetHeight) + (tile.offsetY * offsetScale))).toInt()
                    // @TODO: Use matrices to render drawImage without extracting pixels for each transformed tile
                    val image = tiles.getOrPut(tile.tile to tile.orientation) {
                        //info.slice.extract().toBMP32IfRequired()
                        info.slice.copy(orientation = info.slice.orientation.transformed(tile.orientation)).extract().toBMP32IfRequired().also {
                            //runBlockingNoJs { localVfs("C:/temp/${tile.tile}-${tile.orientation.raw}.png").writeBitmap(it, PNG) }
                        }
                    }
                    ctx.drawImage(image, Point(px, py))
                    //println("DRAW: tx=$tx, ty=$ty, px=$px, py=$py, tile=${tile}")
                    //out.put(image, px, py)
                }
            }
        }
    }
    //runBlockingNoJs { out.showImageAndWait() }
}
