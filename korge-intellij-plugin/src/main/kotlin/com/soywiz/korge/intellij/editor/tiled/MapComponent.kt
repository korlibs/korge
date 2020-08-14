package com.soywiz.korge.intellij.editor.tiled

import com.intellij.util.ui.*
import com.soywiz.korge.intellij.util.*
import com.soywiz.korim.awt.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korim.color.*
import com.soywiz.korio.async.*
import com.soywiz.korma.geom.*
import com.soywiz.korge.tiled.*
import java.awt.*
import java.awt.Point
import java.awt.Rectangle
import java.awt.event.*
import java.awt.image.*
import javax.swing.*

class MapComponent(val tmx: TiledMap) : JComponent() {
	val downRightTileSignal = Signal<PointInt>()
	val onZoom = Signal<Int>()
	val upTileSignal = Signal<PointInt>()
	val downTileSignal = Signal<PointInt>()
	val middleTileSignal = Signal<PointInt>()
	val overTileSignal = Signal<PointInt>()
	val outTileSignal = Signal<PointInt>()

	fun mapRepaint(invalidateBitmapCache: Boolean) {
		if (invalidateBitmapCache) {
			cachedBitmap.invalidate()
		}
		updateSize()
		revalidate()
		parent?.revalidate()
		parent?.repaint()
	}

	var scale: Double = 2.0
		set(value) {
			field = value
			mapRepaint(false)
		}
	val tilesSize = tmx.tilesets.sumBy { it.tileset.textures.size }

	val tiles = HashMap<Int, BitmapSlice<Bitmap32>?>(tilesSize).also { tiles ->
		for (tileset in tmx.tilesets) {
			for (tileIdx in tileset.tileset.textures.indices) {
				val tile = tileset.tileset.textures[tileIdx]
				if (tile != null) {
					val tileBmp = (tile as BitmapSlice<*>).extract().toBMP32()
                    if (tileBmp.any { it.a != 0 }) { // not transparent
                        tiles[tileset.firstgid + tileIdx] = tileBmp.slice()
					}
				}
			}
		}
	}

	init {
		updateSize()
		addMouseMotionListener(object : MouseMotionAdapter() {
			override fun mouseDragged(e: MouseEvent) {
				//println("mouseDragged: $e")
				when {
					SwingUtilities.isLeftMouseButton(e) -> onPressMouse(e.point)
					SwingUtilities.isMiddleMouseButton(e) -> Unit
					else -> Unit
				}
			}

			override fun mouseMoved(e: MouseEvent) {
				//println("mouseMoved: $e")
				overTileSignal(getTileIndex(e.point))
			}

		})
		addMouseWheelListener { e ->
			if (e.isControlDown) {
				//val dir = e.wheelRotation
				//println("mouseWheelMoved: $e")
				onZoom(-e.wheelRotation)
			} else {
				parent.dispatchEvent(e)
			}
		}
		addMouseListener(object : MouseAdapter() {
			override fun mouseExited(e: MouseEvent) {
				outTileSignal(getTileIndex(e.point))
			}

			override fun mouseReleased(e: MouseEvent) {
				if (e.button == MouseEvent.BUTTON1) {
					upTileSignal(getTileIndex(e.point))
				}
			}

			override fun mousePressed(e: MouseEvent) {
				//println("mousePressed: $e")
				when {
					SwingUtilities.isLeftMouseButton(e) -> onPressMouse(e.point)
					SwingUtilities.isMiddleMouseButton(e) -> onMiddleMouse(e.point)
					else -> onRightPressMouse(e.point)
				}
			}
		})
	}

	var currentTileSelected = 1

	fun onMiddleMouse(point: Point) {
		val tileIndex = getTileIndex(point)
		middleTileSignal(tileIndex)
	}

	fun onPressMouse(point: Point) {
		val tileIndex = getTileIndex(point)
		downTileSignal(tileIndex)
		/*
		tmx.patternLayers[0].map[tileIndex.x, tileIndex.y] =
            RGBA(currentTileSelected)
		repaint()
		*/
		//println(tileIndex)
	}

	fun onRightPressMouse(point: Point) {
		val tileIndex = getTileIndex(point)
		downRightTileSignal(tileIndex)
		//currentTileSelected = tmx.patternLayers[0].map[tileIndex.x, tileIndex.y].value
	}

	fun updateSize() {
		this.preferredSize =
            Dimension((tmx.pixelWidth * scale).toInt(), (tmx.pixelHeight * scale).toInt())
	}

	val iscale get() = 1.0 / scale
	fun getTileIndex(coords: Point): PointInt =
		PointInt(
            ((iscale * coords.x) / tmx.tilewidth).toInt(),
            ((iscale * coords.y) / tmx.tileheight).toInt()
        )

	data class CacheKey(
		val offsetX: Int, val offsetY: Int,
		val displayTilesX: Int, val displayTilesY: Int,
		val TILE_WIDTH: Int, val TILE_HEIGHT: Int
	)
	data class GridCacheKey(
		val displayTilesX: Int, val displayTilesY: Int,
		val TILE_WIDTH: Int, val TILE_HEIGHT: Int,
		val scale: Double
	)
	private var cachedBitmap = SingleCache<CacheKey, BufferedImage>()
	private var cachedGrid = SingleCache<GridCacheKey, BufferedImage>()

	override fun paintComponent(g: Graphics) {
		val g = (g as Graphics2D)

		val TILE_WIDTH = tmx.tilewidth
		val TILE_HEIGHT = tmx.tileheight

		g.setRenderingHint(
            RenderingHints.KEY_INTERPOLATION,
            RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR
        )
		val clipBounds = g.clipBounds
		val displayTilesX = ((clipBounds.width / TILE_WIDTH / scale) + 3).toInt().coerceIn(1, tmx.width)
		val displayTilesY = ((clipBounds.height / TILE_HEIGHT / scale) + 3).toInt().coerceIn(1, tmx.height)
		val offsetX = (clipBounds.x / TILE_WIDTH / scale).toInt()
		val offsetY = (clipBounds.y / TILE_HEIGHT / scale).toInt()

		val tilesBitmap = cachedBitmap.get(CacheKey(
			offsetX = offsetX, offsetY = offsetY,
			displayTilesX = displayTilesX,
			displayTilesY = displayTilesY,
			TILE_WIDTH = TILE_WIDTH, TILE_HEIGHT = TILE_HEIGHT
		)) {
			val temp = Bitmap32(
				(displayTilesX * TILE_WIDTH),
				(displayTilesY * TILE_HEIGHT)
			)
			for (layer in tmx.allLayers) {
				if (!layer.visible) continue
				when (layer) {
					is TiledMap.Layer.Tiles -> {
						for (x in 0 until displayTilesX) {
							for (y in 0 until displayTilesY) {
								val rx = x + offsetX
								val ry = y + offsetY

								if (rx < 0 || rx >= layer.map.width) continue
								if (ry < 0 || ry >= layer.map.height) continue

								val tileIdx = layer.map[rx, ry].value
								val tile = tiles.getOrDefault(tileIdx, null)
								if (tile != null) {
									temp._fastMix(tile, x * TILE_WIDTH, y * TILE_HEIGHT)
								}
							}
						}
					}
				}
			}
			temp.toAwt()
		}

		//val oldTransform = g.transform
		g.preserveTransform {
			g.translate(offsetX * TILE_WIDTH * scale, offsetY * TILE_HEIGHT * scale)
			g.scale(scale, scale)
			g.drawImage(tilesBitmap, 0, 0, null)
		}

		//g.transform = oldTransform

		//g.translate(offsetX * TILE_WIDTH * scale, offsetY * TILE_HEIGHT * scale)

		if (drawGrid) {
			g.preserveTransform {
				g.translate(offsetX * TILE_WIDTH * scale, offsetY * TILE_HEIGHT * scale)
				g.drawImage(cachedGrid.get(GridCacheKey(
					displayTilesX = displayTilesX,
					displayTilesY = displayTilesY,
					TILE_WIDTH = TILE_WIDTH,
					TILE_HEIGHT = TILE_HEIGHT,
					scale = scale
				)) {
					UIUtil.createImage(this, (displayTilesX * TILE_WIDTH * scale).toInt() + 1, (displayTilesY * TILE_HEIGHT * scale).toInt() + 1, BufferedImage.TYPE_INT_ARGB_PRE).also {
						val g2 = it.createGraphics()
						g2.scale(scale, scale)
						g2.preserveStroke {
							g2.color = Color.DARK_GRAY
							g2.stroke = BasicStroke((1f / scale).toFloat(), BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0f, floatArrayOf(2f), 0f)
							for (y in 0..displayTilesY) g2.drawLine(0, y * TILE_HEIGHT, displayTilesX * TILE_WIDTH, y * TILE_HEIGHT)
							for (x in 0..displayTilesX) g2.drawLine(x * TILE_WIDTH, 0, x * TILE_WIDTH, displayTilesY * TILE_HEIGHT)
						}
					}
				}, 0, 0, null)
			}
		}

		selected?.let { selected ->
			g.preserveTransform {
				g.scale(scale, scale)
				g.preserveStroke {
					g.stroke = BasicStroke((2f / scale).toFloat())
					g.color = Color.RED
					g.drawRect(
						selected.x * TILE_WIDTH,
						selected.y * TILE_HEIGHT,
						selected.width * TILE_WIDTH,
						selected.height * TILE_HEIGHT
					)
				}
			}
		}
	}

	var drawGrid = true
	var selected: Rectangle? = null
	fun selectedRange(x: Int, y: Int, width: Int = 1, height: Int = 1) {
		selected = Rectangle(x, y, width, height)
		mapRepaint(false)
	}
	fun unselect() {
		selected = null
		mapRepaint(false)
	}
}

fun Bitmap32._fastMix(src: BitmapSlice<Bitmap32>, dx: Int = 0, dy: Int = 0) {
	val b = src.bounds
	val availableWidth = width - dx
	val availableHeight = height - dy
	val awidth = kotlin.math.min(availableWidth, b.width)
	val aheight = kotlin.math.min(availableHeight, b.height)
	_fastMix(src.bmp, dx, dy, b.x, b.y, b.x + awidth, b.y + aheight)
}

fun Bitmap32._fastMix(src: Bitmap32, dx: Int, dy: Int, sleft: Int, stop: Int, sright: Int, sbottom: Int) {
	val dst = this
	val width = sright - sleft
	val height = sbottom - stop
	val dstDataPremult = dst.dataPremult
	val srcDataPremult = src.dataPremult

	for (y in 0 until height) {
		val dstOffset = dst.index(dx, dy + y)
		val srcOffset = src.index(sleft, stop + y)
		for (x in 0 until width) dstDataPremult[dstOffset + x] = fastBlend(dstDataPremult[dstOffset + x], srcDataPremult[srcOffset + x])
	}
}

fun fastBlend32(dst: RGBAPremultiplied, src: RGBAPremultiplied): RGBAPremultiplied  {
	val alpha = (0x100 - src.a).coerceIn(0, 0x100)
	val colora = src.value
	val colorb = dst.value
	val a = (src.a) + ((alpha * (dst.a and 0xFF)) ushr 8)
	val rb = (colora and 0xFF00FF) + ((alpha * (colorb and 0xFF00FF)) ushr 8)
	val g = (colora and 0x00FF00) + ((alpha * (colorb and 0x00FF00)) ushr 8)
	return RGBAPremultiplied(((rb and 0xFF00FF) + (g and 0x00FF00)) or (a shl 24))
}

fun fastBlend(dst: RGBAPremultiplied, src: RGBAPremultiplied): RGBAPremultiplied  {
	val alpha = (0xFF - src.a) and 0xFF
	val colora = src.value.toLong()
	val colorb = dst.value.toLong()
	val rb = (colora and 0x00FF00FFL) + ((alpha * (colorb and 0x00FF00FFL)) ushr 8)
	val ag = (colora and 0xFF00FF00L) + ((alpha * (colorb and 0xFF00FF00L)) ushr 8)
	return RGBAPremultiplied(((rb and 0xFF00FFL) + (ag and 0xFF00FF00L)).toInt())
}
