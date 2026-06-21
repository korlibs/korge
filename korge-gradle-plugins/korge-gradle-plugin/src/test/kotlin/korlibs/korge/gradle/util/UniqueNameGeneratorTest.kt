package korlibs.korge.gradle.util

import org.junit.*

class UniqueNameGeneratorTest {
    @Test
    fun test() {
        val names = UniqueNameGenerator()
        Assert.assertEquals("hello", names.get("hello"))
        Assert.assertEquals("hello0", names.get("hello"))
        Assert.assertEquals("hello1", names.get("hello"))
        Assert.assertEquals("hello00", names.get("hello0"))
        Assert.assertEquals("hello01", names.get("hello0"))
        Assert.assertEquals("hello10", names.get("hello1"))
        Assert.assertEquals("hello11", names.get("hello1"))
        Assert.assertEquals("hello110", names.get("hello11"))
        Assert.assertEquals("hello2", names.get("hello"))
    }
}
