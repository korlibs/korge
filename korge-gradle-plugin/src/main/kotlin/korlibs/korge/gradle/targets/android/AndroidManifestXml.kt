package korlibs.korge.gradle.targets.android

import korlibs.korge.gradle.Orientation
import korlibs.korge.gradle.util.Indenter

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
            line("xmlns:android=\"http://schemas.android.com/apk/res/android\"")
        }
        line(">")
        indent {
            line("<uses-feature android:name=\"android.hardware.touchscreen\" android:required=\"false\" />")
            line("<uses-feature android:name=\"android.software.leanback\" android:required=\"false\" />")

            line("<application")
            indent {
                line("")
                line("android:allowBackup=\"true\"")

                for ((key, value) in config.androidCustomApplicationAttributes) {
                    line("$key=\"${value.htmlspecialchars()}\"")
                }

                if (!config.androidLibrary) {
                    line("android:label=\"${config.androidAppName}\"")
                    line("android:icon=\"@mipmap/icon\"")
                    line("android:roundIcon=\"@android:drawable/sym_def_app_icon\"")
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

private fun String.htmlspecialchars(): String = buildString(this@htmlspecialchars.length + 16) {
    for (it in this@htmlspecialchars) {
        when (it) {
            '"' -> append("&quot;")
            '\'' -> append("&apos;")
            '<' -> append("&lt;")
            '>' -> append("&gt;")
            '&' -> append("&amp;")
            else -> append(it)
        }
    }
}
