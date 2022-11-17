package com.soywiz.korge.ui

import com.soywiz.korge.render.*
import com.soywiz.korim.color.*
import com.soywiz.korma.geom.*
import com.soywiz.korma.interpolation.*

interface UIBaseCheckBoxSkin {
    enum class Kind { CHECKBOX, RADIO, SWITCH }
    fun render(
        ctx2d: RenderContext2D,
        width: Double,
        height: Double,
        item: UIBaseCheckBox<*>,
        kind: Kind,
    )
}

open class UIBaseCheckBoxSkinMaterial : UIBaseCheckBoxSkin {
    companion object : UIBaseCheckBoxSkinMaterial()

    override fun render(
        ctx2d: RenderContext2D,
        width: Double,
        height: Double,
        item: UIBaseCheckBox<*>,
        kind: UIBaseCheckBoxSkin.Kind,
    ) {
        run {
            val extraPad = -0.0
            val extraPad2 = extraPad * 2
            ctx2d.materialRoundRect(
                0.0 + extraPad, 0.0 + extraPad, height - extraPad2, height - extraPad2, radius = RectCorners((height - extraPad2) * 0.5),
                color = item.overRatio.interpolate(Colors.TRANSPARENT_BLACK, if (item.checked) MaterialColors.BLUE_100.withAd(0.3) else MaterialColors.GRAY_700.withAd(0.3))
            )
            item.highlights.fastForEach {
                ctx2d.materialRoundRect(
                    0.0 + extraPad, 0.0 + extraPad, height - extraPad2, height - extraPad2, radius = RectCorners(height * 0.5),
                    color = Colors.TRANSPARENT_BLACK,
                    highlightPos = it.pos,
                    highlightRadius = it.radiusRatio,
                    highlightColor = MaterialColors.BLUE_700.withAd(it.alpha * 0.7),
                    //colorMul = renderColorMul,
                )
            }
        }

        when (kind) {
            UIBaseCheckBoxSkin.Kind.SWITCH, UIBaseCheckBoxSkin.Kind.CHECKBOX -> {
                val padding = 6.0
                val padding2 = padding * 2
                ctx2d.materialRoundRect(
                    0.0 + padding, 0.0 + padding, height - padding2, height - padding2, radius = RectCorners(4.0),
                    //color = if (this@UIBaseCheckBox.checked) MaterialColors.BLUE_600 else Colors.TRANSPARENT_BLACK,
                    color = Colors.TRANSPARENT_BLACK,
                    borderColor = item.checkedRatio.interpolate(MaterialColors.GRAY_700, MaterialColors.BLUE_600),
                    borderSize = 2.0,
                )
                run {
                    val padding = item.checkedRatio.interpolate(height, 10.0)
                    val padding2 = padding * 2
                    ctx2d.materialRoundRect(
                        0.0 + padding, 0.0 + padding, height - padding2, height - padding2, radius = RectCorners(1.0),
                        color = item.checkedRatio.interpolate(Colors.TRANSPARENT_BLACK, MaterialColors.BLUE_600),
                    )
                }
            }
            else -> {
                val padding = 6.0
                val padding2 = padding * 2
                ctx2d.materialRoundRect(
                    0.0 + padding, 0.0 + padding, height - padding2, height - padding2, radius = RectCorners((height - padding2) * 0.5),
                    borderSize = 2.0,
                    borderColor = item.checkedRatio.interpolate(MaterialColors.GRAY_700, MaterialColors.BLUE_600),
                    color = Colors.TRANSPARENT_BLACK,
                    highlightRadius = item.checkedRatio.interpolate(0.0, 0.2),
                    highlightPos = Point(0.5, 0.5),
                    highlightColor = MaterialColors.BLUE_600,
                )
            }
        }
    }
}

/*
import com.soywiz.klock.*
import com.soywiz.korge.animate.*
import com.soywiz.korge.render.*
import com.soywiz.korge.tween.*
import com.soywiz.korge.view.*
import com.soywiz.korim.color.*
import com.soywiz.korma.geom.*
import com.soywiz.korma.interpolation.*

interface NewUIButtonSkin {
    fun renderUIButton(view: RenderableView, button: UIButton)
    fun updatedUIButton(button: UIButton, down: Boolean? = null, over: Boolean? = null, px: Double = 0.0, py: Double = 0.0)
}

interface NewUISkin : NewUIButtonSkin {
}

private var UIButton.highlightRadius: Double by extraViewProp { 0.0 }
private var UIButton.highlightColor: RGBA by extraViewProp { Colors.WHITE }
private var UIButton.highlightPos: Point by extraViewProp { Point() }

private var UIButton.borderSize: Double by extraViewProp { 0.0 }
private var UIButton.borderColor: RGBA by extraViewProp { Colors.WHITE }

object DefaultUISkin : NewUISkin {

    override fun renderUIButton(view: RenderableView, button: UIButton) {
        view.ctx2d.materialRoundRect(
            0.0, 0.0, button.width, button.height,
            color = button.bgcolor,
            radius = RectCorners(button.radiusPoints()),
            shadowColor = Colors.BLACK.withAd(0.7),
            shadowOffset = if (button.elevation) Point(0.0, -3.0) else Point(0.0, 0.0),
            shadowRadius = if (button.elevation) 6.0 else 0.0,
            highlightRadius = button.highlightRadius,
            highlightColor = button.highlightColor,
            highlightPos = button.highlightPos,
            borderSize = button.borderSize,
            borderColor = button.borderColor,
        )
    }

    override fun updatedUIButton(button: UIButton, down: Boolean?, over: Boolean?, px: Double, py: Double) {
        if (!button.enabled) {
            //button.animStateManager.set(
            //    AnimState(
            //        button::bgcolor[button.bgColorDisabled]
            //))
            //button.animatorEffects.cancel()
            button.bgcolor = button.bgColorDisabled
            button.invalidateRender()
            return
        }
        //println("UPDATED: down=$down, over=$over, px=$px, py=$py")
        if (down == true) {
            //button.animStateManager.set(
            //    AnimState(
            //        button::highlightRadius[0.0, 1.0],
            //        button::highlightAlpha[1.0],
            //        button::highlightPos[Point(px / button.width, py / button.height), Point(px / button.width, py / button.height)],
            //    ))
            button.highlightPos.setTo(px / button.width, py / button.height)
            button.animatorEffects.tween(
                button::highlightRadius[0.0, 1.0],
                button::highlightColor[Colors.WHITE.withAd(0.5), Colors.WHITE.withAd(0.5)],
                time = 0.5.seconds, easing = Easing.EASE_IN
            )
        }
        if (down == false) {
            //button.animStateManager.set(
            //    AnimState(button::highlightAlpha[0.0])
            //)
            button.animatorEffects.tween(button::highlightColor[Colors.TRANSPARENT_BLACK], time = 0.2.seconds)
        }
        if (over != null) {
            val bgcolor = when {
                !button.enabled -> button.bgColorDisabled
                over -> button.bgColorOver
                else -> button.bgColorOut
            }
            //button.animStateManager.set(
            //    AnimState(
            //        button::bgcolor[bgcolor]
            //    )
            //)
            button.animator.tween(button::bgcolor[bgcolor], time = 0.25.seconds)
        }
    }
}
*/
