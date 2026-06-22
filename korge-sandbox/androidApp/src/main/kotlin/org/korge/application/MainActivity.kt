package org.korge.application

class MainActivity : KorgwActivity(config = GameWindowCreationConfig(msaa = 1)) {
    override suspend fun activityMain() {
        // Calls the shared `suspend fun main()` defined in shared/commonMain/Main.kt
        main()
    }
}
