package com.soywiz.korag

open class NAGLog(width: Int = 1024, height: Int = 1024) : NAG() {
    init {
        resized(0, 0, width, height)
    }
    val log = arrayListOf<String>()
    override fun execute(command: NAGCommand) {
        log += command.toString()
    }
}
