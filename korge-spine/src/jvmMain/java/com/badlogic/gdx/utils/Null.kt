package com.badlogic.gdx.utils

import java.lang.annotation.Documented

/** An element with this annotation claims that the element may have a `null` value. Apart from documentation purposes this
 * annotation is intended to be used by static analysis tools to validate against probable runtime errors or contract violations.
 * @author maltaisn
 */
@Documented
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER, AnnotationTarget.FIELD, AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.LOCAL_VARIABLE)
annotation class Null
