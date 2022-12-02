package com.soywiz.korag

open class NAGLog : NAG() {
    val log = arrayListOf<String>()
    override fun execute(command: NAGCommand) {
        log += command.toString()
    }
}
