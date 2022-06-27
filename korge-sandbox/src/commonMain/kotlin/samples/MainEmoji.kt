package samples

import com.soywiz.korge.scene.Scene
import com.soywiz.korge.view.Container
import com.soywiz.korge.view.graphics
import com.soywiz.korge.view.image
import com.soywiz.korge.view.text
import com.soywiz.korge.view.xy
import com.soywiz.korim.color.Colors
import com.soywiz.korim.font.DefaultTtfFont
import com.soywiz.korim.font.SystemFont
import com.soywiz.korim.font.readTtfFont
import com.soywiz.korim.font.withFallback
import com.soywiz.korim.format.readBitmap
import com.soywiz.korim.text.text
import com.soywiz.korio.file.std.localVfs
import com.soywiz.korio.file.std.resourcesVfs

class MainEmoji : Scene() {
    override suspend fun Container.sceneMain() {
        println("coroutineContext: $coroutineContext")
        image(resourcesVfs["korge.png"].readBitmap())
        //val fontEmojiOther = localVfs("C:/temp/emoji.ttf").takeIfExists()?.readTtfFont()
        val fontEmojiOther = SystemFont("emoji")
        val fontEmojiApple = localVfs("C:/temp/AppleColorEmoji.ttf").takeIfExists()?.readTtfFont()
        val fontEmojiSystem = SystemFont.getEmojiFont()
        val font0 = DefaultTtfFont.withFallback(SystemFont.getDefaultFont())
        println("fontEmojiOther=$fontEmojiOther")
        println("fontEmojiApple=$fontEmojiApple")
        println("fontEmojiSystem=$fontEmojiSystem")
        println("font0=$font0")
        println("fontList=${kotlin.runCatching { localVfs("/system/fonts").listNames() }}")
        println("/System/Library/Fonts/Cache=${kotlin.runCatching { localVfs("/System/Library/Fonts/Cache").listNames() }}")
        println("/System/Library/Fonts=${kotlin.runCatching { localVfs("/System/Library/Fonts").listNames() }}")
        println("/System/Library/Fonts/Core=${kotlin.runCatching { localVfs("/System/Library/Fonts/Core").listNames() }}")
        println("listFontNamesWithFiles=${kotlin.runCatching { SystemFont.listFontNamesWithFiles() }}")
        //val font0 = localVfs("C:/temp/FrankRuhlHofshi-Regular-ttf.ttf").readTtfFont()
        //val font0 = SystemFont.getDefaultFont().ttf
        //val font0 = SystemFont("Arial Unicode").ttf
        //val font0 = localVfs("c:/temp/arialuni.ttf").readTtfFont()
        val font1 = font0.withFallback(fontEmojiApple, fontEmojiSystem)
        val font2 = font0.withFallback(fontEmojiSystem)
        val font3 = font0.withFallback(fontEmojiOther)

        text("HELLO　зклмн 😃😀😁😂🥰🤩🦍", font = font1, textSize = 90.0).xy(100, 100)
        text("HELLO　쌍디귿 😃😀😁😂🥰🤩🦍", font = font2, textSize = 90.0).xy(100, 228)
        text("HELLO　あかめ私 😃\uD83D\uDDB9", font = font3, textSize = 90.0).xy(100, 368)

        graphics({
            fill(Colors.RED) {
                text("h̷̷̶̨͋ͩ̏ͣ̒̉ͤ͛̓̄͢͡͠͡͏͈̬̜̲̙̤̙̤̯e̷͛̒ͪ́ͤ̒̃͏̶͠͏̞̰̻͙̟̜͕̞̮͟͟͡ļ̸̥͎̼̪̘̜̞͓̩ͧ̈̌ͣͨ́̕͡͞ͅl̡̡̛̦̫͖̞̯̻̓̆͆̑̅ͣ̑̕̕͡ͅǫ̴̸̊͐̈́̈̀͛̾́͏̸̡̡̦̤̦͚̬̯͔͉͇́͞HELLO　зклмн 쌍디귿 あかめ私 😃\uD83D\uDDB9", font = font3, textSize = 90.0, x = 100.0, y = 368.0)
            }
        })
    }
}
