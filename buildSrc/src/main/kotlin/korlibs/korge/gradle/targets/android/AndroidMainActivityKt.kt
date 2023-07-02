package korlibs.korge.gradle.targets.android

import korlibs.korge.gradle.util.*

object AndroidMainActivityKt {
    fun genAndroidMainActivityKt(config: AndroidGenerated): String = Indenter {
        line("package ${config.androidPackageName}")

        //line("import korlibs.io.android.withAndroidContext")
        line("import korlibs.render.*")
        line("import ${config.realEntryPoint}")

        line("class MainActivity : KorgwActivity(config = GameWindowCreationConfig(msaa = ${config.androidMsaa ?: 1}, windowConfig = WindowConfig(fullscreen = ${config.fullscreen})))") {
            line("override suspend fun activityMain()") {
                //line("withAndroidContext(this)") { // @TODO: Probably we should move this to KorgwActivity itself
                for (text in config.androidInit) {
                    line(text)
                }
                line("${config.realEntryPoint}()")
                //}
            }
        }
    }.toString()
}
