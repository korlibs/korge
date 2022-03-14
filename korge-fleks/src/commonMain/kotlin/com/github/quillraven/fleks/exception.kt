package com.github.quillraven.fleks

import kotlin.reflect.KClass

abstract class FleksException(message: String) : RuntimeException(message)

class FleksSystemAlreadyAddedException(system: KClass<*>) :
    FleksException("System '${system.simpleName}' is already part of the '${WorldConfiguration::class.simpleName}'.")

class FleksComponentAlreadyAddedException(comp: String) :
    FleksException("Component '$comp' is already part of the '${WorldConfiguration::class.simpleName}'.")

class FleksSystemCreationException(system: IteratingSystem) :
    FleksException("Cannot create system '$system'. IteratingSystem must define at least one of AllOf, NoneOf or AnyOf properties.")

class FleksNoSuchSystemException(system: KClass<*>) :
    FleksException("There is no system of type '${system.simpleName}' in the world.")

class FleksNoSuchComponentException(component: String) :
    FleksException("There is no component of type '$component' in the ComponentMapper. Did you add the component to the '${WorldConfiguration::class.simpleName}'?")

class FleksInjectableAlreadyAddedException(type: String) :
    FleksException("Injectable with type name '$type' is already part of the '${WorldConfiguration::class.simpleName}'. Please add a unique 'type' string as parameter " +
        "to inject() function in world configuration and to Inject.dependency() in your systems or component listeners.")

class FleksInjectableTypeHasNoName(type: KClass<*>) :
    FleksException("Injectable '$type' does not have simpleName in its class type.")

class FleksSystemDependencyInjectException(injectType: String) :
    FleksException("Injection object of type '$injectType' cannot be found. Did you add all necessary injectables?")

class FleksSystemComponentInjectException(injectType: String) :
    FleksException("Component mapper for type '$injectType' cannot be found. Did you add that component to the world configuration?")

class FleksNoSuchEntityComponentException(entity: Entity, component: String) :
    FleksException("Entity '$entity' has no component of type '$component'.")

class FleksUnusedInjectablesException(unused: List<KClass<*>>) :
    FleksException("There are unused injectables of following types: ${unused.map { it.simpleName }}")
