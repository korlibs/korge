package korlibs.modules

import java.io.*

object GithubCI {
    fun setOutput(name: String, value: String) {
        val GITHUB_OUTPUT = System.getenv("GITHUB_OUTPUT")
        if (GITHUB_OUTPUT != null) {
            File(GITHUB_OUTPUT).appendText("$name=$value\n")
        } else {
            println("::set-output name=$name::$value")
        }
    }
}
