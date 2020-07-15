#-keep,allowobfuscation @interface com.soywiz.korio.annotations.Keep
-keep @com.soywiz.korio.annotations.Keep public class *
-keepclassmembers class * {
    @com.soywiz.korio.annotations.Keep *;
}