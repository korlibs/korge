package korlibs.image.tiles

import korlibs.math.geom.slice.*

fun Tile(tile: Int, orientation: SliceOrientation = SliceOrientation.NORMAL, offsetX: Int = 0, offsetY: Int = 0): Tile =
    Tile(tile, offsetX, offsetY, orientation.tileFlipX, orientation.tileFlipY, orientation.tileRot)

fun Tile.toOrientationString(): String = "Tile($tile, $orientation, $offsetX, $offsetY)"

//val flipX = listOf(0, 1, 1, 0,   1, 1, 0, 0)
val SliceOrientation.tileFlipX: Boolean get() = raw == 1 || raw == 2 || raw == 4 || raw == 5
//val flipY = listOf(0, 0, 1, 1,   0, 1, 1, 0)
val SliceOrientation.tileFlipY: Boolean get() = raw == 2 || raw == 3 || raw == 5 || raw == 6
//val rot   = listOf(0, 1, 0, 1,   0, 1, 0, 1)
val SliceOrientation.tileRot: Boolean get() = raw % 2 == 1

//val TILE_ORIENTATIONS =
val Tile.orientation: SliceOrientation
    get() = when {
        flipY -> when {
            flipX -> if (rotate) SliceOrientation.MIRROR_HORIZONTAL_ROTATE_90 else SliceOrientation.ROTATE_180
            else -> if (rotate) SliceOrientation.ROTATE_270 else SliceOrientation.MIRROR_HORIZONTAL_ROTATE_180
        }
        else -> when {
            flipX -> if (rotate) SliceOrientation.ROTATE_90 else SliceOrientation.MIRROR_HORIZONTAL_ROTATE_0
            else -> if (rotate) SliceOrientation.MIRROR_HORIZONTAL_ROTATE_270 else SliceOrientation.ROTATE_0
        }
    }
