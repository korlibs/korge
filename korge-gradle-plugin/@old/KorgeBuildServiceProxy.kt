package com.soywiz.korge.gradle

/*

object KorgeBuildService {
	fun processResourcesFolder(resourceFolder: File, output: File) {
		TODO("KorgeBuildService")
	}
}

class RemoteProxy(val urls: Array<URL>) {
    private val rclassLoader = URLClassLoader(urls, ClassLoader.getSystemClassLoader())

    inline fun <reified T, reified T2 : T> proxy(): T = proxy(T::class.java, T2::class.java)

    fun <T, T2 : T> proxy(ifc: Class<T>, implementation: Class<T2>): T {
        //val remoteIfc = rclassLoader.loadClass(ifc.name)
        val remoteClazz = rclassLoader.loadClass(implementation.name)
        val remoteInstance = remoteClazz.constructors.first().newInstance()
        return Proxy.newProxyInstance(ifc.classLoader, arrayOf(ifc)) { proxy, method, args ->
            val rargs = args ?: arrayOf()
            //println("proxy: $proxy")
            //println("method: $method")
            //println("args: $rargs")
            val remoteMethod = remoteClazz.getDeclaredMethod(method.name, *method.parameterTypes) ?: error("Couldn't locate remote method $method in $remoteClazz")
            //println("remoteClazz: $remoteClazz")
            //println("remoteInstance: $remoteInstance")
            //println("remoteMethod: $remoteMethod")
            //println("args: $rargs")
            remoteMethod.invoke(remoteInstance, *rargs)
        } as T
    }
}

private val rp by lazy { RemoteProxy((com.soywiz.korge.build.swf.SwfResourceProcessor::class.java.classLoader as URLClassLoader).urLs) }
private val kbsp by lazy { rp.proxy<IKorgeBuildService, KorgeBuildService>() }

object KorgeBuildServiceProxy : IKorgeBuildService by kbsp
*/
