package com.soywiz.korio.android

/*
import android.app.Activity
import android.os.Bundle
import com.soywiz.kds.Extra
import com.soywiz.korio.async.EventLoop
import com.soywiz.korio.async.Signal
import com.soywiz.korio.util.Once

object KorioApp {
	val initOnce = Once()
	val resized = Signal<Unit>()
}

open class KorioActivity : Activity(), Extra by Extra.Mixin() {
	lateinit var eventLoop: EventLoop

	override final fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		KorioAndroidInit(this)
		KorioApp.initOnce {
			EventLoop.main {
				eventLoop = this@main
				println()
				main(arrayOf())
			}
		}
	}

	override fun onWindowFocusChanged(hasFocus: Boolean) {
		super.onWindowFocusChanged(hasFocus)
		eventLoop.queue { KorioApp.resized(Unit) }
	}

	suspend open fun requestPermission(name: String): Boolean {
		eventLoop.sleep(1000)
		return false
	}

	/*
	override fun onConfigurationChanged(newConfig: Configuration) {
		//println(newConfig.orientation)
		resized(Unit)
	}
	*/

	suspend protected open fun main(args: Array<String>) {
	}
}
*/
