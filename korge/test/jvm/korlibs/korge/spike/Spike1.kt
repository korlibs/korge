package korlibs.korge.spike

import korlibs.image.color.*
import korlibs.korge.*
import korlibs.korge.view.*

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
