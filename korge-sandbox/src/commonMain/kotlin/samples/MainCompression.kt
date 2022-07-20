package samples

import com.soywiz.klock.measureTime
import com.soywiz.korge.scene.Scene
import com.soywiz.korge.view.SContainer
import com.soywiz.korio.file.std.MemoryVfsMix
import com.soywiz.korio.file.std.localVfs
import com.soywiz.korio.file.std.openAsZip
import com.soywiz.korio.stream.DummyAsyncOutputStream
import com.soywiz.korio.stream.openAsync
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MainCompression : Scene() {
    override suspend fun SContainer.sceneMain() {
        //run {
        withContext(Dispatchers.Unconfined) {
            val mem = MemoryVfsMix()
            val zipFile = localVfs("c:/temp", async = true)["1.zip"]
            val zipBytes = zipFile.readAll()
            println("ELAPSED TIME [NATIVE]: " + measureTime {
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
            println("ELAPSED TIME [PORTABLE]: " + measureTime {
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
