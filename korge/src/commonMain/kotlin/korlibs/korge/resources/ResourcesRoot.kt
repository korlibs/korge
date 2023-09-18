package korlibs.korge.resources

import korlibs.memory.*
import korlibs.inject.*
import korlibs.io.file.*
import korlibs.io.file.std.*
import korlibs.io.resources.*
import korlibs.platform.*

//@Singleton
class ResourcesRoot : InjectorDependency {
    private lateinit var root: VfsFile
    private lateinit var mountable: Mountable

    fun mount(path: String, file: VfsFile) {
        mountable.mount(path, file)
    }

    operator fun get(
        @ResourcePath
        //@Language("resource") // @TODO: This doesn't work on Kotlin Common. reported already on Kotlin issue tracker
        //language=resource
        path: String
    ) = root[path]
    operator fun get(path: VPath) = root[path.path]

    override fun init(injector: Injector) {
        root = MountableVfs() {
            mountable = this
        }
        mount("/", resourcesVfs)
    }

    suspend fun redirected(redirector: VfsFile.(String) -> String) {
        this.root = this.root.redirected { this.redirector(it) }
    }

    suspend fun mapExtensions(vararg maps: Pair<String, String>) {
        val mapsLC = maps.map { it.first.lowercase() to it.second }.toMap()
        redirected {
            val pi = PathInfo(it)
            val map = mapsLC[pi.extensionLC]
            if (map != null) {
                pi.fullPathWithExtension(map)
            } else {
                pi.fullPath
            }
        }
    }

    suspend fun mapExtensionsJustInJS(vararg maps: Pair<String, String>) {
        if (Platform.isJs) {
            mapExtensions(*maps)
        }
    }

    override fun toString(): String = "ResourcesRoot[$root]"
}
