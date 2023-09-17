package korlibs.korge.gradle.targets.android

import korlibs.korge.gradle.*
import korlibs.korge.gradle.util.*

object AndroidManifestXml {
    fun genStylesXml(config: AndroidGenerated): String = Indenter {
        line("<?xml version=\"1.0\" encoding=\"utf-8\" ?>")
        line("<resources>")
        indent {
            line("<style name=\"AppThemeOverride\" parent=\"@android:style/Theme\">")
                line("<item name=\"android:windowNoTitle\">true</item>")
                line("<item name=\"android:windowFullscreen\">true</item>")
                line("<item name=\"android:windowContentOverlay\">@null</item>")
                line("<item name=\"android:windowLayoutInDisplayCutoutMode\">${config.displayCutout.lc}</item>")
                line("<item name=\"android:windowTranslucentStatus\">true</item>")
                line("<item name=\"android:windowTranslucentNavigation\">true</item>")
                line("<item name=\"android:windowDrawsSystemBarBackgrounds\">false</item>")
            line("</style>")
        }
        line("</resources>")
    }

    fun genAndroidManifestXml(config: AndroidGenerated): String = Indenter {
        line("<?xml version=\"1.0\" encoding=\"utf-8\"?>")
        line("<manifest")
        indent {
            //line("xmlns:tools=\"http://schemas.android.com/tools\"")
            line("xmlns:android=\"http://schemas.android.com/apk/res/android\"")
            //line("package=\"$androidPackageName\"")
        }
        line(">")
        indent {
            line("<uses-feature android:name=\"android.hardware.touchscreen\" android:required=\"false\" />")
            line("<uses-feature android:name=\"android.software.leanback\" android:required=\"false\" />")

            line("<application")
            indent {
                line("")
                //line("tools:replace=\"android:appComponentFactory\"")
                line("android:allowBackup=\"true\"")

                if (!config.androidLibrary) {
                    line("android:label=\"${config.androidAppName}\"")
                    line("android:icon=\"@mipmap/icon\"")
                    // // line("android:icon=\"@android:drawable/sym_def_app_icon\"")
                    line("android:roundIcon=\"@android:drawable/sym_def_app_icon\"")
                    //line("android:theme=\"@android:style/Theme.Holo.NoActionBar\"")
                    //line("android:theme=\"@android:style/Theme.NoTitleBar.Fullscreen\"")
                    line("android:theme=\"@style/AppThemeOverride\"")
                }
                line("android:supportsRtl=\"true\"")
            }
            line(">")
            indent {
                line("<profileable android:shell=\"true\" />")
                for (text in config.androidManifest) {
                    line(text)
                }
                for (text in config.androidManifestApplicationChunks) {
                    line(text)
                }

                line("<activity android:name=\".MainActivity\"")
                indent {
                    val orientationString = when (config.orientation) {
                        Orientation.LANDSCAPE -> "landscape"
                        Orientation.PORTRAIT -> "portrait"
                        Orientation.DEFAULT -> "sensor"
                    }
                    line("android:banner=\"@drawable/app_banner\"")
                    line("android:icon=\"@drawable/app_icon\"")
                    line("android:label=\"${config.androidAppName}\"")
                    line("android:logo=\"@drawable/app_icon\"")
                    line("android:configChanges=\"orientation|screenSize|screenLayout|keyboardHidden\"")
                    line("android:screenOrientation=\"$orientationString\"")
                    line("android:exported=\"true\"")
                }
                line(">")

                if (!config.androidLibrary) {
                    indent {
                        line("<intent-filter>")
                        indent {
                            line("<action android:name=\"android.intent.action.MAIN\"/>")
                            line("<category android:name=\"android.intent.category.LAUNCHER\"/>")
                        }
                        line("</intent-filter>")
                    }
                }
                line("</activity>")
            }
            line("</application>")
            for (text in config.androidManifestChunks) {
                line(text)
            }
        }
        line("</manifest>")
    }.toString()
}
