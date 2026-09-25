package org.korge.application

import korlibs.image.color.*
import korlibs.korge.input.*
import korlibs.korge.scene.*
import korlibs.korge.view.*
import korlibs.math.geom.*
import korlibs.time.*

/** A focused first-release workspace: real canvas input, timeline state and responsive studio chrome. */
class AnimoraStudioScene : Scene() {
    private val ink = Colors["#F3F4F7"]
    private val muted = Colors["#8790A5"]
    private val panel = Colors["#181D28"]
    private val panelRaised = Colors["#222938"]
    private val accent = Colors["#8BE9C2"]
    private val purple = Colors["#9C8CFF"]
    private val canvasWhite = Colors["#FBFCFF"]

    override suspend fun SContainer.sceneMain() {
        val project = AnimoraProject("Untitled story")
        val timeline = TimelineState()
        var selectedTool = AnimoraTool.PENCIL
        var selectedColor = Colors["#25283A"]
        var brushSize = 6.0

        fun label(value: String, x: Double, y: Double, size: Double = 14.0, color: RGBA = ink) =
            text(value, textSize = size, color = color).position(x, y)

        fun card(x: Double, y: Double, w: Double, h: Double, color: RGBA = panel) =
            solidRect(w, h, color).position(x, y)

        fun actionButton(
            title: String, x: Double, y: Double, w: Double = 44.0,
            active: Boolean = false, onTap: () -> Unit,
        ) {
            val button = container {
                solidRect(w, 38.0, if (active) accent else panelRaised)
                text(title, textSize = 13.0, color = if (active) Colors["#10131B"] else ink)
                    .position(12.0, 10.0)
            }.position(x, y)
            button.mouse { onClick { onTap() } }
        }

        // Header: product identity, project safety, and the primary render action.
        card(0.0, 0.0, stage.width, 64.0, panel)
        label("A", 24.0, 16.0, 24.0, accent)
        label("ANIMORA", 52.0, 13.0, 16.0, ink)
        label("STUDIO", 53.0, 34.0, 9.0, muted)
        label(project.name, 190.0, 22.0, 15.0, ink)
        label("Saved just now", 190.0, 42.0, 10.0, muted)
        actionButton("Export", stage.width - 108.0, 13.0, 84.0, onTap = { label("Export queued", stage.width - 210.0, 24.0, 11.0, accent) })

        // Left tool rail. Buttons are intentionally text-labelled for discoverability and accessibility.
        card(0.0, 64.0, 80.0, stage.height - 64.0, panel)
        val tools = listOf(
            AnimoraTool.PENCIL to "Pencil", AnimoraTool.INK to "Ink", AnimoraTool.BRUSH to "Brush",
            AnimoraTool.ERASER to "Erase", AnimoraTool.FILL to "Fill", AnimoraTool.TRANSFORM to "Move",
            AnimoraTool.SELECT to "Select", AnimoraTool.TEXT to "Type",
        )
        tools.forEachIndexed { index, (tool, name) ->
            actionButton(name, 8.0, 82.0 + index * 48.0, 64.0, selectedTool == tool) {
                selectedTool = tool
            }
        }

        // Central canvas with a faint production grid and live stroke capture.
        val canvasLeft = 96.0
        val canvasTop = 88.0
        val timelineTop = stage.height - 196.0
        val canvasWidth = stage.width - 316.0
        val canvasHeight = timelineTop - canvasTop - 18.0
        card(canvasLeft - 8, canvasTop - 8, canvasWidth + 16, canvasHeight + 16, Colors["#0C0F15"])
        val canvas = solidRect(canvasWidth, canvasHeight, canvasWhite).position(canvasLeft, canvasTop)
        val grid = graphics(renderer = GraphicsRenderer.SYSTEM).position(canvasLeft, canvasTop)
        grid.updateShape {
            stroke(Colors["#E4E8F0"], lineWidth = 1.0) {
                for (x in 0..canvasWidth.toInt() step 48) { moveTo(x.toDouble(), 0.0); lineTo(x.toDouble(), canvasHeight) }
                for (y in 0..canvasHeight.toInt() step 48) { moveTo(0.0, y.toDouble()); lineTo(canvasWidth, y.toDouble()) }
            }
        }
        val strokes = mutableListOf<MutableList<Point>>()
        val drawing = graphics(renderer = GraphicsRenderer.SYSTEM).position(canvasLeft, canvasTop)
        fun redraw() {
            drawing.updateShape {
                strokes.forEach { points ->
                    if (points.size > 1) stroke(selectedColor, lineWidth = brushSize) {
                        moveTo(points.first())
                        points.drop(1).forEach { lineTo(it) }
                    }
                }
            }
        }
        canvas.mouse {
            onDown {
                if (selectedTool == AnimoraTool.ERASER) return@onDown
                val position = stage.mousePos - Point(canvasLeft, canvasTop)
                strokes += mutableListOf(position)
                redraw()
            }
            onMouseDrag {
                if (selectedTool == AnimoraTool.ERASER) return@onMouseDrag
                strokes.lastOrNull()?.add(stage.mousePos - Point(canvasLeft, canvasTop))
                redraw()
            }
        }
        label("FRAME ${timeline.currentFrame.toString().padStart(2, '0')}", canvasLeft + 16, canvasTop + 14, 11.0, muted)
        label("24 FPS  ·  1920 × 1080", canvasLeft + canvasWidth - 160, canvasTop + 14, 10.0, muted)

        // Right inspector: brush controls, swatches, and scene layers.
        val inspectorLeft = stage.width - 208.0
        card(inspectorLeft, 64.0, 208.0, timelineTop - 64.0, panel)
        label("BRUSH", inspectorLeft + 18, 86.0, 11.0, muted)
        label("${selectedTool.name.lowercase().replaceFirstChar { it.uppercase() }}", inspectorLeft + 18, 108.0, 17.0, ink)
        label("Size", inspectorLeft + 18, 150.0, 11.0, muted)
        label("${brushSize.toInt()} px", inspectorLeft + 138, 150.0, 11.0, ink)
        actionButton("−", inspectorLeft + 18, 168.0, 48.0, onTap = { brushSize = (brushSize - 1).coerceAtLeast(1.0) })
        actionButton("+", inspectorLeft + 74, 168.0, 48.0, onTap = { brushSize = (brushSize + 1).coerceAtMost(80.0) })
        label("COLOR", inspectorLeft + 18, 228.0, 11.0, muted)
        listOf("#25283A", "#EF6F8E", "#F6C85F", "#8BE9C2", "#9C8CFF").forEachIndexed { i, hex ->
            val swatch = solidRect(26.0, 26.0, Colors[hex]).position(inspectorLeft + 18 + i * 32, 248.0)
            swatch.mouse { onClick { selectedColor = Colors[hex] } }
        }
        label("LAYERS", inspectorLeft + 18, 308.0, 11.0, muted)
        project.scenes.first().layers.take(3).forEachIndexed { i, layer ->
            card(inspectorLeft + 12, 330.0 + i * 42, 184.0, 34.0, if (i == 0) panelRaised else Colors["#1D222E"])
            label(if (i == 0) "◉" else "○", inspectorLeft + 22, 339.0 + i * 42, 12.0, if (i == 0) accent else muted)
            label(layer.name, inspectorLeft + 46, 338.0 + i * 42, 12.0, ink)
        }

        // Timeline: frame ruler, exposure blocks, transport controls, onion-skin affordance.
        card(80.0, timelineTop, stage.width - 80.0, stage.height - timelineTop, panel)
        label("TIMELINE", 98.0, timelineTop + 14.0, 11.0, muted)
        label("Opening", 98.0, timelineTop + 36.0, 14.0, ink)
        actionButton(if (timeline.isPlaying) "Pause" else "Play", 210.0, timelineTop + 14.0, 64.0, active = timeline.isPlaying) { timeline.togglePlayback() }
        actionButton("Onion", 282.0, timelineTop + 14.0, 64.0, active = true) { }
        actionButton("−", 358.0, timelineTop + 14.0, 38.0) { timeline.setFrame(timeline.currentFrame - 1) }
        actionButton("+", 402.0, timelineTop + 14.0, 38.0) { timeline.setFrame(timeline.currentFrame + 1) }
        val rulerLeft = 220.0
        val rulerTop = timelineTop + 78.0
        for (frame in 1..24) {
            val x = rulerLeft + (frame - 1) * 28.0
            solidRect(24.0, 28.0, if (frame == timeline.currentFrame) accent else if (frame % 4 == 1) purple else panelRaised)
                .position(x, rulerTop)
            if (frame % 4 == 1) label("$frame", x + 4, rulerTop - 18, 9.0, muted)
        }
        label("Character", 98.0, rulerTop + 8.0, 11.0, muted)
        addFastUpdater {
            if (timeline.isPlaying) timeline.setFrame(timeline.currentFrame + 1)
        }
    }
}
