package org.korge.application

import korlibs.render.GameWindowCreationConfig
import korlibs.render.KorgwActivity

class MainActivity : KorgwActivity(config = GameWindowCreationConfig(msaa = 1)) {
    override suspend fun activityMain() {
        // Calls the shared `suspend fun main()` defined in shared/commonMain/Main.kt
        main()
    }
}
