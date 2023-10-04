package korlibs.korge.view.property

import korlibs.io.async.*
import korlibs.math.interpolation.*

fun <T, R> ObservableProperty<T>.transform(convert: (T) -> R, rconvert: (R) -> T): ObservableProperty<R> {
    return ObservableProperty(name, internalSet = { this.value = rconvert(it) }, internalGet = { convert(this.value) })
}

@JvmName("ObservablePropertyRatio_toDouble")
fun ObservableProperty<Ratio>.toDouble(): ObservableProperty<Double> = transform({ it.toDouble() }, { it.toRatio() })
@JvmName("ObservablePropertyFloat_toDouble")
fun ObservableProperty<Float>.toDouble(): ObservableProperty<Double> = transform({ it.toDouble() }, { it.toFloat() })

class ObservableProperty<T>(
    val name: String,
    val internalSet: (T) -> Unit,
    val internalGet: () -> T,
) {
    val onChange = Signal<T>()

    fun forceUpdate(value: T) {
        internalSet(value)
        onChange(value)
    }

    fun forceRefresh() {
        //println("forceRefresh: $value")
        //forceUpdate(value)
        onChange(value)
    }

    var value: T
        get() = internalGet()
        set(value) {
            if (this.value != value) {
                forceUpdate(value)
            }
        }

    override fun toString(): String = "ObservableProperty($name, $value)"
}

interface ObservablePropertyHolder<T> {
    val prop: ObservableProperty<T>
}
