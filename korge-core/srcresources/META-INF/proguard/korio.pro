#-keep,allowobfuscation @interface korlibs.io.annotations.Keep
-keep @korlibs.io.annotations.Keep public class *
-keepclassmembers class * {
    @korlibs.io.annotations.Keep *;
}