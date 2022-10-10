package samples

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.soywiz.klock.milliseconds
import com.soywiz.klock.seconds
import com.soywiz.korev.Key
import com.soywiz.korge.compose.Animatable
import com.soywiz.korge.compose.Box
import com.soywiz.korge.compose.Button
import com.soywiz.korge.compose.Canvas
import com.soywiz.korge.compose.HStack
import com.soywiz.korge.compose.Image
import com.soywiz.korge.compose.KeyDown
import com.soywiz.korge.compose.Modifier
import com.soywiz.korge.compose.Text
import com.soywiz.korge.compose.VStack
import com.soywiz.korge.compose.anchor
import com.soywiz.korge.compose.backgroundColor
import com.soywiz.korge.compose.clickable
import com.soywiz.korge.compose.clip
import com.soywiz.korge.compose.fillMaxWidth
import com.soywiz.korge.compose.padding
import com.soywiz.korge.compose.setComposeContent
import com.soywiz.korge.compose.size
import com.soywiz.korge.scene.Scene
import com.soywiz.korge.view.SContainer
import com.soywiz.korge.view.Container
import com.soywiz.korim.bitmap.BmpSlice
import com.soywiz.korim.color.Colors
import com.soywiz.korim.color.interpolate
import com.soywiz.korim.format.readBitmapSlice
import com.soywiz.korio.async.delay
import com.soywiz.korio.file.std.resourcesVfs
import com.soywiz.korma.geom.Anchor
import com.soywiz.korma.geom.vector.roundRect
import kotlin.coroutines.cancellation.CancellationException

class MainComposable : Scene() {
    override suspend fun SContainer.sceneMain() {
        setComposeContent(this) {
            MainApp()
        }
    }
}

@Composable
private fun MainApp() {
    //App()
    var color by remember { mutableStateOf(Colors.RED) }
    //val color2 by remember { Animatable(Colors.RED) }
    var count by remember { mutableStateOf(0) }
    var ratio by remember { mutableStateOf(0.0) }
    var bitmap by remember { mutableStateOf<BmpSlice?>(null) }

    //LaunchedEffect(true) { color2.animateTo(Colors.GREEN) }

    LaunchedEffect(count) {
        println("LaunchedEffect=$count..started")
        try {
            val nsteps = 20
            for (n in 0..nsteps) {
                val r = n.toDouble() / nsteps.toDouble()
                ratio = r
                color = r.interpolate(Colors.RED, Colors.WHITE)
                delay(10.milliseconds)
            }
            println("LaunchedEffect=$count..ended")
        } catch (e: CancellationException) {
            println("LaunchedEffect=$count..cancelled")
        }
        //stage!!.tween(::color[Colors.BLUE])
    }
    LaunchedEffect(true) {
        while (true) {
            bitmap = resourcesVfs["korge.png"].readBitmapSlice()
            delay(2.0.seconds)
            bitmap = resourcesVfs["korim.png"].readBitmapSlice()
            delay(2.0.seconds)
        }
    }
    VStack {
        Text("$count", color)
        HStack {
            Button("-") { count-- }
            Button("+") { count++ }
        }
        Canvas(color) {
            fill(color) {
                roundRect(0.0, 0.0, 100.0, 100.0, 50 * ratio, 50 * ratio)
            }
        }
        HStack {
            Image(bitmap)
            Image(bitmap)
        }
        Image(bitmap, modifier = Modifier.size(64.0).clip())
        OkErrorComponent()
    }
    Box(
        Modifier
            .anchor(Anchor.BOTTOM_RIGHT)
            .padding(16.0)
            .backgroundColor(color)
            .size(300.0, 200.0)
            .clickable {
                color = Colors.GREEN
                count++
            }
    ) {
        Box(Modifier.backgroundColor(Colors.RED).size(50.0))
    }
    Box(
        Modifier
            .anchor(Anchor.BOTTOM_LEFT)
            .padding(16.0)
            .backgroundColor(color)
            .size(300.0, 200.0)
            .clickable {
                color = Colors.GREEN
                count--
            }
    )
    Box(
        Modifier
            .anchor(Anchor.BOTTOM_LEFT)
            .padding(16.0)
            .backgroundColor(color)
            .fillMaxWidth()
            .clickable {
                color = Colors.GREEN
            }
    )
    KeyDown(Key.DOWN) { count-- }
    KeyDown(Key.UP) { count++ }

}

@Composable
fun OkErrorComponent() {
    var ok by remember { mutableStateOf<Boolean?>(null) }
    val color = remember { Animatable(Colors.DARKGREY) }
    LaunchedEffect(ok) {
        color.animateTo(
            when (ok) {
                null -> Colors.DARKGREY
                true -> Colors.GREEN
                false -> Colors.RED
            }
        )
    }
    VStack {
        Box(Modifier.backgroundColor(color.value))
        Button("ok") { ok = true }
        Button("error") { ok = false }
    }
}
