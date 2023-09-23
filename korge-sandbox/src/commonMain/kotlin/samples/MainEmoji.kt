package samples

import korlibs.image.color.*
import korlibs.image.font.*
import korlibs.image.format.*
import korlibs.image.text.*
import korlibs.io.file.std.*
import korlibs.korge.scene.*
import korlibs.korge.view.*
import korlibs.math.geom.*

class MainEmoji : Scene() {
    override suspend fun SContainer.sceneMain() {
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

        cpuGraphics({
            fill(Colors.RED) {
                text("h̷̷̶̨͋ͩ̏ͣ̒̉ͤ͛̓̄͢͡͠͡͏͈̬̜̲̙̤̙̤̯e̷͛̒ͪ́ͤ̒̃͏̶͠͏̞̰̻͙̟̜͕̞̮͟͟͡ļ̸̥͎̼̪̘̜̞͓̩ͧ̈̌ͣͨ́̕͡͞ͅl̡̡̛̦̫͖̞̯̻̓̆͆̑̅ͣ̑̕̕͡ͅǫ̴̸̊͐̈́̈̀͛̾́͏̸̡̡̦̤̦͚̬̯͔͉͇́͞HELLO　зклмн 쌍디귿 あかめ私 😃\uD83D\uDDB9", font = font3, textSize = 90.0, pos = Point(100, 368))
            }
        })
    }
}
