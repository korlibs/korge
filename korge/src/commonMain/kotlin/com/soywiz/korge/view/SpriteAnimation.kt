package com.soywiz.korge.view

import com.soywiz.kds.*
import com.soywiz.klock.*
import com.soywiz.kmem.umod
import com.soywiz.korim.atlas.*
import com.soywiz.korim.bitmap.*

class SpriteAnimation constructor(
    val sprites: List<BmpSlice>,
    val defaultTimePerFrame: TimeSpan = TimeSpan.NIL
) : Collection<BmpSlice> by sprites {
    companion object {
        operator fun invoke(
            spriteMap: Bitmap,
            spriteWidth: Int = 16,
            spriteHeight: Int = 16,
            marginTop: Int = 0,
            marginLeft: Int = 0,
            columns: Int = 1,
            rows: Int = 1,
            offsetBetweenColumns: Int = 0,
            offsetBetweenRows: Int = 0,
            numberOfFrames: Int = rows * columns,
            byRows: Boolean = true,
        ): SpriteAnimation = invoke(spriteMap.slice(), spriteWidth, spriteHeight, marginTop, marginLeft, columns, rows, offsetBetweenColumns, offsetBetweenRows, numberOfFrames, byRows)

        operator fun invoke(
            spriteMap: BmpSlice,
            spriteWidth: Int = 16,
            spriteHeight: Int = 16,
            marginTop: Int = 0,
            marginLeft: Int = 0,
            columns: Int = 1,
            rows: Int = 1,
            offsetBetweenColumns: Int = 0,
            offsetBetweenRows: Int = 0,
            numberOfFrames: Int = rows * columns,
            byRows: Boolean = true,
        ): SpriteAnimation {
            return SpriteAnimation(
                FastArrayList<BmpSlice>().apply {
                    for (n in 0 until numberOfFrames) {
                        val col = if (byRows) n % columns else n / rows
                        val row = if (byRows) n / columns else n % rows
                        add(
                            spriteMap.sliceWithSize(
                                marginLeft + (spriteWidth + offsetBetweenColumns) * col,
                                marginTop + (spriteHeight + offsetBetweenRows) * row,
                                spriteWidth,
                                spriteHeight,
                                name = "slice$size"
                            )
                        )
                    }
                }
            )
        }
    }

    val spriteStackSize: Int get() = sprites.size
    val firstSprite: BmpSlice get() = sprites[0]
    fun getSprite(index: Int): BmpSlice = sprites[index umod sprites.size]
    operator fun get(index: Int) = getSprite(index)
}

fun Atlas.getSpriteAnimation(prefix: String = "", defaultTimePerFrame: TimeSpan = TimeSpan.NIL): SpriteAnimation =
    SpriteAnimation(this.entries.filter { it.filename.startsWith(prefix) }.map { it.slice }.toFastList(), defaultTimePerFrame)

fun Atlas.getSpriteAnimation(regex: Regex, defaultTimePerFrame: TimeSpan = TimeSpan.NIL): SpriteAnimation =
    SpriteAnimation(this.entries.filter { regex.matches(it.filename) }.map { it.slice }.toFastList(), defaultTimePerFrame)
