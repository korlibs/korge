package korlibs.datastructure

class Ref<T : Any>() {
    constructor(value: T) : this() { this.value = value }
    lateinit var value: T
}
