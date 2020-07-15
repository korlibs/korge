import com.soywiz.korlibs.*

apply<KorlibsPlugin>()

korlibs {
    exposeVersion()
}

val kdsVersion: String by project

dependencies {
    add("commonMainApi", "com.soywiz.korlibs.kds:kds:$kdsVersion")
}
