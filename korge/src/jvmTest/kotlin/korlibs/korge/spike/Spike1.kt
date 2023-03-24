package korlibs.korge.spike

import korlibs.korge.*
import korlibs.korge.view.*
import korlibs.image.color.*

object Spike1 {
	@JvmStatic suspend fun main(args: Array<String>) {
		Korge {
			solidRect(100, 100, Colors.RED) {
				x = 100.0
				y = 100.0
			}
		}
	}
}
