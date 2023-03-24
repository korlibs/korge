#!/usr/bin/env kotlinc -script -J-Xmx2g

import java.io.*

File(".").walkBottomUp().filter { it.extension == "kt" }.forEach {
    val text = it.readText()
    println("it: $it")
    it.writeText(text.trimEnd() + "\n")
}
