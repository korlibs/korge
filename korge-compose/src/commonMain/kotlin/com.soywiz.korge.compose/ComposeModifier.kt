package com.soywiz.korge.compose

import com.soywiz.korge.input.mouse
import com.soywiz.korge.view.Circle
import com.soywiz.korge.view.Container
import com.soywiz.korge.view.View
import com.soywiz.korge.view.mask.mask
import com.soywiz.korge.view.position
import com.soywiz.korim.color.RGBA
import com.soywiz.korma.geom.Anchor
import com.soywiz.korma.geom.Rectangle

open class Modifier private constructor(val parts: List<ModifierPart> = listOf()) {
    fun then(other: ModifierPart): Modifier = Modifier(parts + listOf(other))

    companion object : Modifier()
}

interface ModifierPart

data class AnchorModifier(val anchor: Anchor) : ModifierPart
data class PaddingModifier(val padding: Double) : ModifierPart
data class ClickableModifier(val onClick: (() -> Unit)? = null) : ModifierPart
data class BackgroundColorModifier(val bgcolor: RGBA) : ModifierPart
data class FillMaxWidthModifier(val ratio: Double) : ModifierPart
data class SizeModifier(val width: Double, val height: Double) : ModifierPart
data class ClipModifier(val dummy: Unit = Unit) : ModifierPart


fun Modifier.backgroundColor(color: RGBA): Modifier = this.then(BackgroundColorModifier(color))
fun Modifier.anchor(anchor: Anchor): Modifier = this.then(AnchorModifier(anchor))
fun Modifier.padding(padding: Double): Modifier = this.then(PaddingModifier(padding))
fun Modifier.size(width: Number, height: Number): Modifier = this.then(SizeModifier(width.toDouble(), height.toDouble()))
fun Modifier.size(side: Number): Modifier = this.then(SizeModifier(side.toDouble(), side.toDouble()))
fun Modifier.clip(): Modifier = this.then(ClipModifier())
fun Modifier.fillMaxWidth(ratio: Double = 1.0): Modifier = this.then(FillMaxWidthModifier(ratio))
fun Modifier.clickable(onClick: (() -> Unit)? = null): Modifier = this.then(ClickableModifier(onClick))

fun View.applyModifiers(modifier: Modifier) {
    var anchor: Anchor? = null
    var padding: Double? = null
    for (mod in modifier.parts) {
        when (mod) {
            is PaddingModifier -> padding = mod.padding
            is AnchorModifier -> anchor = mod.anchor
            is BackgroundColorModifier -> colorMul = mod.bgcolor
            is ClickableModifier -> this.mouse.click.also { it.clear() }.add { mod.onClick?.invoke() }
            is SizeModifier -> setSize(mod.width, mod.height)
            is FillMaxWidthModifier -> {
                this.x = 0.0
                this.scaledWidth = parent!!.width * mod.ratio
            }
            is ClipModifier -> {
                // @TODO: Fix this!
                val mask = Circle(32.0)
                this.mask = mask
                (this as Container).addChild(mask)
            }
        }
    }
    if (anchor != null || padding != null) {
        val anchor = anchor ?: Anchor.TOP_LEFT
        val padding = padding ?: 0.0
        val parentBounds = this.parent!!.getLocalBounds(Rectangle())
        this.position((parentBounds.width - this.width) * anchor.sx - padding, (parentBounds.height - this.height) * anchor.sy - padding)
    }
}
