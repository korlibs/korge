package me.emig.engineEmi.graphics.animationen

import com.soywiz.klock.TimeSpan
import com.soywiz.klock.measureTime
import com.soywiz.klock.milliseconds
import com.soywiz.korge.view.Image
import com.soywiz.korim.bitmap.Bitmap
import com.soywiz.korim.bitmap.BmpSlice
import com.soywiz.korim.bitmap.sliceWithSize
import com.soywiz.korio.async.delay

class SpriteAnimation(
    val id : String = "",
    private val spriteMap: Bitmap,
    private val spriteWidth: Int = 16,
    private val spriteHeight: Int = 16,
    private val marginTop: Int = 0,
    private val marginLeft: Int = 0,
    private val columns: Int = 1,
    private val rows: Int = 1,
    private val offsetBetweenColumns: Int = 0,
    private val offsetBetweenRows: Int = 0) {

    private var spriteStack: MutableList<BmpSlice> = mutableListOf()
    private var cycles = 0
    private var currentSpriteIndex = 0
    private var stop = false
    var frameTime = 25.milliseconds
    val spriteStackSize : Int
        get() = spriteStack.size
    val firstSprite : BmpSlice
        get() = spriteStack[0]

    init{
        for (col in 0 until columns){
            for (row in 0 until rows){
                spriteStack.add(
                    spriteMap.sliceWithSize(
                        marginLeft + (spriteWidth + offsetBetweenColumns) * col,
                        marginTop + (spriteHeight + offsetBetweenRows) * row,
                        spriteWidth,
                        spriteHeight
                    )
                )
            }
        }
    }

    fun nextSprite() = spriteStack[(++currentSpriteIndex % spriteStack.size)]

    fun previousSprite() =spriteStack[(--currentSpriteIndex % spriteStack.size)]

}
