import com.soywiz.klock.TimeSpan
import com.soywiz.klock.measureTime
import com.soywiz.klock.milliseconds
import com.soywiz.korge.view.Image
import com.soywiz.korim.bitmap.Bitmap
import com.soywiz.korim.bitmap.BmpSlice
import com.soywiz.korim.bitmap.sliceWithSize
import com.soywiz.korio.async.delay

class SpriteAnimation(
    var spriteView : Image,
    val spriteMap: Bitmap,
    val spriteWidth: Int = 16,
    val spriteHeight: Int = 16,
    val marginTop: Int = 0,
    val marginLeft: Int = 0,
    val columns: Int = 1,
    val lines: Int = 1,
    val offsetBetweenColumns: Int = 0,
    val offsetBetweenLines: Int = 0,
    val scale: Number = 1.0) {

    private var spriteStack: MutableList<BmpSlice> = mutableListOf(spriteView.bitmap)
    private var cycles = 0
    private var currentSpriteIndex = 0
    private var stop = false

    init{
        spriteStack.removeAt(0) //remove transparent initializer
        var line = 0
        repeat(columns) { col ->
            val resourceBitmap = spriteMap
            spriteStack.add(
                resourceBitmap.sliceWithSize(
                    marginLeft + (spriteWidth + offsetBetweenColumns) * col,
                    marginTop + (spriteHeight + offsetBetweenLines) * line,
                    spriteWidth,
                    spriteHeight
                )
            )
            if (col % columns == 0 && col != 0) {
                line++
            }
        }
        spriteView.bitmap=spriteStack[0]
    }


    fun nextSprite(){
        spriteView.bitmap=spriteStack[(++currentSpriteIndex % spriteStack.size)]
    }

    fun previousSprite(){
        spriteView.bitmap=spriteStack[(--currentSpriteIndex % spriteStack.size)]
    }

    suspend fun playForDuration(duration: TimeSpan, spriteDisplayTime: TimeSpan = 25.milliseconds) {
        if (duration > 0.milliseconds) {
            var timeCounter = duration
            while (timeCounter > 0.milliseconds && !stop ) {
                val measuredTime = measureTime {
                    nextSprite()
                    delay(spriteDisplayTime)
                }
                timeCounter -= measuredTime
            }
        }
    }

    suspend fun play(times: Int = 1, spriteDisplayTime: TimeSpan = 25.milliseconds) {
        val cycleCount = times*spriteStack.size
        var cycles = 0
        while (cycles < cycleCount && !stop) {
            nextSprite()
            delay(spriteDisplayTime)
            cycles++
        }
    }

    suspend fun playLooped(spriteDisplayTime: TimeSpan = 25.milliseconds) {
        stop = false
        while (!stop) {
            nextSprite()
            delay(spriteDisplayTime)
        }
    }

    fun stop() {
        stop = true
    }
}
