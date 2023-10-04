plugins {
    //id "com.dorongold.task-tree" version "2.1.1"
    // ./gradlew :kds:compileKotlinJs taskTree
}

korlibs.root.RootKorlibsPlugin.doInit(rootProject)

// Used to verify we are publishing with iOS references even if we do the publishing from another machine like windows or linux
tasks {
    val checkModulePublication by creating {
        doLast {
            val publishedKorgeModule = File("${System.getProperty("user.home")}/.m2/repository/com/soywiz/korge/korge/999.0.0.999/korge-999.0.0.999.module")
            val publishedKorgeModuleText = publishedKorgeModule.readText()
            for (ref in listOf("jvmApiElements", "jsApiElements", "android", "iosArm64")) {
                check (ref in publishedKorgeModuleText) {
                    System.err.println(publishedKorgeModuleText)
                    "Can't find '$ref' on the published '$publishedKorgeModule'"
                }
            }
        }
    }
}
