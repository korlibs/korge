import com.soywiz.korlibs.modules.*

description = "Memory utilities for Kotlin"

/*
project.ext.props = [
    "project.scm.url" : "https://github.com/korlibs/klogger",
"project.description" : "Logger system for Kotlin",
"project.license.name" : "MIT License",
"project.license.url" : "https://raw.githubusercontent.com/korlibs/klogger/master/LICENSE",
"project.author.id" : "soywiz",
"project.author.name" : "Carlos Ballesteros Velasco",
"project.author.email" : "soywiz@gmail.com",
]
 */

dependencies {
    //add("androidMainApi", "com.implimentz:unsafe:0.0.6")
}

kotlin {
    for (target in allNativeTargets(project) ) {
        target.compilations["main"].cinterops {
            maybeCreate("fastmem")
        }
    }
}
