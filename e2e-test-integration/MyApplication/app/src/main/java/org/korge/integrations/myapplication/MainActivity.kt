package org.korge.integrations.myapplication

import android.content.Context
import android.os.Bundle
import android.view.*
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.*
import androidx.compose.ui.viewinterop.*
import korlibs.image.color.*
import korlibs.korge.*
import korlibs.korge.android.*
import korlibs.korge.scene.*
import korlibs.korge.view.*
import korlibs.render.*
import org.korge.integrations.myapplication.korge.*
import org.korge.integrations.myapplication.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MyApplicationTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Greeting("Android")
                        MyKorgeView()
                        Greeting("World")
                    }
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    MyApplicationTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            Greeting("Android")
            MyKorgeView()
            Greeting("World")
        }
    }
}

class MyKorgeAndroidView(context: Context) : KorgeAndroidView(context) {
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val desiredWidth = 100
        val desiredHeight = 100

        val width = resolveSize(desiredWidth, widthMeasureSpec)
        val height = resolveSize(desiredHeight, heightMeasureSpec)

        //setMeasuredDimension(width, height)
        setMeasuredDimension(100, 100)
    }
}

@Composable
fun MyKorgeView() {
    AndroidView(
        modifier = Modifier.size(100.dp),
        factory = { context ->
            println("!!!FACTORY!")
            MyKorgeAndroidView(context).also {
                println("!!!LOAD MODULE!")
                it.loadModule(Korge(main = {
                    println("!!!MAIN METHOD!")
                    solidRect(100, 100, Colors.RED)
                    //sceneContainer().changeTo { KorgeMainScene() }
                }))
                println("!!!LOAD MODULE FINISH!")
            }
        }, update = { view ->
        }
    )
}
