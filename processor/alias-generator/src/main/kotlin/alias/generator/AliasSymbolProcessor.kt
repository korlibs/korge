package alias.generator

import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.*
import java.io.OutputStream

internal class AliasSymbolProcessor(
    options: Map<String, String>,
    private val logger: KSPLogger,
    private val codeGenerator: CodeGenerator,
) : SymbolProcessor {

    private val sourcePackage = options["sourcePackage"]
        ?: "sourcePackage must be provided to alias generator KSP plugin"
    private val targetPackage = options["targetPackage"]
        ?: "targetPackage must be provided to alias generator KSP plugin"

    override fun process(resolver: Resolver): List<KSAnnotated> {
        logger.info("start to process symbols on $sourcePackage")

        resolver.getAllFiles()
            .filterFromSourcePackage()
            .createTargetFile(resolver)
            .filterNotNull()
            .injectDeclarations()
            .forEach(OutputStream::close)

        return listOf()
    }

    private fun Sequence<Pair<Sequence<KSDeclaration>, OutputStream>>.injectDeclarations()
        = map { (declarations, outputStream) ->

        declarations.forEach { declaration ->
            logger.info("visit symbol ${declaration.qualifiedName?.getQualifier()} of type ${declaration::class.simpleName}")

            outputStream += "@Deprecated(\"use new package $sourcePackage instead\")\n"
            outputStream += when (declaration) {
                is KSPropertyDeclaration -> declaration.toPropertyAlias()
                else -> declaration.toTypeAlias()
            }

            outputStream += "\n"
        }

        outputStream

    }

    private fun KSDeclaration.toTypeAlias() = "typealias $name = $fullName"

    private fun KSPropertyDeclaration.toPropertyAlias(): String {
        val hasGetterOrSetter = setter != null || getter != null
        val leftDeclaration = "$declaratorKeyword $receiver$name"
        val rightDeclaration = fullName
        return when  {
            !hasGetterOrSetter -> "$leftDeclaration = $fullName"
            else -> leftDeclaration + (getter?.generate(rightDeclaration) ?: "") + (setter?.generate(rightDeclaration) ?: "")
        }
    }

    private fun Sequence<KSFile>.filterFromSourcePackage() =
        filter { it.packageName.asString().startsWith(sourcePackage) }

    private fun Sequence<KSFile>.createTargetFile(resolver: Resolver): Sequence<Pair<Sequence<KSDeclaration>, OutputStream>?> = map {
        try {
            val newPackage = targetPackage + it.packageName.asString().replace(sourcePackage, "")
            it.declarations to codeGenerator.createNewFile(
                dependencies = Dependencies(false, *resolver.getAllFiles().toList().toTypedArray()),
                packageName = newPackage,
                fileName = it.fileName
            ).also { stream ->
                stream += "package $newPackage\n"
            }
        } catch (error: FileAlreadyExistsException) {
            logger.warn("process multiple time file ${it.fileName}")
            null
        }
    }

}
private fun KSPropertyGetter.generate(value: String): String {
    return "\n get() = $value"
}

private fun KSPropertySetter.generate(value: String): String {
    return "\n set(value) { $value = value }"
}

private operator fun OutputStream.plusAssign(str: String) {
    this.write(str.toByteArray())
}

private val KSPropertyDeclaration.declaratorKeyword: String
    get() = if (isMutable) "var" else "val"
private val KSDeclaration.name: String
    get() = simpleName.asString()
private val KSDeclaration.fullName: String
    get() = "${packageName.asString()}.$name"

private val KSPropertyDeclaration.receiver: String
    get() = extensionReceiver?.element?.toString()?.plus(".") ?: ""
