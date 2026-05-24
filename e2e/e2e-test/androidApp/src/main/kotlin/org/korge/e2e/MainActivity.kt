package org.korge.e2e

import korlibs.render.GameWindowCreationConfig
import korlibs.render.KorgwActivity

class MainActivity : KorgwActivity(config = GameWindowCreationConfig(msaa = 1, fullscreen = true)) {
    override suspend fun activityMain() {
        main()
    }
}
