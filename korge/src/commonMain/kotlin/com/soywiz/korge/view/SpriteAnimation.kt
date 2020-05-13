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
    spriteMap: Bitmap,
    spriteWidth: Int = 16,
    spriteHeight: Int = 16,
    marginTop: Int = 0,
    marginLeft: Int = 0,
    columns: Int = 1,
    rows: Int = 1,
    offsetBetweenColumns: Int = 0,
    offsetBetweenRows: Int = 0) {

    private var spriteStack: MutableList<BmpSlice> = mutableListOf()
    private var cycles = 0
    private var currentSpriteIndex = 0
    private var stop = false
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

    fun getSprite(index : Int) = spriteStack[(index % spriteStack.size)]
}
