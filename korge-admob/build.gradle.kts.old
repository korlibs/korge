import com.soywiz.korlibs.*

apply<KorlibsPlugin>()

val playServicesVersion = "16.0.0"
//def playServicesVersion = "15.0.0"

korlibs {
	dependencyProject(":korge")
	dependencies {
        if (korlibs.hasAndroid) {
            add("androidMainApi", "com.google.android.gms:play-services-ads:$playServicesVersion")
            // https://github.com/Kotlin/kotlinx.coroutines/blob/344e93211779c3a25789babcf92f51aee8f286cb/ui/kotlinx-coroutines-android/build.gradle#L22
            //androidMainApi 'com.android.support:support-annotations:26.1.0'
            //androidMainApi 'com.android.support:support-annotations:28.0.0'

            configure<com.android.build.gradle.BaseExtension> {
                lintOptions {
                    // @TODO: ../../build.gradle: All com.android.support libraries must use the exact same version specification (mixing versions can lead to runtime crashes). Found versions 28.0.0, 26.1.0. Examples include com.android.support:animated-vector-drawable:28.0.0 and com.android.support:customtabs:26.1.0
                    disable("GradleCompatible")
                }
            }
        }
	}
}
