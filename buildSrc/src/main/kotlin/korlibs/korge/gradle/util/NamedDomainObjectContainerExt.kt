package korlibs.korge.gradle.util

import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectContainer

fun <T> NamedDomainObjectContainer<T>.createOnce(name: String, configureAction: T.() -> Unit): T {
    val item = findByName(name)
    if (item != null) return item
    return create(name, configureAction)
}