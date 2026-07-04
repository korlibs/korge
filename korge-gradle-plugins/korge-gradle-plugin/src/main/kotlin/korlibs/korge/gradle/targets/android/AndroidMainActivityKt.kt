package korlibs.korge.gradle.targets.android

import korlibs.korge.gradle.util.Indenter

object AndroidMainActivityKt {
    fun genAndroidMainActivityKt(config: AndroidGenerated): String = Indenter {
        line("package ${config.androidPackageName}")

        line("import korlibs.render.*")
        line("import ${config.realEntryPoint}")

        line("class MainActivity : KorgwActivity(config = GameWindowCreationConfig(msaa = ${config.androidMsaa ?: 1}, fullscreen = ${config.fullscreen}))") {
            line("override suspend fun activityMain()") {
                for (text in config.androidInit) {
                    line(text)
                }
                line("${config.realEntryPoint}()")
            }
        }
    }.toString()
}
