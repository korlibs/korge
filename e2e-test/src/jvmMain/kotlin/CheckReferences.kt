import com.soywiz.korim.bitmap.*
import com.soywiz.korim.format.*
import com.soywiz.korio.*
import com.soywiz.korio.file.*
import com.soywiz.korio.file.std.*
import kotlinx.coroutines.flow.*
import kotlin.jvm.*
import kotlin.system.*

object CheckReferences {
    @JvmStatic
    fun main(args: Array<String>) = Korio {
        val references = localCurrentDirVfs["references"]
        var notSimilarCount = 0
        for (kind in listOf("jvm", "mingwx64")) {
            val generatedVfs = localCurrentDirVfs["build/screenshots/$kind"]
            val exists = generatedVfs.exists()
            println("generatedVfs=$generatedVfs . exists=${exists}")
            if (exists) {
                references.list().filter { it.extensionLC == "png" }.collect { file ->
                    val otherFile = generatedVfs[file.baseName]
                    println("Comparing ${file.absolutePath} <-> ${otherFile.absolutePath}")
                    val bitmap1 = file.readBitmap().toBMP32()
                    val bitmap2 = otherFile.readBitmap().toBMP32()
                    val similar = Bitmap32.matches(bitmap1, bitmap2, threshold = 32)
                    val equals = bitmap1.contentEquals(bitmap2)
                    val psnr = Bitmap32.computePsnr(bitmap1, bitmap2)
                    println(" --> equals=$equals, similar=$similar, psnr=$psnr")
                    if (!similar) notSimilarCount++
                }
            }
        }

        println("Exiting with... exitCode=$notSimilarCount")
        exitProcess(notSimilarCount)
    }
}

