import korlibs.applyProjectProperties

description = "Logger system for Kotlin"

project.extensions.extraProperties.properties.apply {
    applyProjectProperties(
        "https://github.com/korlibs/klogger",
        "MIT License",
        "https://raw.githubusercontent.com/korlibs/klogger/master/LICENSE"
    )
}


//project.ext.props = [
//    "project.scm.url" : "https://github.com/korlibs/klogger",
//"project.license.name" : "MIT License",
//"project.license.url" : "https://raw.githubusercontent.com/korlibs/klogger/master/LICENSE",
//"project.author.id" : "soywiz",
//"project.author.name" : "Carlos Ballesteros Velasco",
//"project.author.email" : "soywiz@gmail.com",
//]
