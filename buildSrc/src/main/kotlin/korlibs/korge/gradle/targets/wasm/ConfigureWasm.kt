package korlibs.korge.gradle.targets.wasm

import korlibs.*
import org.gradle.api.*

fun Project.configureWasm(executable: Boolean, binaryen: Boolean = false) {
    kotlin {
        wasm {
            if (binaryen) applyBinaryen()

            if (executable) {
                binaries.executable()
            }
            //applyBinaryen()
            browser {
                //commonWebpackConfig { experiments = mutableSetOf("topLevelAwait") }
                if (executable) {
                    this.distribution {
                    }
                }
                //testTask {
                //    it.useKarma {
                //        //useChromeHeadless()
                //        this.webpackConfig.configDirectory = File(rootProject.rootDir, "karma.config.d")
                //    }
                //}
            }
        }
    }
}
