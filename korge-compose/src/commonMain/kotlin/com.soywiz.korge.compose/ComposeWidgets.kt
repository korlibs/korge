package com.soywiz.korge.compose

import androidx.compose.runtime.*
import com.soywiz.korev.Key
import com.soywiz.korge.annotations.KorgeExperimental
import com.soywiz.korge.input.*
import com.soywiz.korge.ui.*
import com.soywiz.korge.view.DummyView
import com.soywiz.korge.view.SolidRect
import com.soywiz.korge.view.View
import com.soywiz.korge.view.addUpdater
import com.soywiz.korge.view.vector.GpuShapeView
import com.soywiz.korim.bitmap.Bitmaps
import com.soywiz.korim.bitmap.BmpSlice
import com.soywiz.korim.color.Colors
import com.soywiz.korim.color.RGBA
import com.soywiz.korim.vector.Context2d
import com.soywiz.korma.geom.Anchor
import com.soywiz.korma.geom.ScaleMode

@Composable
fun Text(text: String, color: RGBA = Colors.WHITE, onClick: () -> Unit = {}) {
    ComposeKorgeView({
        UIText("DUMMY", height = UIButton.DEFAULT_HEIGHT).also {
            println("Created UIText")
        }
    }) {
        set(text) { this.text = it }
        set(color) { this.colorMul = it }
        set(onClick) {
            this.onClick { onClick() }
        }
    }
}

@Composable
fun Button(text: String, onClick: () -> Unit = {}) {
    ComposeKorgeView({
        UIButton().also {
            println("Created UIButton")
        }
    }) {
        set(text) { this.text = it }
        set(onClick) { this.onClick { onClick() } }
    }
}

@Composable
fun VStack(content: @Composable () -> Unit) {
    ComposeKorgeView(::UIVerticalStack, {}, content)
}

@Composable
fun HStack(content: @Composable () -> Unit) {
    ComposeKorgeView(::UIHorizontalStack, {}, content)
}

@Composable
@Deprecated("Let's use Modifier instead")
fun KeyDown(key: Key, onPress: (Key) -> Unit = {}) {
    KeyDown { if (it == key) onPress(it) }
}

@Composable
@Deprecated("Let's use Modifier instead")
fun KeyDown(onPress: (Key) -> Unit = {}) {
    ComposeNode<DummyView, NodeApplier>({
        DummyView()
    }) {
        set(onPress) { this.keys.down { onPress(it.key) } }
    }
}

/**
 * [keys] determine if the object changed to redraw it if required.
 */
@OptIn(KorgeExperimental::class)
@Composable
fun Canvas(vararg keys: Any?, onDraw: Context2d.() -> Unit = {}) {
    ComposeKorgeView({
        GpuShapeView().also {
            it.updateShape { onDraw() }
        }
    }) {
        set(keys.toList()) {
            this.updateShape(onDraw)
        }
    }
}


@Composable
fun Box(modifier: Modifier = Modifier, content: @Composable () -> Unit = {}) {
    ComposeKorgeView(
        { SolidRect(100.0, 100.0, Colors.WHITE) },
        {
            set(modifier) { applyModifiers(modifier) }
        },
        content
    )
}

@Composable
fun Image(bitmap: BmpSlice?, modifier: Modifier = Modifier, content: @Composable () -> Unit = {}) {
    ComposeKorgeView(
        { UIImage(100.0, 100.0, Bitmaps.transparent, ScaleMode.SHOW_ALL, Anchor.CENTER) },
        {
            set(bitmap) { this.bitmap = bitmap ?: Bitmaps.transparent }
            set(modifier) { applyModifiers(modifier) }
        },
        content
    )
}
