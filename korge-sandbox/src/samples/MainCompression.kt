package samples

import korlibs.time.measureTime
import korlibs.korge.scene.Scene
import korlibs.korge.view.SContainer
import korlibs.io.file.std.MemoryVfsMix
import korlibs.io.file.std.localVfs
import korlibs.io.file.std.openAsZip
import korlibs.io.stream.DummyAsyncOutputStream
import korlibs.io.stream.openAsync
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MainCompression : Scene() {
    override suspend fun SContainer.sceneMain() {
        //run {
        withContext(Dispatchers.Unconfined) {
            val mem = MemoryVfsMix()
            val zipFile = localVfs("c:/temp", async = true)["1.zip"]
            val zipBytes = zipFile.readAll()
            println("ELAPSED TIME [NATIVE]: " + kotlin.time.measureTime {
                //localVfs("c:/temp")["1.zip"].openAsZip()["2012-07-15-wheezy-raspbian.img"].copyTo(mem["test.img"])
                //localVfs("c:/temp")["iTunes64Setup.zip"].openAsZip().listSimple().first().copyTo(mem["test.out"])
                //localVfs("c:/temp")["iTunes64Setup.zip"].readAll().openAsync().openAsZip().listSimple().first().copyTo(mem["test.out"])
                //localVfs("c:/temp")["iTunes64Setup.zip"].openAsZip().listSimple().first().copyTo(mem["test.out"])
                //localVfs("c:/temp")["1.zip"].readAll().openAsync().openAsZip()["2012-07-15-wheezy-raspbian.img"].copyTo(localVfs("c:/temp/temp.img"))
                //localVfs("c:/temp")["1.zip"].openAsZip(useNativeDecompression = true)["2012-07-15-wheezy-raspbian.img"].copyTo(localVfs("c:/temp/temp.img"))
                zipBytes.openAsync().openAsZip(useNativeDecompression = true)["2012-07-15-wheezy-raspbian.img"].copyTo(
                    DummyAsyncOutputStream
                )
            })
            println("ELAPSED TIME [PORTABLE]: " + kotlin.time.measureTime {
                //localVfs("c:/temp")["1.zip"].openAsZip()["2012-07-15-wheezy-raspbian.img"].copyTo(mem["test.img"])
                //localVfs("c:/temp")["iTunes64Setup.zip"].openAsZip().listSimple().first().copyTo(mem["test.out"])
                //localVfs("c:/temp")["iTunes64Setup.zip"].readAll().openAsync().openAsZip().listSimple().first().copyTo(mem["test.out"])
                //localVfs("c:/temp")["iTunes64Setup.zip"].openAsZip().listSimple().first().copyTo(mem["test.out"])
                //localVfs("c:/temp")["1.zip"].readAll().openAsync().openAsZip()["2012-07-15-wheezy-raspbian.img"].copyTo(localVfs("c:/temp/temp.img"))
                //localVfs("c:/temp")["1.zip"].openAsZip(useNativeDecompression = false)["2012-07-15-wheezy-raspbian.img"].copyTo(localVfs("c:/temp/temp2.img"))
                //localVfs("c:/temp", async = true)["1.zip"].openAsZip(useNativeDecompression = false)["2012-07-15-wheezy-raspbian.img"].copyTo(DummyAsyncOutputStream)
                zipBytes.openAsync().openAsZip(useNativeDecompression = false)["2012-07-15-wheezy-raspbian.img"].copyTo(
                    DummyAsyncOutputStream
                )
            })
        }
    }
}
